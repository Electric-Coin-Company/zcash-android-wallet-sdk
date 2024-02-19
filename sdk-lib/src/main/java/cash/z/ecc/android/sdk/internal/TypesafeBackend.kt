package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.internal.model.ScanRange
import cash.z.ecc.android.sdk.internal.model.ScanSummary
import cash.z.ecc.android.sdk.internal.model.SubtreeRoot
import cash.z.ecc.android.sdk.internal.model.TreeState
import cash.z.ecc.android.sdk.internal.model.WalletSummary
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.Proposal
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork

@Suppress("TooManyFunctions")
internal interface TypesafeBackend {
    val network: ZcashNetwork

    suspend fun createAccountAndGetSpendingKey(
        seed: ByteArray,
        treeState: TreeState,
        recoverUntil: BlockHeight?
    ): UnifiedSpendingKey

    @Suppress("LongParameterList")
    suspend fun proposeTransfer(
        account: Account,
        to: String,
        value: Long,
        memo: ByteArray? = byteArrayOf()
    ): Proposal

    suspend fun proposeShielding(
        account: Account,
        memo: ByteArray? = byteArrayOf()
    ): Proposal

    suspend fun createProposedTransaction(
        proposal: Proposal,
        usk: UnifiedSpendingKey
    ): FirstClassByteArray

    suspend fun getCurrentAddress(account: Account): String

    suspend fun listTransparentReceivers(account: Account): List<String>

    fun getBranchIdForHeight(height: BlockHeight): Long

    suspend fun getNearestRewindHeight(height: BlockHeight): BlockHeight

    suspend fun rewindToHeight(height: BlockHeight)

    suspend fun getLatestCacheHeight(): BlockHeight?

    suspend fun findBlockMetadata(height: BlockHeight): JniBlockMeta?

    suspend fun rewindBlockMetadataToHeight(height: BlockHeight)

    suspend fun getDownloadedUtxoBalance(address: String): Zatoshi

    @Suppress("LongParameterList")
    suspend fun putUtxo(
        tAddress: String,
        txId: ByteArray,
        index: Int,
        script: ByteArray,
        value: Long,
        height: BlockHeight
    )

    suspend fun getMemoAsUtf8(
        txId: ByteArray,
        outputIndex: Int
    ): String?

    suspend fun initDataDb(seed: ByteArray?): Int

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun putSaplingSubtreeRoots(
        startIndex: UInt,
        roots: List<SubtreeRoot>,
    )

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun updateChainTip(height: BlockHeight)

    /**
     * Returns the height to which the wallet has been fully scanned.
     *
     * This is the height for which the wallet has fully trial-decrypted this and all
     * preceding blocks above the wallet's birthday height.
     *
     * @return The height to which the wallet has been fully scanned, or Null if no blocks have been scanned.
     * @throws RuntimeException as a common indicator of the operation failure
     */
    suspend fun getFullyScannedHeight(): BlockHeight?

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
    suspend fun getMaxScannedHeight(): BlockHeight?

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun scanBlocks(
        fromHeight: BlockHeight,
        limit: Long
    ): ScanSummary

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun getWalletSummary(): WalletSummary?

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun suggestScanRanges(): List<ScanRange>

    suspend fun decryptAndStoreTransaction(tx: ByteArray)

    fun getSaplingReceiver(ua: String): String?

    fun getTransparentReceiver(ua: String): String?

    suspend fun initBlockMetaDb(): Int

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun writeBlockMetadata(blockMetadata: List<JniBlockMeta>)

    fun isValidSaplingAddr(addr: String): Boolean

    fun isValidTransparentAddr(addr: String): Boolean

    fun isValidUnifiedAddr(addr: String): Boolean
}
