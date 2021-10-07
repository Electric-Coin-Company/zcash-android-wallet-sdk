package cash.z.ecc.android.sdk

import cash.z.ecc.android.sdk.integration.SanityTest
import cash.z.ecc.android.sdk.integration.SmokeTest
import cash.z.ecc.android.sdk.integration.service.ChangeServiceTest
import cash.z.ecc.android.sdk.jni.BranchIdTest
import cash.z.ecc.android.sdk.jni.TransparentTest
import cash.z.ecc.android.sdk.internal.transaction.PersistentTransactionManagerTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Suite of tests to run before submitting a pull request.
 *
 * For now, these are just the tests that are known to be recently updated and that pass. In the
 * near future this suite will contain only fast running tests that can be used to quickly validate
 * that a PR hasn't broken anything major.
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Fast tests that only run locally and don't require darksidewalletd or lightwalletd
    BranchIdTest::class,
    TransparentTest::class,
    PersistentTransactionManagerTest::class,

    // potentially exclude because these are long-running (and hit external srvcs)
    SanityTest::class,

    // potentially exclude because these hit external services
    ChangeServiceTest::class,
    SmokeTest::class,
)
class PullRequestSuite
