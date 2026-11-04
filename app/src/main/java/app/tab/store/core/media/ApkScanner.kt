package app.tab.store.core.media

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sprout Safe: reads a build's manifest before anyone installs it.
 *
 * The scan is real rather than decorative — the APK is parsed with the platform's
 * own package parser, which yields the requested permissions, the package name,
 * the version and the SDK range. From those we judge what the app can reach and
 * warn about the ones that matter.
 *
 * It cannot prove an app is safe. It can say "this app can read your contacts and
 * send SMS", which is the thing a person actually needs to know before installing.
 */
@Singleton
class ApkScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    enum class Risk { LOW, MEDIUM, HIGH }

    data class Report(
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val minSdk: Int,
        val targetSdk: Int,
        val permissions: List<String>,
        val warnings: List<String>,
        val risk: Risk,
    ) {
        val isReadable: Boolean get() = packageName.isNotBlank()
    }

    /**
     * Permissions worth warning about, and why.
     *
     * Ordered roughly by how badly they can be abused. Anything not listed is
     * shown but not warned about.
     */
    private val explained: Map<String, Pair<String, Risk>> = mapOf(
        "android.permission.READ_SMS" to ("can read your text messages" to Risk.HIGH),
        "android.permission.RECEIVE_SMS" to ("can receive your text messages" to Risk.HIGH),
        "android.permission.SEND_SMS" to ("can send text messages, which can cost money" to Risk.HIGH),
        "android.permission.CALL_PHONE" to ("can place calls without asking" to Risk.HIGH),
        "android.permission.READ_CALL_LOG" to ("can read your call history" to Risk.HIGH),
        "android.permission.MANAGE_EXTERNAL_STORAGE" to ("can read every file on your device" to Risk.HIGH),
        "android.permission.SYSTEM_ALERT_WINDOW" to ("can draw over other apps" to Risk.HIGH),
        "android.permission.REQUEST_INSTALL_PACKAGES" to ("can install other apps" to Risk.HIGH),
        "android.permission.READ_CONTACTS" to ("can read your contacts" to Risk.MEDIUM),
        "android.permission.WRITE_CONTACTS" to ("can change your contacts" to Risk.MEDIUM),
        "android.permission.ACCESS_FINE_LOCATION" to ("can track your precise location" to Risk.MEDIUM),
        "android.permission.ACCESS_BACKGROUND_LOCATION" to ("can track your location in the background" to Risk.HIGH),
        "android.permission.CAMERA" to ("can use your camera" to Risk.MEDIUM),
        "android.permission.RECORD_AUDIO" to ("can use your microphone" to Risk.MEDIUM),
        "android.permission.READ_PHONE_STATE" to ("can see your device identifiers" to Risk.MEDIUM),
        "android.permission.READ_MEDIA_IMAGES" to ("can read your photos" to Risk.MEDIUM),
        "android.permission.READ_MEDIA_VIDEO" to ("can read your videos" to Risk.MEDIUM),
        "android.permission.READ_EXTERNAL_STORAGE" to ("can read your files" to Risk.MEDIUM),
        "android.permission.WRITE_EXTERNAL_STORAGE" to ("can change your files" to Risk.MEDIUM),
        "android.permission.GET_ACCOUNTS" to ("can see the accounts on your device" to Risk.MEDIUM),
        "android.permission.BODY_SENSORS" to ("can read your health sensors" to Risk.MEDIUM),
        "android.permission.BLUETOOTH_CONNECT" to ("can connect to nearby devices" to Risk.LOW),
        "android.permission.POST_NOTIFICATIONS" to ("can send you notifications" to Risk.LOW),
    )

    /** Parses [bytes] as an APK. Returns null when the file is not a readable package. */
    fun scan(bytes: ByteArray, fileName: String): Report? {
        val dir = File(context.cacheDir, "apks").apply { mkdirs() }
        val file = File(dir, fileName.replace(Regex("[^A-Za-z0-9._-]"), "_"))
        return runCatching {
            file.writeBytes(bytes)
            val flags = PackageManager.GET_PERMISSIONS or
                PackageManager.GET_ACTIVITIES or
                PackageManager.GET_SERVICES or
                PackageManager.GET_RECEIVERS
            val info = context.packageManager.getPackageArchiveInfo(file.path, flags)
            if (info == null) {
                null
            } else {
                val permissions = info.requestedPermissions?.toList().orEmpty()
                val warnings = mutableListOf<String>()
                var risk = Risk.LOW

                permissions.forEach { permission ->
                    val entry: Pair<String, Risk> = explained[permission] ?: return@forEach
                    warnings += entry.first.replaceFirstChar { it.uppercase() }
                    if (entry.second.ordinal > risk.ordinal) risk = entry.second
                }

                // Exported components are how one app reaches into another.
                val exported = (info.activities.orEmpty().count { it.exported } +
                    info.services.orEmpty().count { it.exported } +
                    info.receivers.orEmpty().count { it.exported })
                if (exported > 0) {
                    warnings += "$exported component${if (exported == 1) " is" else "s are"} " +
                        "open to other apps"
                }

                val app = info.applicationInfo
                val target = app?.targetSdkVersion ?: 0
                if (target in 1..30) {
                    warnings += "Built for an older Android version (target SDK $target)"
                    if (risk == Risk.LOW) risk = Risk.MEDIUM
                }

                Report(
                    packageName = info.packageName.orEmpty(),
                    versionName = info.versionName.orEmpty(),
                    versionCode = info.longVersionCode,
                    minSdk = app?.minSdkVersion ?: 0,
                    targetSdk = target,
                    permissions = permissions,
                    warnings = warnings,
                    risk = risk,
                )
            }
        }.getOrNull().also { runCatching { file.delete() } }
    }

    /** A short label for the risk level. */
    fun label(risk: String?): String = when (risk) {
        Risk.HIGH.name -> "High risk"
        Risk.MEDIUM.name -> "Worth a look"
        Risk.LOW.name -> "Looks fine"
        else -> "Not scanned"
    }
}
