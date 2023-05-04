package cash.z.ecc.android.sdk.jni

import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import java.io.File

/**
 * Contract defining the exposed capabilities of the Rust backend.
 * This is what welds the SDK to the Rust layer.
 * It is not documented because it is not intended to be used, directly.
 * Instead, use the synchronizer or one of its subcomponents.
 */
// TODO [#920]: Tweak RustBackend public APIs to have void return values
// TODO [#920]: https://github.com/zcash/zcash-android-wallet-sdk/issues/920
@Suppress("TooManyFunctions")
internal interface Backend {

    val network: ZcashNetwork

    val saplingParamDir: File

    suspend fun initBlockMetaDb(): Int

    @Suppress("LongParameterList")
    suspend fun createToAddress(
        account: Int,
        unifiedSpendingKey: ByteArray,
        to: String,
        value: Long,
        memo: ByteArray? = byteArrayOf()
    ): Long

    suspend fun shieldToAddress(
        account: Int,
        unifiedSpendingKey: ByteArray,
        memo: ByteArray? = byteArrayOf()
    ): Long

    suspend fun decryptAndStoreTransaction(tx: ByteArray)

    /**
     * @param keys A list of UFVKs to initialize the accounts table with
     */
    suspend fun initAccountsTable(vararg keys: String): Boolean

    suspend fun initBlocksTable(
        checkpointHeight: Long,
        checkpointHash: String,
        checkpointTime: Long,
        checkpointSaplingTree: String,
    ): Boolean

    suspend fun initDataDb(seed: ByteArray?): Int

    suspend fun createAccount(seed: ByteArray): UnifiedSpendingKeyJni

    fun isValidShieldedAddr(addr: String): Boolean

    fun isValidTransparentAddr(addr: String): Boolean

    fun isValidUnifiedAddr(addr: String): Boolean

    suspend fun getCurrentAddress(account: Int): String

    fun getTransparentReceiver(ua: String): String?

    fun getSaplingReceiver(ua: String): String?

    suspend fun listTransparentReceivers(account: Int): List<String>

    suspend fun getBalance(account: Int): Long

    fun getBranchIdForHeight(height: Long): Long

    suspend fun getReceivedMemoAsUtf8(idNote: Long): String?

    suspend fun getSentMemoAsUtf8(idNote: Long): String?

    suspend fun getVerifiedBalance(account: Int): Long

    suspend fun getNearestRewindHeight(height: Long): Long

    suspend fun rewindToHeight(height: Long): Boolean

    suspend fun scanBlocks(limit: Long?): Boolean

    suspend fun writeBlockMetadata(blockMetadata: List<JniBlockMeta>): Boolean

    /**
     * @return The latest height in the CompactBlock cache metadata DB, or Null if no blocks have been cached.
     */
    suspend fun getLatestHeight(): Long?

    suspend fun findBlockMetadata(height: Long): JniBlockMeta?

    suspend fun rewindBlockMetadataToHeight(height: Long)

    /**
     * @param limit The limit provides an efficient way how to restrict the portion of blocks, which will be validated.
     * @return Null if successful. If an error occurs, the height will be the blockheight where the error was detected.
     */
    suspend fun validateCombinedChainOrErrorHeight(limit: Long?): Long?

    suspend fun getVerifiedTransparentBalance(address: String): Long
    suspend fun getTotalTransparentBalance(address: String): Long

    @Suppress("LongParameterList")
    suspend fun putUtxo(
        tAddress: String,
        txId: ByteArray,
        index: Int,
        script: ByteArray,
        value: Long,
        height: BlockHeight
    ): Boolean
}
