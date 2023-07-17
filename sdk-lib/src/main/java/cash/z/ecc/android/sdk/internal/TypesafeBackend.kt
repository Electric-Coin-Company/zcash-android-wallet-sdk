package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.internal.model.ScanRange
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.UnifiedFullViewingKey
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork

@Suppress("TooManyFunctions")
internal interface TypesafeBackend {

    val network: ZcashNetwork

    suspend fun initAccountsTable(vararg keys: UnifiedFullViewingKey)

    suspend fun initAccountsTable(
        seed: ByteArray,
        numberOfAccounts: Int
    ): List<UnifiedFullViewingKey>

    suspend fun initBlocksTable(checkpoint: Checkpoint)

    suspend fun createAccountAndGetSpendingKey(seed: ByteArray): UnifiedSpendingKey

    @Suppress("LongParameterList")
    suspend fun createToAddress(
        usk: UnifiedSpendingKey,
        to: String,
        value: Long,
        memo: ByteArray? = byteArrayOf()
    ): Long

    suspend fun shieldToAddress(
        usk: UnifiedSpendingKey,
        memo: ByteArray? = byteArrayOf()
    ): Long

    suspend fun getCurrentAddress(account: Account): String

    suspend fun listTransparentReceivers(account: Account): List<String>

    suspend fun getBalance(account: Account): Zatoshi

    fun getBranchIdForHeight(height: BlockHeight): Long

    suspend fun getVerifiedBalance(account: Account): Zatoshi

    suspend fun getNearestRewindHeight(height: BlockHeight): BlockHeight

    suspend fun rewindToHeight(height: BlockHeight)

    suspend fun getLatestBlockHeight(): BlockHeight?

    suspend fun findBlockMetadata(height: BlockHeight): JniBlockMeta?

    suspend fun rewindBlockMetadataToHeight(height: BlockHeight)

    suspend fun getDownloadedUtxoBalance(address: String): WalletBalance

    @Suppress("LongParameterList")
    suspend fun putUtxo(
        tAddress: String,
        txId: ByteArray,
        index: Int,
        script: ByteArray,
        value: Long,
        height: BlockHeight
    )

    suspend fun getSentMemoAsUtf8(idNote: Long): String?

    suspend fun getReceivedMemoAsUtf8(idNote: Long): String?

    suspend fun initDataDb(seed: ByteArray?): Int

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun scanBlocks(fromHeight: BlockHeight, limit: Long)

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

    fun isValidShieldedAddr(addr: String): Boolean

    fun isValidTransparentAddr(addr: String): Boolean

    fun isValidUnifiedAddr(addr: String): Boolean
}
