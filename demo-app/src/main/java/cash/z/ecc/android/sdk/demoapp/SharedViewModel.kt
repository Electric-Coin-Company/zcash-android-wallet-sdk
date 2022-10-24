package cash.z.ecc.android.sdk.demoapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking

/**
 * Shared mutable state for the demo
 */
class SharedViewModel(application: Application) : AndroidViewModel(application) {

    private val _seedPhrase = MutableStateFlow(DemoConstants.INITIAL_SEED_WORDS)

    private val _blockHeight = MutableStateFlow<BlockHeight?>(
        runBlocking {
            BlockHeight.ofLatestCheckpoint(
                getApplication(),
                ZcashNetwork.fromResources(application)
            )
        }
    )

    // publicly, this is read-only
    val seedPhrase: StateFlow<String> get() = _seedPhrase

    // publicly, this is read-only
    val birthdayHeight: StateFlow<BlockHeight?> get() = _blockHeight

    fun updateSeedPhrase(newPhrase: String?): Boolean {
        return if (isValidSeedPhrase(newPhrase)) {
            _seedPhrase.value = newPhrase!!
            true
        } else {
            false
        }
    }

    private fun isValidSeedPhrase(phrase: String?): Boolean {
        if (phrase.isNullOrEmpty()) {
            return false
        }
        return try {
            Mnemonics.MnemonicCode(phrase).validate()
            true
        } catch (e: Mnemonics.WordCountException) {
            twig("Seed phrase validation failed with WordCountException: ${e.message}, cause: ${e.cause}")
            false
        } catch (e: Mnemonics.InvalidWordException) {
            twig("Seed phrase validation failed with InvalidWordException: ${e.message}, cause: ${e.cause}")
            false
        } catch (e: Mnemonics.ChecksumException) {
            twig("Seed phrase validation failed with ChecksumException: ${e.message}, cause: ${e.cause}")
            false
        }
    }
}
