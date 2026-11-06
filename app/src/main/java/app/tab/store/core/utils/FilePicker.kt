package app.tab.store.core.utils

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Picks an APK.
 *
 * Some file providers report the build as `application/octet-stream` rather
 * than the Android package MIME type, so both are requested.
 */
@Composable
fun rememberApkPicker(onPicked: (Uri?) -> Unit) =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        onPicked(uri)
    }

/**
 * Android builds, iOS builds and zips of either.
 *
 * `application/octet-stream` is in the list because many file managers report
 * every unknown binary that way; the extension is checked after picking, so a
 * wrongly-typed pick is still refused with a reason.
 */
val APK_MIME_TYPES = arrayOf(
    "application/vnd.android.package-archive",
    "application/x-ios-app",
    "application/zip",
    "application/x-zip-compressed",
    "application/octet-stream",
)
