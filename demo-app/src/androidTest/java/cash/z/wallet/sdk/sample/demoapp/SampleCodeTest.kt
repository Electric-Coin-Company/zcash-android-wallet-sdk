package cash.z.wallet.sdk.sample.demoapp

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.db.entity.isFailure
import cash.z.ecc.android.sdk.internal.TroubleshootingTwig
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import cash.z.ecc.android.sdk.ext.toHex
import cash.z.ecc.android.sdk.internal.twig
import cash.z.ecc.android.sdk.internal.service.LightWalletGrpcService
import cash.z.ecc.android.sdk.tool.DerivationTool
import cash.z.ecc.android.sdk.type.ZcashNetwork
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

/**
 * Sample code to demonstrate key functionality without UI, inspired by:
 * https://github.com/EdgeApp/eosjs-node-cli/blob/paul/cleanup/app.js
 */
class SampleCodeTest {

    // ///////////////////////////////////////////////////
    // Seed derivation
    @Ignore("This test is not implemented") @Test fun createBip39Seed_fromSeedPhrase() {
        // TODO: log(seedPhrase.asRawEntropy().asBip39seed())
    }

    @Ignore("This test is not implemented") @Test fun createRawEntropy() {
        // TODO: call: Mnemonic::from_phrase(seed_phrase, Language::English).unwrap().entropy()
        // log(seedPhrase.asRawEntropy())
    }

    @Ignore("This test is not implemented") @Test fun createBip39Seed_fromRawEntropy() {
        // get the 64 byte bip39 entropy
        // TODO: call: bip39::Seed::new(&Mnemonic::from_entropy(&seed_bytes, Language::English).unwrap(), "")
        // log(rawEntropy.asBip39Seed())
    }

    @Ignore("This test is not implemented") @Test fun deriveSeedPhraseFrom() {
        // TODO: let mnemonic = Mnemonic::from_entropy(entropy, Language::English).unwrap();
        // log(entropy.asSeedPhrase())
    }

    // ///////////////////////////////////////////////////
    // Derive Extended Spending Key
    @Test fun deriveSpendingKey() {
        val spendingKeys = DerivationTool.deriveSpendingKeys(seed, ZcashNetwork.Mainnet)
        assertEquals(1, spendingKeys.size)
        log("Spending Key: ${spendingKeys?.get(0)}")
    }

    // ///////////////////////////////////////////////////
    // Get Address
    @Test fun getAddress() = runBlocking {
        val address = synchronizer.getAddress()
        assertFalse(address.isNullOrBlank())
        log("Address: $address")
    }

    // ///////////////////////////////////////////////////
    // Derive address from Extended Full Viewing Key
    @Test fun getAddressFromViewingKey() {
    }

    // ///////////////////////////////////////////////////
    // Query latest block height
    @Test fun getLatestBlockHeightTest() {
        val lightwalletService = LightWalletGrpcService(context, lightwalletdHost)
        log("Latest Block: ${lightwalletService.getLatestBlockHeight()}")
    }

    // ///////////////////////////////////////////////////
    // Download compact block range
    @Test fun getBlockRange() {
        val blockRange = 500_000..500_009
        val lightwalletService = LightWalletGrpcService(context, lightwalletdHost)
        val blocks = lightwalletService.getBlockRange(blockRange)
        assertEquals(blockRange.count(), blocks.size)

        blocks.forEachIndexed { i, block ->
            log("Block #$i:    height:${block.height}   hash:${block.hash.toByteArray().toHex()}")
        }
    }

    // ///////////////////////////////////////////////////
    // Query account outgoing transactions
    @Test fun queryOutgoingTransactions() {
    }

    // ///////////////////////////////////////////////////
    // Query account incoming transactions
    @Test fun queryIncomingTransactions() {
    }

//    // ///////////////////////////////////////////////////
//    // Create a signed transaction (with memo)
//    @Test fun createTransaction() = runBlocking {
//        val rustBackend = RustBackend.init(context)
//        val repository = PagedTransactionRepository(context)
//        val encoder = WalletTransactionEncoder(rustBackend, repository)
//        val spendingKey = DerivationTool.deriveSpendingKeys(seed, ZcashNetwork.Mainnet)[0]
//
//        val amount = 0.123.convertZecToZatoshi()
//        val address = "ztestsapling1tklsjr0wyw0d58f3p7wufvrj2cyfv6q6caumyueadq8qvqt8lda6v6tpx474rfru9y6u75u7qnw"
//        val memo = "Test Transaction".toByteArray()
//
//        val encodedTx = encoder.createTransaction(spendingKey, amount, address, memo)
//        assertTrue(encodedTx.raw.isNotEmpty())
//        log("Transaction ID: ${encodedTx.txId.toHex()}")
//    }

    // ///////////////////////////////////////////////////
    // Create a signed transaction (with memo) and broadcast
    @Test fun submitTransaction() = runBlocking {
        val amount = 0.123.convertZecToZatoshi()
        val address = "ztestsapling1tklsjr0wyw0d58f3p7wufvrj2cyfv6q6caumyueadq8qvqt8lda6v6tpx474rfru9y6u75u7qnw"
        val memo = "Test Transaction"
        val spendingKey = DerivationTool.deriveSpendingKeys(seed, ZcashNetwork.Mainnet)[0]
        val transactionFlow = synchronizer.sendToAddress(spendingKey, amount, address, memo)
        transactionFlow.collect {
            log("pending transaction updated $it")
            assertTrue("Failed to send funds. See log for details.", !it?.isFailure())
        }
    }

    // /////////////////////////////////////////////////////
    // Utility Functions
    // ////////////////////////////////////////////////////

    companion object {
        private val seed = "Insert seed for testing".toByteArray()
        private val lightwalletdHost: String = ZcashNetwork.Mainnet.defaultHost

        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private val synchronizer = Synchronizer(Initializer(context) {})

        @BeforeClass
        @JvmStatic
        fun init() {
            Twig.plant(TroubleshootingTwig())
        }

        fun log(message: String?) = twig(message ?: "null")
    }
}
