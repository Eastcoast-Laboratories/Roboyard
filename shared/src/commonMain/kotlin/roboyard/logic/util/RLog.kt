package roboyard.logic.util

import co.touchlab.kermit.Logger

/**
 * Multiplatform-compatible logging facade for Roboyard shared module.
 *
 * Wraps kermit Logger to provide a Timber-compatible API with printf-style
 * format strings. Works on Android, iOS, and other KMP targets.
 *
 * Usage:
 *   private val log = RLog.tag("MyClass")
 *   log.d("Loading level %d", levelId)
 *   log.e(exception, "Error: %s", message)
 */
class RLog private constructor(private val logger: Logger) {

    fun d(message: String?, vararg args: Any?) {
        if (message != null) logger.d { formatMsg(message, args) }
    }

    fun i(message: String?, vararg args: Any?) {
        if (message != null) logger.i { formatMsg(message, args) }
    }

    fun w(message: String?, vararg args: Any?) {
        if (message != null) logger.w { formatMsg(message, args) }
    }

    fun e(message: String?, vararg args: Any?) {
        if (message != null) logger.e { formatMsg(message, args) }
    }

    fun e(t: Throwable?, message: String?, vararg args: Any?) {
        logger.e(t ?: Exception(message)) { if (message != null) formatMsg(message, args) else "" }
    }

    private fun formatMsg(message: String, args: Array<out Any?>): String {
        return if (args.isEmpty()) message else String.format(message, *args)
    }

    companion object {
        /** Create a tagged logger (Timber.tag() equivalent) */
        fun tag(tag: String): RLog = RLog(Logger.withTag(tag))

        /** Create a logger for a class (uses class simple name as tag) */
        fun <T : Any> forClass(clazz: kotlin.reflect.KClass<T>): RLog =
            RLog(Logger.withTag(clazz.simpleName ?: "Unknown"))
    }
}
