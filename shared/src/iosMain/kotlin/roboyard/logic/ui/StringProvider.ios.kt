package roboyard.logic.ui

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSLocale
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.currentLocale
import platform.Foundation.languageCode
import platform.Foundation.stringWithContentsOfFile
import roboyard.logic.core.Preferences
import roboyard.logic.json.JsonParser
import roboyard.logic.util.RLog

/**
 * iOS implementation of StringProvider.
 * Loads strings from strings/strings.json bundled with the app; the file is
 * generated from the Android strings.xml during the build.
 */
@OptIn(ExperimentalForeignApi::class)
object IosStringProvider : StringProvider {

    private val log = RLog.tag("IosStringProvider")

    private val stringsByLocale: Map<String, Map<String, String>> by lazy { loadStrings() }

    private fun loadStrings(): Map<String, Map<String, String>> {
        val bundle = NSBundle.mainBundle
        val path = bundle.pathForResource("strings", "json", "strings")
            ?: bundle.pathForResource("strings", "json", "compose-resources/strings")
            ?: bundle.pathForResource("strings", "json", null)
        if (path == null) {
            log.e("strings.json not found in bundle")
            return emptyMap()
        }
        val content = NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
            ?: run {
                log.e("Failed to read strings.json at $path")
                return emptyMap()
            }
        return try {
            val root = JsonParser.parseString(content).asJsonObject
            val result = mutableMapOf<String, Map<String, String>>()
            for ((lang, langEl) in root.entrySet()) {
                if (!langEl.isJsonObject) continue
                val map = mutableMapOf<String, String>()
                for ((key, value) in langEl.asJsonObject.entrySet()) {
                    if (value.isJsonPrimitive) map[key] = value.asString
                }
                result[lang] = map
            }
            result
        } catch (e: Exception) {
            log.e("Failed to parse strings.json: ${e.message}")
            emptyMap()
        }
    }

    private fun getCurrentLanguage(): String {
        return Preferences.appLanguage
            ?: (NSLocale.currentLocale.languageCode ?: "en")
    }

    override fun getString(name: String): String? {
        val lang = getCurrentLanguage()
        return stringsByLocale[lang]?.get(name) ?: stringsByLocale["en"]?.get(name)
    }
}

/**
 * iOS implementation of getStringProvider.
 */
actual fun getStringProvider(): StringProvider {
    return IosStringProvider
}
