package roboyard.platform

import android.content.Context
import roboyard.eclabs.R
import roboyard.logic.ui.StringProvider

/**
 * Android implementation of StringProvider using R.string resources
 */
class AndroidStringProvider(private val context: Context) : StringProvider {
    
    override fun getString(name: String): String? {
        val resId = context.resources.getIdentifier(name, "string", context.packageName)
        return if (resId != 0) {
            context.getString(resId)
        } else {
            null
        }
    }
    
    override fun getString(name: String, vararg formatArgs: Any): String? {
        val resId = context.resources.getIdentifier(name, "string", context.packageName)
        return if (resId != 0) {
            context.getString(resId, *formatArgs)
        } else {
            null
        }
    }
    
    companion object {
        @Volatile
        private var instance: AndroidStringProvider? = null
        
        @JvmStatic
        fun getInstance(context: Context): AndroidStringProvider {
            return instance ?: AndroidStringProvider(context.applicationContext).also {
                instance = it
            }
        }
    }
}
