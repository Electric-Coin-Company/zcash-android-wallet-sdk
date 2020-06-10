package cash.z.ecc.android.sdk.ext

import cash.z.ecc.android.sdk.Initializer
import cash.z.ecc.android.sdk.Initializer.DefaultBirthdayStore.Companion.ImportedWalletBirthdayStore
import cash.z.ecc.android.sdk.util.SimpleMnemonics
import kotlinx.coroutines.*
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import java.util.concurrent.TimeoutException

fun Initializer.import(
    seedPhrase: String,
    birthdayHeight: Int,
    alias: String = ZcashSdk.DEFAULT_DB_NAME_PREFIX
) {
    SimpleMnemonics().toSeed(seedPhrase.toCharArray()).let { seed ->
        ImportedWalletBirthdayStore(context, birthdayHeight, alias).getBirthday().let {
            import(seed, it, true, true)
        }
    }
}

open class ScopedTest(val defaultTimeout: Long = 2000L) {
    protected lateinit var testScope: CoroutineScope

    @Before
    fun start() {
        twig("===================== TEST STARTED ==================================")
        testScope = CoroutineScope(
            Job(classScope.coroutineContext[Job]!!) + newFixedThreadPoolContext(5, this.javaClass.simpleName)
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
        fun destroyScope() = runBlocking<Unit>{
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
