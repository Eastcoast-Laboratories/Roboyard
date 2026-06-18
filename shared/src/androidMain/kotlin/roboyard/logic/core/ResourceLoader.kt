package roboyard.logic.core

import java.io.InputStream

/**
 * Android-specific implementation for loading level content from resources.
 */
actual object ResourceLoader {
    actual fun loadLevelContent(levelId: Int): String? {
        val resourcePath = "Maps/level_$levelId.txt"
        val inputStream: InputStream? = Thread.currentThread().contextClassLoader?.getResourceAsStream(resourcePath)
            ?: ResourceLoader::class.java.classLoader?.getResourceAsStream(resourcePath)
        return inputStream?.use { it.bufferedReader().readText() }
    }
}
