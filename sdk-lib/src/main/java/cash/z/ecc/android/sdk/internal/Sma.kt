package cash.z.ecc.android.sdk.internal

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
