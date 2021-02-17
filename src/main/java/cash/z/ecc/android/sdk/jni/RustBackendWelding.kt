package cash.z.ecc.android.sdk.jni

import cash.z.ecc.android.sdk.block.CompactBlockProcessor

/**
 * Contract defining the exposed capabilities of the Rust backend.
 * This is what welds the SDK to the Rust layer.
 * It is not documented because it is not intended to be used, directly.
 * Instead, use the synchronizer or one of its subcomponents.
 */
interface RustBackendWelding {

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

    fun initAccountsTable(seed: ByteArray, numberOfAccounts: Int): Array<String>

    fun initAccountsTable(vararg extfvks: String): Boolean

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

    fun getDownloadedUtxoBalance(address: String): CompactBlockProcessor.WalletBalance

    // Implemented by `DerivationTool`
    interface Derivation {
        fun deriveShieldedAddress(viewingKey: String): String

        fun deriveShieldedAddress(seed: ByteArray, accountIndex: Int = 0): String

        fun deriveSpendingKeys(seed: ByteArray, numberOfAccounts: Int = 1): Array<String>

        fun deriveTransparentAddress(seed: ByteArray, account: Int = 0, index: Int = 0): String

        fun deriveTransparentAddress(transparentSecretKey: String): String

        fun deriveTransparentSecretKey(seed: ByteArray, account: Int = 0, index: Int = 0): String

        fun deriveViewingKey(spendingKey: String): String

        fun deriveViewingKeys(seed: ByteArray, numberOfAccounts: Int = 1): Array<String>
    }
}
