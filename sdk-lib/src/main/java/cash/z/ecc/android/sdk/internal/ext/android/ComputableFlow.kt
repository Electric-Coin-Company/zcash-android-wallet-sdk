package cash.z.ecc.android.sdk.internal.ext.android

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

/* Adapted from ComputableLiveData */
abstract class ComputableFlow<T>(dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private val computationScope: CoroutineScope = CoroutineScope(dispatcher)
    private val computationFlow: MutableSharedFlow<T> = MutableSharedFlow(replay = 1)
    internal val flow = computationFlow.asSharedFlow().onStart { invalidate() }

    /**
     * Invalidates the flow.
     * This will trigger a call to [.compute].
     */
    fun invalidate() {
        computationScope.launch { computationFlow.emit(compute()) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun cancel() {
        computationScope.cancel()
        computationFlow.resetReplayCache()
    }

    protected abstract fun compute(): T
}
