package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.jni.RustBackendWelding
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.type.UnifiedFullViewingKey
import java.io.File

internal class FakeRustBackend : RustBackendWelding {

    val metadata = mutableListOf<JniBlockMeta>()

    override val network: ZcashNetwork
        get() = ZcashNetwork.Testnet
    override val saplingParamDir: File
        get() = File("") // TODO

    override suspend fun initBlockMetaDb(): Int {
        TODO("Not yet implemented")
    }

    override suspend fun createToAddress(usk: UnifiedSpendingKey, to: String, value: Long, memo: ByteArray?): Long {
        TODO("Not yet implemented")
    }

    override suspend fun shieldToAddress(usk: UnifiedSpendingKey, memo: ByteArray?): Long {
        TODO("Not yet implemented")
    }

    override suspend fun decryptAndStoreTransaction(tx: ByteArray) {
        TODO("Not yet implemented")
    }

    override suspend fun initAccountsTable(seed: ByteArray, numberOfAccounts: Int): Array<UnifiedFullViewingKey> {
        TODO("Not yet implemented")
    }

    override suspend fun initAccountsTable(vararg keys: UnifiedFullViewingKey): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun initBlocksTable(checkpoint: Checkpoint): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun initDataDb(seed: ByteArray?): Int {
        TODO("Not yet implemented")
    }

    override suspend fun createAccount(seed: ByteArray): UnifiedSpendingKey {
        TODO("Not yet implemented")
    }

    override fun isValidShieldedAddr(addr: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun isValidTransparentAddr(addr: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun isValidUnifiedAddr(addr: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getCurrentAddress(account: Int): String {
        TODO("Not yet implemented")
    }

    override fun getTransparentReceiver(ua: String): String? {
        TODO("Not yet implemented")
    }

    override fun getSaplingReceiver(ua: String): String? {
        TODO("Not yet implemented")
    }

    override suspend fun getBalance(account: Int): Zatoshi {
        TODO("Not yet implemented")
    }

    override fun getBranchIdForHeight(height: BlockHeight): Long {
        TODO("Not yet implemented")
    }

    override suspend fun getReceivedMemoAsUtf8(idNote: Long): String? {
        TODO("Not yet implemented")
    }

    override suspend fun getSentMemoAsUtf8(idNote: Long): String? {
        TODO("Not yet implemented")
    }

    override suspend fun getVerifiedBalance(account: Int): Zatoshi {
        TODO("Not yet implemented")
    }

    override suspend fun getNearestRewindHeight(height: BlockHeight): BlockHeight {
        TODO("Not yet implemented")
    }

    override suspend fun rewindToHeight(height: BlockHeight): Boolean {
        metadata.removeAll { it.height > height.value }
        return true
    }

    override suspend fun scanBlocks(limit: Int): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun writeBlockMetadata(blockMetadata: Array<JniBlockMeta>): Boolean =
        metadata.addAll(blockMetadata)

    override suspend fun getLatestHeight(): BlockHeight = BlockHeight(metadata.maxOf { it.height })

    override suspend fun validateCombinedChain(): BlockHeight? {
        TODO("Not yet implemented")
    }

    override suspend fun putUtxo(
        tAddress: String,
        txId: ByteArray,
        index: Int,
        script: ByteArray,
        value: Long,
        height: BlockHeight
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getDownloadedUtxoBalance(address: String): WalletBalance {
        TODO("Not yet implemented")
    }
}
