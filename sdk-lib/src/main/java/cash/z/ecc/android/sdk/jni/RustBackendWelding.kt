package cash.z.ecc.android.sdk.jni

import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.type.UnifiedFullViewingKey
import java.io.File

/**
 * Contract defining the exposed capabilities of the Rust backend.
 * This is what welds the SDK to the Rust layer.
 * It is not documented because it is not intended to be used, directly.
 * Instead, use the synchronizer or one of its subcomponents.
 */
@Suppress("TooManyFunctions")
internal interface RustBackendWelding {

    val network: ZcashNetwork

    val saplingParamDir: File

    suspend fun initBlockMetaDb(): Int

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

    suspend fun decryptAndStoreTransaction(tx: ByteArray)

    suspend fun initAccountsTable(seed: ByteArray, numberOfAccounts: Int): Array<UnifiedFullViewingKey>

    suspend fun initAccountsTable(vararg keys: UnifiedFullViewingKey): Boolean

    suspend fun initBlocksTable(checkpoint: Checkpoint): Boolean

    suspend fun initDataDb(seed: ByteArray?): Int

    suspend fun createAccount(seed: ByteArray): UnifiedSpendingKey

    fun isValidShieldedAddr(addr: String): Boolean

    fun isValidTransparentAddr(addr: String): Boolean

    fun isValidUnifiedAddr(addr: String): Boolean

    suspend fun getCurrentAddress(account: Int = 0): String

    fun getTransparentReceiver(ua: String): String?

    fun getSaplingReceiver(ua: String): String?

    suspend fun getBalance(account: Int = 0): Zatoshi

    fun getBranchIdForHeight(height: BlockHeight): Long

    suspend fun getReceivedMemoAsUtf8(idNote: Long): String?

    suspend fun getSentMemoAsUtf8(idNote: Long): String?

    suspend fun getVerifiedBalance(account: Int = 0): Zatoshi

//    fun parseTransactionDataList(tdl: LocalRpcTypes.TransactionDataList): LocalRpcTypes.TransparentTransactionList

    suspend fun getNearestRewindHeight(height: BlockHeight): BlockHeight

    suspend fun rewindToHeight(height: BlockHeight): Boolean

    suspend fun scanBlocks(limit: Int = -1): Boolean

    suspend fun writeBlockMetadata(blockMetadata: Array<JniBlockMeta>): Boolean

    /**
     * @return Null if successful. If an error occurs, the height will be the height where the error was detected.
     */
    suspend fun validateCombinedChain(): BlockHeight?

    @Suppress("LongParameterList")
    suspend fun putUtxo(
        tAddress: String,
        txId: ByteArray,
        index: Int,
        script: ByteArray,
        value: Long,
        height: BlockHeight
    ): Boolean

    suspend fun getDownloadedUtxoBalance(address: String): WalletBalance

    // Implemented by `DerivationTool`
    interface Derivation {
        suspend fun deriveUnifiedAddress(
            viewingKey: String,
            network: ZcashNetwork
        ): String

        suspend fun deriveUnifiedAddress(
            seed: ByteArray,
            network: ZcashNetwork,
            account: Account
        ): String

        suspend fun deriveUnifiedSpendingKey(
            seed: ByteArray,
            network: ZcashNetwork,
            account: Account
        ): UnifiedSpendingKey

        suspend fun deriveUnifiedFullViewingKey(
            usk: UnifiedSpendingKey,
            network: ZcashNetwork
        ): UnifiedFullViewingKey

        suspend fun deriveUnifiedFullViewingKeys(
            seed: ByteArray,
            network: ZcashNetwork,
            numberOfAccounts: Int = 1
        ): Array<UnifiedFullViewingKey>
    }
}
