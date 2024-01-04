package cash.z.ecc.android.sdk.test

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
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
open class ScopedTest(val defaultTimeout: Long = 2000L) {
    protected lateinit var testScope: CoroutineScope

    // if an androidTest doesn't need a context, then maybe it should be a unit test instead?!
    val context: Context = InstrumentationRegistry.getInstrumentation().context

    @Before
    fun start() {
        testScope =
            CoroutineScope(
                Job(classScope.coroutineContext[Job]!!) +
                    newFixedThreadPoolContext(
                        5,
                        this.javaClass.simpleName
                    )
            )
    }

    @After
    fun end() =
        runBlocking<Unit> {
            testScope.cancel()
            testScope.coroutineContext[Job]?.join()
        }

    fun timeout(
        duration: Long,
        block: suspend () -> Unit
    ) = timeoutWith(testScope, duration, block)

    companion object {
        @JvmStatic
        lateinit var classScope: CoroutineScope

        @BeforeClass
        @JvmStatic
        fun createScope() {
            classScope =
                CoroutineScope(
                    SupervisorJob() + newFixedThreadPoolContext(2, this::class.java.simpleName)
                )
        }

        @AfterClass
        @JvmStatic
        fun destroyScope() =
            runBlocking<Unit> {
                classScope.cancel()
                classScope.coroutineContext[Job]?.join()
            }

        @JvmStatic
        fun timeoutWith(
            scope: CoroutineScope,
            duration: Long,
            block: suspend () -> Unit
        ) {
            scope.launch {
                delay(duration)
                val message = "ERROR: Test timed out after ${duration}ms"
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
