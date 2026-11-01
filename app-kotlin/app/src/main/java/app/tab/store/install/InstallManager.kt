package app.tab.store.install

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Installs, rather than merely redirecting.
 *
 * Three paths, chosen from the listing's primary link:
 *  * `.apk` - request the install-packages permission, then hand the URL to the
 *    system downloader with an install intent. Android still shows its own
 *    confirmation dialog; nothing installs silently.
 *  * Play Store / App Store links - open the native store client.
 *  * anything else - the browser.
 */
object InstallManager {

    sealed interface Outcome {
        data class Launched(val message: String) : Outcome
        data class NeedsPermission(val message: String) : Outcome
        data class Failed(val message: String) : Outcome
    }

    fun install(context: Context, url: String?): Outcome {
        if (url.isNullOrBlank()) return Outcome.Failed("This listing has no install link yet.")
        val uri = runCatching { Uri.parse(url) }.getOrNull()
            ?: return Outcome.Failed("That link is not valid.")
        val host = uri.host?.lowercase().orEmpty()

        return when {
            host.contains("play.google.com") -> {
                // Prefer the native store client; fall back to the https link.
                val packageId = uri.getQueryParameter("id")
                if (packageId != null) {
                    open(context, Uri.parse("market://details?id=$packageId"))
                } else {
                    open(context, uri)
                }
            }

            host.contains("apps.apple.com") || host.contains("itunes.apple.com") ->
                open(context, uri)

            uri.path?.endsWith(".apk", ignoreCase = true) == true -> {
                if (!canInstallPackages(context)) {
                    Outcome.NeedsPermission(
                        "Android needs permission to install apps from Sproutly. " +
                            "Enable \"Install unknown apps\", then try again.",
                    )
                } else {
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        setDataAndType(uri, "application/vnd.android.package-archive")
                    }
                    runCatching { context.startActivity(intent) }
                        .fold(
                            onSuccess = { Outcome.Launched("Confirm the install in the Android dialog.") },
                            onFailure = { Outcome.Failed("The installer could not be opened.") },
                        )
                }
            }

            else -> open(context, uri)
        }
    }

    private fun canInstallPackages(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    /** Deep link into the system's "install unknown apps" screen for this app. */
    fun openInstallSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private fun open(context: Context, uri: Uri): Outcome {
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(intent) }
            .fold(
                onSuccess = { Outcome.Launched("Opening ${uri.host ?: "the link"}.") },
                onFailure = { Outcome.Failed("No app can open that link.") },
            )
    }
}
