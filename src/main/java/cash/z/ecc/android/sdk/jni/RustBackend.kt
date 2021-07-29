package cash.z.ecc.android.sdk.jni

import cash.z.ecc.android.sdk.exception.BirthdayException
import cash.z.ecc.android.sdk.ext.ZcashSdk.OUTPUT_PARAM_FILE_NAME
import cash.z.ecc.android.sdk.ext.ZcashSdk.SPEND_PARAM_FILE_NAME
import cash.z.ecc.android.sdk.ext.twig
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.UnifiedViewingKey
import cash.z.ecc.android.sdk.type.WalletBalance
import cash.z.ecc.android.sdk.type.ZcashNetwork
import java.io.File

/**
 * Serves as the JNI boundary between the Kotlin and Rust layers. Functions in this class should
 * not be called directly by code outside of the SDK. Instead, one of the higher-level components
 * should be used such as Wallet.kt or CompactBlockProcessor.kt.
 */
class RustBackend private constructor() : RustBackendWelding {

    init {
        load()
    }

    // Paths
    lateinit var pathDataDb: String
        internal set
    lateinit var pathCacheDb: String
        internal set
    lateinit var pathParamsDir: String
        internal set

    override lateinit var network: ZcashNetwork

    internal var birthdayHeight: Int = -1
        get() = if (field != -1) field else throw BirthdayException.UninitializedBirthdayException
        private set

    fun clear(clearCacheDb: Boolean = true, clearDataDb: Boolean = true) {
        if (clearCacheDb) {
            twig("Deleting the cache database!")
            File(pathCacheDb).delete()
        }
        if (clearDataDb) {
            twig("Deleting the data database!")
            File(pathDataDb).delete()
        }
    }

    //
    // Wrapper Functions
    //

    override fun initDataDb() = initDataDb(pathDataDb, networkId = network.id)

    override fun initAccountsTable(vararg keys: UnifiedViewingKey): Boolean {
        val extfvks = Array(keys.size) { "" }
        val extpubs = Array(keys.size) { "" }
        keys.forEachIndexed { i, key ->
            extfvks[i] = key.extfvk
            extpubs[i] = key.extpub
        }
        return initAccountsTableWithKeys(pathDataDb, extfvks, extpubs, networkId = network.id)
    }

    override fun initAccountsTable(
        seed: ByteArray,
        numberOfAccounts: Int
    ): Array<UnifiedViewingKey> {
        return DerivationTool.deriveUnifiedViewingKeys(seed, network, numberOfAccounts).apply {
            initAccountsTable(*this)
        }
    }

    override fun initBlocksTable(
        height: Int,
        hash: String,
        time: Long,
        saplingTree: String
    ): Boolean {
        return initBlocksTable(pathDataDb, height, hash, time, saplingTree, networkId = network.id)
    }

    override fun getShieldedAddress(account: Int) = getShieldedAddress(pathDataDb, account, networkId = network.id)

    override fun getTransparentAddress(account: Int, index: Int): String {
        throw NotImplementedError("TODO: implement this at the zcash_client_sqlite level. But for now, use DerivationTool, instead to derive addresses from seeds")
    }

    override fun getBalance(account: Int) = getBalance(pathDataDb, account, networkId = network.id)

    override fun getVerifiedBalance(account: Int) = getVerifiedBalance(pathDataDb, account, networkId = network.id)

    override fun getReceivedMemoAsUtf8(idNote: Long) =
        getReceivedMemoAsUtf8(pathDataDb, idNote, networkId = network.id)

    override fun getSentMemoAsUtf8(idNote: Long) = getSentMemoAsUtf8(pathDataDb, idNote, networkId = network.id)

    override fun validateCombinedChain() = validateCombinedChain(pathCacheDb, pathDataDb, networkId = network.id,)

    override fun getNearestRewindHeight(height: Int): Int = getNearestRewindHeight(pathDataDb, height, networkId = network.id)

    /**
     * Deletes data for all blocks above the given height. Boils down to:
     *
     * DELETE FROM blocks WHERE height > ?
     */
    override fun rewindToHeight(height: Int) = rewindToHeight(pathDataDb, height, networkId = network.id)

    override fun scanBlocks(limit: Int): Boolean {
        return if (limit > 0) {
            scanBlockBatch(pathCacheDb, pathDataDb, limit, networkId = network.id)
        } else {
            scanBlocks(pathCacheDb, pathDataDb, networkId = network.id)
        }
    }

    override fun decryptAndStoreTransaction(tx: ByteArray) = decryptAndStoreTransaction(pathDataDb, tx, networkId = network.id)

    override fun createToAddress(
        consensusBranchId: Long,
        account: Int,
        extsk: String,
        to: String,
        value: Long,
        memo: ByteArray?
    ): Long = createToAddress(
        pathDataDb,
        consensusBranchId,
        account,
        extsk,
        to,
        value,
        memo ?: ByteArray(0),
        "$pathParamsDir/$SPEND_PARAM_FILE_NAME",
        "$pathParamsDir/$OUTPUT_PARAM_FILE_NAME",
        networkId = network.id,
    )

    override fun shieldToAddress(
        extsk: String,
        tsk: String,
        memo: ByteArray?
    ): Long {
        twig("TMP: shieldToAddress with db path: $pathDataDb, ${memo?.size}")
        return shieldToAddress(
            pathDataDb,
            0,
            extsk,
            tsk,
            memo ?: ByteArray(0),
            "$pathParamsDir/$SPEND_PARAM_FILE_NAME",
            "$pathParamsDir/$OUTPUT_PARAM_FILE_NAME",
            networkId = network.id,
        )
    }

    override fun putUtxo(
        tAddress: String,
        txId: ByteArray,
        index: Int,
        script: ByteArray,
        value: Long,
        height: Int
    ): Boolean = putUtxo(pathDataDb, tAddress, txId, index, script, value, height, networkId = network.id)

    override fun clearUtxos(
        tAddress: String,
        aboveHeight: Int,
    ): Boolean = clearUtxos(pathDataDb, tAddress, aboveHeight, networkId = network.id)

    override fun getDownloadedUtxoBalance(address: String): WalletBalance {
        val verified = getVerifiedTransparentBalance(pathDataDb, address, networkId = network.id)
        val total = getTotalTransparentBalance(pathDataDb, address, networkId = network.id)
        return WalletBalance(total, verified)
    }

    override fun isValidShieldedAddr(addr: String) = isValidShieldedAddress(addr, networkId = network.id)

    override fun isValidTransparentAddr(addr: String) = isValidTransparentAddress(addr, networkId = network.id)

    override fun getBranchIdForHeight(height: Int): Long = branchIdForHeight(height, networkId = network.id)

//    /**
//     * This is a proof-of-concept for doing Local RPC, where we are effectively using the JNI
//     * boundary as a grpc server. It is slightly inefficient in terms of both space and time but
//     * given that it is all done locally, on the heap, it seems to be a worthwhile tradeoff because
//     * it reduces the complexity and expands the capacity for the two layers to communicate.
//     *
//     * We're able to keep the "unsafe" byteArray functions private and wrap them in typeSafe
//     * equivalents and, eventually, surface any parse errors (for now, errors are only logged).
//     */
//    override fun parseTransactionDataList(tdl: LocalRpcTypes.TransactionDataList): LocalRpcTypes.TransparentTransactionList {
//        return try {
//            // serialize the list, send it over to rust and get back a serialized set of results that we parse out and return
//            return LocalRpcTypes.TransparentTransactionList.parseFrom(parseTransactionDataList(tdl.toByteArray()))
//        } catch (t: Throwable) {
//            twig("ERROR: failed to parse transaction data list due to: $t caused by: ${t.cause}")
//            LocalRpcTypes.TransparentTransactionList.newBuilder().build()
//        }
//    }

    /**
     * Exposes all of the librustzcash functions along with helpers for loading the static library.
     */
    companion object {
        private var loaded = false

        /**
         * Loads the library and initializes path variables. Although it is best to only call this
         * function once, it is idempotent.
         */
        fun init(
            cacheDbPath: String,
            dataDbPath: String,
            paramsPath: String,
            zcashNetwork: ZcashNetwork,
            birthdayHeight: Int? = null
        ): RustBackend {
            return RustBackend().apply {
                pathCacheDb = cacheDbPath
                pathDataDb = dataDbPath
                pathParamsDir = paramsPath
                network = zcashNetwork
                if (birthdayHeight != null) {
                    this.birthdayHeight = birthdayHeight
                }
            }
        }

        fun load() {
            // It is safe to call these things twice but not efficient. So we add a loose check and
            // ignore the fact that it's not thread-safe.
            if (!loaded) {
                twig("Loading RustBackend") {
                    loadRustLibrary()
                }
            }
        }

        /**
         * Forwards Rust logs to logcat. This is a function that is intended for debug purposes. All
         * logs will be tagged with `cash.z.rust.logs`. Typically, a developer would clone
         * librustzcash locally and then modify Cargo.toml in this project to point to their local
         * build (see Cargo.toml for details). From there, they can add any log messages they want
         * and have them surfaced into the Android logging system. By default, this behavior is
         * disabled and this is the function that enables it. Initially only the logs in
         * [src/main/rust/lib.rs] will appear and any additional logs would need to be added by the
         * developer.
         */
        fun enableRustLogs() = initLogs()

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

        @JvmStatic private external fun initDataDb(dbDataPath: String, networkId: Int): Boolean

        @JvmStatic private external fun initAccountsTableWithKeys(
            dbDataPath: String,
            extfvk: Array<out String>,
            extpub: Array<out String>,
            networkId: Int,
        ): Boolean

        @JvmStatic private external fun initBlocksTable(
            dbDataPath: String,
            height: Int,
            hash: String,
            time: Long,
            saplingTree: String,
            networkId: Int,
        ): Boolean

        @JvmStatic
        private external fun getShieldedAddress(
            dbDataPath: String,
            account: Int,
            networkId: Int,
        ): String

        @JvmStatic
        private external fun isValidShieldedAddress(addr: String, networkId: Int): Boolean

        @JvmStatic
        private external fun isValidTransparentAddress(addr: String, networkId: Int): Boolean

        @JvmStatic
        private external fun getBalance(dbDataPath: String, account: Int, networkId: Int): Long

        @JvmStatic
        private external fun getVerifiedBalance(
            dbDataPath: String,
            account: Int,
            networkId: Int,
        ): Long

        @JvmStatic
        private external fun getReceivedMemoAsUtf8(
            dbDataPath: String,
            idNote: Long,
            networkId: Int,
        ): String

        @JvmStatic
        private external fun getSentMemoAsUtf8(
            dbDataPath: String,
            dNote: Long,
            networkId: Int,
        ): String

        @JvmStatic
        private external fun validateCombinedChain(
            dbCachePath: String,
            dbDataPath: String,
            networkId: Int,
        ): Int

        @JvmStatic
        private external fun getNearestRewindHeight(
            dbDataPath: String,
            height: Int,
            networkId: Int,
        ): Int

        @JvmStatic
        private external fun rewindToHeight(
            dbDataPath: String,
            height: Int,
            networkId: Int,
        ): Boolean

        @JvmStatic
        private external fun scanBlocks(
            dbCachePath: String,
            dbDataPath: String,
            networkId: Int,
        ): Boolean

        @JvmStatic
        private external fun scanBlockBatch(
            dbCachePath: String,
            dbDataPath: String,
            limit: Int,
            networkId: Int,
        ): Boolean

        @JvmStatic
        private external fun decryptAndStoreTransaction(
            dbDataPath: String,
            tx: ByteArray,
            networkId: Int,
        )

        @JvmStatic private external fun createToAddress(
            dbDataPath: String,
            consensusBranchId: Long,
            account: Int,
            extsk: String,
            to: String,
            value: Long,
            memo: ByteArray,
            spendParamsPath: String,
            outputParamsPath: String,
            networkId: Int,
        ): Long

        @JvmStatic private external fun shieldToAddress(
            dbDataPath: String,
            account: Int,
            extsk: String,
            tsk: String,
            memo: ByteArray,
            spendParamsPath: String,
            outputParamsPath: String,
            networkId: Int,
        ): Long

        @JvmStatic private external fun initLogs()

        @JvmStatic private external fun branchIdForHeight(height: Int, networkId: Int): Long

        @JvmStatic private external fun putUtxo(
            dbDataPath: String,
            tAddress: String,
            txId: ByteArray,
            index: Int,
            script: ByteArray,
            value: Long,
            height: Int,
            networkId: Int,
        ): Boolean

        @JvmStatic private external fun clearUtxos(
            dbDataPath: String,
            tAddress: String,
            aboveHeight: Int,
            networkId: Int,
        ): Boolean

        @JvmStatic private external fun getVerifiedTransparentBalance(
            pathDataDb: String,
            taddr: String,
            networkId: Int,
        ): Long

        @JvmStatic private external fun getTotalTransparentBalance(
            pathDataDb: String,
            taddr: String,
            networkId: Int,
        ): Long
    }
}
