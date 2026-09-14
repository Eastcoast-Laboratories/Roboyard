package roboyard.platform

import roboyard.logic.ui.StringProvider
import roboyard.logic.ui.setStringProvider as setStringProviderKt

/**
 * Java-callable bridge to set the StringProvider from the app module.
 * Kotlin extension functions aren't directly callable from Java,
 * so this wrapper provides a static method.
 */
object StringProviderBridge {
    @JvmStatic
    fun setStringProvider(provider: StringProvider) {
        setStringProviderKt(provider)
    }
}
