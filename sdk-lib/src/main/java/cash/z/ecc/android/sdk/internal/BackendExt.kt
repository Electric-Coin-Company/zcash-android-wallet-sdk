@file:Suppress("TooManyFunctions")

package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.internal.model.Checkpoint
import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.UnifiedFullViewingKey
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.tool.DerivationTool
import kotlinx.coroutines.withContext

internal val Backend.network: ZcashNetwork
    get() = ZcashNetwork.from(networkId)

internal suspend fun Backend.initAccountsTable(vararg keys: UnifiedFullViewingKey) {
    val ufvks = Array(keys.size) { keys[it].encoding }

    @Suppress("SpreadOperator")
    return initAccountsTable(*ufvks)
}

internal suspend fun Backend.initAccountsTableTypesafe(
    seed: ByteArray,
    numberOfAccounts: Int
): List<UnifiedFullViewingKey> {
    return DerivationTool.getInstance().deriveUnifiedFullViewingKeys(seed, network, numberOfAccounts)
}

internal suspend fun Backend.initBlocksTable(checkpoint: Checkpoint) = initBlocksTable(
    checkpoint.height.value,
    checkpoint.hash,
    checkpoint.epochSeconds,
    checkpoint.tree
)

internal suspend fun Backend.createAccountAndGetSpendingKey(seed: ByteArray): UnifiedSpendingKey = UnifiedSpendingKey(
    createAccount(seed)
)

@Suppress("LongParameterList")
internal suspend fun Backend.createToAddress(
    usk: UnifiedSpendingKey,
    to: String,
    value: Long,
    memo: ByteArray? = byteArrayOf()
): Long = createToAddress(
    usk.account.value,
    usk.copyBytes(),
    to,
    value,
    memo
)

internal suspend fun Backend.shieldToAddress(
    usk: UnifiedSpendingKey,
    memo: ByteArray? = byteArrayOf()
): Long = shieldToAddress(
    usk.account.value,
    usk.copyBytes(),
    memo
)

internal suspend fun Backend.getCurrentAddress(account: Account): String = getCurrentAddress(account.value)

internal suspend fun Backend.listTransparentReceivers(account: Account): List<String> =
    listTransparentReceivers(account.value)

internal suspend fun Backend.getBalance(account: Account): Zatoshi = Zatoshi(getBalance(account.value))

internal fun Backend.getBranchIdForHeight(height: BlockHeight): Long = getBranchIdForHeight(height.value)

internal suspend fun Backend.getVerifiedBalance(account: Account): Zatoshi = Zatoshi(
    getVerifiedBalance
    (account.value)
)

internal suspend fun Backend.getNearestRewindHeight(height: BlockHeight): BlockHeight = BlockHeight.new(
    ZcashNetwork.from(networkId),
    getNearestRewindHeight(height.value)
)

internal suspend fun Backend.rewindToHeight(height: BlockHeight) = rewindToHeight(height.value)

internal suspend fun Backend.getLatestBlockHeight(): BlockHeight? = getLatestHeight()?.let {
    BlockHeight.new(
        ZcashNetwork.from(networkId),
        it
    )
}

internal suspend fun Backend.findBlockMetadata(height: BlockHeight): JniBlockMeta? =
    findBlockMetadata(height.value)

internal suspend fun Backend.rewindBlockMetadataToHeight(height: BlockHeight) =
    rewindBlockMetadataToHeight(height.value)

/**
 * @param limit The limit provides an efficient way how to restrict the portion of blocks, which will be validated.
 * @return Null if successful. If an error occurs, the height will be the height where the error was detected.
 */
internal suspend fun Backend.validateCombinedChainOrErrorBlockHeight(limit: Long?): BlockHeight? =
    validateCombinedChainOrErrorHeight(limit)?.let {
        BlockHeight.new(
            ZcashNetwork.from(networkId),
            it
        )
    }

internal suspend fun Backend.getDownloadedUtxoBalance(address: String): WalletBalance {
    // Note this implementation is not ideal because it requires two database queries without a transaction, which makes
    // the data potentially inconsistent.  However the verified amount is queried first which makes this less bad.
    val verified = withContext(SdkDispatchers.DATABASE_IO) {
        getVerifiedTransparentBalance(address)
    }
    val total = withContext(SdkDispatchers.DATABASE_IO) {
        getTotalTransparentBalance(
            address
        )
    }
    return WalletBalance(Zatoshi(total), Zatoshi(verified))
}

@Suppress("LongParameterList")
internal suspend fun Backend.putUtxo(
    tAddress: String,
    txId: ByteArray,
    index: Int,
    script: ByteArray,
    value: Long,
    height: BlockHeight
) = putUtxo(
    tAddress,
    txId,
    index,
    script,
    value,
    height.value
)
