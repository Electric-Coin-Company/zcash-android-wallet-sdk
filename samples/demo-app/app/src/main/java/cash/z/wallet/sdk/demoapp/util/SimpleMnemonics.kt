package cash.z.wallet.sdk.demoapp.util

import cash.z.android.plugin.MnemonicPlugin
import io.github.novacrypto.bip39.MnemonicGenerator
import io.github.novacrypto.bip39.SeedCalculator
import io.github.novacrypto.bip39.Words
import io.github.novacrypto.bip39.wordlists.English
import java.security.SecureRandom

/**
 * A sample implementation of a plugin for handling Mnemonic phrases. Any library can easily be
 * plugged into the SDK in this manner. In this case, we are wrapping a few example 3rd party
 * libraries with a thin layer that converts from their API to ours via the MnemonicPlugin
 * interface. We do not endorse these libraries, rather we just use them as an example of how to
 * take existing infrastructure and plug it into the SDK.
 */
class SimpleMnemonics : MnemonicPlugin {

    override fun nextEntropy(): ByteArray {
        return ByteArray(Words.TWENTY_FOUR.byteLength()).apply {
            SecureRandom().nextBytes(this)
        }
    }

    override fun nextMnemonic(): CharArray {
        return nextMnemonic(nextEntropy())
    }

    override fun nextMnemonic(entropy: ByteArray): CharArray {
        return StringBuilder().let { builder ->
            MnemonicGenerator(English.INSTANCE).createMnemonic(entropy) { c ->
                builder.append(c)
            }
            builder.toString().toCharArray()
        }
    }

    override fun nextMnemonicList(): List<CharArray> {
        return nextMnemonicList(nextEntropy())
    }

    override fun nextMnemonicList(entropy: ByteArray): List<CharArray> {
        return WordListBuilder().let { builder ->
            MnemonicGenerator(English.INSTANCE).createMnemonic(entropy) { c ->
                builder.append(c)
            }
            builder.wordList
        }
    }

    override fun toSeed(mnemonic: CharArray): ByteArray {
        return SeedCalculator().calculateSeed(String(mnemonic), "")
    }

    override fun toWordList(mnemonic: CharArray): List<CharArray> {
        val wordList = mutableListOf<CharArray>()
        var cursor = 0
        repeat(mnemonic.size) { i ->
            val isSpace = mnemonic[i] == ' '
            if (isSpace || i == (mnemonic.size - 1)) {
                val wordSize = i - cursor + if (isSpace) 0 else 1
                wordList.add(CharArray(wordSize).apply {
                    repeat(wordSize) {
                        this[it] = mnemonic[cursor + it]
                    }
                })
                cursor = i + 1
            }
        }
        return wordList
    }

    class WordListBuilder {
        val wordList = mutableListOf<CharArray>()
        fun append(c: CharSequence) {
            if (c[0] != English.INSTANCE.space) addWord(c)
        }

        private fun addWord(c: CharSequence) {
            c.length.let { size ->
                val word = CharArray(size)
                repeat(size) {
                    word[it] = c[it]
                }
                wordList.add(word)
            }
        }
    }
}