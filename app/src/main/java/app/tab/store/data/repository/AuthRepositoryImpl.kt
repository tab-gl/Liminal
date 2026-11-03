package app.tab.store.data.repository

import android.util.Log
import app.tab.store.core.datastore.SessionStore
import app.tab.store.core.firebase.orFallback
import app.tab.store.core.firebase.snapshotFlow
import app.tab.store.core.utils.toAuthMessage
import app.tab.store.data.remote.FirestorePaths
import app.tab.store.data.remote.toUser
import app.tab.store.domain.model.User
import app.tab.store.domain.model.UserRole
import app.tab.store.domain.repository.AuthRepository
import app.tab.store.domain.repository.DataResult
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val TAG = "AuthRepository"

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val sessionStore: SessionStore,
) : AuthRepository {

    private val uidFlow = MutableStateFlow(auth.currentUser?.uid)

    init {
        auth.addAuthStateListener { uidFlow.value = it.currentUser?.uid }
    }

    override val currentUser: Flow<User?> = uidFlow.flatMapLatest { uid ->
        if (uid == null) {
            flowOf(null)
        } else {
            // A restored session (second account, reinstall) may have no profile
            // document yet. Repair it before anyone queries rules that read the
            // role, otherwise those rules evaluate on a missing document.
            runCatching { auth.currentUser?.let { ensureProfile(it) } }
            firestore.collection(FirestorePaths.USERS).document(uid)
                .snapshotFlow()
                .map { snap -> if (snap.exists()) snap.toUser() else pendingUser() }.orFallback(pendingUser())
        }
    }

    private fun pendingUser(): User {
        val fbUser = auth.currentUser
        return User(
            uid = fbUser?.uid.orEmpty(),
            email = fbUser?.email.orEmpty(),
            displayName = fbUser?.displayName.orEmpty(),
            photoUrl = fbUser?.photoUrl?.toString(),
            role = UserRole.CUSTOMER,
        )
    }

    override fun isSignedIn(): Boolean = auth.currentUser != null

    override suspend fun signInWithEmail(email: String, password: String): DataResult<User> = try {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: return DataResult.Failure("Sign in did not complete.")
        ensureProfile(user)
        DataResult.Success(loadProfile(user.uid))
    } catch (t: Throwable) {
        Log.e(TAG, "auth call failed", t)
        DataResult.Failure(t.toAuthMessage(), t)
    }

    override suspend fun registerWithEmail(name: String, email: String, password: String): DataResult<User> = try {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = result.user ?: return DataResult.Failure("Registration did not complete.")

        // The account exists from here on. Anything below is best-effort: if the
        // profile document cannot be written (rules not deployed, App Check,
        // offline) we must not strand the user with "email already in use" on a
        // retry. ensureProfile() repairs it on the next sign-in.
        runCatching {
            user.updateProfile(
                UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build(),
            ).await()
        }.onFailure { Log.w(TAG, "updateProfile failed during sign-up", it) }

        runCatching {
            firestore.collection(FirestorePaths.USERS).document(user.uid)
                .set(
                    mapOf(
                        "uid" to user.uid,
                        "email" to user.email.orEmpty(),
                        "displayName" to name.trim(),
                        "photoUrl" to null,
                        "phone" to null,
                        "role" to UserRole.CUSTOMER.name,
                        "storeId" to null,
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                ).await()
        }.onFailure { Log.w(TAG, "users/${user.uid} write failed during sign-up", it) }

        DataResult.Success(loadProfile(user.uid))
    } catch (t: Throwable) {
        Log.e(TAG, "auth call failed", t)
        DataResult.Failure(t.toAuthMessage(), t)
    }

    override suspend fun signInWithGoogle(idToken: String): DataResult<User> = try {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: return DataResult.Failure("Google sign in did not complete.")
        ensureProfile(user)
        DataResult.Success(loadProfile(user.uid))
    } catch (t: Throwable) {
        Log.e(TAG, "auth call failed", t)
        DataResult.Failure(t.toAuthMessage(), t)
    }

    override suspend fun sendPasswordReset(email: String): DataResult<Unit> = try {
        auth.sendPasswordResetEmail(email.trim()).await()
        DataResult.Success(Unit)
    } catch (t: Throwable) {
        Log.e(TAG, "auth call failed", t)
        DataResult.Failure(t.toAuthMessage(), t)
    }

    override suspend fun signOut() {
        auth.signOut()
        sessionStore.clear()
    }

    override suspend fun refreshProfile(): DataResult<User> = try {
        val uid = auth.currentUser?.uid ?: return DataResult.Failure("You are signed out.")
        DataResult.Success(loadProfile(uid))
    } catch (t: Throwable) {
        Log.e(TAG, "auth call failed", t)
        DataResult.Failure(t.toAuthMessage(), t)
    }

    /** Creates the users/{uid} document on first sign-in, preserving any existing role. */
    private suspend fun ensureProfile(user: FirebaseUser) {
        val ref = firestore.collection(FirestorePaths.USERS).document(user.uid)
        val snapshot = ref.get().await()
        val base = mutableMapOf<String, Any?>(
            "uid" to user.uid,
            "email" to user.email.orEmpty(),
            "displayName" to (user.displayName ?: user.email?.substringBefore("@").orEmpty()),
            "photoUrl" to user.photoUrl?.toString(),
        )
        if (!snapshot.exists()) {
            base["role"] = UserRole.CUSTOMER.name
            base["createdAt"] = FieldValue.serverTimestamp()
            base["phone"] = null
            base["storeId"] = null
        }
        ref.set(base, SetOptions.merge()).await()
    }

    private suspend fun loadProfile(uid: String): User {
        val snap = firestore.collection(FirestorePaths.USERS).document(uid).get().await()
        val user = if (snap.exists()) snap.toUser() else pendingUser()
        sessionStore.save(user)
        return user
    }
}
