package cash.z.ecc.android.sdk.fixture

import cash.z.ecc.android.sdk.CloseableSynchronizer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.SynchronizerKey
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.exception.TransactionEncoderException
import cash.z.ecc.android.sdk.exception.TransactionSubmitException
import cash.z.ecc.android.sdk.internal.Backend
import cash.z.ecc.android.sdk.internal.repository.DerivedDataRepository
import cash.z.ecc.android.sdk.internal.transaction.OutboundTransactionManager
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.PercentDecimal
import cash.z.ecc.android.sdk.model.TransactionOverview
import cash.z.ecc.android.sdk.model.TransactionRecipient
import cash.z.ecc.android.sdk.model.UnifiedSpendingKey
import cash.z.ecc.android.sdk.model.WalletBalance
import cash.z.ecc.android.sdk.model.Zatoshi
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.type.AddressType
import cash.z.ecc.android.sdk.type.ConsensusMatchType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * This provides a fake SDK Synchronizer implementation, which is useful for example for Compose Previews in the Demo
 * app
 */
@Suppress("TooManyFunctions", "UnusedPrivateProperty")
internal class FakeSynchronizer internal constructor(
    private val synchronizerKey: SynchronizerKey,
    private val storage: DerivedDataRepository,
    private val txManager: OutboundTransactionManager,
    val processor: CompactBlockProcessor,
    private val backend: Backend
) : CloseableSynchronizer {

    override val orchardBalances = emptyFlow<WalletBalance?>() as StateFlow<WalletBalance?>

    override val saplingBalances = emptyFlow<WalletBalance?>() as StateFlow<WalletBalance?>

    override val transparentBalances = emptyFlow<WalletBalance?>() as StateFlow<WalletBalance?>

    override val transactions = emptyFlow<List<TransactionOverview>>()

    override val network = ZcashNetwork.Testnet

    override val status = emptyFlow<Synchronizer.Status>()

    override val progress = emptyFlow<PercentDecimal>()

    override val processorInfo = emptyFlow<CompactBlockProcessor.ProcessorInfo>()

    override val networkHeight = emptyFlow<BlockHeight?>() as StateFlow<BlockHeight?>

    override var onCriticalErrorHandler: ((Throwable?) -> Boolean)? = null

    override var onProcessorErrorHandler: ((Throwable?) -> Boolean)? = null

    override var onSubmissionErrorHandler: ((Throwable?) -> Boolean)? = null

    override var onSetupErrorHandler: ((Throwable?) -> Boolean)? = null

    override var onChainErrorHandler: ((errorHeight: BlockHeight, rewindHeight: BlockHeight) -> Any)? = null

    override val latestHeight = ZcashNetwork.Testnet.saplingActivationHeight

    override val latestBirthdayHeight = ZcashNetwork.Testnet.saplingActivationHeight

    override fun close() = TODO("Not yet implemented")

    override suspend fun getNearestRewindHeight(height: BlockHeight): BlockHeight =
        TODO("Not yet implemented")

    override suspend fun rewindToNearestHeight(height: BlockHeight, alsoClearBlockCache: Boolean) {
        TODO("Not yet implemented")
    }

    override suspend fun quickRewind() {
        TODO("Not yet implemented")
    }

    override fun getMemos(transactionOverview: TransactionOverview): Flow<String> {
        TODO("Not yet implemented")
    }

    override fun getRecipients(transactionOverview: TransactionOverview): Flow<TransactionRecipient> {
        TODO("Not yet implemented")
    }

    override suspend fun getUnifiedAddress(account: Account): String =
        TODO("Not yet implemented")

    override suspend fun getSaplingAddress(account: Account): String =
        TODO("Not yet implemented")

    override suspend fun getTransparentAddress(account: Account): String =
        TODO("Not yet implemented")

    @Throws(TransactionEncoderException::class, TransactionSubmitException::class)
    override suspend fun sendToAddress(
        usk: UnifiedSpendingKey,
        amount: Zatoshi,
        toAddress: String,
        memo: String
    ): Long {
        TODO("Not yet implemented")
    }

    @Throws(TransactionEncoderException::class, TransactionSubmitException::class)
    override suspend fun shieldFunds(
        usk: UnifiedSpendingKey,
        memo: String
    ): Long {
        TODO("Not yet implemented")
    }

    override suspend fun refreshUtxos(account: Account, since: BlockHeight): Int {
        TODO("Not yet implemented")
    }

    override suspend fun getTransparentBalance(tAddr: String): WalletBalance {
        TODO("Not yet implemented")
    }

    override suspend fun isValidShieldedAddr(address: String) =
        TODO("Not yet implemented")

    override suspend fun isValidTransparentAddr(address: String) =
        TODO("Not yet implemented")

    override suspend fun isValidUnifiedAddr(address: String) =
        TODO("Not yet implemented")

    override suspend fun validateAddress(address: String): AddressType {
        TODO("Not yet implemented")
    }

    override suspend fun validateConsensusBranch(): ConsensusMatchType {
        TODO("Not yet implemented")
    }
}
