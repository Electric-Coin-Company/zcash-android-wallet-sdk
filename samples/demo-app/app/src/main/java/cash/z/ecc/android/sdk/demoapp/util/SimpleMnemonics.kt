package cash.z.ecc.android.sdk.demoapp.util

import cash.z.android.plugin.MnemonicPlugin
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.Mnemonics.MnemonicCode
import cash.z.ecc.android.bip39.Mnemonics.WordCount
import cash.z.ecc.android.bip39.toEntropy
import cash.z.ecc.android.bip39.toSeed
import java.util.*

/**
 * A sample implementation of a plugin for handling Mnemonic phrases. Any library can easily be
 * plugged into the SDK in this manner. In this case, we are wrapping a few example 3rd party
 * libraries with a thin layer that converts from their API to ours via the MnemonicPlugin
 * interface. We do not endorse these libraries, rather we just use them as an example of how to
 * take existing infrastructure and plug it into the SDK.
 */
class SimpleMnemonics : MnemonicPlugin {
    override fun fullWordList(languageCode: String) = Mnemonics.getCachedWords(Locale.ENGLISH.language)
    override fun nextEntropy(): ByteArray = WordCount.COUNT_24.toEntropy()
    override fun nextMnemonic(): CharArray = MnemonicCode(WordCount.COUNT_24).chars
    override fun nextMnemonic(entropy: ByteArray): CharArray = MnemonicCode(entropy).chars
    override fun nextMnemonicList(): List<CharArray> = MnemonicCode(WordCount.COUNT_24).words
    override fun nextMnemonicList(entropy: ByteArray): List<CharArray> = MnemonicCode(entropy).words
    override fun toSeed(mnemonic: CharArray): ByteArray = MnemonicCode(mnemonic).toSeed()
    override fun toWordList(mnemonic: CharArray): List<CharArray> = MnemonicCode(mnemonic).words
}
