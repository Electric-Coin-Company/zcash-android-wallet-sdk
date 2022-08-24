package cash.z.ecc.android.sdk.jni

import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.type.UnifiedFullViewingKey

/**
 * Contract defining the exposed capabilities of the Rust backend.
 * This is what welds the SDK to the Rust layer.
 * It is not documented because it is not intended to be used, directly.
 * Instead, use the synchronizer or one of its subcomponents.
 */
@Suppress("TooManyFunctions")
internal interface RustBackendWelding {

    val network: ZcashNetwork

    @Suppress("LongParameterList")
    suspend fun createToAddress(
        consensusBranchId: Long,
        account: Int,
        extsk: String,
        to: String,
        value: Long,
        memo: ByteArray? = byteArrayOf()
    ): Long

    suspend fun shieldToAddress(
        extsk: String,
        xprv: String,
        memo: ByteArray? = byteArrayOf()
    ): Long

    suspend fun decryptAndStoreTransaction(tx: ByteArray)

    suspend fun initAccountsTable(seed: ByteArray, numberOfAccounts: Int): Array<UnifiedFullViewingKey>

    suspend fun initAccountsTable(vararg keys: UnifiedFullViewingKey): Boolean

    suspend fun initBlocksTable(checkpoint: Checkpoint): Boolean

    suspend fun initDataDb(): Boolean

    fun isValidShieldedAddr(addr: String): Boolean

    fun isValidTransparentAddr(addr: String): Boolean

    fun isValidUnifiedAddr(addr: String): Boolean

    suspend fun getShieldedAddress(account: Int = 0): String

    suspend fun getTransparentAddress(account: Int = 0, index: Int = 0): String

    suspend fun getBalance(account: Int = 0): Zatoshi

    fun getBranchIdForHeight(height: BlockHeight): Long

    suspend fun getReceivedMemoAsUtf8(idNote: Long): String

    suspend fun getSentMemoAsUtf8(idNote: Long): String

    suspend fun getVerifiedBalance(account: Int = 0): Zatoshi

//    fun parseTransactionDataList(tdl: LocalRpcTypes.TransactionDataList): LocalRpcTypes.TransparentTransactionList

    suspend fun getNearestRewindHeight(height: BlockHeight): BlockHeight

    suspend fun rewindToHeight(height: BlockHeight): Boolean

    suspend fun scanBlocks(limit: Int = -1): Boolean

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

    suspend fun clearUtxos(
        tAddress: String,
        aboveHeightInclusive: BlockHeight = BlockHeight(network.saplingActivationHeight.value)
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
            accountIndex: Int = 0
        ): String

        suspend fun deriveSpendingKeys(
            seed: ByteArray,
            network: ZcashNetwork,
            numberOfAccounts: Int = 1
        ): Array<String>

        suspend fun deriveTransparentAddress(
            seed: ByteArray,
            network: ZcashNetwork,
            account: Int = 0,
            index: Int = 0
        ): String

        suspend fun deriveTransparentAddressFromPublicKey(
            publicKey: String,
            network: ZcashNetwork
        ): String

        suspend fun deriveTransparentAddressFromAccountPrivateKey(
            privateKey: String,
            network: ZcashNetwork,
            index: Int = 0
        ): String

        suspend fun deriveTransparentAccountPrivateKey(
            seed: ByteArray,
            network: ZcashNetwork,
            account: Int = 0
        ): String

        suspend fun deriveViewingKey(
            spendingKey: String,
            network: ZcashNetwork
        ): String

        suspend fun deriveUnifiedFullViewingKeys(
            seed: ByteArray,
            network: ZcashNetwork,
            numberOfAccounts: Int = 1
        ): Array<UnifiedFullViewingKey>
    }
}
