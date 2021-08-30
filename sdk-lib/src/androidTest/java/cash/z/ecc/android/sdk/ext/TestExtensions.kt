package cash.z.ecc.android.sdk.ext

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.type.ZcashNetwork
import cash.z.ecc.android.sdk.util.DarksideTestCoordinator
import cash.z.ecc.android.sdk.util.SimpleMnemonics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import ru.gildor.coroutines.okhttp.await
import java.util.concurrent.TimeoutException

fun Initializer.Config.seedPhrase(seedPhrase: String, network: ZcashNetwork) {
    setSeed(SimpleMnemonics().toSeed(seedPhrase.toCharArray()), network)
}

open class ScopedTest(val defaultTimeout: Long = 2000L) {
    protected lateinit var testScope: CoroutineScope

    // if an androidTest doesn't need a context, then maybe it should be a unit test instead?!
    val context: Context = InstrumentationRegistry.getInstrumentation().context

    @Before
    fun start() {
        twig("===================== TEST STARTED ==================================")
        testScope = CoroutineScope(
            Job(classScope.coroutineContext[Job]!!) + newFixedThreadPoolContext(
                5,
                this.javaClass.simpleName
            )
        )
    }

    @After
    fun end() = runBlocking<Unit> {
        twig("======================= TEST CANCELLING =============================")
        testScope.cancel()
        testScope.coroutineContext[Job]?.join()
        twig("======================= TEST ENDED ==================================")
    }

    fun timeout(duration: Long, block: suspend () -> Unit) = timeoutWith(testScope, duration, block)

    companion object {
        @JvmStatic
        lateinit var classScope: CoroutineScope

        init {
            Twig.plant(TroubleshootingTwig())
            twig("================================================================ INIT")
        }

        @BeforeClass
        @JvmStatic
        fun createScope() {
            twig("======================= CLASS STARTED ===============================")
            classScope = CoroutineScope(
                SupervisorJob() + newFixedThreadPoolContext(2, this.javaClass.simpleName)
            )
        }

        @AfterClass
        @JvmStatic
        fun destroyScope() = runBlocking<Unit> {
            twig("======================= CLASS CANCELLING ============================")
            classScope.cancel()
            classScope.coroutineContext[Job]?.join()
            twig("======================= CLASS ENDED =================================")
        }

        @JvmStatic
        fun timeoutWith(scope: CoroutineScope, duration: Long, block: suspend () -> Unit) {
            scope.launch {
                delay(duration)
                val message = "ERROR: Test timed out after ${duration}ms"
                twig(message)
                throw TimeoutException(message)
            }.let { selfDestruction ->
                scope.launch {
                    block()
                    selfDestruction.cancel()
                }
            }
        }
    }
}

open class DarksideTest(name: String = javaClass.simpleName) : ScopedTest() {
    val sithLord = DarksideTestCoordinator(host = host, port = port)
    val validator = sithLord.validator

    fun runOnce(block: () -> Unit) {
        if (!ranOnce) {
            sithLord.enterTheDarkside()
            sithLord.synchronizer.start(classScope)
            block()
            ranOnce = true
        }
    }
    companion object {
        // set the host for all tests. Someday, this will need to be set by CI
        // so have it read from the environment first and give that precidence
        var host = "192.168.1.134"
        val port: Int = 9067
        private var ranOnce = false
    }
}

object BlockExplorer {
    suspend fun fetchLatestHeight(): Int {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.blockchair.com/zcash/blocks?limit=1")
            .build()
        val result = client.newCall(request).await()
        val body = result.body()?.string()
        return JSONObject(body).getJSONArray("data").getJSONObject(0).getInt("id")
    }
}

object Transactions {
    val outbound = arrayOf(
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/t-shielded-spend.txt",
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/sent/c9e35e6ff444b071d63bf9bab6480409d6361760445c8a28d24179adb35c2495.txt",
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/sent/72a29d7db511025da969418880b749f7fc0fc910cdb06f52193b5fa5c0401d9d.txt",
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/sent/ff6ea36765dc29793775c7aa71de19fca039c5b5b873a0497866e9c4bc48af01.txt",
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/sent/34e507cab780546f980176f3ff2695cd404917508c7e5ee18cc1d2ff3858cb08.txt",
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/sent/6edf869063eccff3345676b0fed9f1aa6988fb2524e3d9ca7420a13cfadcd76c.txt",
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/sent/de97394ae220c28a33ba78b944e82dabec8cb404a4407650b134b3d5950358c0.txt",
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/sent/4eaa902279f8380914baf5bcc470d8b7c11d84fda809f67f517a7cb48912b87b.txt",
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/sent/73c5edf8ffba774d99155121ccf07e67fbcf14284458f7e732751fea60d3bcbc.txt"
    )

    val inbound = arrayOf(
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/8f064d23c66dc36e32445e5f3b50e0f32ac3ddb78cff21fb521eb6c19c07c99a.txt",
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/15a677b6770c5505fb47439361d3d3a7c21238ee1a6874fdedad18ae96850590.txt",
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/d2e7be14bbb308f9d4d68de424d622cbf774226d01cd63cc6f155fafd5cd212c.txt",
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/e6566be3a4f9a80035dab8e1d97e40832a639e3ea938fb7972ea2f8482ff51ce.txt",
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/0821a89be7f2fc1311792c3fa1dd2171a8cdfb2effd98590cbd5ebcdcfcf491f.txt",
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/e9527891b5d43d1ac72f2c0a3ac18a33dc5a0529aec04fa600616ed35f8123f8.txt",
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/4dcc95dd0a2f1f51bd64bb9f729b423c6de1690664a1b6614c75925e781662f7.txt",
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/75f2cdd2ff6a94535326abb5d9e663d53cbfa5f31ebb24b4d7e420e9440d41a2.txt",
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/7690c8ec740c1be3c50e2aedae8bf907ac81141ae8b6a134c1811706c73f49a6.txt",
        "https://raw.githubusercontent.com/zcash-hackworks/darksidewalletd-test-data/master/transactions/recv/71935e29127a7de0b96081f4c8a42a9c11584d83adedfaab414362a6f3d965cf.txt"
    )
}
