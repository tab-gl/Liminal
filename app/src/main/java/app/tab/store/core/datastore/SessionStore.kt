package app.tab.store.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.tab.store.domain.model.User
import app.tab.store.domain.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "sproutly_session")

/**
 * Small, fast local mirror of the session so the app can render instantly on
 * cold start, before Firestore answers. Firestore remains the source of truth
 * for roles; this cache is never trusted for authorization decisions.
 */
@Singleton
class SessionStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val uid = stringPreferencesKey("uid")
        val email = stringPreferencesKey("email")
        val name = stringPreferencesKey("name")
        val photo = stringPreferencesKey("photo")
        val role = stringPreferencesKey("role")
        val storeId = stringPreferencesKey("store_id")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
    }

    val cachedUser: Flow<User?> = context.dataStore.data.map { prefs ->
        val uid = prefs[Keys.uid] ?: return@map null
        User(
            uid = uid,
            email = prefs[Keys.email].orEmpty(),
            displayName = prefs[Keys.name].orEmpty(),
            photoUrl = prefs[Keys.photo],
            role = UserRole.from(prefs[Keys.role]),
            storeId = prefs[Keys.storeId],
        )
    }

    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.onboardingDone] ?: false }

    suspend fun save(user: User) {
        context.dataStore.edit { prefs ->
            prefs[Keys.uid] = user.uid
            prefs[Keys.email] = user.email
            prefs[Keys.name] = user.displayName
            prefs[Keys.photo] = user.photoUrl.orEmpty()
            prefs[Keys.role] = user.role.name
            prefs[Keys.storeId] = user.storeId.orEmpty()
        }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[Keys.onboardingDone] = true }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
