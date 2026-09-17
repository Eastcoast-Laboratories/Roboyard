package roboyard.logic.network

/**
 * Percent-encodes a string for use in a URL query component, matching the
 * semantics of java.net.URLEncoder (application/x-www-form-urlencoded):
 * alphanumerics and ".-*_" stay literal, space becomes '+', everything else
 * is UTF-8 percent-encoded.
 */
fun urlEncodeUtf8(value: String): String {
    val out = StringBuilder(value.length)
    for (b in value.encodeToByteArray()) {
        val c = b.toInt() and 0xFF
        when {
            c in 'A'.code..'Z'.code || c in 'a'.code..'z'.code || c in '0'.code..'9'.code ||
                c == '.'.code || c == '-'.code || c == '*'.code || c == '_'.code ->
                out.append(c.toChar())
            c == ' '.code -> out.append('+')
            else -> {
                out.append('%')
                out.append("0123456789ABCDEF"[c shr 4])
                out.append("0123456789ABCDEF"[c and 0x0F])
            }
        }
    }
    return out.toString()
}
