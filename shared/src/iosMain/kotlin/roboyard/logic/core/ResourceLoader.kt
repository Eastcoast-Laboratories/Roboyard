package roboyard.logic.core

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.stringWithContentsOfFile
import roboyard.logic.util.RLog

/**
 * iOS implementation: level files are bundled resources under Maps/.
 * Several candidate locations are tried because resource layout differs
 * between plain bundle copies and Compose Multiplatform resource packaging.
 */
@OptIn(ExperimentalForeignApi::class)
actual object ResourceLoader {
    private val log = RLog.tag("ResourceLoader")

    actual fun loadLevelContent(levelId: Int): String? {
        val bundle = NSBundle.mainBundle
        val name = "level_$levelId"
        val path = bundle.pathForResource(name, "txt", "Maps")
            ?: bundle.pathForResource(name, "txt", "compose-resources/Maps")
            ?: bundle.pathForResource(name, "txt", null)
        if (path == null) {
            log.w("Level resource not found in bundle: Maps/$name.txt")
            return null
        }
        return NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
    }
}
