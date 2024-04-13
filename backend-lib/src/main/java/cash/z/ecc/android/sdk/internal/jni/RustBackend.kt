package cash.z.ecc.android.sdk.internal.jni

import cash.z.ecc.android.sdk.internal.Backend
import cash.z.ecc.android.sdk.internal.SdkDispatchers
import cash.z.ecc.android.sdk.internal.ext.deleteRecursivelySuspend
import cash.z.ecc.android.sdk.internal.ext.deleteSuspend
import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.internal.model.JniScanRange
import cash.z.ecc.android.sdk.internal.model.JniScanSummary
import cash.z.ecc.android.sdk.internal.model.JniSubtreeRoot
import cash.z.ecc.android.sdk.internal.model.JniUnifiedSpendingKey
import cash.z.ecc.android.sdk.internal.model.JniWalletSummary
import cash.z.ecc.android.sdk.internal.model.ProposalUnsafe
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
    suspend fun clear(
        clearCache: Boolean = true,
        clearDataDb: Boolean = true
    ): Boolean {
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

    override suspend fun initBlockMetaDb() =
        withContext(SdkDispatchers.DATABASE_IO) {
            initBlockMetaDb(
                fsBlockDbRoot.absolutePath,
            )
        }

    override suspend fun initDataDb(seed: ByteArray?) =
        withContext(SdkDispatchers.DATABASE_IO) {
            initDataDb(
                dataDbFile.absolutePath,
                seed,
                networkId = networkId
            )
        }

    override suspend fun createAccount(
        seed: ByteArray,
        treeState: ByteArray,
        recoverUntil: Long?
    ): JniUnifiedSpendingKey {
        return withContext(SdkDispatchers.DATABASE_IO) {
            createAccount(
                dataDbFile.absolutePath,
                seed,
                treeState,
                recoverUntil ?: -1,
                networkId = networkId
            )
        }
    }

    override suspend fun isSeedRelevantToAnyDerivedAccounts(seed: ByteArray): Boolean =
        withContext(SdkDispatchers.DATABASE_IO) {
            isSeedRelevantToAnyDerivedAccounts(
                dataDbFile.absolutePath,
                seed,
                networkId = networkId
            )
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

    override suspend fun getMemoAsUtf8(
        txId: ByteArray,
        protocol: Int,
        outputIndex: Int
    ) = withContext(SdkDispatchers.DATABASE_IO) {
        getMemoAsUtf8(
            dataDbFile.absolutePath,
            txId,
            protocol,
            outputIndex,
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

    override suspend fun getLatestCacheHeight() =
        withContext(SdkDispatchers.DATABASE_IO) {
            val height = getLatestCacheHeight(fsBlockDbRoot.absolutePath)

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

    override suspend fun putSubtreeRoots(
        saplingStartIndex: Long,
        saplingRoots: List<JniSubtreeRoot>,
        orchardStartIndex: Long,
        orchardRoots: List<JniSubtreeRoot>,
    ) = withContext(SdkDispatchers.DATABASE_IO) {
        putSubtreeRoots(
            dataDbFile.absolutePath,
            saplingStartIndex,
            saplingRoots.toTypedArray(),
            orchardStartIndex,
            orchardRoots.toTypedArray(),
            networkId = networkId
        )
    }

    override suspend fun updateChainTip(height: Long) =
        withContext(SdkDispatchers.DATABASE_IO) {
            updateChainTip(
                dataDbFile.absolutePath,
                height,
                networkId = networkId
            )
        }

    override suspend fun getFullyScannedHeight() =
        withContext(SdkDispatchers.DATABASE_IO) {
            val height =
                getFullyScannedHeight(
                    dataDbFile.absolutePath,
                    networkId = networkId
                )

            if (-1L == height) {
                null
            } else {
                height
            }
        }

    override suspend fun getMaxScannedHeight() =
        withContext(SdkDispatchers.DATABASE_IO) {
            val height =
                getMaxScannedHeight(
                    dataDbFile.absolutePath,
                    networkId = networkId
                )

            if (-1L == height) {
                null
            } else {
                height
            }
        }

    override suspend fun getWalletSummary(): JniWalletSummary? =
        withContext(SdkDispatchers.DATABASE_IO) {
            getWalletSummary(
                dataDbFile.absolutePath,
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

    override suspend fun scanBlocks(
        fromHeight: Long,
        fromState: ByteArray,
        limit: Long
    ): JniScanSummary {
        return withContext(SdkDispatchers.DATABASE_IO) {
            scanBlocks(
                fsBlockDbRoot.absolutePath,
                dataDbFile.absolutePath,
                fromHeight,
                fromState,
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

    override suspend fun proposeTransfer(
        account: Int,
        to: String,
        value: Long,
        memo: ByteArray?
    ): ProposalUnsafe =
        withContext(SdkDispatchers.DATABASE_IO) {
            ProposalUnsafe.parse(
                proposeTransfer(
                    dataDbFile.absolutePath,
                    account,
                    to,
                    value,
                    memo ?: ByteArray(0),
                    networkId = networkId,
                    useZip317Fees = IS_USE_ZIP_317_FEES
                )
            )
        }

    override suspend fun proposeShielding(
        account: Int,
        shieldingThreshold: Long,
        memo: ByteArray?,
        transparentReceiver: String?
    ): ProposalUnsafe? {
        return withContext(SdkDispatchers.DATABASE_IO) {
            proposeShielding(
                dataDbFile.absolutePath,
                account,
                shieldingThreshold,
                memo ?: ByteArray(0),
                transparentReceiver,
                networkId = networkId,
                useZip317Fees = IS_USE_ZIP_317_FEES
            )?.let {
                ProposalUnsafe.parse(
                    it
                )
            }
        }
    }

    override suspend fun createProposedTransactions(
        proposal: ProposalUnsafe,
        unifiedSpendingKey: ByteArray
    ): List<ByteArray> =
        withContext(SdkDispatchers.DATABASE_IO) {
            createProposedTransactions(
                dataDbFile.absolutePath,
                proposal.toByteArray(),
                unifiedSpendingKey,
                spendParamsPath = saplingSpendFile.absolutePath,
                outputParamsPath = saplingOutputFile.absolutePath,
                networkId = networkId
            ).asList()
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

    override fun isValidSaplingAddr(addr: String) = isValidSaplingAddress(addr, networkId = networkId)

    override fun isValidTransparentAddr(addr: String) = isValidTransparentAddress(addr, networkId = networkId)

    override fun isValidUnifiedAddr(addr: String) = isValidUnifiedAddress(addr, networkId = networkId)

    override fun getBranchIdForHeight(height: Long): Long = branchIdForHeight(height, networkId = networkId)

    /**
     * Exposes all of the librustzcash functions along with helpers for loading the static library.
     */
    companion object {
        internal val rustLibraryLoader = NativeLibraryLoader("zcashwalletsdk")
        private const val IS_USE_ZIP_317_FEES = true

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
        private external fun initDataDb(
            dbDataPath: String,
            seed: ByteArray?,
            networkId: Int
        ): Int

        @JvmStatic
        private external fun createAccount(
            dbDataPath: String,
            seed: ByteArray,
            treeState: ByteArray,
            recoverUntil: Long,
            networkId: Int
        ): JniUnifiedSpendingKey

        @JvmStatic
        private external fun isSeedRelevantToAnyDerivedAccounts(
            dbDataPath: String,
            seed: ByteArray,
            networkId: Int
        ): Boolean

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
        private external fun listTransparentReceivers(
            dbDataPath: String,
            account: Int,
            networkId: Int
        ): Array<String>

        fun validateUnifiedSpendingKey(bytes: ByteArray) = isValidSpendingKey(bytes)

        @JvmStatic
        private external fun isValidSpendingKey(bytes: ByteArray): Boolean

        @JvmStatic
        private external fun isValidSaplingAddress(
            addr: String,
            networkId: Int
        ): Boolean

        @JvmStatic
        private external fun isValidTransparentAddress(
            addr: String,
            networkId: Int
        ): Boolean

        @JvmStatic
        private external fun isValidUnifiedAddress(
            addr: String,
            networkId: Int
        ): Boolean

        @JvmStatic
        private external fun getMemoAsUtf8(
            dbDataPath: String,
            txId: ByteArray,
            poolType: Int,
            outputIndex: Int,
            networkId: Int
        ): String?

        @JvmStatic
        private external fun writeBlockMetadata(
            dbCachePath: String,
            blockMeta: Array<JniBlockMeta>
        )

        @JvmStatic
        private external fun getLatestCacheHeight(dbCachePath: String): Long

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
        @Suppress("LongParameterList")
        private external fun putSubtreeRoots(
            dbDataPath: String,
            saplingStartIndex: Long,
            saplingRoots: Array<JniSubtreeRoot>,
            orchardStartIndex: Long,
            orchardRoots: Array<JniSubtreeRoot>,
            networkId: Int
        )

        @JvmStatic
        private external fun updateChainTip(
            dbDataPath: String,
            height: Long,
            networkId: Int
        )

        @JvmStatic
        private external fun getFullyScannedHeight(
            dbDataPath: String,
            networkId: Int
        ): Long

        @JvmStatic
        private external fun getMaxScannedHeight(
            dbDataPath: String,
            networkId: Int
        ): Long

        @JvmStatic
        private external fun getWalletSummary(
            dbDataPath: String,
            networkId: Int
        ): JniWalletSummary?

        @JvmStatic
        private external fun suggestScanRanges(
            dbDataPath: String,
            networkId: Int
        ): Array<JniScanRange>

        @JvmStatic
        @Suppress("LongParameterList")
        private external fun scanBlocks(
            dbCachePath: String,
            dbDataPath: String,
            fromHeight: Long,
            fromState: ByteArray,
            limit: Long,
            networkId: Int
        ): JniScanSummary

        @JvmStatic
        private external fun decryptAndStoreTransaction(
            dbDataPath: String,
            tx: ByteArray,
            networkId: Int
        )

        @JvmStatic
        @Suppress("LongParameterList")
        private external fun proposeTransfer(
            dbDataPath: String,
            account: Int,
            to: String,
            value: Long,
            memo: ByteArray,
            networkId: Int,
            useZip317Fees: Boolean
        ): ByteArray

        @JvmStatic
        @Suppress("LongParameterList")
        private external fun proposeShielding(
            dbDataPath: String,
            account: Int,
            shieldingThreshold: Long,
            memo: ByteArray,
            transparentReceiver: String?,
            networkId: Int,
            useZip317Fees: Boolean
        ): ByteArray?

        @JvmStatic
        @Suppress("LongParameterList")
        private external fun createProposedTransactions(
            dbDataPath: String,
            proposal: ByteArray,
            usk: ByteArray,
            spendParamsPath: String,
            outputParamsPath: String,
            networkId: Int
        ): Array<ByteArray>

        @JvmStatic
        private external fun branchIdForHeight(
            height: Long,
            networkId: Int
        ): Long

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
        private external fun getTotalTransparentBalance(
            pathDataDb: String,
            taddr: String,
            networkId: Int
        ): Long
    }
}
