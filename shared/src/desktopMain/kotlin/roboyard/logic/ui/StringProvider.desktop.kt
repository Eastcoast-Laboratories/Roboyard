package roboyard.logic.ui

/**
 * Desktop implementation of getStringProvider.
 * Delegates to DesktopStringProvider which loads from strings.json.
 */
actual fun getStringProvider(): StringProvider {
    return DesktopStringProvider
}
