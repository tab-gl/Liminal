package app.tab.store.core.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Downloads a build and hands it to the system installer.
 *
 * Android will not install a package from an app unless the person has allowed
 * that app as a source, so the flow is: fetch the APK, check that permission, ask
 * for it if it is missing, and only then launch the installer. The permission is
 * never assumed.
 */
@Singleton
class AppInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    sealed interface Outcome {
        /** The build is on disk and the system installer was opened. */
        data object Launched : Outcome

        /** The person must allow this app as an install source first. */
        data object NeedsPermission : Outcome

        data class Failed(val message: String) : Outcome
    }

    /** True when this app may currently install packages. */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** Opens the system screen where this app can be allowed as a source. */
    fun requestPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    suspend fun downloadAndInstall(url: String, fileName: String): Outcome = withContext(Dispatchers.IO) {
        if (!url.startsWith("https://", ignoreCase = true)) {
            return@withContext Outcome.Failed("This build link is not a secure direct download.")
        }
        if (!canInstall()) return@withContext Outcome.NeedsPermission

        val file = runCatching { download(url, fileName) }.getOrElse { error ->
            return@withContext Outcome.Failed(
                error.message ?: "The build could not be downloaded.",
            )
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.apkfiles", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return@withContext runCatching {
            context.startActivity(intent)
            Outcome.Launched
        }.getOrElse {
            Outcome.Failed("No installer is available on this device.")
        }
    }

    /**
     * Fetches the build into a partial file and only moves it into place once it
     * is proven to be an installable package. A link that returns a web page, an
     * error body or a half-finished transfer must never reach the installer, which
     * would only answer "There was a problem parsing the package".
     */
    private fun download(url: String, fileName: String): File {
        val dir = File(context.cacheDir, "apks").apply { mkdirs() }
        val safe = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "app.apk" }
            .let { if (it.endsWith(".apk", ignoreCase = true)) it else "$it.apk" }
        val file = File(dir, safe)
        val part = File(dir, "$safe.part")
        part.delete()

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 180_000
            instanceFollowRedirects = true
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("The download failed (HTTP $code).")
            val type = connection.contentType.orEmpty().lowercase()
            if (type.startsWith("text/html")) {
                error("This link opens a web page, not a build. Use a direct link to the APK.")
            }
            connection.inputStream.use { input ->
                part.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
            }
        } catch (e: Exception) {
            part.delete()
            throw e
        } finally {
            connection.disconnect()
        }

        if (part.length() == 0L) {
            part.delete()
            error("The build came back empty.")
        }
        // An APK is a ZIP: it starts with "PK".
        val isZip = part.inputStream().use { it.read() == 0x50 && it.read() == 0x4B }
        val parsed = if (isZip) context.packageManager.getPackageArchiveInfo(part.path, 0) else null
        if (parsed == null) {
            part.delete()
            error("That file is not a valid Android build.")
        }
        file.delete()
        if (!part.renameTo(file)) {
            part.delete()
            error("The build could not be saved.")
        }
        return file
    }
}
