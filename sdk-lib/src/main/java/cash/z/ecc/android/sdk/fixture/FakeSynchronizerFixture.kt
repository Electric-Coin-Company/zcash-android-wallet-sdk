package cash.z.ecc.android.sdk.fixture

import android.content.Context
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.SynchronizerKey
import cash.z.ecc.android.sdk.block.CompactBlockProcessor
import cash.z.ecc.android.sdk.ext.ZcashSdk
import cash.z.ecc.android.sdk.internal.SaplingParamTool
import cash.z.ecc.android.sdk.internal.block.CompactBlockDownloader
import cash.z.ecc.android.sdk.internal.db.DatabaseCoordinator
import cash.z.ecc.android.sdk.internal.ext.getDatabasePathSuspend
import cash.z.ecc.android.sdk.internal.model.JniBlockMeta
import cash.z.ecc.android.sdk.internal.storage.block.FileCompactBlockRepository
import cash.z.ecc.android.sdk.internal.transaction.OutboundTransactionManagerImpl
import cash.z.ecc.android.sdk.internal.transaction.TransactionEncoderImpl
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.Testnet
import cash.z.ecc.android.sdk.model.ZcashNetwork
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.new
import kotlinx.coroutines.runBlocking
import java.io.File

internal object FakeSynchronizerFixture {

    val DEFAULT_NETWORK = ZcashNetwork.Testnet
    val DEFAULT_ALIAS = ZcashSdk.DEFAULT_ALIAS + "_test" // $NON-NLS-1$
    val DEFAULT_BACKEND_METADATA = mutableListOf<JniBlockMeta>()

    fun new(
        context: Context,
        network: ZcashNetwork = DEFAULT_NETWORK,
        alias: String = DEFAULT_ALIAS,
        backendMetadata: MutableList<JniBlockMeta> = DEFAULT_BACKEND_METADATA
    ): Synchronizer {
        val synchronizerKey = SynchronizerKey(network, alias)

        val backend = FakeRustBackendFixture.new(
            network = network,
            metadata = backendMetadata
        )

        val dbFile = runBlocking {
            context.getDatabasePathSuspend("unused.db").parentFile?.mkdirs() // $NON-NLS-1$
            DatabaseCoordinator.getInstance(context).dataDbFile(
                network,
                alias
            ).also {
                it.createNewFile()
            }
        }

        val storage = DbDerivedDataRepositoryFixture.new(
            context = context,
            backend = backend,
            databaseFile = dbFile,
            zcashNetwork = network,
            checkpoint = CheckpointFixture.new(),
            seed = byteArrayOf(),
            viewingKeys = listOf(),
        )

        val saplingParamTool = runBlocking {
            SaplingParamTool.new(context)
        }
        val networkingClient = LightWalletClient.new(
            context = context,
            lightWalletEndpoint = LightWalletEndpoint.Companion.Testnet
        )
        val txManager = OutboundTransactionManagerImpl.new(
            encoder = TransactionEncoderImpl(backend, saplingParamTool, repository = storage),
            lightWalletClient = networkingClient
        )

        val compactBlockRepository = runBlocking {
            FileCompactBlockRepository.new(
                blockCacheRoot = File(""),
                rustBackend = backend
            )
        }
        val processor = CompactBlockProcessor(
            downloader = CompactBlockDownloader(
                lightWalletClient = networkingClient,
                compactBlockRepository = compactBlockRepository
            ),
            repository = storage,
            backend = backend,
            minimumHeight = BlockHeight.new(network, 0)
        )

        return FakeSynchronizer(
            synchronizerKey = synchronizerKey,
            storage = storage,
            txManager = txManager,
            processor = processor,
            backend = backend
        )
    }
}
