package cash.z.ecc.android.sdk.ext

import kotlin.math.max
import kotlin.math.min

class BatchMetrics(val range: IntRange, val batchSize: Int, private val onMetricComplete: ((BatchMetrics, Boolean) -> Unit)? = null) {
    private var completedBatches = 0
    private var rangeStartTime = 0L
    private var batchStartTime = 0L
    private var batchEndTime = 0L
    private var rangeSize = range.last - range.first + 1
    private inline fun now() = System.currentTimeMillis()
    private inline fun ips(blocks: Int, time: Long) = 1000.0f * blocks / time

    val isComplete get() = completedBatches * batchSize >= rangeSize
    val isBatchComplete get() = batchEndTime > batchStartTime
    val cumulativeItems get() = min(completedBatches * batchSize, rangeSize)
    val cumulativeTime get() = (if (isComplete) batchEndTime else now()) - rangeStartTime
    val batchTime get() = max(batchEndTime - batchStartTime, now() - batchStartTime)
    val batchItems get() = min(batchSize, batchSize - (completedBatches * batchSize - rangeSize))
    val batchIps get() = ips(batchItems, batchTime)
    val cumulativeIps get() = ips(cumulativeItems, cumulativeTime)

    fun beginBatch() {
        batchStartTime = now()
        if (rangeStartTime == 0L) rangeStartTime = batchStartTime
    }
    fun endBatch() {
        completedBatches++
        batchEndTime = now()
        onMetricComplete?.let {
            it.invoke(this, isComplete)
        }
    }
}
