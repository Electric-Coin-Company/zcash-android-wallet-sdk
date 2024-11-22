package cash.z.ecc.android.sdk.model

import cash.z.ecc.android.sdk.internal.jni.JNI_ACCOUNT_UUID_BYTES_SIZE

/**
 * Unique identifier for a specific account tracked by a `Synchronizer`.
 *
 * Account identifiers are "one-way stable": a given identifier always points to a
 * specific viewing key within a specific `Synchronizer` instance, but the same viewing
 * key may have multiple account identifiers over time. In particular, this SDK upholds
 * the following properties:
 *
 * - When an account starts being tracked within a `Synchronizer` instance, a new
 *   `Account` is generated.
 * - If an `Account` is present within a `Synchronizer`, it always points to the same
 *   account.
 *
 * What this means is that account identifiers are not stable across "wallet recreation
 * events". Examples of these include:
 * - Restoring a wallet from a backed-up seed.
 * - Importing the same viewing key into two different wallet instances.
 *
 * @param accountUuid The account identifier. Must be length 16.
 */
data class Account(val accountUuid: ByteArray) {
    init {
        require(accountUuid.size == JNI_ACCOUNT_UUID_BYTES_SIZE) {
            "Account UUID must be 16 bytes"
        }
    }

    override fun toString(): String {
        return "Account(accountUuid=${accountUuid.contentToString()})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Account

        return accountUuid.contentEquals(other.accountUuid)
    }

    override fun hashCode(): Int {
        return accountUuid.contentHashCode()
    }
}
