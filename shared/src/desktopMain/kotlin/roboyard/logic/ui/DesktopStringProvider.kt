package roboyard.logic.ui

import java.io.InputStreamReader

/**
 * Desktop implementation of StringProvider.
 * Loads strings from composeApp/src/commonMain/resources/strings/strings.json
 * which is generated from the Android strings.xml files.
 */
object DesktopStringProvider : StringProvider {

    private val stringsByLocale: Map<String, Map<String, String>> by lazy { loadStrings() }

    /**
     * Simple JSON parser for the flat 2-level structure:
     * {"en": {"key": "value", ...}, "de": {"key": "value", ...}}
     */
    private fun loadStrings(): Map<String, Map<String, String>> {
        val result = mutableMapOf<String, Map<String, String>>()
        try {
            val resource = Thread.currentThread().contextClassLoader
                ?.getResourceAsStream("strings/strings.json")
                ?: javaClass.classLoader?.getResourceAsStream("strings/strings.json")
            if (resource != null) {
                val content = InputStreamReader(resource, Charsets.UTF_8).readText()
                parseJson(content, result)
            }
        } catch (e: Exception) {
            System.err.println("[DesktopStringProvider] Failed to load strings: ${e.message}")
        }
        return result
    }

    private fun parseJson(content: String, result: MutableMap<String, Map<String, String>>) {
        var i = 0
        val len = content.length
        while (i < len) {
            val langKey = readString(content, i) ?: return
            i = langKey.second
            while (i < len && content[i] != ':') i++
            i++
            while (i < len && content[i] != '{') i++
            i++
            val langStrings = mutableMapOf<String, String>()
            while (i < len && content[i] != '}') {
                val strKey = readString(content, i)
                if (strKey == null) { i++; continue }
                i = strKey.second
                while (i < len && content[i] != ':') i++
                i++
                while (i < len && content[i].isWhitespace()) i++
                if (i < len && content[i] == '"') {
                    val strVal = readString(content, i)
                    if (strVal != null) {
                        langStrings[strKey.first] = strVal.first
                        i = strVal.second
                    }
                } else {
                    while (i < len && content[i] != ',' && content[i] != '}') i++
                }
                while (i < len && (content[i] == ',' || content[i].isWhitespace())) i++
            }
            i++
            result[langKey.first] = langStrings
            while (i < len && (content[i] == ',' || content[i].isWhitespace())) i++
        }
    }

    private fun readString(content: String, start: Int): Pair<String, Int>? {
        var i = start
        val len = content.length
        while (i < len && content[i] != '"') {
            if (!content[i].isWhitespace()) return null
            i++
        }
        if (i >= len) return null
        i++
        val sb = StringBuilder()
        while (i < len && content[i] != '"') {
            if (content[i] == '\\' && i + 1 < len) {
                when (content[i + 1]) {
                    'n' -> sb.append('\n')
                    't' -> sb.append('\t')
                    'r' -> sb.append('\r')
                    '\\' -> sb.append('\\')
                    '"' -> sb.append('"')
                    '/' -> sb.append('/')
                    'u' -> {
                        if (i + 5 < len) {
                            val hex = content.substring(i + 2, i + 6)
                            sb.append(hex.toInt(16).toChar())
                            i += 4
                        }
                    }
                    else -> sb.append(content[i + 1])
                }
                i += 2
            } else {
                sb.append(content[i])
                i++
            }
        }
        i++
        return Pair(sb.toString(), i)
    }

    private fun getCurrentLanguage(): String {
        return java.util.Locale.getDefault().language
    }

    override fun getString(name: String): String? {
        val lang = getCurrentLanguage()
        val strings = stringsByLocale[lang] ?: stringsByLocale["en"] ?: return null
        return strings[name]
    }
}
