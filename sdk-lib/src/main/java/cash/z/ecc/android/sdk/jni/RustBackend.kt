package cash.z.ecc.android.sdk.jni

import cash.z.ecc.android.sdk.internal.SaplingParamTool
import cash.z.ecc.android.sdk.internal.SdkDispatchers
import cash.z.ecc.android.sdk.internal.ext.deleteSuspend
import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.UnifiedFullViewingKey
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Serves as the JNI boundary between the Kotlin and Rust layers. Functions in this class should
 * not be called directly by code outside of the SDK. Instead, one of the higher-level components
 * should be used such as Wallet.kt or CompactBlockProcessor.kt.
 */
@Suppress("TooManyFunctions")
internal class RustBackend private constructor(
    override val network: ZcashNetwork,
    val birthdayHeight: BlockHeight,
    val dataDbFile: File,
    val cacheDbFile: File,
    override val saplingParamDir: File
) : RustBackendWelding {

    suspend fun clear(clearCacheDb: Boolean = true, clearDataDb: Boolean = true) {
        if (clearCacheDb) {
            twig("Deleting the cache database!")
            cacheDbFile.deleteSuspend()
        }
        if (clearDataDb) {
            twig("Deleting the data database!")
            dataDbFile.deleteSuspend()
        }
    }

    //
    // Wrapper Functions
    //

    override suspend fun initDataDb(seed: ByteArray?) = withContext(SdkDispatchers.DATABASE_IO) {
        initDataDb(
            dataDbFile.absolutePath,
            seed,
            networkId = network.id
        )
    }

    override suspend fun createAccount(seed: ByteArray): UnifiedSpendingKey {
        return withContext(SdkDispatchers.DATABASE_IO) {
            createAccount(
                dataDbFile.absolutePath,
                seed,
                networkId = network.id
            )
        }
    }

    override suspend fun initAccountsTable(vararg keys: UnifiedFullViewingKey): Boolean {
        val ufvks = Array(keys.size) { keys[it].encoding }

        return withContext(SdkDispatchers.DATABASE_IO) {
            initAccountsTableWithKeys(
                dataDbFile.absolutePath,
                ufvks,
                networkId = network.id
            )
        }
    }

    override suspend fun initAccountsTable(
        seed: ByteArray,
        numberOfAccounts: Int
    ): Array<UnifiedFullViewingKey> {
        return DerivationTool.deriveUnifiedFullViewingKeys(seed, network, numberOfAccounts).apply {
            @Suppress("SpreadOperator")
            initAccountsTable(*this)
        }
    }

    override suspend fun initBlocksTable(
        checkpoint: Checkpoint
    ): Boolean {
        return withContext(SdkDispatchers.DATABASE_IO) {
            initBlocksTable(
                dataDbFile.absolutePath,
                checkpoint.height.value,
                checkpoint.hash,
                checkpoint.epochSeconds,
                checkpoint.tree,
                networkId = network.id
            )
        }
    }

    override suspend fun getCurrentAddress(account: Int) =
        withContext(SdkDispatchers.DATABASE_IO) {
            getCurrentAddress(
                dataDbFile.absolutePath,
                account,
                networkId = network.id
            )
        }

    override fun getTransparentReceiver(ua: String) = getTransparentReceiverForUnifiedAddress(ua)

    override fun getSaplingReceiver(ua: String) = getSaplingReceiverForUnifiedAddress(ua)

    override suspend fun getBalance(account: Int): Zatoshi {
        val longValue = withContext(SdkDispatchers.DATABASE_IO) {
            getBalance(
                dataDbFile.absolutePath,
                account,
                networkId = network.id
            )
        }

        return Zatoshi(longValue)
    }

    override suspend fun getVerifiedBalance(account: Int): Zatoshi {
        val longValue = withContext(SdkDispatchers.DATABASE_IO) {
            getVerifiedBalance(
                dataDbFile.absolutePath,
                account,
                networkId = network.id
            )
        }

        return Zatoshi(longValue)
    }

    override suspend fun getReceivedMemoAsUtf8(idNote: Long) =
        withContext(SdkDispatchers.DATABASE_IO) {
            getReceivedMemoAsUtf8(
                dataDbFile.absolutePath,
                idNote,
                networkId = network.id
            )
        }

    override suspend fun getSentMemoAsUtf8(idNote: Long) = withContext(SdkDispatchers.DATABASE_IO) {
        getSentMemoAsUtf8(
            dataDbFile.absolutePath,
            idNote,
            networkId = network.id
        )
    }

    override suspend fun validateCombinedChain() = withContext(SdkDispatchers.DATABASE_IO) {
        val validationResult = validateCombinedChain(
            cacheDbFile.absolutePath,
            dataDbFile.absolutePath,
            networkId = network.id
        )

        if (-1L == validationResult) {
            null
        } else {
            BlockHeight.new(network, validationResult)
        }
    }

    override suspend fun getNearestRewindHeight(height: BlockHeight): BlockHeight =
        withContext(SdkDispatchers.DATABASE_IO) {
            BlockHeight.new(
                network,
                getNearestRewindHeight(
                    dataDbFile.absolutePath,
                    height.value,
                    networkId = network.id
                )
            )
        }

    /**
     * Deletes data for all blocks above the given height. Boils down to:
     *
     * DELETE FROM blocks WHERE height > ?
     */
    override suspend fun rewindToHeight(height: BlockHeight) =
        withContext(SdkDispatchers.DATABASE_IO) {
            rewindToHeight(
                dataDbFile.absolutePath,
                height.value,
                networkId = network.id
            )
        }

    override suspend fun scanBlocks(limit: Int): Boolean {
        return withContext(SdkDispatchers.DATABASE_IO) {
            scanBlocks(
                cacheDbFile.absolutePath,
                dataDbFile.absolutePath,
                limit,
                networkId = network.id
            )
        }
    }

    override suspend fun decryptAndStoreTransaction(tx: ByteArray) =
        withContext(SdkDispatchers.DATABASE_IO) {
            decryptAndStoreTransaction(
                dataDbFile.absolutePath,
                tx,
                networkId = network.id
            )
        }

    override suspend fun createToAddress(
        usk: UnifiedSpendingKey,
        to: String,
        value: Long,
        memo: ByteArray?
    ): Long = withContext(SdkDispatchers.DATABASE_IO) {
        createToAddress(
            dataDbFile.absolutePath,
            usk.copyBytes(),
            to,
            value,
            memo ?: ByteArray(0),
            File(saplingParamDir, SaplingParamTool.SPEND_PARAM_FILE_NAME).absolutePath,
            File(saplingParamDir, SaplingParamTool.OUTPUT_PARAM_FILE_NAME).absolutePath,
            networkId = network.id,
            useZip317Fees = false
        )
    }

    override suspend fun shieldToAddress(
        usk: UnifiedSpendingKey,
        memo: ByteArray?
    ): Long {
        twig("TMP: shieldToAddress with db path: $dataDbFile, ${memo?.size}")
        return withContext(SdkDispatchers.DATABASE_IO) {
            shieldToAddress(
                dataDbFile.absolutePath,
                usk.copyBytes(),
                memo ?: ByteArray(0),
                File(saplingParamDir, SaplingParamTool.SPEND_PARAM_FILE_NAME).absolutePath,
                File(saplingParamDir, SaplingParamTool.OUTPUT_PARAM_FILE_NAME).absolutePath,
                networkId = network.id,
                useZip317Fees = false
            )
        }
    }

    override suspend fun putUtxo(
        tAddress: String,
        txId: ByteArray,
        index: Int,
        script: ByteArray,
        value: Long,
        height: BlockHeight
    ): Boolean = withContext(SdkDispatchers.DATABASE_IO) {
        putUtxo(
            dataDbFile.absolutePath,
            tAddress,
            txId,
            index,
            script,
            value,
            height.value,
            networkId = network.id
        )
    }

    override suspend fun getDownloadedUtxoBalance(address: String): WalletBalance {
        val verified = withContext(SdkDispatchers.DATABASE_IO) {
            getVerifiedTransparentBalance(
                dataDbFile.absolutePath,
                address,
                networkId = network.id
            )
        }
        val total = withContext(SdkDispatchers.DATABASE_IO) {
            getTotalTransparentBalance(
                dataDbFile.absolutePath,
                address,
                networkId = network.id
            )
        }
        return WalletBalance(Zatoshi(total), Zatoshi(verified))
    }

    override fun isValidShieldedAddr(addr: String) =
        isValidShieldedAddress(addr, networkId = network.id)

    override fun isValidTransparentAddr(addr: String) =
        isValidTransparentAddress(addr, networkId = network.id)

    override fun isValidUnifiedAddr(addr: String) =
        isValidUnifiedAddress(addr, networkId = network.id)

    override fun getBranchIdForHeight(height: BlockHeight): Long =
        branchIdForHeight(height.value, networkId = network.id)

//    /**
//     * This is a proof-of-concept for doing Local RPC, where we are effectively using the JNI
//     * boundary as a grpc server. It is slightly inefficient in terms of both space and time but
//     * given that it is all done locally, on the heap, it seems to be a worthwhile tradeoff because
//     * it reduces the complexity and expands the capacity for the two layers to communicate.
//     *
//     * We're able to keep the "unsafe" byteArray functions private and wrap them in typeSafe
//     * equivalents and, eventually, surface any parse errors (for now, errors are only logged).
//     */
//     override fun parseTransactionDataList(
//         tdl: LocalRpcTypes.TransactionDataList
//     ): LocalRpcTypes.TransparentTransactionList {
//         return try {
//             // serialize the list, send it over to rust and get back a serialized set of results that we parse out
//             // and return
//             return LocalRpcTypes.TransparentTransactionList.parseFrom(parseTransactionDataList(tdl.toByteArray()))
//         } catch (t: Throwable) {
//             twig("ERROR: failed to parse transaction data list due to: $t caused by: ${t.cause}")
//             LocalRpcTypes.TransparentTransactionList.newBuilder().build()
//         }
//     }

    /**
     * Exposes all of the librustzcash functions along with helpers for loading the static library.
     */
    companion object {
        internal val rustLibraryLoader = NativeLibraryLoader("zcashwalletsdk")

        /**
         * Loads the library and initializes path variables. Although it is best to only call this
         * function once, it is idempotent.
         */
        suspend fun init(
            cacheDbFile: File,
            dataDbFile: File,
            saplingParamsDir: File,
            zcashNetwork: ZcashNetwork,
            birthdayHeight: BlockHeight
        ): RustBackend {
            rustLibraryLoader.load()

            return RustBackend(
                zcashNetwork,
                birthdayHeight,
                dataDbFile = dataDbFile,
                cacheDbFile = cacheDbFile,
                saplingParamDir = saplingParamsDir
            )
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

        //
        // External Functions
        //

        @JvmStatic
        private external fun initDataDb(dbDataPath: String, seed: ByteArray?, networkId: Int): Int

        @JvmStatic
        private external fun initAccountsTableWithKeys(
            dbDataPath: String,
            ufvks: Array<out String>,
            networkId: Int
        ): Boolean

        @JvmStatic
        @Suppress("LongParameterList")
        private external fun initBlocksTable(
            dbDataPath: String,
            height: Long,
            hash: String,
            time: Long,
            saplingTree: String,
            networkId: Int
        ): Boolean

        @JvmStatic
        private external fun createAccount(dbDataPath: String, seed: ByteArray, networkId: Int): UnifiedSpendingKey

        @JvmStatic
        private external fun getCurrentAddress(
            dbDataPath: String,
            account: Int,
            networkId: Int
        ): String

        @JvmStatic
        private external fun getTransparentReceiverForUnifiedAddress(ua: String): String?

        @JvmStatic
        private external fun getSaplingReceiverForUnifiedAddress(ua: String): String?

        internal fun validateUnifiedSpendingKey(bytes: ByteArray) =
            isValidSpendingKey(bytes)

        @JvmStatic
        private external fun isValidSpendingKey(bytes: ByteArray): Boolean

        @JvmStatic
        private external fun isValidShieldedAddress(addr: String, networkId: Int): Boolean

        @JvmStatic
        private external fun isValidTransparentAddress(addr: String, networkId: Int): Boolean

        @JvmStatic
        private external fun isValidUnifiedAddress(addr: String, networkId: Int): Boolean

        @JvmStatic
        private external fun getBalance(dbDataPath: String, account: Int, networkId: Int): Long

        @JvmStatic
        private external fun getVerifiedBalance(
            dbDataPath: String,
            account: Int,
            networkId: Int
        ): Long

        @JvmStatic
        private external fun getReceivedMemoAsUtf8(
            dbDataPath: String,
            idNote: Long,
            networkId: Int
        ): String?

        @JvmStatic
        private external fun getSentMemoAsUtf8(
            dbDataPath: String,
            dNote: Long,
            networkId: Int
        ): String?

        @JvmStatic
        private external fun validateCombinedChain(
            dbCachePath: String,
            dbDataPath: String,
            networkId: Int
        ): Long

        @JvmStatic
        private external fun getNearestRewindHeight(
            dbDataPath: String,
            height: Long,
            networkId: Int
        ): Long

        @JvmStatic
        private external fun rewindToHeight(
            dbDataPath: String,
            height: Long,
            networkId: Int
        ): Boolean

        @JvmStatic
        private external fun scanBlocks(
            dbCachePath: String,
            dbDataPath: String,
            limit: Int,
            networkId: Int
        ): Boolean

        @JvmStatic
        private external fun decryptAndStoreTransaction(
            dbDataPath: String,
            tx: ByteArray,
            networkId: Int
        )

        @JvmStatic
        @Suppress("LongParameterList")
        private external fun createToAddress(
            dbDataPath: String,
            usk: ByteArray,
            to: String,
            value: Long,
            memo: ByteArray,
            spendParamsPath: String,
            outputParamsPath: String,
            networkId: Int,
            useZip317Fees: Boolean
        ): Long

        @JvmStatic
        @Suppress("LongParameterList")
        private external fun shieldToAddress(
            dbDataPath: String,
            usk: ByteArray,
            memo: ByteArray,
            spendParamsPath: String,
            outputParamsPath: String,
            networkId: Int,
            useZip317Fees: Boolean
        ): Long

        @JvmStatic
        private external fun initLogs()

        @JvmStatic
        private external fun branchIdForHeight(height: Long, networkId: Int): Long

        @JvmStatic
        @Suppress("LongParameterList")
        private external fun putUtxo(
            dbDataPath: String,
            tAddress: String,
            txId: ByteArray,
            index: Int,
            script: ByteArray,
            value: Long,
            height: Long,
            networkId: Int
        ): Boolean

        @JvmStatic
        private external fun getVerifiedTransparentBalance(
            pathDataDb: String,
            taddr: String,
            networkId: Int
        ): Long

        @JvmStatic
        private external fun getTotalTransparentBalance(
            pathDataDb: String,
            taddr: String,
            networkId: Int
        ): Long
    }
}
