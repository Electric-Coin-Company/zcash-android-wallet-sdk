package cash.z.wallet.sdk.sample.memo

import android.content.Context
import cash.z.wallet.sdk.block.CompactBlockDbStore
import cash.z.wallet.sdk.block.CompactBlockDownloader
import cash.z.wallet.sdk.block.CompactBlockProcessor
import cash.z.wallet.sdk.block.ProcessorConfig
import cash.z.wallet.sdk.data.*
import cash.z.wallet.sdk.ext.SampleSeedProvider
import cash.z.wallet.sdk.ext.SimpleProvider
import cash.z.wallet.sdk.jni.RustBackend
import cash.z.wallet.sdk.secure.Wallet
import cash.z.wallet.sdk.service.LightWalletGrpcService

object Injection {
    private const val host: String = "34.68.177.238"
    private const val port: Int = 9067
    private const val cacheDbName = "memos-cache.db"
    private const val dataDbName = "memos-data.db"
    private val rustBackend = RustBackend()

    fun provideSynchronizer(appContext: Context): Synchronizer {

        // ledger
        val ledger = PollingTransactionRepository(appContext, dataDbName)

        // sender
        val manager = PersistentTransactionManager(appContext)
        val service = LightWalletGrpcService(appContext, host, port)
        val sender = PersistentTransactionSender(manager, service, ledger)

        // processor
        val config = ProcessorConfig(cacheDbName.toDbPath(appContext), dataDbName.toDbPath(appContext))
        val blockStore = CompactBlockDbStore(appContext, cacheDbName)
        val downloader = CompactBlockDownloader(service, blockStore)
        val processor = CompactBlockProcessor(config, downloader, ledger, rustBackend)

        // wrapper for rustbackend
        val wallet = Wallet(
            context = appContext,
            birthday = Wallet.loadBirthdayFromAssets(appContext, 421720),
            rustBackend = rustBackend,
            dataDbName = dataDbName,
            seedProvider = SampleSeedProvider("testreferencecarol"),
            spendingKeyProvider = SimpleProvider("dummyValue")
        )

        // Encoder
        val encoder = WalletTransactionEncoder(wallet, ledger)

        // ties everything together
        return SdkSynchronizer(
            wallet,
            ledger,
            sender,
            processor,
            encoder
        )
    }

    private fun String.toDbPath(context: Context): String {
        return context.getDatabasePath(this).absolutePath
    }
}

