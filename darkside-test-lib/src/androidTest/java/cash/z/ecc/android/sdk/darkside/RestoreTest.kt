package cash.z.ecc.android.sdk.darkside

import androidx.test.ext.junit.runners.AndroidJUnit4
import cash.z.ecc.android.sdk.darkside.fixture.DarksideFixture
import cash.z.ecc.android.sdk.darkside.test.DarksideTestPrerequisites
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RestoreTest : DarksideTestPrerequisites() {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun tenPlusBlock() = runBlockingTest {
        val coordinator = DarksideFixture.newDarksideTestCoordinator()
        coordinator.reset(saplingActivationHeight = 663150, branchId = "2bb40e60", chainName = "main")
        coordinator.stageBlocks(DarksideFixture.blocksUrl)
        coordinator.chainMaker.apply {
            stageTransaction(DarksideFixture.tx663174)
            stageTransaction(DarksideFixture.tx663188)
            applyTipHeight(663200)
        }

        val synchronizer = coordinator.synchronizer
        try {
            synchronizer.start()

            coordinator.await()

        } finally {
            synchronizer.stop()
        }
    }
}