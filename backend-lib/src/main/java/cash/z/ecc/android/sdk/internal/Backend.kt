package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.internal.model.JniScanRange
import cash.z.ecc.android.sdk.internal.model.JniScanSummary
import cash.z.ecc.android.sdk.internal.model.JniSubtreeRoot
import cash.z.ecc.android.sdk.internal.model.JniUnifiedSpendingKey
import cash.z.ecc.android.sdk.internal.model.JniWalletSummary
import cash.z.ecc.android.sdk.internal.model.ProposalUnsafe

/**
 * Contract defining the exposed capabilities of the Rust backend.
 * This is what welds the SDK to the Rust layer.
 * It is not documented because it is not intended to be used, directly.
 * Instead, use the synchronizer or one of its subcomponents.
 */
@Suppress("TooManyFunctions")
interface Backend {
    val networkId: Int

    suspend fun initBlockMetaDb(): Int

    @Suppress("LongParameterList")
    suspend fun proposeTransfer(
        account: Int,
        to: String,
        value: Long,
        memo: ByteArray? = byteArrayOf()
    ): ProposalUnsafe

    suspend fun proposeShielding(
        account: Int,
        shieldingThreshold: Long,
        memo: ByteArray? = byteArrayOf(),
        transparentReceiver: String? = null
    ): ProposalUnsafe?

    suspend fun createProposedTransactions(
        proposal: ProposalUnsafe,
        unifiedSpendingKey: ByteArray
    ): List<ByteArray>

    suspend fun decryptAndStoreTransaction(tx: ByteArray)

    /**
     * Sets up the internal structure of the data database.
     *
     * If `seed` is `null`, database migrations will be attempted without it.
     *
     * @return 0 if successful, 1 if the seed must be provided in order to execute the
     *         requested migrations, 2 if the provided seed is not relevant to any of the
     *         derived accounts in the wallet.
     *
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun initDataDb(seed: ByteArray?): Int

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun createAccount(
        seed: ByteArray,
        treeState: ByteArray,
        recoverUntil: Long?
    ): JniUnifiedSpendingKey

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun isSeedRelevantToAnyDerivedAccounts(seed: ByteArray): Boolean

    fun isValidSaplingAddr(addr: String): Boolean

    fun isValidTransparentAddr(addr: String): Boolean

    fun isValidUnifiedAddr(addr: String): Boolean

    suspend fun getCurrentAddress(account: Int): String

    fun getTransparentReceiver(ua: String): String?

    fun getSaplingReceiver(ua: String): String?

    suspend fun listTransparentReceivers(account: Int): List<String>

    fun getBranchIdForHeight(height: Long): Long

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun getMemoAsUtf8(
        txId: ByteArray,
        protocol: Int,
        outputIndex: Int
    ): String?

    suspend fun getNearestRewindHeight(height: Long): Long

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun rewindToHeight(height: Long)

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun putSubtreeRoots(
        saplingStartIndex: Long,
        saplingRoots: List<JniSubtreeRoot>,
        orchardStartIndex: Long,
        orchardRoots: List<JniSubtreeRoot>,
    )

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun updateChainTip(height: Long)

    /**
     * Returns the height to which the wallet has been fully scanned.
     *
     * This is the height for which the wallet has fully trial-decrypted this and all
     * preceding blocks above the wallet's birthday height.
     *
     * @return The height to which the wallet has been fully scanned, or Null if no blocks have been scanned.
     * @throws RuntimeException as a common indicator of the operation failure
     */
    suspend fun getFullyScannedHeight(): Long?

    /**
     * Returns the maximum height that the wallet has scanned.
     *
     * If the wallet is fully synced, this will be equivalent to `getFullyScannedHeight`;
     * otherwise the maximal scanned height is likely to be greater than the fully scanned
     * height due to the fact that out-of-order scanning can leave gaps.
     *
     * @return The maximum height that the wallet has scanned, or Null if no blocks have been scanned.
     * @throws RuntimeException as a common indicator of the operation failure
     */
    suspend fun getMaxScannedHeight(): Long?

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun getWalletSummary(): JniWalletSummary?

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun suggestScanRanges(): List<JniScanRange>

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun scanBlocks(
        fromHeight: Long,
        fromState: ByteArray,
        limit: Long
    ): JniScanSummary

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun writeBlockMetadata(blockMetadata: List<JniBlockMeta>)

    /**
     * @return The latest height in the CompactBlock cache metadata DB, or Null if no blocks have been cached.
     */
    suspend fun getLatestCacheHeight(): Long?

    suspend fun findBlockMetadata(height: Long): JniBlockMeta?

    suspend fun rewindBlockMetadataToHeight(height: Long)

    suspend fun getTotalTransparentBalance(address: String): Long

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Suppress("LongParameterList")
    @Throws(RuntimeException::class)
    suspend fun putUtxo(
        tAddress: String,
        txId: ByteArray,
        index: Int,
        script: ByteArray,
        value: Long,
        height: Long
    )
}
