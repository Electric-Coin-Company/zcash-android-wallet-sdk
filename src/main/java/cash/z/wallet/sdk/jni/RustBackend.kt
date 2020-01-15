package cash.z.wallet.sdk.jni

import android.content.Context
import cash.z.wallet.sdk.exception.BirthdayException
import cash.z.wallet.sdk.ext.ZcashSdk
import cash.z.wallet.sdk.ext.ZcashSdk.OUTPUT_PARAM_FILE_NAME
import cash.z.wallet.sdk.ext.ZcashSdk.SPEND_PARAM_FILE_NAME
import cash.z.wallet.sdk.ext.twig
import java.io.File

/**
 * Serves as the JNI boundary between the Kotlin and Rust layers. Functions in this class should
 * not be called directly by code outside of the SDK. Instead, one of the higher-level components
 * should be used such as Wallet.kt or CompactBlockProcessor.kt.
 */
class RustBackend : RustBackendWelding {

    init {
        load()
    }

    internal lateinit var dbDataPath: String
    internal lateinit var dbCachePath: String
    internal lateinit var dbNamePrefix: String
    internal lateinit var paramDestinationDir: String
    internal var birthdayHeight: Int = -1
        get() = if (field != -1) field else throw BirthdayException.UninitializedBirthdayException

    fun init(appContext: Context, dbNamePrefix: String = ZcashSdk.DEFAULT_DB_NAME_PREFIX) =
        init(
            appContext.getDatabasePath("unused.db").parentFile.absolutePath,
            "${appContext.cacheDir.absolutePath}/params",
            dbNamePrefix
        )

    /**
     * Loads the library and initializes path variables. Although it is best to only call this
     * function once, it is idempotent.
     */
    fun init(
        dbPath: String,
        paramsPath: String,
        dbNamePrefix: String = ZcashSdk.DEFAULT_DB_NAME_PREFIX
    ): RustBackend {
        this.dbNamePrefix = dbNamePrefix
        twig("Creating RustBackend") {
            dbCachePath = File(dbPath, "${dbNamePrefix}${ZcashSdk.DB_CACHE_NAME}").absolutePath
            dbDataPath = File(dbPath, "${dbNamePrefix}${ZcashSdk.DB_DATA_NAME}").absolutePath
            paramDestinationDir = paramsPath
        }
        return this
    }

    fun clear() {
        twig("Deleting databases")
        File(dbCachePath).delete()
        File(dbDataPath).delete()
    }


    //
    // Wrapper Functions
    //

    override fun initDataDb() = initDataDb(dbDataPath)

//    override fun initAccountsTable(extfvks: Array<String>) =
//        initAccountsTableWithKeys(dbDataPath, extfvks)

    override fun initAccountsTable(
        seed: ByteArray,
        numberOfAccounts: Int
    ) = initAccountsTable(dbDataPath, seed, numberOfAccounts)

    override fun initBlocksTable(
        height: Int,
        hash: String,
        time: Long,
        saplingTree: String
    ): Boolean {
        birthdayHeight = height
        return initBlocksTable(dbDataPath, height, hash, time, saplingTree)
    }

    override fun getAddress(account: Int) = getAddress(dbDataPath, account)

    override fun getBalance(account: Int) = getBalance(dbDataPath, account)

    override fun getVerifiedBalance(account: Int) = getVerifiedBalance(dbDataPath, account)

    override fun getReceivedMemoAsUtf8(idNote: Long) =
        getReceivedMemoAsUtf8(dbDataPath, idNote)

    override fun getSentMemoAsUtf8(idNote: Long) = getSentMemoAsUtf8(dbDataPath, idNote)

    override fun validateCombinedChain() = validateCombinedChain(dbCachePath, dbDataPath)

    override fun rewindToHeight(height: Int) = rewindToHeight(dbDataPath, height)

    override fun scanBlocks(limit: Int): Boolean {
        return if (limit > 0) {
            scanBlockBatch(dbCachePath, dbDataPath, limit)
        } else {
            scanBlocks(dbCachePath, dbDataPath)
        }
    }

    override fun createToAddress(
        account: Int,
        extsk: String,
        to: String,
        value: Long,
        memo: ByteArray?
    ): Long = createToAddress(
        dbDataPath,
        account,
        extsk,
        to,
        value,
        memo ?: ByteArray(0),
        "${paramDestinationDir}/$SPEND_PARAM_FILE_NAME",
        "${paramDestinationDir}/$OUTPUT_PARAM_FILE_NAME"
    )

    override fun deriveSpendingKeys(seed: ByteArray, numberOfAccounts: Int) =
        deriveExtendedSpendingKeys(seed, numberOfAccounts)

    override fun deriveViewingKeys(seed: ByteArray, numberOfAccounts: Int) =
        deriveExtendedFullViewingKeys(seed, numberOfAccounts)

    override fun deriveViewingKey(spendingKey: String) = deriveExtendedFullViewingKey(spendingKey)

    override fun isValidShieldedAddr(addr: String) =
        isValidShieldedAddress(addr)

    override fun isValidTransparentAddr(addr: String) =
        isValidTransparentAddress(addr)

    /**
     * Exposes all of the librustzcash functions along with helpers for loading the static library.
     */
    companion object {
        private var loaded = false

        fun load() {
            // It is safe to call these things twice but not efficient. So we add a loose check and
            // ignore the fact that it's not thread-safe.
            if (!loaded) {
                twig("Loading RustBackend") {
                    loadRustLibrary()
                    initLogs()
                }
            }
        }

        /**
         * The first call made to this object in order to load the Rust backend library. All other
         * external function calls will fail if the libraries have not been loaded.
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
        // External Functions
        //

        @JvmStatic private external fun initDataDb(dbDataPath: String): Boolean

        @JvmStatic private external fun initAccountsTable(
            dbDataPath: String,
            seed: ByteArray,
            accounts: Int
        ): Array<String>

//    @JvmStatic private external fun initAccountsTableWithKeys(
//        dbDataPath: String,
//        extfvk: Array<String>
//    )

        @JvmStatic private external fun initBlocksTable(
            dbDataPath: String,
            height: Int,
            hash: String,
            time: Long,
            saplingTree: String
        ): Boolean

        @JvmStatic private external fun getAddress(dbDataPath: String, account: Int): String

        @JvmStatic private external fun isValidShieldedAddress(addr: String): Boolean

        @JvmStatic private external fun isValidTransparentAddress(addr: String): Boolean

        @JvmStatic private external fun getBalance(dbDataPath: String, account: Int): Long

        @JvmStatic private external fun getVerifiedBalance(dbDataPath: String, account: Int): Long

        @JvmStatic private external fun getReceivedMemoAsUtf8(dbDataPath: String, idNote: Long): String

        @JvmStatic private external fun getSentMemoAsUtf8(dbDataPath: String, idNote: Long): String

        @JvmStatic private external fun validateCombinedChain(dbCachePath: String, dbDataPath: String): Int

        @JvmStatic private external fun rewindToHeight(dbDataPath: String, height: Int): Boolean

        @JvmStatic private external fun scanBlocks(dbCachePath: String, dbDataPath: String): Boolean

        @JvmStatic private external fun scanBlockBatch(dbCachePath: String, dbDataPath: String, limit: Int): Boolean

        @JvmStatic private external fun createToAddress(
            dbDataPath: String,
            account: Int,
            extsk: String,
            to: String,
            value: Long,
            memo: ByteArray,
            spendParamsPath: String,
            outputParamsPath: String
        ): Long

        @JvmStatic private external fun initLogs()

        @JvmStatic private external fun deriveExtendedSpendingKeys(seed: ByteArray, numberOfAccounts: Int): Array<String>

        @JvmStatic private external fun deriveExtendedFullViewingKeys(seed: ByteArray, numberOfAccounts: Int): Array<String>

        @JvmStatic private external fun deriveExtendedFullViewingKey(spendingKey: String): String
    }
}