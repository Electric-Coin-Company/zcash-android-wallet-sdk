package cash.z.ecc.android.sdk.internal

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.Locale

/**
 * A twig is a tiny log. These logs are intended for development rather than for high performance
 * or usage in production.
 */
@Suppress("TooManyFunctions")
object Twig {
    /**
     * Format string for log messages.
     *
     * The format is: <Process> <Thread> <Class>.<method>(): <message>
     */
    private const val FORMAT = "%-27s %-30s %s.%s(): %s" // $NON-NLS-1$

    @Volatile
    private var tag: String = "Twig"

    @Volatile
    private var processName: String = ""

    /**
     *  For best results, call this method before trying to log messages.
     */
    fun initialize(context: Context) {
        tag = getApplicationName(context)
        processName = searchForProcessName(context) ?: "Unknown"
    }

    // JVMStatic is to simplify ProGuard/R8 rules for stripping this
    @JvmStatic
    fun verbose(message: () -> String) {
        Log.v(tag, formatMessage(message))
    }

    // JVMStatic is to simplify ProGuard/R8 rules for stripping this
    @JvmStatic
    fun verbose(
        throwable: Throwable,
        message: () -> String
    ) {
        Log.v(tag, formatMessage(message), throwable)
    }

    // JVMStatic is to simplify ProGuard/R8 rules for stripping this
    @JvmStatic
    fun debug(message: () -> String) {
        Log.d(tag, formatMessage(message))
    }

    // JVMStatic is to simplify ProGuard/R8 rules for stripping this
    @JvmStatic
    fun debug(
        throwable: Throwable,
        message: () -> String
    ) {
        Log.d(tag, formatMessage(message), throwable)
    }

    // JVMStatic is to simplify ProGuard/R8 rules for stripping this
    @JvmStatic
    fun info(message: () -> String) {
        Log.i(tag, formatMessage(message))
    }

    // JVMStatic is to simplify ProGuard/R8 rules for stripping this
    @JvmStatic
    fun info(
        throwable: Throwable,
        message: () -> String
    ) {
        Log.i(tag, formatMessage(message), throwable)
    }

    // JVMStatic is to simplify ProGuard/R8 rules for stripping this
    @JvmStatic
    fun warn(message: () -> String) {
        Log.w(tag, formatMessage(message))
    }

    // JVMStatic is to simplify ProGuard/R8 rules for stripping this
    @JvmStatic
    fun warn(
        throwable: Throwable,
        message: () -> String
    ) {
        Log.w(tag, formatMessage(message), throwable)
    }

    // JVMStatic is to simplify ProGuard/R8 rules for stripping this
    @JvmStatic
    fun error(message: () -> String) {
        Log.e(tag, formatMessage(message))
    }

    // JVMStatic is to simplify ProGuard/R8 rules for stripping this
    @JvmStatic
    fun error(
        throwable: Throwable,
        message: () -> String
    ) {
        Log.e(tag, formatMessage(message), throwable)
    }

    /**
     * Can be called in a release build to test that `assumenosideeffects` ProGuard rules have been
     * properly processed to strip out logging messages.
     *
     * JVMStatic is to simplify ProGuard/R8 rules for stripping this
     */
    @JvmStatic
    fun assertLoggingStripped() {
        @Suppress("MaxLineLength")
        // $NON-NLS-1$
        throw AssertionError(
            "Logging was not disabled by ProGuard or R8.  Logging should be disabled in release builds" +
                " to reduce risk of sensitive information being leaked."
        )
    }

    private const val CALL_DEPTH = 4

    private fun formatMessage(message: () -> String): String {
        val currentThread = Thread.currentThread()
        val trace = currentThread.stackTrace
        val sourceClass = trace[CALL_DEPTH].className
        val sourceMethod = trace[CALL_DEPTH].methodName

        return String.format(
            Locale.ROOT,
            FORMAT,
            processName,
            currentThread.name,
            cleanupClassName(sourceClass),
            sourceMethod,
            message()
        )
    }
}

/**
 * Gets the name of the application or the package name if the application has no name.
 *
 * @param context Application context.
 * @return Label of the application from the Android Manifest or the package name if no label
 * was set.
 */
private fun getApplicationName(context: Context): String {
    val applicationLabel = context.packageManager.getApplicationLabel(context.applicationInfo)

    return applicationLabel.toString().lowercase(Locale.ROOT).replace(" ", "-")
}

private fun cleanupClassName(classNameString: String): String {
    val outerClassName = classNameString.substringBefore('$')
    val simplerOuterClassName = outerClassName.substringAfterLast('.')
    return if (simplerOuterClassName.isEmpty()) {
        classNameString
    } else {
        simplerOuterClassName.removeSuffix("Kt")
    }
}

/**
 * @param context Application context.
 * @return Name of the current process.  May return null if a failure occurs, which is possible
 * due to some race conditions in Android.
 */
private fun searchForProcessName(context: Context): String? =
    if (AndroidApiVersion.isAtLeastTiramisu) {
        getProcessNameTPlus()
    } else if (AndroidApiVersion.isAtLeastP) {
        getProcessNamePPlus()
    } else {
        searchForProcessNameLegacy(context)
    }

@RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
private fun getProcessNameTPlus() = Process.myProcessName()

@RequiresApi(api = Build.VERSION_CODES.P)
private fun getProcessNamePPlus() = Application.getProcessName()

/**
 * @param context Application context.
 * @return Name of the current process.  May return null if a failure occurs, which is possible
 * due to some race conditions in older versions of Android.
 */
private fun searchForProcessNameLegacy(context: Context): String? {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    return activityManager.runningAppProcesses?.find { Process.myPid() == it.pid }?.processName
}
