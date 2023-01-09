@file:Suppress("ktlint:filename")

package cash.z.wallet.sdk.sample.demoapp

fun <T> Iterator<T>.count(): Int {
    var count = 0
    forEach { count++ }

    return count
}
