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
        /**
         * The build is on disk and the system installer was opened. Carries the
         * package name so the app can offer "Open" once Android finishes.
         */
        data class Launched(val packageName: String?) : Outcome

        /** Installed without any prompt, through Shizuku. */
        data class Installed(val packageName: String?, val viaShizuku: Boolean) : Outcome

        /** The person must allow this app as an install source first. */
        data object NeedsPermission : Outcome

        data class Failed(val message: String) : Outcome
    }

    /** Set by download() so Launched can report which app was installed. */
    private var installedPackage: String? = null

    private val shizuku = ShizukuInstaller()

    /** Opens an installed app, if it has a launcher entry. */
    fun openInstalled(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return false
        return runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
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

    suspend fun downloadAndInstall(
        url: String,
        fileName: String,
        onProgress: (Float) -> Unit = {},
    ): Outcome = withContext(Dispatchers.IO) {
        // Links issued before the Worker fix carry /apks/apks/; the Worker
        // tolerates it, and so does this, so old listings keep working.
        val fixedUrl = url.replace("/apks/apks/", "/apks/")

        if (!fixedUrl.startsWith("https://", ignoreCase = true)) {
            return@withContext Outcome.Failed("This build link is not a secure direct download.")
        }
        if (!canInstall()) return@withContext Outcome.NeedsPermission

        val file = runCatching { download(fixedUrl, fileName, onProgress) }.getOrElse { error ->
            // The status code is the difference between "wrong key" (404), "the
            // deployment is broken" (5xx) and "the network blocked it". Without it
            // every failure looked identical.
            return@withContext Outcome.Failed(describe(error))
        }

        // Shizuku, when the device has it, installs with no prompt at all.
        val silent = shizuku.install(file.absolutePath)
        if (silent != null) {
            installedPackage = installedPackage ?: context.packageManager
                .getPackageArchiveInfo(file.absolutePath, 0)?.packageName
            return@withContext Outcome.Installed(installedPackage, viaShizuku = true)
        }

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.apkfiles", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return@withContext runCatching {
            context.startActivity(intent)
            Outcome.Launched(installedPackage)
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
    /** The server answered, but not with a build. */
    private class HttpStatusException(val code: Int) : Exception("HTTP $code")

    private fun describe(error: Throwable): String = when {
        error is HttpStatusException && error.code == 404 ->
            "The build is missing on the server (404). Open /health on the Worker."
        error is HttpStatusException && (error.code == 401 || error.code == 403) ->
            "The build server refused this request (${error.code})."
        error is HttpStatusException && error.code >= 500 ->
            "The build server is misconfigured (${error.code}). Check the R2 binding."
        error is java.net.UnknownHostException -> "No connection to the build server."
        error is java.net.SocketTimeoutException -> "The build server took too long."
        error is javax.net.ssl.SSLException ->
            "The connection to the build server was blocked."
        else -> "The build could not be downloaded."
    }

    private fun download(url: String, fileName: String, onProgress: (Float) -> Unit): File {
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
            if (code !in 200..299) throw HttpStatusException(code)

            val type = connection.contentType.orEmpty().lowercase()
            if (type.startsWith("text/html")) {
                error("This link opens a web page, not a build. Use a direct link to the APK.")
            }

            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var written = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                    }
                    output.flush()
                }
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
        installedPackage = parsed?.packageName
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
