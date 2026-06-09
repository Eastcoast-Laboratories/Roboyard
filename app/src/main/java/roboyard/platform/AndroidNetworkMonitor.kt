package roboyard.platform

import android.content.Context
import android.net.ConnectivityManager
import roboyard.logic.network.NetworkMonitor

/**
 * Android implementation of NetworkMonitor using ConnectivityManager.
 */
class AndroidNetworkMonitor(private val context: Context) : NetworkMonitor {
    override fun isNetworkAvailable(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (cm == null) return false
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    companion object {
        @Volatile
        private var instance: AndroidNetworkMonitor? = null

        @JvmStatic
        fun getInstance(context: Context): AndroidNetworkMonitor {
            return instance ?: AndroidNetworkMonitor(context.applicationContext).also {
                instance = it
            }
        }
    }
}
