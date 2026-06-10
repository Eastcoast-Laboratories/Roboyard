package roboyard.logic.ui

/**
 * Platform-agnostic interface for showing UI messages (Toasts, Snackbars, etc.)
 * Implementations are provided by the UI layer (Android, iOS, Desktop)
 */
fun interface UiNotifier {
    /**
     * Show a short informational message to the user
     * @param message The message to display
     */
    fun showMessage(message: String)
    
    /**
     * Show a long informational message to the user
     * @param message The message to display
     */
    fun showLongMessage(message: String) {
        // Default implementation just calls showMessage
        // Platform implementations can override for different duration
        showMessage(message)
    }
}
