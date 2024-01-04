package cash.z.ecc.android.sdk.ext

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Utility for removing some of the boilerplate around Synchronizers and working with flows. Allows
 * for collecting all the elements of a flow with a given scope this is useful when you want to
 * launch multiple things in the same scope at once. Intuitively a developer may try:
 * ```
 * scope.launch {
 *     flow1.collect { collectingFunction1() }
 *     flow2.collect { collectingFunction2() }
 * }
 * ```
 * But this results in the collections running sequentially rather than in parallel. Alternatively,
 * This code produces the desired behavior but is verbose and a little unclear:
 * ```
 * scope.launch { flow1.collect { collectingFunction1() } }
 * scope.launch { flow1.collect { collectingFunction2() } }
 * ```
 * This extension functions makes the intended behavior a little easier to read by focusing on the
 * flow itself rather than the scope:
 * ```
 * flow1.collectWith(scope, ::collectingFunction1)
 * flow2.collectWith(scope, ::collectingFunction2)
 * ```
 */
fun <T> Flow<T>.collectWith(
    scope: CoroutineScope,
    block: (T) -> Unit
) {
    scope.launch {
        collect {
            block(it)
        }
    }
}

/**
 * Utility for performing the given action on the first emission of a flow and running that action
 * in the given scope.
 *
 * Unused in the SDK but is used by the wallet app
 */
@Suppress("unused")
fun <T, S> Flow<T>.onFirstWith(
    scope: CoroutineScope,
    block: suspend (T) -> S
) {
    scope.launch {
        onEach {
            block(it)
        }.first()
    }
}

/**
 * Utility for performing the given action on the first emission of a flow.
 */
suspend fun <T, S> Flow<T>.onFirst(block: suspend (T) -> S) {
    onEach {
        block(it)
    }.first()
}
