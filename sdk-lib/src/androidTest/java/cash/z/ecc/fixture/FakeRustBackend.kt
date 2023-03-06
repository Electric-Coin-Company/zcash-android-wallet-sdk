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

internal class FakeRustBackend(
    override val network: ZcashNetwork,
    override val saplingParamDir: File,
    val metadata: MutableList<JniBlockMeta>
) : RustBackendWelding {

    override suspend fun writeBlockMetadata(blockMetadata: List<JniBlockMeta>): Boolean =
        metadata.addAll(blockMetadata)

    override suspend fun rewindToHeight(height: BlockHeight): Boolean {
        metadata.removeAll { it.height > height.value }
        return true
    }

    override suspend fun getLatestHeight(): BlockHeight = BlockHeight(metadata.maxOf { it.height })
    override suspend fun findBlockMetadata(height: BlockHeight): JniBlockMeta? {
        return metadata.findLast { it.height == height.value }
    }

    override suspend fun rewindBlockMetadataToHeight(height: BlockHeight) {
        metadata.removeAll { it.height > height.value }
    }

    override suspend fun initBlockMetaDb(): Int =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun createToAddress(usk: UnifiedSpendingKey, to: String, value: Long, memo: ByteArray?): Long =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun shieldToAddress(usk: UnifiedSpendingKey, memo: ByteArray?): Long =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun decryptAndStoreTransaction(tx: ByteArray) =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun initAccountsTable(seed: ByteArray, numberOfAccounts: Int): Array<UnifiedFullViewingKey> =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun initAccountsTable(vararg keys: UnifiedFullViewingKey): Boolean =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun initBlocksTable(checkpoint: Checkpoint): Boolean =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun initDataDb(seed: ByteArray?): Int =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun createAccount(seed: ByteArray): UnifiedSpendingKey =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override fun isValidShieldedAddr(addr: String): Boolean =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override fun isValidTransparentAddr(addr: String): Boolean =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override fun isValidUnifiedAddr(addr: String): Boolean =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun getCurrentAddress(account: Int): String =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override fun getTransparentReceiver(ua: String): String? =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override fun getSaplingReceiver(ua: String): String? =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun getBalance(account: Int): Zatoshi =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override fun getBranchIdForHeight(height: BlockHeight): Long =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun getReceivedMemoAsUtf8(idNote: Long): String? =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun getSentMemoAsUtf8(idNote: Long): String? =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun getVerifiedBalance(account: Int): Zatoshi =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun getNearestRewindHeight(height: BlockHeight): BlockHeight =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun scanBlocks(limit: Int): Boolean =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun validateCombinedChain(): BlockHeight? =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun putUtxo(
        tAddress: String,
        txId: ByteArray,
        index: Int,
        script: ByteArray,
        value: Long,
        height: BlockHeight
    ): Boolean = error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun getDownloadedUtxoBalance(address: String): WalletBalance =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")
}
