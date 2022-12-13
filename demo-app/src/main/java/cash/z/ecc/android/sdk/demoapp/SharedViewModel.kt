package cash.z.ecc.android.sdk.demoapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.LightWalletEndpoint
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.model.defaultForNetwork
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

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

    // Note that seed and birthday shouldn't be changed once a synchronizer is first collected
    val synchronizerFlow: StateFlow<Synchronizer?> = callbackFlow<Synchronizer?> {
        // Use a BIP-39 library to convert a seed phrase into a byte array. Most wallets already
        // have the seed stored
        val seedBytes = Mnemonics.MnemonicCode(seedPhrase.value).toSeed()

        val network = ZcashNetwork.fromResources(application)
        val synchronizer = Synchronizer.new(
            application,
            network,
            lightWalletEndpoint = LightWalletEndpoint.defaultForNetwork(network),
            seed = seedBytes,
            birthday = birthdayHeight.value
        )

        send(synchronizer)
        awaitClose {
            synchronizer.close()
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(DEFAULT_ANDROID_STATE_TIMEOUT.inWholeMilliseconds, 0),
        initialValue =
        null
    )

    fun updateSeedPhrase(newPhrase: String?): Boolean {
        return if (isValidSeedPhrase(newPhrase)) {
            _seedPhrase.value = newPhrase!!
            true
        } else {
            false
        }
    }

    fun resetSDK() {
        viewModelScope.launch {
            with(getApplication<Application>()) {
                Synchronizer.erase(
                    appContext = applicationContext,
                    network = ZcashNetwork.fromResources(applicationContext)
                )
            }
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

    companion object {
        private val DEFAULT_ANDROID_STATE_TIMEOUT = 5.seconds
    }
}
