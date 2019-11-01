package cash.z.wallet.sdk.ext.android

import android.annotation.SuppressLint
import androidx.paging.Config
import androidx.paging.DataSource
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/* Adapted from LiveDataPagedList */

/**
 * Constructs a `Flow<PagedList>`, from this `DataSource.Factory`, convenience for
 * [FlowPagedListBuilder].
 *
 * No work (such as loading) is done immediately, the creation of the first PagedList is is
 * deferred until the Flow is collected.
 *
 * @param config Paging configuration.
 * @param initialLoadKey Initial load key passed to the first PagedList/DataSource.
 * @param boundaryCallback The boundary callback for listening to PagedList load state.
 * @param fetchExecutor Executor for fetching data from DataSources.
 *
 * @see LivePagedListBuilder
 */
@SuppressLint("RestrictedApi")
fun <Key, Value> DataSource.Factory<Key, Value>.toFlowPagedList(
    config: PagedList.Config,
    initialLoadKey: Key? = null,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    fetchContext: CoroutineDispatcher = Dispatchers.IO
): Flow<PagedList<Value>> =
    FlowPagedListBuilder(this, config, initialLoadKey, boundaryCallback, fetchContext)
        .build()

/**
 * Constructs a `Flow<PagedList>`, from this `DataSource.Factory`, convenience for
 * [FlowPagedListBuilder].
 *
 * No work (such as loading) is done immediately, the creation of the first PagedList is is
 * deferred until the Flow is collected.
 *
 * @param pageSize Page size.
 * @param initialLoadKey Initial load key passed to the first PagedList/DataSource.
 * @param boundaryCallback The boundary callback for listening to PagedList load state.
 * @param fetchExecutor Executor for fetching data from DataSources.
 *
 * @see FlowPagedListBuilder
 */
@SuppressLint("RestrictedApi")
inline fun <Key, Value> DataSource.Factory<Key, Value>.toFlowPagedList(
    pageSize: Int,
    initialLoadKey: Key? = null,
    boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
    fetchContext: CoroutineDispatcher = Dispatchers.IO
): Flow<PagedList<Value>> =
    FlowPagedListBuilder(this, Config(pageSize), initialLoadKey, boundaryCallback, fetchContext)
        .build()

/**
 * Allows another component to call invalidate on the most recently created datasource. Although it
 * is expected that a DataSource will invalidate itself, there are times where external components
 * have modified the underlying data and thereby become responsible for invalidation. In our case,
 * there is more than one process updating the database. So the other process must invalidate the
 * data after an update in order to trigger refreshes all the way up the stack.
 */
fun <Key, Value> DataSource.Factory<Key, Value>.toRefreshable(): RefreshableDataSourceFactory<Key, Value> =
    RefreshableDataSourceFactory(this)

class RefreshableDataSourceFactory<Key, Value>(private val delegate: DataSource.Factory<Key, Value>) :
    DataSource.Factory<Key, Value>() {
    private var lastDataSource: DataSource<Key, Value>? = null
    override fun create(): DataSource<Key, Value> {
        refresh()
        return delegate.create().also { lastDataSource = it }
    }

    fun refresh() {
        lastDataSource?.invalidate()
        lastDataSource = null
    }
}
