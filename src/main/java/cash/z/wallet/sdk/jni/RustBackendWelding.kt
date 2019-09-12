package cash.z.wallet.sdk.jni

/**
 * Contract defining the exposed capabilitiies of the Rust backend.
 * This is what welds the SDK to the Rust layer.
 */
interface RustBackendWelding {

    fun initDataDb(dbDataPath: String): Boolean

    fun initAccountsTable(
        dbDataPath: String,
        seed: ByteArray,
        accounts: Int): Array<String>

    fun initBlocksTable(
        dbDataPath: String,
        height: Int,
        hash: String,
        time: Long,
        saplingTree: String): Boolean

    fun getAddress(dbDataPath: String, account: Int): String

    fun isValidShieldedAddress(addr: String): Boolean

    fun isValidTransparentAddress(addr: String): Boolean

    fun getBalance(dbDataPath: String, account: Int): Long

    fun getVerifiedBalance(dbDataPath: String, account: Int): Long

    fun getReceivedMemoAsUtf8(dbDataPath: String, idNote: Long): String

    fun getSentMemoAsUtf8(dbDataPath: String, idNote: Long): String

    fun validateCombinedChain(dbCachePath: String, dbDataPath: String): Int

    fun rewindToHeight(dbDataPath: String, height: Int): Boolean

    fun scanBlocks(dbCachePath: String, dbDataPath: String): Boolean

    fun createToAddress(
        dbDataPath: String,
        account: Int,
        extsk: String,
        to: String,
        value: Long,
        memo: String,
        spendParamsPath: String,
        outputParamsPath: String
    ): Long

    fun initLogs()

}