package cash.z.ecc.android.sdk.demoapp

import android.content.Context
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.demoapp.model.PersistableWallet
import cash.z.ecc.android.sdk.demoapp.util.Twig
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.ext.onFirst
import cash.z.ecc.android.sdk.model.LightWalletEndpoint
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.model.defaultForNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

/**
 * @param persistableWallet flow of the user's stored wallet.  Null indicates that no wallet has been stored.
 */
class WalletCoordinator(context: Context, val persistableWallet: Flow<PersistableWallet?>) {

    private val applicationContext = context.applicationContext

    /*
     * We want a global scope that is independent of the lifecycles of either
     * WorkManager or the UI.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private val walletScope = CoroutineScope(GlobalScope.coroutineContext + Dispatchers.Main)

    private val synchronizerMutex = Mutex()

    private val lockoutMutex = Mutex()
    private val synchronizerLockoutId = MutableStateFlow<UUID?>(null)

    private sealed class InternalSynchronizerStatus {
        object NoWallet : InternalSynchronizerStatus()
        class Available(val synchronizer: Synchronizer) : InternalSynchronizerStatus()
        class Lockout(val id: UUID) : InternalSynchronizerStatus()
    }

    private val synchronizerOrLockoutId: Flow<Flow<InternalSynchronizerStatus>> = persistableWallet
        .combine(synchronizerLockoutId) { persistableWallet: PersistableWallet?, lockoutId: UUID? ->
            if (null != lockoutId) { // this one needs to come first
                flowOf(InternalSynchronizerStatus.Lockout(lockoutId))
            } else if (null == persistableWallet) {
                flowOf(InternalSynchronizerStatus.NoWallet)
            } else {
                callbackFlow<InternalSynchronizerStatus.Available> {
                    val closeableSynchronizer = Synchronizer.new(
                        context = context,
                        zcashNetwork = persistableWallet.network,
                        lightWalletEndpoint = LightWalletEndpoint.defaultForNetwork(persistableWallet.network),
                        birthday = persistableWallet.birthday,
                        seed = persistableWallet.seedPhrase.toByteArray(),
                        alias = NEW_UI_SYNCHRONIZER_ALIAS
                    )

                    trySend(InternalSynchronizerStatus.Available(closeableSynchronizer))
                    awaitClose {
                        Twig.info { "Closing flow and stopping synchronizer" }
                        closeableSynchronizer.close()
                    }
                }
            }
        }

    /**
     * Synchronizer for the Zcash SDK. Emits null until a wallet secret is persisted.
     *
     * Note that this synchronizer is closed as soon as it stops being collected.  For UI use
     * cases, see [WalletViewModel].
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val synchronizer: StateFlow<Synchronizer?> = synchronizerOrLockoutId
        .flatMapLatest {
            it
        }
        .map {
            when (it) {
                is InternalSynchronizerStatus.Available -> it.synchronizer
                is InternalSynchronizerStatus.Lockout -> null
                InternalSynchronizerStatus.NoWallet -> null
            }
        }
        .stateIn(
            walletScope,
            SharingStarted.WhileSubscribed(),
            null
        )

    /**
     * Rescans the blockchain.
     *
     * In order for a rescan to occur, the synchronizer must be loaded already
     * which would happen if the UI is collecting it.
     *
     * @return True if the rescan was performed and false if the rescan was not performed.
     */
    suspend fun rescanBlockchain(): Boolean {
        synchronizerMutex.withLock {
            synchronizer.value?.let {
                it.latestBirthdayHeight?.let { height ->
                    it.rewindToNearestHeight(height, true)
                    return true
                }
            }
        }

        return false
    }

    /**
     * Resets persisted data in the SDK, but preserves the wallet secret.  This will cause the
     * WalletCoordinator to emit a new synchronizer instance.
     */
    @OptIn(FlowPreview::class)
    fun resetSdk() {
        walletScope.launch {
            lockoutMutex.withLock {
                val lockoutId = UUID.randomUUID()
                synchronizerLockoutId.value = lockoutId

                synchronizerOrLockoutId
                    .flatMapConcat { it }
                    .filterIsInstance<InternalSynchronizerStatus.Lockout>()
                    .filter { it.id == lockoutId }
                    .onFirst {
                        synchronizerMutex.withLock {
                            val didDelete = Synchronizer.erase(
                                appContext = applicationContext,
                                network = ZcashNetwork.fromResources(applicationContext)
                            )
                            Twig.info { "SDK erase result: $didDelete" }
                        }
                    }

                synchronizerLockoutId.value = null
            }
        }
    }

    // Allows for extension functions
    companion object {
        internal const val NEW_UI_SYNCHRONIZER_ALIAS = "new_ui"
    }
}
