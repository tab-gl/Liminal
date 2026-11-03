package app.tab.store.core.account

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The accounts that have signed in on this device.
 *
 * Firebase holds one live session at a time, so a true simultaneous multi-login
 * is not available to a client app. What we *can* do — and what this does — is
 * remember every account that has signed in here, so the switcher can list them
 * all with their name and photo instead of asking you to type an address again.
 *
 * Selecting another account ends the current session and hands over to the
 * provider's own picker, which is the one step the SDK will not do for us.
 */
@Singleton
class AccountRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
) {
    data class KnownAccount(
        val uid: String,
        val email: String,
        val displayName: String,
        val photoUrl: String?,
        val provider: String,
        val lastUsedAt: Long,
    )

    private val prefs = context.getSharedPreferences("sproutly_accounts", Context.MODE_PRIVATE)

    init {
        // Recording from the auth listener catches every route in: password,
        // Google, and a restored session after a reinstall.
        auth.addAuthStateListener { state ->
            state.currentUser?.let { record(it) }
        }
    }

    private fun record(user: FirebaseUser) {
        // Every provider, joined: a linked account can have several, and the
        // first one is not always Google even when Google sign-in was used.
        val provider = user.providerData
            .mapNotNull { it.providerId }
            .joinToString(",")
            .ifBlank { "password" }
        val account = KnownAccount(
            uid = user.uid,
            email = user.email.orEmpty(),
            displayName = user.displayName.orEmpty().ifBlank {
                user.email?.substringBefore("@").orEmpty()
            },
            photoUrl = user.photoUrl?.toString(),
            provider = provider,
            lastUsedAt = System.currentTimeMillis(),
        )
        val existing = all().filterNot { it.uid == account.uid }
        write(existing + account)
    }

    /** Most recently used first. */
    fun all(): List<KnownAccount> = runCatching {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            KnownAccount(
                uid = item.optString("uid"),
                email = item.optString("email"),
                displayName = item.optString("displayName"),
                photoUrl = item.optString("photoUrl").takeIf { it.isNotBlank() && it != "null" },
                provider = item.optString("provider", "password"),
                lastUsedAt = item.optLong("lastUsedAt"),
            )
        }.filter { it.uid.isNotBlank() }.sortedByDescending { it.lastUsedAt }
    }.getOrDefault(emptyList())

    fun forget(uid: String) = write(all().filterNot { it.uid == uid })

    private fun write(accounts: List<KnownAccount>) {
        val array = JSONArray()
        accounts.forEach { account ->
            array.put(
                JSONObject().apply {
                    put("uid", account.uid)
                    put("email", account.email)
                    put("displayName", account.displayName)
                    put("photoUrl", account.photoUrl ?: "")
                    put("provider", account.provider)
                    put("lastUsedAt", account.lastUsedAt)
                },
            )
        }
        prefs.edit { putString(KEY, array.toString()) }
    }

    private companion object {
        const val KEY = "accounts"
    }
}
