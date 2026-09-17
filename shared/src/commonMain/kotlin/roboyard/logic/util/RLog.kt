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
class RLog private constructor(private val logger: Logger, private val tag: String) {

    fun d(message: String?, vararg args: Any?) {
        if (message != null) {
            val msg = formatMsg(message, args)
            LogBuffer.add("D", tag, msg)
            logger.d { msg }
        }
    }

    fun i(message: String?, vararg args: Any?) {
        if (message != null) {
            val msg = formatMsg(message, args)
            LogBuffer.add("I", tag, msg)
            logger.i { msg }
        }
    }

    fun w(message: String?, vararg args: Any?) {
        if (message != null) {
            val msg = formatMsg(message, args)
            LogBuffer.add("W", tag, msg)
            logger.w { msg }
        }
    }

    fun e(message: String?, vararg args: Any?) {
        if (message != null) {
            val msg = formatMsg(message, args)
            LogBuffer.add("E", tag, msg)
            logger.e { msg }
        }
    }

    fun e(t: Throwable?, message: String?, vararg args: Any?) {
        val msg = if (message != null) formatMsg(message, args) else ""
        LogBuffer.add("E", tag, "$msg ${t?.message ?: ""}")
        logger.e(t ?: Exception(message)) { msg }
    }

    private fun formatMsg(message: String, args: Array<out Any?>): String {
        if (args.isEmpty()) return message
        var result = message
        var argIndex = 0
        val regex = Regex("%[sd]")
        regex.findAll(message).forEach { match ->
            if (argIndex < args.size) {
                result = result.replaceFirst(match.value, args[argIndex].toString())
                argIndex++
            }
        }
        return result
    }

    companion object {
        /** Create a tagged logger (Timber.tag() equivalent) */
        fun tag(tag: String): RLog = RLog(Logger.withTag(tag), tag)

        /** Create a logger for a class (uses class simple name as tag) */
        fun <T : Any> forClass(clazz: kotlin.reflect.KClass<T>): RLog {
            val tag = clazz.simpleName ?: "Unknown"
            return RLog(Logger.withTag(tag), tag)
        }
    }
}

/**
 * In-memory ring buffer of recent log lines — desktop equivalent of the
 * Android "View Logs" logcat viewer (keeps the last 500 lines like
 * `logcat -d -t 500`).
 */
object LogBuffer {
    private const val MAX_LINES = 500
    private val buffer = ArrayDeque<String>()

    @Synchronized
    fun add(level: String, tag: String, message: String) {
        buffer.addLast("$level/$tag: $message")
        while (buffer.size > MAX_LINES) buffer.removeFirst()
    }

    @Synchronized
    fun getLines(): List<String> = buffer.toList()

    @Synchronized
    fun clear() = buffer.clear()
}
