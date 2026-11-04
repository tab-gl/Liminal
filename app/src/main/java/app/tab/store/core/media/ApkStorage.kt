package app.tab.store.core.media

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds go to Firebase Storage.
 *
 * Cloudinary's unsigned preset caps uploads at 10 MB and rejects ZIP-based
 * content, which is exactly what an APK is — so an app of any real size could
 * never be published. Storage has no such restriction on this plan, and the
 * download URL it returns is what Firestore keeps on the listing.
 */
@Singleton
class ApkStorage @Inject constructor(
    private val storage: FirebaseStorage,
) {
    data class Uploaded(val url: String, val path: String, val sizeBytes: Long)

    /** The hard ceiling, mirrored in storage.rules. */
    val maxBytes: Long = 100L * 1024 * 1024

    fun upload(
        bytes: ByteArray,
        fileName: String,
        storeId: String,
        onProgress: (Float) -> Unit = {},
    ): Flow<Result<Uploaded>> = callbackFlow {
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
                            "The limit is ${maxBytes / 1048576} MB — split the release or " +
                            "host the build yourself and paste the link.",
                    ),
                ),
            )
            close()
            return@callbackFlow
        }

        val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").takeLast(60)
        val ref = storage.reference.child("apks/$storeId/${System.currentTimeMillis()}-$safeName")
        val task = ref.putBytes(bytes)
        task.addOnProgressListener { snapshot ->
            val total = snapshot.totalByteCount.coerceAtLeast(1)
            onProgress(snapshot.bytesTransferred.toFloat() / total)
        }
        task.addOnSuccessListener {
            ref.downloadUrl
                .addOnSuccessListener { uri ->
                    trySend(Result.success(Uploaded(uri.toString(), ref.path, bytes.size.toLong())))
                    close()
                }
                .addOnFailureListener { error ->
                    trySend(Result.failure(error))
                    close()
                }
        }
        task.addOnFailureListener { error ->
            trySend(Result.failure(error))
            close()
        }

        awaitClose { task.cancel() }
    }

    suspend fun delete(path: String): Boolean = runCatching {
        storage.reference.child(path).delete().await()
        true
    }.getOrDefault(false)

    /** Kept for symmetry with the picker, which hands back a content Uri. */
    fun pathFor(uri: Uri): String = uri.lastPathSegment.orEmpty()
}
