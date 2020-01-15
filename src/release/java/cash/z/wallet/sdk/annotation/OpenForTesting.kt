package cash.z.wallet.sdk.annotation

/**
 * Used in conjunction with the kotlin-allopen plugin to make any class with this annotation open for extension.
 * Typically, we apply this to classes that we want to mock in androidTests because unit tests don't have this problem,
 * it's only an issue with JUnit4 Instrumentation tests. This annotation is only leveraged in debug builds.
 *
 * Note: the counterpart to this annotation in the debug buildType applies the OpenClass annotation but here we do not.
 */
@Target(AnnotationTarget.CLASS)
annotation class OpenForTesting