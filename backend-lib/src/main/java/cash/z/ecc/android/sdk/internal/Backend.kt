package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.internal.model.JniScanRange
import cash.z.ecc.android.sdk.internal.model.JniUnifiedSpendingKey
import java.lang.RuntimeException
import kotlin.jvm.Throws

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
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun initAccountsTable(vararg keys: String)

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun initBlocksTable(
        checkpointHeight: Long,
        checkpointHash: String,
        checkpointTime: Long,
        checkpointSaplingTree: String,
    )

    suspend fun initDataDb(seed: ByteArray?): Int

    suspend fun createAccount(seed: ByteArray): JniUnifiedSpendingKey

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

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun rewindToHeight(height: Long)

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun suggestScanRanges(): List<JniScanRange>

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun scanBlocks(fromHeight: Long, limit: Long)

    /**
     * @throws RuntimeException as a common indicator of the operation failure
     */
    @Throws(RuntimeException::class)
    suspend fun writeBlockMetadata(blockMetadata: List<JniBlockMeta>)

    /**
     * @return The latest height in the CompactBlock cache metadata DB, or Null if no blocks have been cached.
     */
    suspend fun getLatestHeight(): Long?

    suspend fun findBlockMetadata(height: Long): JniBlockMeta?

    suspend fun rewindBlockMetadataToHeight(height: Long)

    suspend fun getVerifiedTransparentBalance(address: String): Long
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
