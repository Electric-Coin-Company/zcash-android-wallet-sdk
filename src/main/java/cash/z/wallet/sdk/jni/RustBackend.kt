package cash.z.wallet.sdk.jni

import android.content.Context
import cash.z.wallet.sdk.data.twig
import cash.z.wallet.sdk.ext.ZcashSdk.OUTPUT_PARAM_FILE_NAME
import cash.z.wallet.sdk.ext.ZcashSdk.SPEND_PARAM_FILE_NAME
import java.io.File

/**
 * Serves as the JNI boundary between the Kotlin and Rust layers. Functions in this class should
 * not be called directly by code outside of the SDK. Instead, one of the higher-level components
 * should be used such as Wallet.kt or CompactBlockProcessor.kt.
 */
object RustBackend : RustBackendWelding {
    private var loaded = false
    private lateinit var dbDataPath: String
    private lateinit var dbCachePath: String
    lateinit var paramDestinationDir: String

    /**
     * Loads the library and initializes path variables. Although it is best to only call this
     * function once, it is idempotent.
     */
    override fun create(appContext: Context, dbCacheName: String, dbDataName: String): RustBackend {
        twig("Creating RustBackend") {
            // It is safe to call these things twice but not efficient. So we add a loose check and
            // ignore the fact that it's not thread-safe.
            if (!loaded) {
                loadRustLibrary()
                initLogs()
            }
            dbCachePath = appContext.getDatabasePath(dbCacheName).absolutePath
            dbDataPath = appContext.getDatabasePath(dbDataName).absolutePath
            paramDestinationDir = "${appContext.cacheDir.absolutePath}/params"
        }
        return this
    }

    fun clear() {
        twig("Deleting databases")
        File(dbCachePath).delete()
        File(dbDataPath).delete()
    }

    /**
     * The first call made to this object in order to load the Rust backend library. All other calls
     * will fail if this function has not been invoked.
     */
    private fun loadRustLibrary() {
        try {
            System.loadLibrary("zcashwalletsdk")
            loaded = true
        } catch (e: Throwable) {
            twig("Error while loading native library: ${e.message}")
        }
    }

    //
    // Wrapper Functions
    //

    override fun initDataDb(): Boolean = initDataDb(dbDataPath)

    override fun initAccountsTable(seed: ByteArray, numberOfAccounts: Int): Array<String> =
        initAccountsTable(dbDataPath, seed, numberOfAccounts)

    override fun initBlocksTable(
        height: Int,
        hash: String,
        time: Long,
        saplingTree: String
    ): Boolean = initBlocksTable(dbDataPath, height, hash, time, saplingTree)

    override fun getAddress(account: Int): String = getAddress(dbDataPath, account)

    override fun getBalance(account: Int): Long = getBalance(dbDataPath, account)

    override fun getVerifiedBalance(account: Int): Long = getVerifiedBalance(dbDataPath, account)

    override fun getReceivedMemoAsUtf8(idNote: Long): String =
        getReceivedMemoAsUtf8(dbDataPath, idNote)

    override fun getSentMemoAsUtf8(idNote: Long): String = getSentMemoAsUtf8(dbDataPath, idNote)

    override fun validateCombinedChain(): Int = validateCombinedChain(dbCachePath, dbDataPath)

    override fun rewindToHeight(height: Int): Boolean = rewindToHeight(dbDataPath, height)

    override fun scanBlocks(): Boolean = scanBlocks(dbCachePath, dbDataPath)

    override fun createToAddress(
        account: Int,
        extsk: String,
        to: String,
        value: Long,
        memo: String
    ): Long = createToAddress(
        dbDataPath,
        account,
        extsk,
        to,
        value,
        memo,
        "${paramDestinationDir}/$SPEND_PARAM_FILE_NAME",
        "${paramDestinationDir}/$OUTPUT_PARAM_FILE_NAME"
    )


    //
    // External Functions
    //

    private external fun initDataDb(dbDataPath: String): Boolean

    private external fun initAccountsTable(
        dbDataPath: String,
        seed: ByteArray,
        accounts: Int
    ): Array<String>

    private external fun initBlocksTable(
        dbDataPath: String,
        height: Int,
        hash: String,
        time: Long,
        saplingTree: String
    ): Boolean

    private external fun getAddress(dbDataPath: String, account: Int): String

    external override fun isValidShieldedAddress(addr: String): Boolean

    external override fun isValidTransparentAddress(addr: String): Boolean

    private external fun getBalance(dbDataPath: String, account: Int): Long

    private external fun getVerifiedBalance(dbDataPath: String, account: Int): Long

    private external fun getReceivedMemoAsUtf8(dbDataPath: String, idNote: Long): String

    private external fun getSentMemoAsUtf8(dbDataPath: String, idNote: Long): String

    private external fun validateCombinedChain(dbCachePath: String, dbDataPath: String): Int

    private external fun rewindToHeight(dbDataPath: String, height: Int): Boolean

    private external fun scanBlocks(dbCachePath: String, dbDataPath: String): Boolean

    private external fun createToAddress(
        dbDataPath: String,
        account: Int,
        extsk: String,
        to: String,
        value: Long,
        memo: String,
        spendParamsPath: String,
        outputParamsPath: String
    ): Long

    private external fun initLogs()

}