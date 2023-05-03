package cash.z.ecc.android.sdk.jni

import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.UnifiedFullViewingKey
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi

@Suppress("TooManyFunctions")
internal class TypesafeBackendImpl(private val backend: Backend) : TypesafeBackend {
    override suspend fun initAccountsTable(vararg keys: UnifiedFullViewingKey): Boolean =
        backend.initAccountsTable(*keys)

    override suspend fun initAccountsTable(
        seed: ByteArray,
        numberOfAccounts: Int
    ): Array<UnifiedFullViewingKey> = backend.initAccountsTable(seed, numberOfAccounts)

    override suspend fun initBlocksTable(checkpoint: Checkpoint): Boolean = backend.initBlocksTable(checkpoint)

    override suspend fun getCurrentAddress(account: Account): String = getCurrentAddress(account)

    override suspend fun listTransparentReceivers(account: Account): List<String> =
        backend.listTransparentReceivers(account)

    override suspend fun getBalance(account: Account): Zatoshi = backend.getBalance(account)

    override fun getBranchIdForHeight(height: BlockHeight): Long = backend.getBranchIdForHeight(height.value)

    override suspend fun getVerifiedBalance(account: Account): Zatoshi = backend.getVerifiedBalance(account)

    override suspend fun getNearestRewindHeight(height: BlockHeight): BlockHeight =
        backend.getNearestRewindHeight(height)

    override suspend fun rewindToHeight(height: BlockHeight): Boolean = backend.rewindToHeight(height)

    override suspend fun getLatestBlockHeight(): BlockHeight? = backend.getLatestBlockHeight()

    override suspend fun findBlockMetadata(height: BlockHeight): JniBlockMeta? = backend.findBlockMetadata(height)

    override suspend fun rewindBlockMetadataToHeight(height: BlockHeight) = backend.rewindBlockMetadataToHeight(height)

    /**
     * @param limit The limit provides an efficient way how to restrict the portion of blocks, which will be validated.
     * @return Null if successful. If an error occurs, the height will be the height where the error was detected.
     */
    override suspend fun validateCombinedChainOrErrorBlockHeight(limit: Int): BlockHeight? =
        backend.validateCombinedChainOrErrorBlockHeight(limit)

    override suspend fun getDownloadedUtxoBalance(address: String): WalletBalance =
        backend.getDownloadedUtxoBalance(address)
}
