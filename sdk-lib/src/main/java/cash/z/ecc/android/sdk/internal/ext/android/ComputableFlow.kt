package cash.z.ecc.android.sdk.internal.ext.android

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/* Adapted from ComputableLiveData */
// TODO [#658] https://github.com/zcash/zcash-android-wallet-sdk/issues/658
@Suppress("DEPRECATION")
@ObsoleteCoroutinesApi
abstract class ComputableFlow<T>(dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private val computationScope: CoroutineScope = CoroutineScope(dispatcher + SupervisorJob())
    private val computationChannel: ConflatedBroadcastChannel<T> = ConflatedBroadcastChannel()
    internal val flow = computationChannel.asFlow().flowOn(dispatcher).onStart {
        invalidate()
    }

    /**
     * Invalidates the flow.
     * This will trigger a call to [.compute].
     */
    fun invalidate() {
        computationScope.launch { computationChannel.send(compute()) }
    }

    fun cancel() {
        computationScope.cancel()
        computationChannel.cancel()
    }

    protected abstract fun compute(): T
}
