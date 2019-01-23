package cash.z.wallet.sdk.data

import kotlin.system.measureTimeMillis

/**
 * A tiny log.
 */
interface Twig {
    fun twig(logMessage: String = "")
}

/**
 * A tiny log that does nothing. No one hears this twig fall in the woods.
 */
class SilentTwig : Twig {
    override fun twig(logMessage: String) {
        // shh
    }
}

/**
 * A tiny log for detecting troubles. Aim at your troubles and pull the twigger.
 *
 * @param formatter a formatter for the twigs. The default one is pretty spiffy.
 * @param printer a printer for the twigs. The default is System.err.println.
 */
open class TroubleshootingTwig(
    val formatter: (String) -> String = spiffy(5),
    val printer: (String) -> Any = System.err::println
) : Twig {
    override fun twig(logMessage: String) {
        printer(formatter(logMessage))
    }
}

/**
 * A tiny log task. Execute the block of code with some twigging around the outside.
 */
// for silent twigs, this adds a small amount of overhead at the call site but still avoids logging
//
// note: being an extension function (i.e. static rather than a member of the Twig interface) allows this function to be
//       inlined and simplifies its use with suspend functions
//       (otherwise the function and its "block" param would have to suspend)
inline fun Twig.twigTask(logMessage: String, block: () -> Unit) {
    twig("$logMessage - started    | on thread ${Thread.currentThread().name})")
    val time = measureTimeMillis(block)
    twig("$logMessage - completed  | in ${time}ms on thread ${Thread.currentThread().name}")
}

/**
 * A tiny log formatter that makes twigs pretty spiffy.
 *
 * @param stackFrame the stack frame from which we try to derive the class. This can vary depending on how the code is
 * called so we expose it for flexibility. Jiggle the handle on this whenever the line numbers appear incorrect.
 */
inline fun spiffy(stackFrame: Int = 4, tag: String = "@TWIG"): (String) -> String = { logMessage: String ->
    val stack = Thread.currentThread().stackTrace[stackFrame]
    val time = String.format("${tag} %1\$tD %1\$tI:%1\$tM:%1\$tS.%1\$tN", System.currentTimeMillis())
    val className = stack.className.split(".").lastOrNull()?.split("\$")?.firstOrNull()
    "$time[$className:${stack.lineNumber}]    $logMessage"
}
