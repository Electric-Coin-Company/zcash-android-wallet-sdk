package cash.z.wallet.sdk.sample.demoapp

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.Synchronizer
import cash.z.ecc.android.sdk.WalletInitMode
import cash.z.ecc.android.sdk.demoapp.ext.Mainnet
import cash.z.ecc.android.sdk.demoapp.ext.defaultForNetwork
import cash.z.ecc.android.sdk.demoapp.util.fromResources
import cash.z.ecc.android.sdk.ext.convertZecToZatoshi
import cash.z.ecc.android.sdk.ext.toHex
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.model.Account
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.ZcashNetwork
import cash.z.ecc.android.sdk.tool.DerivationTool
import co.electriccoin.lightwallet.client.LightWalletClient
import co.electriccoin.lightwallet.client.model.BlockHeightUnsafe
import co.electriccoin.lightwallet.client.model.CompactBlockUnsafe
import co.electriccoin.lightwallet.client.model.LightWalletEndpoint
import co.electriccoin.lightwallet.client.model.Response
import co.electriccoin.lightwallet.client.new
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

/**
 * Sample code to demonstrate key functionality without UI, inspired by:
 * https://github.com/EdgeApp/eosjs-node-cli/blob/paul/cleanup/app.js
 */
class SampleCodeTest {
    // ///////////////////////////////////////////////////
    // Seed derivation
    @Ignore("This test is not implemented")
    @Test
    fun createBip39Seed_fromSeedPhrase() {
        // TODO: log(seedPhrase.asRawEntropy().asBip39seed())
    }

    @Ignore("This test is not implemented")
    @Test
    fun createRawEntropy() {
        // TODO: call: Mnemonic::from_phrase(seed_phrase, Language::English).unwrap().entropy()
        // log(seedPhrase.asRawEntropy())
    }

    @Ignore("This test is not implemented")
    @Test
    fun createBip39Seed_fromRawEntropy() {
        // get the 64 byte bip39 entropy
        // TODO: call: bip39::Seed::new(&Mnemonic::from_entropy(&seed_bytes, Language::English).unwrap(), "")
        // log(rawEntropy.asBip39Seed())
    }

    @Ignore("This test is not implemented")
    @Test
    fun deriveSeedPhraseFrom() {
        // TODO: let mnemonic = Mnemonic::from_entropy(entropy, Language::English).unwrap();
        // log(entropy.asSeedPhrase())
    }

    // ///////////////////////////////////////////////////
    // Get Address
    @Test
    fun getAddress() =
        runBlocking {
            val address = synchronizer.getUnifiedAddress(Account.DEFAULT)
            assertFalse(address.isBlank())
            log("Address: $address")
        }

    // ///////////////////////////////////////////////////
    // Derive address from Extended Full Viewing Key
    @Test
    fun getAddressFromViewingKey() {
    }

    // ///////////////////////////////////////////////////
    // Query latest block height
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getLatestBlockHeightTest() =
        runTest {
            // Test the result, only if there is no server communication problem.
            runCatching {
                LightWalletClient.new(context, lightwalletdHost).getLatestBlockHeight()
            }.onFailure {
                Twig.debug(it) { "Failed to retrieve data" }
            }.onSuccess {
                assertTrue(it is Response.Success<BlockHeightUnsafe>)
                Twig.debug { "Latest Block: ${(it as Response.Success<BlockHeightUnsafe>).result}" }
            }
        }

    // ///////////////////////////////////////////////////
    // Download compact block range
    @Test
    fun getBlockRange() =
        runTest {
            @Suppress("ktlint:standard:multiline-expression-wrapping")
            val blockRange =
                BlockHeightUnsafe(
                    BlockHeight.new(
                        ZcashNetwork.Mainnet,
                        500_000
                    ).value
                )..BlockHeightUnsafe(
                    (
                        BlockHeight.new(
                            ZcashNetwork.Mainnet,
                            500_009
                        ).value
                    )
                )

            val lightWalletClient = LightWalletClient.new(context, lightwalletdHost)

            // Test the result, only if there is no server communication problem.
            runCatching {
                lightWalletClient.getBlockRange(blockRange)
            }.onFailure {
                Twig.debug(it) { "Failed to retrieve data" }
            }.onSuccess {
                it.onEach { response ->
                    assert(response is Response.Success) { "Server communication failed." }
                }
                    .filterIsInstance<Response.Success<CompactBlockUnsafe>>()
                    .map { response ->
                        response.result
                    }.toList()
                    .also { blocks ->
                        assertEquals(blockRange.endInclusive.value - blockRange.start.value, blocks.count())

                        blocks.forEachIndexed { i, block ->
                            log("Block #$i:    height:${block.height}   hash:${block.hash.toHex()}")
                        }
                    }
            }
        }

    // ///////////////////////////////////////////////////
    // Query account outgoing transactions
    @Test
    fun queryOutgoingTransactions() {
    }

    // ///////////////////////////////////////////////////
    // Query account incoming transactions
    @Test
    fun queryIncomingTransactions() {
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
    @Test
    fun submitTransaction() =
        runBlocking {
            val amount = 0.123.convertZecToZatoshi()
            val address = "ztestsapling1tklsjr0wyw0d58f3p7wufvrj2cyfv6q6caumyueadq8qvqt8lda6v6tpx474rfru9y6u75u7qnw"
            val memo = "Test Transaction"
            val spendingKey =
                DerivationTool.getInstance().deriveUnifiedSpendingKey(
                    seed,
                    ZcashNetwork.Mainnet,
                    Account.DEFAULT
                )
            synchronizer.createProposedTransactions(
                synchronizer.proposeTransfer(
                    spendingKey.account,
                    address,
                    amount,
                    memo
                ),
                spendingKey
            )
        }

    // /////////////////////////////////////////////////////
    // Utility Functions
    // ////////////////////////////////////////////////////

    companion object {
        private val seed = "Insert seed for testing".toByteArray()
        private val lightwalletdHost = LightWalletEndpoint.Mainnet

        private val context = InstrumentationRegistry.getInstrumentation().targetContext
        private val synchronizer: Synchronizer =
            run {
                val network = ZcashNetwork.fromResources(context)
                Synchronizer.newBlocking(
                    context,
                    network,
                    lightWalletEndpoint = LightWalletEndpoint.defaultForNetwork(network),
                    seed = seed,
                    birthday = null,
                    // Using existing wallet init mode as simplification for the test
                    walletInitMode = WalletInitMode.ExistingWallet
                )
            }

        fun log(message: String?) = Twig.debug { message ?: "null" }
    }
}
