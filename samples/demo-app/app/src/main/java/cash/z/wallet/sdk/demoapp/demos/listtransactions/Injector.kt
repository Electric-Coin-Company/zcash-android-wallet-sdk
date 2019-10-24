package cash.z.wallet.sdk.demoapp.demos.listtransactions

import cash.z.wallet.sdk.transaction.PagedTransactionRepository
import cash.z.wallet.sdk.Synchronizer
import cash.z.wallet.sdk.transaction.TransactionRepository
import cash.z.wallet.sdk.demoapp.App
import cash.z.wallet.sdk.demoapp.util.SampleStorageBridge
import cash.z.wallet.sdk.secure.Wallet

object Injector {
    private val appContext = App.instance
    private val sampleSeed = App.instance.defaultConfig.seed
    private val birthdayHeight: Int = App.instance.defaultConfig.birthdayHeight
    private val host: String = App.instance.defaultConfig.host

    private fun provideKeyManager(): Wallet.KeyManager {
        return SampleStorageBridge().securelyStoreSeed(sampleSeed)
    }
    
    private fun provideWallet(keyManager: Wallet.KeyManager): Wallet {
        return Wallet().apply {
            initialize(appContext, keyManager.seed, birthdayHeight)?.let { privateKeys ->
                keyManager.key = privateKeys.first()
            }
        }
    }

    fun provideLedger(): PagedTransactionRepository {
        return PagedTransactionRepository(appContext, 2)
    }

    fun provideSynchronizer(ledger: TransactionRepository): Synchronizer {
        val keyManager = provideKeyManager()
        return Synchronizer(
            appContext, provideWallet(keyManager), host, keyManager,
            ledger = ledger
        )
    }
}