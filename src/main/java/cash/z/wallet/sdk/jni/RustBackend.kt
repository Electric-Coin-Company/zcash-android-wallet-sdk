package cash.z.wallet.sdk.jni

import cash.z.wallet.sdk.data.twig

/**
 * Serves as the JNI boundary between the Kotlin and Rust layers. Functions in this class should not be called directly
 * by code outside of the SDK. Instead, one of the higher-level components should be used such as Wallet.kt or
 * CompactBlockProcessor.kt.
 */
class RustBackend : RustBackendWelding {

    external override fun initDataDb(dbDataPath: String): Boolean

    external override fun initAccountsTable(
        dbDataPath: String,
        seed: ByteArray,
        accounts: Int): Array<String>

    external override fun initBlocksTable(
        dbDataPath: String,
        height: Int,
        hash: String,
        time: Long,
        saplingTree: String): Boolean

    external override fun getAddress(dbDataPath: String, account: Int): String

    external override fun isValidShieldedAddress(addr: String): Boolean

    external override fun isValidTransparentAddress(addr: String): Boolean

    external override fun getBalance(dbDataPath: String, account: Int): Long

    external override fun getVerifiedBalance(dbDataPath: String, account: Int): Long

    external override fun getReceivedMemoAsUtf8(dbDataPath: String, idNote: Long): String

    external override fun getSentMemoAsUtf8(dbDataPath: String, idNote: Long): String

    external override fun validateCombinedChain(dbCachePath: String, dbDataPath: String): Int

    external override fun rewindToHeight(dbDataPath: String, height: Int): Boolean

    external override fun scanBlocks(dbCachePath: String, dbDataPath: String): Boolean

    external override fun createToAddress(
        dbDataPath: String,
        account: Int,
        extsk: String,
        to: String,
        value: Long,
        memo: String,
        spendParamsPath: String,
        outputParamsPath: String
    ): Long

    external override fun initLogs()

    companion object {
        init {
            try {
                System.loadLibrary("zcashwalletsdk")
            } catch (e: Throwable) {
                twig("Error while loading native library: ${e.message}")
            }
        }
    }

}