package cash.z.ecc.android.sdk.ext

import kotlin.math.max
import kotlin.math.min

/**
 * Simple implementation of Simple moving average.
 */
class Sma(val window: Int = 3) {
    private val values = Array(window) { 0.0 }
    var average = 0.0
        private set

    var count: Int = 0
    var index: Int = 0

    fun add(value: Number) = add(value.toDouble())

    fun add(value: Double): Double {
        when {
            // full window
            count == window -> {
                index = (index + 1) % window
                average += ((value - values[index]) / count.toFloat())
                values[index] = value
            }
            // partially-filled window
            count != 0 -> {
                index = (index + 1) % window
                average = ((value + count.toFloat() * average) / (count + 1).toFloat())
                values[index] = value
                count++
            }
            // empty window
            else -> {
                // simply assign given value as current average:
                average = value
                values[0] = value
                count = 1
            }
        }
        return average
    }

    fun format(places: Int = 0) = "%.${places}f".format(average)
}

class BatchMetrics(val range: IntRange, val batchSize: Int, val onMetricComplete: ((BatchMetrics, Boolean) -> Unit)? = null) {
    private var completedBatches = 0
    private var rangeStartTime = 0L
    private var batchStartTime = 0L
    private var batchEndTime = 0L
    private var rangeSize = range.last - range.first + 1
    private inline fun now() = System.currentTimeMillis()
    private inline fun ips(blocks: Int, time: Long) = 1000.0f * blocks / time

    val isComplete = completedBatches * batchSize >= rangeSize
    val isBatchComplete = batchEndTime > batchStartTime
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
