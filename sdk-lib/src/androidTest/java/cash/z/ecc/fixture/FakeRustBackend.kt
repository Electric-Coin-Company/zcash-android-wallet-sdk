package cash.z.ecc.fixture

import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.jni.Backend
import cash.z.ecc.android.sdk.jni.UnifiedSpendingKeyJni
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import java.io.File

internal class FakeRustBackend(
    override val network: ZcashNetwork,
    override val saplingParamDir: File,
    val metadata: MutableList<JniBlockMeta>
) : Backend {

    override suspend fun writeBlockMetadata(blockMetadata: List<JniBlockMeta>): Boolean =
        metadata.addAll(blockMetadata)

    override suspend fun rewindToHeight(height: Long): Boolean {
        metadata.removeAll { it.height > height }
        return true
    }

    override suspend fun getLatestHeight(): Long = metadata.maxOf { it.height }
    override suspend fun validateCombinedChainOrErrorHeight(): Long? {
        TODO("Not yet implemented")
    }

    override suspend fun getVerifiedTransparentBalance(address: String): Long {
        TODO("Not yet implemented")
    }

    override suspend fun getTotalTransparentBalance(address: String): Long {
        TODO("Not yet implemented")
    }

    override suspend fun findBlockMetadata(height: Long): JniBlockMeta? {
        return metadata.findLast { it.height == height }
    }

    override suspend fun rewindBlockMetadataToHeight(height: Long) {
        metadata.removeAll { it.height > height }
    }

    override suspend fun initBlockMetaDb(): Int =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun createToAddress(
        account: Int,
        unifiedSpendingKey: ByteArray,
        to: String,
        value: Long,
        memo: ByteArray?
    ): Long {
        TODO("Not yet implemented")
    }

    override suspend fun shieldToAddress(account: Int, unifiedSpendingKey: ByteArray, memo: ByteArray?): Long {
        TODO("Not yet implemented")
    }

    override suspend fun decryptAndStoreTransaction(tx: ByteArray) =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun initAccountsTable(vararg keys: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun initBlocksTable(
        checkpointHeight: Long,
        checkpointHash: String,
        checkpointTime: Long,
        checkpointSaplingTree: String
    ): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun initDataDb(seed: ByteArray?): Int =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun createAccount(seed: ByteArray): UnifiedSpendingKeyJni =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override fun isValidShieldedAddr(addr: String): Boolean =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override fun isValidTransparentAddr(addr: String): Boolean =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override fun isValidUnifiedAddr(addr: String): Boolean =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun getCurrentAddress(account: Int): String {
        TODO("Not yet implemented")
    }

    override fun getTransparentReceiver(ua: String): String? =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override fun getSaplingReceiver(ua: String): String? =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun listTransparentReceivers(account: Int): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun getBalance(account: Int): Long {
        TODO("Not yet implemented")
    }

    override fun getBranchIdForHeight(height: Long): Long {
        TODO("Not yet implemented")
    }

    override suspend fun getReceivedMemoAsUtf8(idNote: Long): String? =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun getSentMemoAsUtf8(idNote: Long): String? =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun getVerifiedBalance(account: Int): Long {
        TODO("Not yet implemented")
    }

    override suspend fun getNearestRewindHeight(height: Long): Long {
        TODO("Not yet implemented")
    }

    override suspend fun scanBlocks(limit: Int): Boolean =
        error("Intentionally not implemented in mocked FakeRustBackend implementation.")

    override suspend fun putUtxo(
        tAddress: String,
        txId: ByteArray,
        index: Int,
        script: ByteArray,
        value: Long,
        height: BlockHeight
    ): Boolean = error("Intentionally not implemented in mocked FakeRustBackend implementation.")
}
