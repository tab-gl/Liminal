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

    private fun download(url: String, fileName: String): File {
        val dir = File(context.cacheDir, "apks").apply { mkdirs() }
        val safe = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "app.apk" }
        val file = File(dir, safe)

        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 180_000
            instanceFollowRedirects = true
        }
        connection.inputStream.use { input ->
            file.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
        }
        connection.disconnect()
        if (file.length() == 0L) error("The build came back empty.")
        return file
    }
}
