package roboyard.logic.network

import android.content.Context
import android.os.Build
import java.time.OffsetDateTime
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import roboyard.logic.achievements.AchievementManagerFactory
import roboyard.logic.managers.SyncHooks
import roboyard.logic.managers.SyncManager
import roboyard.logic.storage.PlatformStorage
import roboyard.platform.AndroidNetworkMonitor
import roboyard.platform.AndroidStorage
import timber.log.Timber.Forest.d
import timber.log.Timber.Forest.e
import timber.log.Timber.Forest.w
import java.io.File
import java.util.Scanner

/**
 * Android wiring for the shared RoboyardApiClient and SyncManager.
 * All app code should obtain clients through this provider so the shared
 * singletons are created with the correct Android dependencies
 * (main-thread callbacks, install source, file locations).
 */
object ApiClientProvider {

    @JvmStatic
    fun api(context: Context): RoboyardApiClient {
        val appContext = context.applicationContext
        return RoboyardApiClient.getInstance(
            AndroidStorage.getInstance(appContext),
            CoroutineScope(Dispatchers.Main),
            { installSource(appContext) }
        )
    }

    @JvmStatic
    fun sync(context: Context): SyncManager {
        val appContext = context.applicationContext
        val storage = AndroidStorage.getInstance(appContext)
        return SyncManager.getInstance(
            storage,
            AndroidNetworkMonitor.getInstance(appContext),
            api(appContext),
            SyncHooks(
                listSaveFileNames = {
                    File(appContext.filesDir, roboyard.logic.core.Constants.SAVE_DIRECTORY)
                        .list()?.toList() ?: emptyList()
                },
                readMapAsset = { path -> readMapAsset(appContext, path) },
                parseIsoTimestamp = { iso -> parseIsoTimestamp(iso) },
                syncAchievementsToServer = {
                    AchievementManagerFactory.getInstance(appContext).syncToServer()
                }
            )
        )
    }

    /**
     * Get the install source (store) of this app using PackageManager.
     * Uses getInstallSourceInfo() on API 30+ and getInstallerPackageName() on older versions.
     * @return e.g. "com.android.vending" (Play Store), "sideload", etc.
     */
    private fun installSource(context: Context): String {
        return try {
            val packageName = context.packageName
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = context.packageManager.getInstallSourceInfo(packageName)
                info.installingPackageName ?: "sideload"
            } else {
                @Suppress("deprecation")
                context.packageManager.getInstallerPackageName(packageName) ?: "sideload"
            }
        } catch (ex: Exception) {
            e(ex, "[INSTALL_SOURCE] Failed to get install source")
            "unknown"
        }
    }

    private fun readMapAsset(context: Context, path: String): String? {
        return try {
            context.assets.open("Maps/$path").use { input ->
                Scanner(input).useDelimiter("\\A").next()
            }
        } catch (ex: Exception) {
            w(ex, "[SYNC] Failed to load map asset %s", path)
            null
        }
    }

    /**
     * Parse an ISO-8601 timestamp. Uses java.time on API 26+ (correct offset
     * handling), SimpleDateFormat fallback on older versions.
     */
    private fun parseIsoTimestamp(iso: String): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            OffsetDateTime.parse(iso).toInstant().toEpochMilli()
        } else {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
            sdf.parse(iso)?.time ?: System.currentTimeMillis()
        }
    }
}
