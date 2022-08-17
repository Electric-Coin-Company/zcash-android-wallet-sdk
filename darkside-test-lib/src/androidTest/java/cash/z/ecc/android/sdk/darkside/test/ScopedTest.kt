package cash.z.ecc.android.sdk.darkside.test

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.sdk.internal.TroubleshootingTwig
import cash.z.ecc.android.sdk.internal.Twig
import cash.z.ecc.android.sdk.internal.twig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import java.util.concurrent.TimeoutException

@OptIn(DelicateCoroutinesApi::class)
open class ScopedTest(val defaultTimeout: Long = 2000L) : DarksideTestPrerequisites() {
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
                SupervisorJob() + newFixedThreadPoolContext(2, this::class.java.simpleName)
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
