package app.tab.store.core.media

import app.tab.store.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds are uploaded to Cloudflare R2 through the Sproutly uploader Worker.
 *
 * Why not straight to the bucket: writing to R2 needs credentials, and anything
 * shipped inside an APK can be extracted. The Worker verifies the caller's
 * Firebase ID token with Google and writes with its own binding, so the app never
 * holds a secret.
 *
 * Why not Firestore: a document is capped at 1 MiB. Why not Cloudinary: its
 * unsigned preset rejects ZIP content and caps at 10 MB, and an APK is a ZIP.
 */
@Singleton
class ApkStorage @Inject constructor(
    private val auth: FirebaseAuth,
) {
    data class Uploaded(val url: String, val path: String, val sizeBytes: Long)

    /** Mirrors the Worker's ceiling so the error arrives before the bytes do. */
    val maxBytes: Long = 100L * 1024 * 1024

    /** True when an uploader has been configured for this build. */
    val isConfigured: Boolean get() = BuildConfig.APK_UPLOAD_URL.isNotBlank()

    fun upload(
        bytes: ByteArray,
        fileName: String,
        onProgress: (Float) -> Unit = {},
    ): Flow<Result<Uploaded>> = callbackFlow {
        if (!isConfigured) {
            trySend(
                Result.failure(
                    IllegalStateException(
                        "Build uploads are not configured in this build. " +
                            "Paste a link to the build instead, or set " +
                            "SPROUTLY_APK_UPLOAD_URL and rebuild.",
                    ),
                ),
            )
            close()
            return@callbackFlow
        }
        if (bytes.isEmpty()) {
            trySend(Result.failure(IllegalArgumentException("That file is empty.")))
            close()
            return@callbackFlow
        }
        if (bytes.size > maxBytes) {
            trySend(
                Result.failure(
                    IllegalArgumentException(
                        "That build is ${"%.1f".format(bytes.size / 1048576.0)} MB. " +
                            "The limit is ${maxBytes / 1048576} MB.",
                    ),
                ),
            )
            close()
            return@callbackFlow
        }

        val outcome = withContext(Dispatchers.IO) { runCatching { post(bytes, fileName, onProgress) } }
        outcome
            .onSuccess { trySend(Result.success(it)) }
            .onFailure { trySend(Result.failure(it)) }
        close()
        awaitClose { }
    }.flowOn(Dispatchers.IO)

    private suspend fun post(
        bytes: ByteArray,
        fileName: String,
        onProgress: (Float) -> Unit,
    ): Uploaded {
        val token = auth.currentUser?.getIdToken(false)?.await()?.token
            ?: error("Sign in again to upload a build.")

        val connection = (URL(BuildConfig.APK_UPLOAD_URL.trimEnd('/') + "/upload")
            .openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 30_000
            readTimeout = 180_000
            setFixedLengthStreamingMode(bytes.size)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/vnd.android.package-archive")
            setRequestProperty("X-File-Name", fileName)
        }

        try {
            connection.outputStream.use { out: OutputStream ->
                // Stream in slices so progress is real and memory stays flat.
                val slice = 64 * 1024
                var sent = 0
                while (sent < bytes.size) {
                    val end = minOf(sent + slice, bytes.size)
                    out.write(bytes, sent, end - sent)
                    sent = end
                    onProgress(sent.toFloat() / bytes.size)
                }
                out.flush()
            }

            val code = connection.responseCode
            val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code !in 200..299) {
                val message = runCatching {
                    JSONObject(body).optString("error")
                }.getOrNull()?.takeIf { it.isNotBlank() }
                error(
                    when {
                        message != null -> message
                        code == 401 -> "Sign in again to upload a build."
                        code == 413 -> "That build is larger than the 100 MB limit."
                        else -> "The upload failed (HTTP $code)."
                    },
                )
            }

            val json = JSONObject(body)
            val url = json.optString("url")
            if (url.isBlank()) error("The uploader did not return a link.")
            return Uploaded(
                url = url,
                path = json.optString("path"),
                sizeBytes = json.optLong("size", bytes.size.toLong()),
            )
        } finally {
            connection.disconnect()
        }
    }
}
