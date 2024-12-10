package cash.z.ecc.android.sdk.internal

import cash.z.ecc.android.sdk.exception.InitializeException
import cash.z.ecc.android.sdk.exception.RustLayerException
import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.internal.model.JniSubtreeRoot
import cash.z.ecc.android.sdk.internal.model.RewindResult
import cash.z.ecc.android.sdk.internal.model.ScanRange
import cash.z.ecc.android.sdk.internal.model.ScanSummary
import cash.z.ecc.android.sdk.internal.model.SubtreeRoot
import cash.z.ecc.android.sdk.internal.model.TransactionDataRequest
import cash.z.ecc.android.sdk.internal.model.TransactionStatus
import cash.z.ecc.android.sdk.internal.model.TreeState
import cash.z.ecc.android.sdk.internal.model.WalletSummary
import cash.z.ecc.android.sdk.internal.model.ZcashProtocol
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.AccountImportSetup
import cash.z.ecc.android.sdk.model.AccountPurpose
import cash.z.ecc.android.sdk.model.AccountUsk
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

    override suspend fun getAccounts(): List<Account> = backend.getAccounts().map { Account.new(it) }

    override suspend fun createAccountAndGetSpendingKey(
        accountName: String,
        keySource: String?,
        seed: ByteArray,
        treeState: TreeState,
        recoverUntil: BlockHeight?
    ): AccountUsk {
        return AccountUsk.new(
            backend.createAccount(
                accountName = accountName,
                keySource = keySource,
                seed = seed,
                treeState = treeState.encoded,
                recoverUntil = recoverUntil?.value
            )
        )
    }

    override suspend fun importAccountUfvk(
        purpose: AccountPurpose,
        recoverUntil: Long?,
        setup: AccountImportSetup,
        treeState: ByteArray,
    ): Account {
        return Account.new(
            jniAccount =
                backend.importAccountUfvk(
                    accountName = setup.accountName,
                    keySource = setup.keySource,
                    purpose = purpose.value,
                    recoverUntil = recoverUntil,
                    treeState = treeState,
                    ufvk = setup.ufvk.encoding,
                    seedFingerprint = setup.seedFingerprint,
                    zip32AccountIndex = setup.zip32AccountIndex?.index,
                )
        )
    }

    override suspend fun proposeTransferFromUri(
        account: Account,
        uri: String
    ): Proposal =
        Proposal.fromUnsafe(
            backend.proposeTransferFromUri(
                account.accountUuid.value,
                uri
            )
        )

    @Suppress("LongParameterList")
    override suspend fun proposeTransfer(
        account: Account,
        to: String,
        value: Long,
        memo: ByteArray?
    ): Proposal =
        Proposal.fromUnsafe(
            backend.proposeTransfer(
                account.accountUuid.value,
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
            account.accountUuid.value,
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
        return runCatching {
            backend.getCurrentAddress(account.accountUuid.value)
        }.onFailure {
            Twig.warn(it) { "Currently unable to get current address" }
        }.getOrElse { throw RustLayerException.GetCurrentAddressException(it) }
    }

    override suspend fun listTransparentReceivers(account: Account): List<String> {
        return backend.listTransparentReceivers(account.accountUuid.value)
    }

    override fun getBranchIdForHeight(height: BlockHeight): Long {
        return backend.getBranchIdForHeight(height.value)
    }

    override suspend fun rewindToHeight(height: BlockHeight): RewindResult {
        return RewindResult.new(backend.rewindToHeight(height.value))
    }

    override suspend fun getLatestCacheHeight(): BlockHeight? {
        return backend.getLatestCacheHeight()?.let {
            BlockHeight.new(it)
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
        txId: ByteArray,
        index: Int,
        script: ByteArray,
        value: Long,
        height: BlockHeight
    ) {
        return backend.putUtxo(
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
        return runCatching {
            backend.getFullyScannedHeight()?.let {
                BlockHeight.new(it)
            }
        }.onFailure {
            Twig.warn(it) { "Currently unable to get fully scanned height" }
        }.getOrElse { throw RustLayerException.GetFullyScannedHeight(it) }
    }

    override suspend fun getMaxScannedHeight(): BlockHeight? {
        return runCatching {
            backend.getMaxScannedHeight()?.let {
                BlockHeight.new(it)
            }
        }.onFailure {
            Twig.warn(it) { "Currently unable to get max scanned height" }
        }.getOrElse { throw RustLayerException.GetMaxScannedHeight(it) }
    }

    override suspend fun scanBlocks(
        fromHeight: BlockHeight,
        fromState: TreeState,
        limit: Long
    ): ScanSummary = ScanSummary.new(backend.scanBlocks(fromHeight.value, fromState.encoded, limit))

    override suspend fun transactionDataRequests(): List<TransactionDataRequest> =
        backend.transactionDataRequests().map { jniRequest ->
            TransactionDataRequest.new(jniRequest)
        }

    override suspend fun getWalletSummary(): WalletSummary? =
        backend.getWalletSummary()?.let { jniWalletSummary ->
            WalletSummary.new(jniWalletSummary)
        }

    override suspend fun suggestScanRanges(): List<ScanRange> =
        backend.suggestScanRanges().map { jniScanRange ->
            ScanRange.new(jniScanRange)
        }

    override suspend fun decryptAndStoreTransaction(
        tx: ByteArray,
        minedHeight: BlockHeight?
    ) = backend
        .decryptAndStoreTransaction(tx, minedHeight?.value)

    override suspend fun setTransactionStatus(
        txId: ByteArray,
        status: TransactionStatus
    ) = backend.setTransactionStatus(
        txId = txId,
        status = status.toPrimitiveValue()
    )

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

    override fun isValidTexAddr(addr: String): Boolean = backend.isValidTexAddr(addr)
}
