package cash.z.ecc.android.sdk.internal.jni

import cash.z.ecc.android.sdk.internal.Backend
import cash.z.ecc.android.sdk.internal.SdkDispatchers
import cash.z.ecc.android.sdk.internal.ext.deleteRecursivelySuspend
import cash.z.ecc.android.sdk.internal.ext.deleteSuspend
import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.internal.model.JniUnifiedSpendingKey
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Serves as the JNI boundary between the Kotlin and Rust layers. Functions in this class should
 * not be called directly by code outside of the SDK. Instead, one of the higher-level components
 * should be used such as WalletCoordinator.kt or CompactBlockProcessor.kt.
 */
@Suppress("TooManyFunctions")
class RustBackend private constructor(
    override val networkId: Int,
    private val dataDbFile: File,
    private val fsBlockDbRoot: File,
    private val saplingSpendFile: File,
    private val saplingOutputFile: File,
) : Backend {

    /**
     * This function deletes the data database file and the cache directory (compact blocks files) if set by input
     * parameters.
     *
     * @param clearCache to request the cache directory and its content deletion
     * @param clearDataDb to request the data database file deletion
     *
     * @return false in case of any required and failed deletion, true otherwise.
     */
    suspend fun clear(clearCache: Boolean = true, clearDataDb: Boolean = true): Boolean {
        var cacheClearResult = true
        var dataClearResult = true
        if (clearCache) {
            fsBlockDbRoot.deleteRecursivelySuspend().also { result ->
                cacheClearResult = result
            }
        }
        if (clearDataDb) {
            dataDbFile.deleteSuspend().also { result ->
                dataClearResult = result
            }
        }
        return cacheClearResult && dataClearResult
    }

    //
    // Wrapper Functions
    //

    override suspend fun initBlockMetaDb() = withContext(SdkDispatchers.DATABASE_IO) {
        initBlockMetaDb(
            fsBlockDbRoot.absolutePath,
        )
    }

    override suspend fun initDataDb(seed: ByteArray?) = withContext(SdkDispatchers.DATABASE_IO) {
        initDataDb(
            dataDbFile.absolutePath,
            seed,
            networkId = networkId
        )
    }

    override suspend fun createAccount(seed: ByteArray): JniUnifiedSpendingKey {
        return withContext(SdkDispatchers.DATABASE_IO) {
            createAccount(
                dataDbFile.absolutePath,
                seed,
                networkId = networkId
            )
        }
    }

    /**
     * @param keys A list of UFVKs to initialize the accounts table with
     */
    override suspend fun initAccountsTable(vararg keys: String) {
        return withContext(SdkDispatchers.DATABASE_IO) {
            initAccountsTableWithKeys(
                dataDbFile.absolutePath,
                keys,
                networkId = networkId
            )
        }
    }

    override suspend fun initBlocksTable(
        checkpointHeight: Long,
        checkpointHash: String,
        checkpointTime: Long,
        checkpointSaplingTree: String,
    ) {
        return withContext(SdkDispatchers.DATABASE_IO) {
            initBlocksTable(
                dataDbFile.absolutePath,
                checkpointHeight,
                checkpointHash,
                checkpointTime,
                checkpointSaplingTree,
                networkId = networkId
            )
        }
    }

    override suspend fun getCurrentAddress(account: Int) =
        withContext(SdkDispatchers.DATABASE_IO) {
            getCurrentAddress(
                dataDbFile.absolutePath,
                account,
                networkId = networkId
            )
        }

    override fun getTransparentReceiver(ua: String) = getTransparentReceiverForUnifiedAddress(ua)

    override fun getSaplingReceiver(ua: String) = getSaplingReceiverForUnifiedAddress(ua)

    override suspend fun listTransparentReceivers(account: Int): List<String> {
        return withContext(SdkDispatchers.DATABASE_IO) {
            listTransparentReceivers(
                dbDataPath = dataDbFile.absolutePath,
                account = account,
                networkId = networkId
            ).asList()
        }
    }

    override suspend fun getBalance(account: Int): Long {
        val longValue = withContext(SdkDispatchers.DATABASE_IO) {
            getBalance(
                dataDbFile.absolutePath,
                account,
                networkId = networkId
            )
        }

        return longValue
    }

    override suspend fun getVerifiedBalance(account: Int): Long {
        val longValue = withContext(SdkDispatchers.DATABASE_IO) {
            getVerifiedBalance(
                dbDataPath = dataDbFile.absolutePath,
                account = account,
                networkId = networkId
            )
        }

        return longValue
    }

    override suspend fun getReceivedMemoAsUtf8(idNote: Long) =
        withContext(SdkDispatchers.DATABASE_IO) {
            getReceivedMemoAsUtf8(
                dataDbFile.absolutePath,
                idNote,
                networkId = networkId
            )
        }

    override suspend fun getSentMemoAsUtf8(idNote: Long) =
        withContext(SdkDispatchers.DATABASE_IO) {
            getSentMemoAsUtf8(
                dataDbFile.absolutePath,
                idNote,
                networkId = networkId
            )
        }

    override suspend fun writeBlockMetadata(blockMetadata: List<JniBlockMeta>) =
        withContext(SdkDispatchers.DATABASE_IO) {
            writeBlockMetadata(
                fsBlockDbRoot.absolutePath,
                blockMetadata.toTypedArray()
            )
        }

    override suspend fun getLatestHeight() =
        withContext(SdkDispatchers.DATABASE_IO) {
            val height = getLatestHeight(fsBlockDbRoot.absolutePath)

            if (-1L == height) {
                null
            } else {
                height
            }
        }

    override suspend fun findBlockMetadata(height: Long) =
        withContext(SdkDispatchers.DATABASE_IO) {
            findBlockMetadata(
                fsBlockDbRoot.absolutePath,
                height
            )
        }

    override suspend fun rewindBlockMetadataToHeight(height: Long) =
        withContext(SdkDispatchers.DATABASE_IO) {
            rewindBlockMetadataToHeight(
                fsBlockDbRoot.absolutePath,
                height
            )
        }

    override suspend fun getVerifiedTransparentBalance(address: String): Long =
        withContext(SdkDispatchers.DATABASE_IO) {
            getVerifiedTransparentBalance(
                dataDbFile.absolutePath,
                address,
                networkId = networkId
            )
        }

    override suspend fun getTotalTransparentBalance(address: String): Long =
        withContext(SdkDispatchers.DATABASE_IO) {
            getTotalTransparentBalance(
                dataDbFile.absolutePath,
                address,
                networkId = networkId
            )
        }

    override suspend fun getNearestRewindHeight(height: Long): Long =
        withContext(SdkDispatchers.DATABASE_IO) {
            getNearestRewindHeight(
                dataDbFile.absolutePath,
                height,
                networkId = networkId
            )
        }

    /**
     * Deletes data for all blocks above the given height. Boils down to:
     *
     * DELETE FROM blocks WHERE height > ?
     */
    override suspend fun rewindToHeight(height: Long) =
        withContext(SdkDispatchers.DATABASE_IO) {
            rewindToHeight(
                dataDbFile.absolutePath,
                height,
                networkId = networkId
            )
        }

    override suspend fun suggestScanRanges(): List<JniScanRange> {
        return withContext(SdkDispatchers.DATABASE_IO) {
            suggestScanRanges(
                dataDbFile.absolutePath,
                networkId = networkId
            ).asList()
        }
    }

    override suspend fun scanBlocks(fromHeight: Long, limit: Long) {
        return withContext(SdkDispatchers.DATABASE_IO) {
            scanBlocks(
                fsBlockDbRoot.absolutePath,
                dataDbFile.absolutePath,
                fromHeight,
                limit,
                networkId = networkId
            )
        }
    }

    override suspend fun decryptAndStoreTransaction(tx: ByteArray) =
        withContext(SdkDispatchers.DATABASE_IO) {
            decryptAndStoreTransaction(
                dataDbFile.absolutePath,
                tx,
                networkId = networkId
            )
        }

    override suspend fun createToAddress(
        account: Int,
        unifiedSpendingKey: ByteArray,
        to: String,
        value: Long,
        memo: ByteArray?
    ): Long = withContext(SdkDispatchers.DATABASE_IO) {
        createToAddress(
            dataDbFile.absolutePath,
            unifiedSpendingKey,
            to,
            value,
            memo ?: ByteArray(0),
            spendParamsPath = saplingSpendFile.absolutePath,
            outputParamsPath = saplingOutputFile.absolutePath,
            networkId = networkId,
            useZip317Fees = IS_USE_ZIP_317_FEES
        )
    }

    override suspend fun shieldToAddress(
        account: Int,
        unifiedSpendingKey: ByteArray,
        memo: ByteArray?
    ): Long {
        return withContext(SdkDispatchers.DATABASE_IO) {
            shieldToAddress(
                dataDbFile.absolutePath,
                unifiedSpendingKey,
                memo ?: ByteArray(0),
                spendParamsPath = saplingSpendFile.absolutePath,
                outputParamsPath = saplingOutputFile.absolutePath,
                networkId = networkId,
                useZip317Fees = IS_USE_ZIP_317_FEES
            )
        }
    }

    override suspend fun putUtxo(
        tAddress: String,
        txId: ByteArray,
        index: Int,
        script: ByteArray,
        value: Long,
        height: Long
    ) = withContext(SdkDispatchers.DATABASE_IO) {
        putUtxo(
            dataDbFile.absolutePath,
            tAddress,
            txId,
            index,
            script,
            value,
            height,
            networkId = networkId
        )
    }

    override fun isValidShieldedAddr(addr: String) =
        isValidShieldedAddress(addr, networkId = networkId)

    override fun isValidTransparentAddr(addr: String) =
        isValidTransparentAddress(addr, networkId = networkId)

    override fun isValidUnifiedAddr(addr: String) =
        isValidUnifiedAddress(addr, networkId = networkId)

    override fun getBranchIdForHeight(height: Long): Long =
        branchIdForHeight(height, networkId = networkId)

    /**
     * Exposes all of the librustzcash functions along with helpers for loading the static library.
     */
    companion object {
        internal val rustLibraryLoader = NativeLibraryLoader("zcashwalletsdk")
        private const val IS_USE_ZIP_317_FEES = false

        suspend fun loadLibrary() {
            rustLibraryLoader.load {
                initOnLoad()
            }
        }

        /**
         * Loads the library and initializes path variables. Although it is best to only call this
         * function once, it is idempotent.
         */
        suspend fun new(
            fsBlockDbRoot: File,
            dataDbFile: File,
            saplingSpendFile: File,
            saplingOutputFile: File,
            zcashNetworkId: Int,
        ): RustBackend {
            loadLibrary()

            return RustBackend(
                zcashNetworkId,
                dataDbFile = dataDbFile,
                fsBlockDbRoot = fsBlockDbRoot,
                saplingSpendFile = saplingSpendFile,
                saplingOutputFile = saplingOutputFile
            )
        }

        //
        // External Functions
        //

        @JvmStatic
        private external fun initOnLoad()

        @JvmStatic
        private external fun initBlockMetaDb(fsBlockDbRoot: String): Int

        @JvmStatic
        private external fun initDataDb(dbDataPath: String, seed: ByteArray?, networkId: Int): Int

        @JvmStatic
        private external fun initAccountsTableWithKeys(
            dbDataPath: String,
            ufvks: Array<out String>,
            networkId: Int
        )

        @JvmStatic
        @Suppress("LongParameterList")
        private external fun initBlocksTable(
            dbDataPath: String,
            height: Long,
            hash: String,
            time: Long,
            saplingTree: String,
            networkId: Int
        )

        @JvmStatic
        private external fun createAccount(dbDataPath: String, seed: ByteArray, networkId: Int): JniUnifiedSpendingKey

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

        @JvmStatic
        private external fun listTransparentReceivers(dbDataPath: String, account: Int, networkId: Int): Array<String>

        fun validateUnifiedSpendingKey(bytes: ByteArray) =
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
        private external fun writeBlockMetadata(
            dbCachePath: String,
            blockMeta: Array<JniBlockMeta>
        )

        @JvmStatic
        private external fun getLatestHeight(dbCachePath: String): Long

        @JvmStatic
        private external fun findBlockMetadata(
            dbCachePath: String,
            height: Long
        ): JniBlockMeta?

        @JvmStatic
        private external fun rewindBlockMetadataToHeight(
            dbCachePath: String,
            height: Long
        )

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
        )

        @JvmStatic
        private external fun suggestScanRanges(
            dbDataPath: String,
            networkId: Int
        ): Array<JniScanRange>

        @JvmStatic
        private external fun scanBlocks(
            dbCachePath: String,
            dbDataPath: String,
            fromHeight: Long,
            limit: Long,
            networkId: Int
        )

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
        )

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
