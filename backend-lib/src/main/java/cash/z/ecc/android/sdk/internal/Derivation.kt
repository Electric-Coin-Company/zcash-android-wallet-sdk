package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.internal.model.JniUnifiedSpendingKey

interface Derivation {
    fun deriveUnifiedAddress(
        viewingKey: String,
        networkId: Int
    ): String

    fun deriveUnifiedAddress(
        seed: ByteArray,
        networkId: Int,
        accountIndex: Int
    ): String

    fun deriveUnifiedSpendingKey(
        seed: ByteArray,
        networkId: Int,
        accountIndex: Int
    ): JniUnifiedSpendingKey

    /**
     * @return a unified full viewing key.
     */
    fun deriveUnifiedFullViewingKey(
        usk: JniUnifiedSpendingKey,
        networkId: Int
    ): String

    /**
     * @param numberOfAccounts Use [DEFAULT_NUMBER_OF_ACCOUNTS] to derive a single key.
     * @return an array of unified full viewing keys, one for each account.
     */
    fun deriveUnifiedFullViewingKeys(
        seed: ByteArray,
        networkId: Int,
        numberOfAccounts: Int
    ): Array<String>

    companion object {
        const val DEFAULT_NUMBER_OF_ACCOUNTS = 1
    }
}
