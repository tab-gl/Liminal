package app.tab.store.core.media

import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

/**
 * Silent installs through Shizuku.
 *
 * Shizuku lets an ordinary app run a few shell commands with the privileges the
 * user has already granted it through ADB or root. `pm install` is one of them,
 * which removes the Android "do you want to install this application?" prompt —
 * the thing that makes installing feel like 2012.
 *
 * Everything here is optional: if Shizuku is not running, or the user has not
 * granted this app, [install] returns null and the caller falls back to the
 * system installer. Nothing depends on it.
 */
class ShizukuInstaller {

    /** True when the Shizuku service is running and reachable. */
    fun isAvailable(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    /** True when this app has been granted Shizuku access. */
    fun hasPermission(): Boolean = runCatching {
        isAvailable() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
    }.getOrDefault(false)

    /** Asks the user, through Shizuku's own dialog. */
    fun requestPermission(requestCode: Int) {
        runCatching {
            if (isAvailable() && !hasPermission()) Shizuku.requestPermission(requestCode)
        }
    }

    /**
     * Installs [apkPath] with no prompt. Returns the installer's output on
     * success, or null when Shizuku cannot do it and the caller should fall back.
     */
    suspend fun install(apkPath: String): String? = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext null
        runCatching {
            // newProcess is hidden in the public API; it is the documented route
            // for shell commands through Shizuku, reached reflectively.
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java,
            ).apply { isAccessible = true }
            val process = method.invoke(
                null,
                arrayOf("pm", "install", "-r", "-g", apkPath),
                null,
                null,
            ) as rikka.shizuku.ShizukuRemoteProcess
            val output = process.inputStream.bufferedReader().readText() +
                process.errorStream.bufferedReader().readText()
            val exit = process.waitFor()
            if (exit == 0 && !output.contains("Failure")) output.trim() else null
        }.getOrNull()
    }
}
