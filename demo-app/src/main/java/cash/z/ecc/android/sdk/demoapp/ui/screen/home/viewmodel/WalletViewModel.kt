package cash.z.ecc.android.sdk.demoapp.ui.screen.home.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.WalletCoordinator
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.demoapp.getInstance
import cash.z.ecc.android.sdk.demoapp.preference.EncryptedPreferenceKeys
import cash.z.ecc.android.sdk.demoapp.preference.EncryptedPreferenceSingleton
import cash.z.ecc.android.sdk.demoapp.ui.common.ANDROID_STATE_FLOW_TIMEOUT
import cash.z.ecc.android.sdk.demoapp.ui.common.throttle
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.PercentDecimal
import cash.z.ecc.android.sdk.model.PersistableWallet
import cash.z.ecc.android.sdk.model.WalletAddresses
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.model.ZecSend
import cash.z.ecc.android.sdk.model.send
import cash.z.ecc.android.sdk.tool.DerivationTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

// To make this more multiplatform compatible, we need to remove the dependency on Context
// for loading the preferences.
class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val walletCoordinator = WalletCoordinator.getInstance(application)

    /*
     * Using the Mutex may be overkill, but it ensures that if multiple calls are accidentally made
     * that they have a consistent ordering.
     */
    private val persistWalletMutex = Mutex()

    /**
     * Synchronizer that is retained long enough to survive configuration changes.
     */
    val synchronizer = walletCoordinator.synchronizer.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
        null
    )

    val secretState: StateFlow<SecretState> = walletCoordinator.persistableWallet
        .map { persistableWallet ->
            if (null == persistableWallet) {
                SecretState.None
            } else {
                SecretState.Ready(persistableWallet)
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            SecretState.Loading
        )

    val spendingKey = secretState
        .filterIsInstance<SecretState.Ready>()
        .map { it.persistableWallet }
        .map {
            val bip39Seed = withContext(Dispatchers.IO) {
                Mnemonics.MnemonicCode(it.seedPhrase.joinToString()).toSeed()
            }
            DerivationTool.deriveUnifiedSpendingKey(
                seed = bip39Seed,
                network = it.network,
                account = Account.DEFAULT
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            null
        )

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
    val walletSnapshot: StateFlow<WalletSnapshot?> = synchronizer
        .flatMapLatest {
            if (null == it) {
                flowOf(null)
            } else {
                it.toWalletSnapshot()
            }
        }
        .throttle(1.seconds)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            null
        )

    val addresses: StateFlow<WalletAddresses?> = synchronizer
        .filterNotNull()
        .map {
            WalletAddresses.new(it)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(ANDROID_STATE_FLOW_TIMEOUT),
            null
        )

    private val mutableSendState = MutableStateFlow<SendState>(SendState.None)

    val sendState: StateFlow<SendState> = mutableSendState

    /**
     * Creates a wallet asynchronously and then persists it.  Clients observe
     * [secretState] to see the side effects.  This would be used for a user creating a new wallet.
     */
    /*
     * Although waiting for the wallet to be written and then read back is slower, it is probably
     * safer because it 1. guarantees the wallet is written to disk and 2. has a single source of truth.
     */
    fun persistNewWallet() {
        val application = getApplication<Application>()

        viewModelScope.launch {
            val newWallet = PersistableWallet.new(application, ZcashNetwork.fromResources(application))
            persistExistingWallet(newWallet)
        }
    }

    /**
     * Persists a wallet asynchronously.  Clients observe [secretState]
     * to see the side effects.  This would be used for a user restoring a wallet from a backup.
     */
    fun persistExistingWallet(persistableWallet: PersistableWallet) {
        val application = getApplication<Application>()

        viewModelScope.launch {
            val preferenceProvider = EncryptedPreferenceSingleton.getInstance(application)
            persistWalletMutex.withLock {
                EncryptedPreferenceKeys.PERSISTABLE_WALLET.putValue(preferenceProvider, persistableWallet)
            }
        }
    }

    /**
     * Asynchronously sends funds.  Note that two sending operations cannot occur at the same time.
     *
     * Observe the result via [sendState].
     */
    fun send(zecSend: ZecSend) {
        if (sendState.value is SendState.Sending) {
            return
        }

        mutableSendState.value = SendState.Sending

        val synchronizer = synchronizer.value
        if (null != synchronizer) {
            viewModelScope.launch {
                val spendingKey = spendingKey.filterNotNull().first()
                runCatching { synchronizer.send(spendingKey, zecSend) }
                    .onSuccess { mutableSendState.value = SendState.Sent(it) }
                    .onFailure { mutableSendState.value = SendState.Error(it) }
            }
        } else {
            SendState.Error(IllegalStateException("Unable to send funds because synchronizer is not loaded."))
        }
    }

    /**
     * Asynchronously shields transparent funds.  Note that two shielding operations cannot occur at the same time.
     *
     * Observe the result via [sendState].
     */
    fun shieldFunds() {
        if (sendState.value is SendState.Sending) {
            return
        }

        mutableSendState.value = SendState.Sending

        val synchronizer = synchronizer.value
        if (null != synchronizer) {
            viewModelScope.launch {
                val spendingKey = spendingKey.filterNotNull().first()
                kotlin.runCatching { synchronizer.shieldFunds(spendingKey) }
                    .onSuccess { mutableSendState.value = SendState.Sent(it) }
                    .onFailure { mutableSendState.value = SendState.Error(it) }
            }
        } else {
            SendState.Error(IllegalStateException("Unable to send funds because synchronizer is not loaded."))
        }
    }

    fun clearSendOrShieldState() {
        mutableSendState.value = SendState.None
    }

    /**
     * This method only has an effect if the synchronizer currently is loaded.
     */
    fun rescanBlockchain() {
        viewModelScope.launch {
            walletCoordinator.rescanBlockchain()
        }
    }

    /**
     * This asynchronously resets the SDK state.  This is non-destructive, as SDK state can be rederived.
     *
     * This could be used as a troubleshooting step in debugging.
     */
    fun resetSdk() {
        walletCoordinator.resetSdk()
    }
}

/**
 * Represents the state of the wallet secret.
 */
sealed class SecretState {
    object Loading : SecretState()
    object None : SecretState()
    class Ready(val persistableWallet: PersistableWallet) : SecretState()
}

sealed class SendState {
    object None : SendState() {
        override fun toString(): String = "None"
    }
    object Sending : SendState() {
        override fun toString(): String = "Sending"
    }
    class Sent(val localTxId: Long) : SendState() {
        override fun toString(): String = "Sent"
    }
    class Error(val error: Throwable) : SendState() {
        override fun toString(): String = "Error ${error.message}"
    }
}

/**
 * Represents all kind of Synchronizer errors
 */
// TODO [#529] https://github.com/zcash/secant-android-wallet/issues/529
sealed class SynchronizerError {
    abstract fun getCauseMessage(): String?

    class Critical(val error: Throwable?) : SynchronizerError() {
        override fun getCauseMessage(): String? = error?.localizedMessage
    }

    class Processor(val error: Throwable?) : SynchronizerError() {
        override fun getCauseMessage(): String? = error?.localizedMessage
    }

    class Submission(val error: Throwable?) : SynchronizerError() {
        override fun getCauseMessage(): String? = error?.localizedMessage
    }

    class Setup(val error: Throwable?) : SynchronizerError() {
        override fun getCauseMessage(): String? = error?.localizedMessage
    }

    class Chain(val x: BlockHeight, val y: BlockHeight) : SynchronizerError() {
        override fun getCauseMessage(): String = "$x, $y"
    }
}

private fun Synchronizer.toCommonError(): Flow<SynchronizerError?> = callbackFlow {
    // just for initial default value emit
    trySend(null)

    onCriticalErrorHandler = {
        Twig.error { "WALLET - Error Critical: $it" }
        trySend(SynchronizerError.Critical(it))
        false
    }
    onProcessorErrorHandler = {
        Twig.error { "WALLET - Error Processor: $it" }
        trySend(SynchronizerError.Processor(it))
        false
    }
    onSubmissionErrorHandler = {
        Twig.error { "WALLET - Error Submission: $it" }
        trySend(SynchronizerError.Submission(it))
        false
    }
    onSetupErrorHandler = {
        Twig.error { "WALLET - Error Setup: $it" }
        trySend(SynchronizerError.Setup(it))
        false
    }
    onChainErrorHandler = { x, y ->
        Twig.error { "WALLET - Error Chain: $x, $y" }
        trySend(SynchronizerError.Chain(x, y))
    }

    awaitClose {
        // nothing to close here
    }
}

// No good way around needing magic numbers for the indices
@Suppress("MagicNumber")
private fun Synchronizer.toWalletSnapshot() =
    combine(
        status, // 0
        processorInfo, // 1
        orchardBalances, // 2
        saplingBalances, // 3
        transparentBalances, // 4
        progress, // 5
        toCommonError() // 6
    ) { flows ->
        val orchardBalance = flows[2] as WalletBalance?
        val saplingBalance = flows[3] as WalletBalance?
        val transparentBalance = flows[4] as WalletBalance?

        val progressPercentDecimal = (flows[5] as Int).let { value ->
            if (value > PercentDecimal.MAX || value < PercentDecimal.MIN) {
                PercentDecimal.ZERO_PERCENT
            }
            PercentDecimal(value / 100f)
        }

        WalletSnapshot(
            flows[0] as Synchronizer.Status,
            flows[1] as CompactBlockProcessor.ProcessorInfo,
            orchardBalance ?: WalletBalance(Zatoshi(0), Zatoshi(0)),
            saplingBalance ?: WalletBalance(Zatoshi(0), Zatoshi(0)),
            transparentBalance ?: WalletBalance(Zatoshi(0), Zatoshi(0)),
            progressPercentDecimal,
            flows[6] as SynchronizerError?
        )
    }
