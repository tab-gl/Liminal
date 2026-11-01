package app.tab.store.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import app.tab.store.core.network.FunctionsClient
import app.tab.store.domain.repository.DataResult
import app.tab.store.domain.repository.MediaRepository
import app.tab.store.domain.repository.UploadedAsset
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import org.json.JSONObject

/**
 * Direct-to-Cloudinary uploads.
 *
 * The Android client never holds the Cloudinary API secret. It asks a Cloud
 * Function for a short-lived signature, then posts the bytes straight to
 * Cloudinary's CDN ingest endpoint with that signature attached.
 */
@Singleton
class CloudinaryUploader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val functionsClient: FunctionsClient,
) : MediaRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun upload(
        bytes: ByteArray,
        fileName: String,
        folder: String,
        onProgress: (Float) -> Unit,
    ): DataResult<UploadedAsset> = withContext(Dispatchers.IO) {
        val signature = functionsClient.call(
            "getCloudinaryUploadSignature",
            mapOf("folder" to folder),
        )
        val params = when (signature) {
            is DataResult.Failure -> return@withContext signature
            is DataResult.Success -> signature.value
        }

        val cloudName = params["cloudName"] as? String
        val apiKey = params["apiKey"] as? String
        val timestamp = params["timestamp"] as? String
        val signatureValue = params["signature"] as? String
        val uploadUrl = params["uploadUrl"] as? String
        if (cloudName.isNullOrBlank() || apiKey.isNullOrBlank() ||
            timestamp.isNullOrBlank() || signatureValue.isNullOrBlank() || uploadUrl.isNullOrBlank()
        ) {
            return@withContext DataResult.Failure("Media service is not configured correctly.")
        }

        val mime = guessMime(fileName)
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", fileName, ProgressRequestBody(bytes, mime.toMediaType(), onProgress))
            .addFormDataPart("api_key", apiKey)
            .addFormDataPart("timestamp", timestamp)
            .addFormDataPart("signature", signatureValue)
            .addFormDataPart("folder", folder)
            .build()

        val request = Request.Builder().url(uploadUrl).post(body).build()
        try {
            client.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = runCatching {
                        JSONObject(text).optJSONObject("error")?.optString("message")
                    }.getOrNull() ?: "Upload failed (${response.code})."
                    return@withContext DataResult.Failure(message)
                }
                val json = JSONObject(text)
                DataResult.Success(
                    UploadedAsset(
                        url = json.optString("secure_url"),
                        publicId = json.optString("public_id"),
                        width = json.optInt("width"),
                        height = json.optInt("height"),
                        bytes = json.optLong("bytes"),
                    ),
                )
            }
        } catch (t: Throwable) {
            DataResult.Failure("Upload failed. Check your connection and try again.", t)
        }
    }

    override suspend fun delete(publicId: String): DataResult<Unit> {
        val res = functionsClient.call("deleteCloudinaryAsset", mapOf("publicId" to publicId))
        return when (res) {
            is DataResult.Failure -> res
            is DataResult.Success -> DataResult.Success(Unit)
        }
    }

    /** Reads a picked [Uri], downscales it to a sane max edge, and returns JPEG bytes. */
    suspend fun prepare(uri: Uri, maxEdge: Int = 1600, quality: Int = 86): ByteArray? =
        withContext(Dispatchers.IO) {
            runCatching {
                val input = context.contentResolver.openInputStream(uri) ?: return@runCatching null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, bounds)
                input.close()

                var sample = 1
                val longest = maxOf(bounds.outWidth, bounds.outHeight)
                while (longest / sample > maxEdge) sample *= 2

                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, opts)
                } ?: return@runCatching null

                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                bitmap.recycle()
                out.toByteArray()
            }.getOrNull()
        }

    private fun guessMime(fileName: String): String = when {
        fileName.endsWith(".png", true) -> "image/png"
        fileName.endsWith(".webp", true) -> "image/webp"
        else -> "image/jpeg"
    }

    private class ProgressRequestBody(
        private val bytes: ByteArray,
        private val contentType: okhttp3.MediaType,
        private val onProgress: (Float) -> Unit,
    ) : RequestBody() {
        override fun contentType() = contentType
        override fun contentLength() = bytes.size.toLong()
        override fun writeTo(sink: BufferedSink) {
            val chunk = 16 * 1024
            var written = 0
            while (written < bytes.size) {
                val end = minOf(written + chunk, bytes.size)
                sink.write(bytes, written, end - written)
                written = end
                onProgress(written.toFloat() / bytes.size)
            }
        }
    }
}
