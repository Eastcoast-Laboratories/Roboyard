package roboyard.logic.json

/**
 * Minimal Gson-compatible JSON model for Kotlin Multiplatform.
 *
 * Mirrors the com.google.gson API surface used by the shared module so the
 * wire format stays byte-identical: insertion-ordered objects, compact
 * serialization, Gson's HTML-safe string escaping. Parsing is strict JSON.
 */
sealed class JsonElement {
    open val isJsonObject: Boolean get() = false
    open val isJsonArray: Boolean get() = false
    open val isJsonNull: Boolean get() = false
    open val isJsonPrimitive: Boolean get() = false

    open val asJsonObject: JsonObject
        get() = throw UnsupportedOperationException("Not a JsonObject: $this")
    open val asJsonArray: JsonArray
        get() = throw UnsupportedOperationException("Not a JsonArray: $this")
    open val asJsonPrimitive: JsonPrimitive
        get() = throw UnsupportedOperationException("Not a JsonPrimitive: $this")

    open val asString: String
        get() = throw UnsupportedOperationException("Not a primitive: $this")
    open val asInt: Int get() = asNumber.toInt()
    open val asLong: Long get() = asNumber.toLong()
    open val asDouble: Double get() = asNumber.toDouble()
    open val asBoolean: Boolean
        get() = throw UnsupportedOperationException("Not a primitive: $this")
    open val asNumber: Number
        get() = throw UnsupportedOperationException("Not a number: $this")
}

object JsonNull : JsonElement() {
    override val isJsonNull: Boolean get() = true
    val INSTANCE: JsonNull get() = this
    override fun toString(): String = "null"
}

class JsonPrimitive : JsonElement {
    private val value: Any

    constructor(value: String?) {
        this.value = value ?: JsonNull
    }

    constructor(value: Number) {
        this.value = value
    }

    constructor(value: Boolean) {
        this.value = value
    }

    override val isJsonPrimitive: Boolean get() = true
    override val asJsonPrimitive: JsonPrimitive get() = this

    val isBoolean: Boolean get() = value is Boolean
    val isNumber: Boolean get() = value is Number
    val isString: Boolean get() = value is String

    override val asBoolean: Boolean
        get() = when (val v = value) {
            is Boolean -> v
            is String -> v.toBoolean()
            else -> throw UnsupportedOperationException("Not a boolean: $v")
        }

    override val asNumber: Number
        get() = when (val v = value) {
            is Number -> v
            is String -> v.toLongOrNull() ?: v.toDoubleOrNull()
                ?: throw NumberFormatException("Not a number: $v")
            else -> throw UnsupportedOperationException("Not a number: $v")
        }

    override val asString: String
        get() = when (val v = value) {
            is String -> v
            is Boolean -> v.toString()
            is Number -> numberToString(v)
            JsonNull -> "null"
            else -> v.toString()
        }

    override fun toString(): String = when (val v = value) {
        is String -> quote(v)
        is Boolean -> v.toString()
        is Number -> numberToString(v)
        JsonNull -> "null"
        else -> quote(v.toString())
    }
}

class JsonObject : JsonElement() {
    private val members = LinkedHashMap<String, JsonElement>()

    override val isJsonObject: Boolean get() = true
    override val asJsonObject: JsonObject get() = this

    fun add(property: String, value: JsonElement?) {
        members[property] = value ?: JsonNull
    }

    fun addProperty(property: String, value: String?) {
        add(property, value?.let { JsonPrimitive(it) })
    }

    fun addProperty(property: String, value: Number?) {
        add(property, value?.let { JsonPrimitive(it) })
    }

    fun addProperty(property: String, value: Boolean?) {
        add(property, value?.let { JsonPrimitive(it) })
    }

    operator fun get(memberName: String): JsonElement? = members[memberName]

    fun has(memberName: String): Boolean = members.containsKey(memberName)

    fun remove(memberName: String): JsonElement? = members.remove(memberName)

    fun getAsJsonObject(memberName: String): JsonObject =
        members[memberName]!!.asJsonObject

    fun getAsJsonArray(memberName: String): JsonArray =
        members[memberName]!!.asJsonArray

    fun entrySet(): Set<Map.Entry<String, JsonElement>> = members.entries

    fun keySet(): Set<String> = members.keys

    fun size(): Int = members.size

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append('{')
        var first = true
        for ((k, v) in members) {
            if (!first) sb.append(',')
            first = false
            sb.append(quote(k)).append(':').append(v.toString())
        }
        sb.append('}')
        return sb.toString()
    }
}

class JsonArray : JsonElement(), Iterable<JsonElement> {
    private val elements = ArrayList<JsonElement>()

    override val isJsonArray: Boolean get() = true
    override val asJsonArray: JsonArray get() = this

    fun add(element: JsonElement?) {
        elements.add(element ?: JsonNull)
    }

    fun add(value: String?) {
        elements.add(value?.let { JsonPrimitive(it) } ?: JsonNull)
    }

    fun add(value: Number) {
        elements.add(JsonPrimitive(value))
    }

    fun add(value: Boolean) {
        elements.add(JsonPrimitive(value))
    }

    operator fun get(i: Int): JsonElement = elements[i]

    fun size(): Int = elements.size

    override fun iterator(): Iterator<JsonElement> = elements.iterator()

    override fun toString(): String =
        elements.joinToString(",", "[", "]")
}

object JsonParser {
    fun parseString(json: String): JsonElement {
        val parser = Parser(json)
        parser.skipWhitespace()
        val result = parser.parseValue()
        parser.skipWhitespace()
        return result
    }

    private class Parser(private val s: String) {
        private var pos = 0

        fun skipWhitespace() {
            while (pos < s.length && s[pos] in " \t\n\r") pos++
        }

        fun parseValue(): JsonElement {
            if (pos >= s.length) throw JsonSyntaxException("Unexpected end of input")
            return when (s[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> JsonPrimitive(parseString())
                't' -> { expect("true"); JsonPrimitive(true) }
                'f' -> { expect("false"); JsonPrimitive(false) }
                'n' -> { expect("null"); JsonNull }
                else -> parseNumber()
            }
        }

        private fun parseObject(): JsonObject {
            pos++ // '{'
            val obj = JsonObject()
            skipWhitespace()
            if (pos < s.length && s[pos] == '}') { pos++; return obj }
            while (true) {
                skipWhitespace()
                if (pos >= s.length || s[pos] != '"')
                    throw JsonSyntaxException("Expected string key at $pos")
                val key = parseString()
                skipWhitespace()
                if (pos >= s.length || s[pos] != ':')
                    throw JsonSyntaxException("Expected ':' at $pos")
                pos++
                skipWhitespace()
                obj.add(key, parseValue())
                skipWhitespace()
                when {
                    pos >= s.length -> throw JsonSyntaxException("Unterminated object")
                    s[pos] == ',' -> pos++
                    s[pos] == '}' -> { pos++; return obj }
                    else -> throw JsonSyntaxException("Expected ',' or '}' at $pos")
                }
            }
        }

        private fun parseArray(): JsonArray {
            pos++ // '['
            val arr = JsonArray()
            skipWhitespace()
            if (pos < s.length && s[pos] == ']') { pos++; return arr }
            while (true) {
                skipWhitespace()
                arr.add(parseValue())
                skipWhitespace()
                when {
                    pos >= s.length -> throw JsonSyntaxException("Unterminated array")
                    s[pos] == ',' -> pos++
                    s[pos] == ']' -> { pos++; return arr }
                    else -> throw JsonSyntaxException("Expected ',' or ']' at $pos")
                }
            }
        }

        private fun parseString(): String {
            pos++ // '"'
            val sb = StringBuilder()
            while (pos < s.length) {
                when (val c = s[pos++]) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (pos >= s.length) break
                        when (val e = s[pos++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (pos + 4 > s.length)
                                    throw JsonSyntaxException("Bad unicode escape at $pos")
                                val hex = s.substring(pos, pos + 4).toIntOrNull(16)
                                    ?: throw JsonSyntaxException("Bad unicode escape at $pos")
                                sb.append(hex.toChar())
                                pos += 4
                            }
                            else -> throw JsonSyntaxException("Bad escape '\\$e' at $pos")
                        }
                    }
                    else -> sb.append(c)
                }
            }
            throw JsonSyntaxException("Unterminated string")
        }

        private fun parseNumber(): JsonElement {
            val start = pos
            if (pos < s.length && s[pos] == '-') pos++
            var isDouble = false
            while (pos < s.length) {
                val c = s[pos]
                if (c in '0'..'9') pos++
                else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    isDouble = true; pos++
                } else break
            }
            val num = s.substring(start, pos)
            if (num.isEmpty()) throw JsonSyntaxException("Expected value at $start")
            return if (isDouble) {
                JsonPrimitive(num.toDoubleOrNull()
                    ?: throw JsonSyntaxException("Bad number '$num'"))
            } else {
                JsonPrimitive(num.toLongOrNull() ?: num.toDoubleOrNull()
                    ?: throw JsonSyntaxException("Bad number '$num'"))
            }
        }

        private fun expect(literal: String) {
            if (s.startsWith(literal, pos)) pos += literal.length
            else throw JsonSyntaxException("Expected '$literal' at $pos")
        }
    }
}

class JsonSyntaxException(message: String) : Exception(message)

private fun numberToString(n: Number): String = when (n) {
    is Double -> if (n == n.toLong().toDouble() && n.isFinite() &&
        (n > Long.MIN_VALUE.toDouble() && n < Long.MAX_VALUE.toDouble()) &&
        !n.toString().contains('E')) {
        // Gson writes doubles like 5.0 as "5.0"; keep that format
        n.toString()
    } else n.toString()
    else -> n.toString()
}

/**
 * Gson-style HTML-safe JSON string escaping: control characters plus
 * < > & = ' and U+2028/U+2029 are emitted as \uXXXX.
 */
private fun quote(s: String): String {
    val sb = StringBuilder(s.length + 2)
    sb.append('"')
    for (c in s) {
        when (c) {
            '"' -> sb.append("\\\"")
            '\\' -> sb.append("\\\\")
            '\b' -> sb.append("\\b")
            '\u000C' -> sb.append("\\f")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '<', '>', '&', '=', '\'', '\u2028', '\u2029' ->
                sb.append("\\u").append(c.code.toString(16).padStart(4, '0'))
            else -> if (c.code < 0x20) {
                sb.append("\\u").append(c.code.toString(16).padStart(4, '0'))
            } else sb.append(c)
        }
    }
    sb.append('"')
    return sb.toString()
}
