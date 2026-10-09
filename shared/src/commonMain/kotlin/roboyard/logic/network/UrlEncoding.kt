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

/**
 * Decodes application/x-www-form-urlencoded data, matching
 * java.net.URLDecoder.decode(value, "UTF-8"): '+' becomes space, %XX
 * sequences are UTF-8 bytes. Malformed input is decoded leniently
 * (literal characters pass through).
 */
fun urlDecodeUtf8(value: String): String {
    val bytes = ArrayList<Byte>(value.length)
    var i = 0
    while (i < value.length) {
        val c = value[i]
        when {
            c == '+' -> bytes.add(' '.code.toByte())
            c == '%' && i + 2 < value.length -> {
                val hex = value.substring(i + 1, i + 3).toIntOrNull(16)
                if (hex != null) {
                    bytes.add(hex.toByte())
                    i += 2
                } else {
                    bytes.add(c.code.toByte())
                }
            }
            else -> {
                // encode the char itself as UTF-8 (handles non-ASCII literals)
                c.toString().encodeToByteArray().forEach { bytes.add(it) }
            }
        }
        i++
    }
    return bytes.toByteArray().decodeToString()
}
