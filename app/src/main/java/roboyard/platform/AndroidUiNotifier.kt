package roboyard.platform

import android.content.Context
import android.widget.Toast
import roboyard.logic.ui.UiNotifier

/**
 * Android implementation of UiNotifier using Toast
 */
class AndroidUiNotifier(private val context: Context) : UiNotifier {
    
    override fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun showLongMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    
    companion object {
        @Volatile
        private var instance: AndroidUiNotifier? = null
        
        @JvmStatic
        fun getInstance(context: Context): AndroidUiNotifier {
            return instance ?: AndroidUiNotifier(context.applicationContext).also {
                instance = it
            }
        }
    }
}
