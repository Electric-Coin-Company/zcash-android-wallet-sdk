package cash.z.wallet.sdk.sample.demoapp

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.wallet.sdk.SdkSynchronizer
import cash.z.wallet.sdk.transaction.*
import cash.z.wallet.sdk.demoapp.util.SampleStorageBridge
import cash.z.wallet.sdk.ext.*
import cash.z.wallet.sdk.secure.Wallet
import cash.z.wallet.sdk.service.LightWalletGrpcService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.BeforeClass
import org.junit.Ignore
import org.junit.Test

/**
 * Sample code to demonstrate key functionality without UI, inspired by:
 * https://github.com/EdgeApp/eosjs-node-cli/blob/paul/cleanup/app.js
 */
class SampleCodeTest {

    private val seed = "Insert seed for testing".toByteArray()
    private val lightwalletdHost: String = "34.68.177.238"

    // ///////////////////////////////////////////////////
    // Seed derivation
    @Ignore @Test fun createBip39Seed_fromSeedPhrase() {
        // TODO: log(seedPhrase.asRawEntropy().asBip39seed())
    }

    @Ignore @Test fun createRawEntropy() {
        // TODO: call: Mnemonic::from_phrase(seed_phrase, Language::English).unwrap().entropy()
        // log(seedPhrase.asRawEntropy())
    }

    @Ignore @Test fun createBip39Seed_fromRawEntropy() {
        // get the 64 byte bip39 entropy
        // TODO: call: bip39::Seed::new(&Mnemonic::from_entropy(&seed_bytes, Language::English).unwrap(), "")
        // log(rawEntropy.asBip39Seed())
    }

    @Ignore @Test fun deriveSeedPhraseFrom() {
        // TODO: let mnemonic = Mnemonic::from_entropy(entropy, Language::English).unwrap();
        // log(entropy.asSeedPhrase())
    }


    /////////////////////////////////////////////////////
    // Derive Extended Spending Key
    @Test fun deriveSpendingKey() {
        val wallet = Wallet()
        val privateKeys = wallet.initialize(context, seed)
        assertNotNull("Wallet already existed.", privateKeys)

        log("Spending Key: ${privateKeys?.get(0)}")
        log("Address: ${wallet.getAddress()}")
    }

    /////////////////////////////////////////////////////
    // Derive address from Extended Spending Key
    @Test fun getAddressFromSpendingKey() {
        // TODO: turn spending key into viewing key via:
        // let extfvks: Vec<_> = extsks.iter().map(ExtendedFullViewingKey::from).collect();
//        val viewingKey = spendingKey //.asViewingKey()
//        return getAddressFromViewingKey(viewingKey)
    }

    /////////////////////////////////////////////////////
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

    // ///////////////////////////////////////////////////
    // Create a signed transaction (with memo)
    @Test fun createTransaction() = runBlocking {
        val wallet = Wallet()
        val repository = PagedTransactionRepository(context)
        val keyManager = SampleStorageBridge().securelyStoreSeed(seed)
        val encoder = WalletTransactionEncoder(wallet, repository, keyManager)
        val amount = 0.123.toZec().convertZecToZatoshi()
        val address = "ztestsapling1tklsjr0wyw0d58f3p7wufvrj2cyfv6q6caumyueadq8qvqt8lda6v6tpx474rfru9y6u75u7qnw"
        val memo = "Test Transaction"
        val encodedTx = encoder.create(amount, address, memo ?: "")
    }

    // ///////////////////////////////////////////////////
    // Create a signed transaction (with memo) and broadcast
    @Test fun submitTransaction() = runBlocking {
        val amount = 0.123.toZec().convertZecToZatoshi()
        val address = "ztestsapling1tklsjr0wyw0d58f3p7wufvrj2cyfv6q6caumyueadq8qvqt8lda6v6tpx474rfru9y6u75u7qnw"
        val memo = "Test Transaction"
        val transaction = synchronizer.sendToAddress(amount, address, memo)
        log("transaction: $transaction")
    }



    ///////////////////////////////////////////////////////
    // Utility Functions
    //////////////////////////////////////////////////////

    companion object {
        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private lateinit var synchronizer: SdkSynchronizer

        @BeforeClass
        @JvmStatic
        fun init() {
            Twig.plant(TroubleshootingTwig())
            reset()
        }

        fun log(message: String?) = twig(message ?: "null")

        private fun reset() {
            context.getDatabasePath(ZcashSdk.DB_DATA_NAME).absoluteFile.delete()
            context.getDatabasePath(ZcashSdk.DB_CACHE_NAME).absoluteFile.delete()
        }


        private fun ByteArray.toHex(): String {
            val sb = StringBuilder(size * 2)
            for (b in this)
                sb.append(String.format("%02x", b))
            return sb.toString()
        }
    }
}
