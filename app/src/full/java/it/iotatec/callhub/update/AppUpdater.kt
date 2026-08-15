package it.iotatec.callhub.update

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import it.iotatec.callhub.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * FULL (sideload) flavor auto-updater. Checks the GitHub Releases API for a
 * newer version, downloads the APK asset, and launches the system installer.
 * The Play flavor ships a no-op — Play Store handles its updates.
 */
object AppUpdater {

    private const val TAG = "AppUpdater"

    fun checkForUpdates(activity: Activity) {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val latest = fetchLatestRelease() ?: return@launch
                if (!isNewer(latest.version, BuildConfig.VERSION_NAME)) {
                    Log.i(TAG, "Up to date (${BuildConfig.VERSION_NAME})")
                    return@launch
                }
                val apk = downloadApk(activity, latest.apkUrl, latest.version) ?: return@launch
                withContext(Dispatchers.Main) { installApk(activity, apk) }
            }.onFailure { Log.w(TAG, "Update check failed", it) }
        }
    }

    private data class Release(val version: String, val apkUrl: String)

    private fun fetchLatestRelease(): Release? {
        val url = URL("https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases/latest")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        conn.inputStream.bufferedReader().use { reader ->
            val obj = JSONObject(reader.readText())
            val version = obj.getString("tag_name").removePrefix("v").trim()
            val assets = obj.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.getString("name").endsWith(".apk")) {
                    return Release(version, a.getString("browser_download_url"))
                }
            }
        }
        return null
    }

    private fun downloadApk(activity: Activity, apkUrl: String, version: String): File? {
        val dest = File(activity.cacheDir, "callhub-$version.apk")
        (URL(apkUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 20000
        }.inputStream.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        return dest.takeIf { it.length() > 0 }
    }

    private fun installApk(activity: Activity, apk: File) {
        val uri: Uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }

    /** Semantic-version comparison: returns true if [candidate] > [current]. */
    private fun isNewer(candidate: String, current: String): Boolean {
        fun parts(v: String) = v.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val a = parts(candidate); val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }; val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
