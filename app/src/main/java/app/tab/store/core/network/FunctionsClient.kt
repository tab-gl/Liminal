package app.tab.store.core.network

import app.tab.store.core.firebase.toUserMessage
import app.tab.store.domain.repository.DataResult
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin, typed wrapper around Cloud Functions. Every trusted operation in
 * Sproutly (role changes, order creation, stock movement, Cloudinary signing)
 * goes through here — the client never decides these things itself.
 */
@Singleton
class FunctionsClient @Inject constructor(
    private val functions: FirebaseFunctions,
) {
    suspend fun call(name: String, data: Map<String, Any?> = emptyMap()): DataResult<Map<String, Any?>> =
        try {
            val result: HttpsCallableResult = functions.getHttpsCallable(name).call(data).await()
            @Suppress("UNCHECKED_CAST")
            val payload = result.data as? Map<String, Any?> ?: emptyMap()
            if (payload["ok"] == false) {
                DataResult.Failure(payload["message"] as? String ?: "Request was rejected.")
            } else {
                DataResult.Success(payload)
            }
        } catch (t: Throwable) {
            DataResult.Failure(t.toUserMessage(), t)
        }

    suspend fun callString(name: String, data: Map<String, Any?> = emptyMap(), key: String = "id"): DataResult<String> {
        val res = call(name, data)
        return when (res) {
            is DataResult.Failure -> res
            is DataResult.Success -> {
                val value = res.value[key] as? String
                if (value.isNullOrBlank()) DataResult.Failure("The server returned an unexpected response.")
                else DataResult.Success(value)
            }
        }
    }
}
