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

val APK_MIME_TYPES = arrayOf(
    "application/vnd.android.package-archive",
    "application/octet-stream",
)
