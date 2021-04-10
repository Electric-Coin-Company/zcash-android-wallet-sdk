package cash.z.ecc.android.sdk.jni

import cash.z.ecc.android.sdk.type.UnifiedViewingKey
import cash.z.ecc.android.sdk.type.WalletBalance
import cash.z.ecc.android.sdk.type.ZcashNetwork

/**
 * Contract defining the exposed capabilities of the Rust backend.
 * This is what welds the SDK to the Rust layer.
 * It is not documented because it is not intended to be used, directly.
 * Instead, use the synchronizer or one of its subcomponents.
 */
interface RustBackendWelding {

    val network: ZcashNetwork

    fun createToAddress(
        consensusBranchId: Long,
        account: Int,
        extsk: String,
        to: String,
        value: Long,
        memo: ByteArray? = byteArrayOf()
    ): Long

    fun shieldToAddress(
        extsk: String,
        tsk: String,
        memo: ByteArray? = byteArrayOf()
    ): Long

    fun decryptAndStoreTransaction(tx: ByteArray)

    fun initAccountsTable(seed: ByteArray, numberOfAccounts: Int): Array<UnifiedViewingKey>

    fun initAccountsTable(vararg keys: UnifiedViewingKey): Boolean

    fun initBlocksTable(height: Int, hash: String, time: Long, saplingTree: String): Boolean

    fun initDataDb(): Boolean

    fun isValidShieldedAddr(addr: String): Boolean

    fun isValidTransparentAddr(addr: String): Boolean

    fun getShieldedAddress(account: Int = 0): String

    fun getTransparentAddress(account: Int = 0, index: Int = 0): String

    fun getBalance(account: Int = 0): Long

    fun getBranchIdForHeight(height: Int): Long

    fun getReceivedMemoAsUtf8(idNote: Long): String

    fun getSentMemoAsUtf8(idNote: Long): String

    fun getVerifiedBalance(account: Int = 0): Long

//    fun parseTransactionDataList(tdl: LocalRpcTypes.TransactionDataList): LocalRpcTypes.TransparentTransactionList

    fun rewindToHeight(height: Int): Boolean

    fun scanBlocks(limit: Int = -1): Boolean

    fun validateCombinedChain(): Int

    fun putUtxo(
        tAddress: String,
        txId: ByteArray,
        index: Int,
        script: ByteArray,
        value: Long,
        height: Int
    ): Boolean

    fun clearUtxos(tAddress: String, aboveHeight: Int = network.saplingActivationHeight - 1): Boolean

    fun getDownloadedUtxoBalance(address: String): WalletBalance

    // Implemented by `DerivationTool`
    interface Derivation {
        fun deriveShieldedAddress(
            viewingKey: String,
            network: ZcashNetwork
        ): String

        fun deriveShieldedAddress(
            seed: ByteArray,
            network: ZcashNetwork,
            accountIndex: Int = 0,
        ): String

        fun deriveSpendingKeys(
            seed: ByteArray,
            network: ZcashNetwork,
            numberOfAccounts: Int = 1,
        ): Array<String>

        fun deriveTransparentAddress(
            seed: ByteArray,
            network: ZcashNetwork,
            account: Int = 0,
            index: Int = 0,
        ): String

        fun deriveTransparentAddressFromPublicKey(
            publicKey: String,
            network: ZcashNetwork
        ): String

        fun deriveTransparentAddressFromPrivateKey(
            privateKey: String,
            network: ZcashNetwork
        ): String

        fun deriveTransparentSecretKey(
            seed: ByteArray,
            network: ZcashNetwork,
            account: Int = 0,
            index: Int = 0,
        ): String

        fun deriveViewingKey(
            spendingKey: String,
            network: ZcashNetwork
        ): String

        fun deriveUnifiedViewingKeys(
            seed: ByteArray,
            network: ZcashNetwork,
            numberOfAccounts: Int = 1,
        ): Array<UnifiedViewingKey>
    }
}
