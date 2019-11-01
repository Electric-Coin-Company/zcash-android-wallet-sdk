package cash.z.wallet.sdk.jni

/**
 * Contract defining the exposed capabilities of the Rust backend.
 * This is what welds the SDK to the Rust layer.
 */
interface RustBackendWelding {

    fun initDataDb(): Boolean

//    fun initAccountsTable(extfvks: Array<ByteArray>, numberOfAccounts: Int)

    fun initAccountsTable(seed: ByteArray, numberOfAccounts: Int): Array<String>

    fun initBlocksTable(height: Int, hash: String, time: Long, saplingTree: String): Boolean

    fun getAddress(account: Int = 0): String

    fun isValidShieldedAddr(addr: String): Boolean

    fun isValidTransparentAddr(addr: String): Boolean

    fun getBalance(account: Int = 0): Long

    fun getVerifiedBalance(account: Int = 0): Long

    fun getReceivedMemoAsUtf8(idNote: Long): String

    fun getSentMemoAsUtf8(idNote: Long): String

    fun validateCombinedChain(): Int

    fun rewindToHeight(height: Int): Boolean

    fun scanBlocks(): Boolean

    fun createToAddress(
        account: Int,
        extsk: String,
        to: String,
        value: Long,
        memo: String
    ): Long

    fun deriveSpendingKeys(seed: ByteArray, numberOfAccounts: Int = 1): Array<String>

    fun deriveViewingKeys(seed: ByteArray, numberOfAccounts: Int = 1): Array<String>

    fun deriveViewingKey(spendingKey: String): String
}