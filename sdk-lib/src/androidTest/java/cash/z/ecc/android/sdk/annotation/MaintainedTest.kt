package cash.z.ecc.android.sdk.annotation

enum class TestPurpose {

    /**
     * These tests are explicitly designed to preserve behavior that we do not want to lose after
     * major upgrades or refactors. It is acceptable for these test to run long and require
     * additional infrastructure.
     */
    REGRESSION,

    /**
     * These tests are designed to be run against new pull requests and generally before any changes
     * are committed. It is not ideal for these tests to run long.
     */
    COMMIT,

    /**
     * These tests require a running instance of
     * [darksidewalletd](https://github.com/zcash/lightwalletd/blob/master/docs/darksidewalletd.md).
     */
    DARKSIDE
}

/**
 * Signals that this test is explicitly intended to be maintained and run regularly in order to
 * achieve the given purpose. Eventually, we will run all such tests nightly.
 */
@Target(AnnotationTarget.CLASS)
annotation class MaintainedTest(vararg val purpose: TestPurpose)
