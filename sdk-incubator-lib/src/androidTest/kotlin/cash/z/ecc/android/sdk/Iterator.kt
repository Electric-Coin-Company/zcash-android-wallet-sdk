@file:Suppress("ktlint:filename")

package cash.z.ecc.android.sdk

fun <T> Iterator<T>.count(): Int {
    var count = 0
    forEach { count++ }

    return count
}
