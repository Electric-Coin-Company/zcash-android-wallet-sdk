package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.exception.InitializeException
import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.internal.model.JniSubtreeRoot
import cash.z.ecc.android.sdk.internal.model.ScanRange
import cash.z.ecc.android.sdk.internal.model.ScanSummary
import cash.z.ecc.android.sdk.internal.model.SubtreeRoot
import cash.z.ecc.android.sdk.internal.model.TreeState
import cash.z.ecc.android.sdk.internal.model.WalletSummary
import cash.z.ecc.android.sdk.internal.model.ZcashProtocol
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.FirstClassByteArray
import cash.z.ecc.android.sdk.model.Proposal
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import kotlinx.coroutines.withContext

@Suppress("TooManyFunctions")
internal class TypesafeBackendImpl(private val backend: Backend) : TypesafeBackend {
    override val network: ZcashNetwork
        get() = ZcashNetwork.from(backend.networkId)

    override suspend fun createAccountAndGetSpendingKey(
        seed: ByteArray,
        treeState: TreeState,
        recoverUntil: BlockHeight?
    ): UnifiedSpendingKey {
        return UnifiedSpendingKey(
            backend.createAccount(
                seed = seed,
                treeState = treeState.encoded,
                recoverUntil = recoverUntil?.value
            )
        )
    }

    @Suppress("LongParameterList")
    override suspend fun proposeTransfer(
        account: Account,
        to: String,
        value: Long,
        memo: ByteArray?
    ): Proposal =
        Proposal.fromUnsafe(
            backend.proposeTransfer(
                account.value,
                to,
                value,
                memo
            )
        )

    override suspend fun proposeShielding(
        account: Account,
        shieldingThreshold: Long,
        memo: ByteArray?,
        transparentReceiver: String?
    ): Proposal? =
        backend.proposeShielding(
            account.value,
            shieldingThreshold,
            memo,
            transparentReceiver
        )?.let {
            Proposal.fromUnsafe(
                it
            )
        }

    override suspend fun createProposedTransactions(
        proposal: Proposal,
        usk: UnifiedSpendingKey
    ): List<FirstClassByteArray> =
        backend.createProposedTransactions(
            proposal.toUnsafe(),
            usk.copyBytes()
        ).map { FirstClassByteArray(it) }

    override suspend fun getCurrentAddress(account: Account): String {
        return backend.getCurrentAddress(account.value)
    }

    override suspend fun listTransparentReceivers(account: Account): List<String> {
        return backend.listTransparentReceivers(account.value)
    }

    override fun getBranchIdForHeight(height: BlockHeight): Long {
        return backend.getBranchIdForHeight(height.value)
    }

    override suspend fun getNearestRewindHeight(height: BlockHeight): BlockHeight {
        return BlockHeight.new(
            ZcashNetwork.from(backend.networkId),
            backend.getNearestRewindHeight(height.value)
        )
    }

    override suspend fun rewindToHeight(height: BlockHeight) {
        backend.rewindToHeight(height.value)
    }

    override suspend fun getLatestCacheHeight(): BlockHeight? {
        return backend.getLatestCacheHeight()?.let {
            BlockHeight.new(
                ZcashNetwork.from(backend.networkId),
                it
            )
        }
    }

    override suspend fun findBlockMetadata(height: BlockHeight): JniBlockMeta? {
        return backend.findBlockMetadata(height.value)
    }

    override suspend fun rewindBlockMetadataToHeight(height: BlockHeight) {
        backend.rewindBlockMetadataToHeight(height.value)
    }

    override suspend fun getDownloadedUtxoBalance(address: String): Zatoshi {
        val total =
            withContext(SdkDispatchers.DATABASE_IO) {
                backend.getTotalTransparentBalance(address)
            }
        return Zatoshi(total)
    }

    @Suppress("LongParameterList")
    override suspend fun putUtxo(
        tAddress: String,
        txId: ByteArray,
        index: Int,
        script: ByteArray,
        value: Long,
        height: BlockHeight
    ) {
        return backend.putUtxo(
            tAddress,
            txId,
            index,
            script,
            value,
            height.value
        )
    }

    override suspend fun getMemoAsUtf8(
        txId: ByteArray,
        protocol: ZcashProtocol,
        outputIndex: Int
    ): String? =
        backend.getMemoAsUtf8(
            txId = txId,
            protocol = protocol.poolCode,
            outputIndex = outputIndex
        )

    override suspend fun initDataDb(seed: ByteArray?) {
        val ret = backend.initDataDb(seed)
        when (ret) {
            2 -> throw InitializeException.SeedNotRelevant
            1 -> throw InitializeException.SeedRequired
            0 -> { /* Successful case - no action needed */ }
            -1 -> error("Rust backend only uses -1 as an error sentinel")
            else -> error("Rust backend used a code that needs to be defined here")
        }
    }

    override suspend fun putSubtreeRoots(
        saplingStartIndex: UInt,
        saplingRoots: List<SubtreeRoot>,
        orchardStartIndex: UInt,
        orchardRoots: List<SubtreeRoot>
    ) = backend.putSubtreeRoots(
        saplingStartIndex = saplingStartIndex.toLong(),
        saplingRoots =
            saplingRoots.map {
                JniSubtreeRoot.new(
                    rootHash = it.rootHash,
                    completingBlockHeight = it.completingBlockHeight.value
                )
            },
        orchardStartIndex = orchardStartIndex.toLong(),
        orchardRoots =
            orchardRoots.map {
                JniSubtreeRoot.new(
                    rootHash = it.rootHash,
                    completingBlockHeight = it.completingBlockHeight.value
                )
            },
    )

    override suspend fun updateChainTip(height: BlockHeight) = backend.updateChainTip(height.value)

    override suspend fun getFullyScannedHeight(): BlockHeight? {
        return backend.getFullyScannedHeight()?.let {
            BlockHeight.new(
                ZcashNetwork.from(backend.networkId),
                it
            )
        }
    }

    override suspend fun getMaxScannedHeight(): BlockHeight? {
        return backend.getMaxScannedHeight()?.let {
            BlockHeight.new(
                ZcashNetwork.from(backend.networkId),
                it
            )
        }
    }

    override suspend fun scanBlocks(
        fromHeight: BlockHeight,
        fromState: TreeState,
        limit: Long
    ): ScanSummary = ScanSummary.new(backend.scanBlocks(fromHeight.value, fromState.encoded, limit), network)

    override suspend fun getWalletSummary(): WalletSummary? =
        backend.getWalletSummary()?.let { jniWalletSummary ->
            WalletSummary.new(jniWalletSummary)
        }

    override suspend fun suggestScanRanges(): List<ScanRange> =
        backend.suggestScanRanges().map { jniScanRange ->
            ScanRange.new(
                jniScanRange,
                network
            )
        }

    override suspend fun decryptAndStoreTransaction(tx: ByteArray) = backend.decryptAndStoreTransaction(tx)

    override fun getSaplingReceiver(ua: String): String? = backend.getSaplingReceiver(ua)

    override fun getTransparentReceiver(ua: String): String? = backend.getTransparentReceiver(ua)

    override suspend fun initBlockMetaDb(): Int = backend.initBlockMetaDb()

    override suspend fun writeBlockMetadata(blockMetadata: List<JniBlockMeta>) =
        backend.writeBlockMetadata(
            blockMetadata
        )

    override fun isValidSaplingAddr(addr: String): Boolean = backend.isValidSaplingAddr(addr)

    override fun isValidTransparentAddr(addr: String): Boolean = backend.isValidTransparentAddr(addr)

    override fun isValidUnifiedAddr(addr: String): Boolean = backend.isValidUnifiedAddr(addr)
}
