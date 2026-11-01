package app.tab.store.data.repo

import app.tab.store.data.FirestorePaths
import app.tab.store.data.model.AppListing
import app.tab.store.data.model.AppNotification
import app.tab.store.data.model.DailyStat
import app.tab.store.data.model.Review
import app.tab.store.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/** A page of results plus the cursor for the next one. */
data class Paged<T>(val items: List<T>, val cursor: DocumentSnapshot?, val hasMore: Boolean)

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val currentUser: FirebaseUser? get() = auth.currentUser
    val uid: String? get() = auth.currentUser?.uid

    val authState: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        ensureProfile()
    }

    suspend fun signUp(email: String, password: String, displayName: String) {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = requireNotNull(result.user) { "Sign-up did not complete" }
        user.updateProfile(com.google.firebase.auth.userProfileChangeRequest {
            this.displayName = displayName.trim()
        }).await()
        createProfile(user, displayName.trim())
        runCatching { user.sendEmailVerification().await() }
    }

    suspend fun signOut() = auth.signOut()

    /** Creates the profile document on first sign-in if it is missing. */
    suspend fun ensureProfile(): UserProfile? {
        val user = auth.currentUser ?: return null
        val ref = FirestorePaths.user(user.uid)
        val snapshot = ref.get().await()
        if (!snapshot.exists()) {
            return createProfile(user, user.displayName ?: nameFromEmail(user.email))
        }
        return snapshot.toObject(UserProfile::class.java)?.copy(uid = user.uid)
    }

    private suspend fun createProfile(user: FirebaseUser, displayName: String): UserProfile {
        val profile = mapOf(
            "uid" to user.uid,
            "displayName" to displayName,
            "photoUrl" to user.photoUrl?.toString(),
            "role" to "user",
            "isDeveloper" to false,
            "isPublic" to true,
            "followerCount" to 0,
            "publishedAppCount" to 0,
            "reviewCount" to 0,
            "onboarded" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        FirestorePaths.user(user.uid).set(profile).await()
        return UserProfile(uid = user.uid, displayName = displayName)
    }

    private fun nameFromEmail(email: String?): String =
        email?.substringBefore('@')?.replace(Regex("[._-]+"), " ") ?: "Sproutly member"
}

class ProfileRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    fun watch(uid: String): Flow<UserProfile?> = callbackFlow {
        val registration = FirestorePaths.user(uid).addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.takeIf { it.exists() }?.toObject(UserProfile::class.java)?.copy(uid = uid))
        }
        awaitClose { registration.remove() }
    }
}

class ApplicationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun watch(id: String): Flow<AppListing?> = callbackFlow {
        val registration = FirestorePaths.application(id).addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.takeIf { it.exists() }?.toObject(AppListing::class.java)?.copy(id = id))
        }
        awaitClose { registration.remove() }
    }

    /**
     * The main feed. Cursor pagination via `startAfter` - no offsets, because an
     * offset query is a full scan and Firestore bills for it.
     */
    suspend fun feed(
        limit: Long = 12,
        cursor: DocumentSnapshot? = null,
        sort: FeedSort = FeedSort.NEWEST,
        categoryId: String? = null,
    ): Paged<AppListing> {
        var query: Query = FirestorePaths.liveApplications()
        if (categoryId != null) query = query.whereEqualTo("categoryId", categoryId)
        query = when (sort) {
            FeedSort.NEWEST -> query.orderBy("createdAt", Query.Direction.DESCENDING)
            FeedSort.TOP_RATED -> query.orderBy("rating", Query.Direction.DESCENDING)
            FeedSort.MOST_VIEWED -> query.orderBy("viewCount", Query.Direction.DESCENDING)
            FeedSort.TRENDING -> query.orderBy("trendingScore", Query.Direction.DESCENDING)
        }.limit(limit)
        if (cursor != null) query = query.startAfter(cursor)

        val snapshot = query.get().await()
        val items = snapshot.documents.mapNotNull { doc ->
            doc.toObject(AppListing::class.java)?.copy(id = doc.id)
        }
        return Paged(items, snapshot.documents.lastOrNull(), snapshot.size().toLong() == limit)
    }

    suspend fun rail(field: String, limit: Long = 10, onlyFeatured: Boolean = false): List<AppListing> {
        var query = FirestorePaths.liveApplications()
        if (onlyFeatured) query = query.whereEqualTo("featured", true)
        val snapshot = query.orderBy(field, Query.Direction.DESCENDING).limit(limit).get().await()
        return snapshot.documents.mapNotNull { it.toObject(AppListing::class.java)?.copy(id = it.id) }
    }

    /** Prefix search on the denormalised title, plus a tag match. */
    suspend fun search(term: String, limit: Long = 30): List<AppListing> {
        val clean = term.trim().lowercase()
        if (clean.isEmpty()) return emptyList()
        val byTitle = FirestorePaths.liveApplications()
            .whereGreaterThanOrEqualTo("search.title", clean)
            .whereLessThanOrEqualTo("search.title", "$clean\uf8ff")
            .orderBy("search.title")
            .limit(limit)
            .get()
            .await()
        val byTag = FirestorePaths.liveApplications()
            .whereArrayContains("search.keywords", clean)
            .limit(limit)
            .get()
            .await()
        return (byTitle.documents + byTag.documents)
            .distinctBy { it.id }
            .mapNotNull { it.toObject(AppListing::class.java)?.copy(id = it.id) }
            .sortedByDescending { it.trendingScore }
    }

    /** Appends a product event; the hourly function folds it into the counters. */
    suspend fun recordEvent(applicationId: String, type: String, uid: String?) {
        if (uid == null) return
        runCatching {
            FirestorePaths.events.add(
                mapOf(
                    "applicationId" to applicationId,
                    "userId" to uid,
                    "type" to type,
                    "platform" to "android",
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    fun reviews(applicationId: String, limit: Long = 20): Flow<List<Review>> = callbackFlow {
        val registration = FirestorePaths.reviews
            .whereEqualTo("applicationId", applicationId)
            .whereEqualTo("status", "published")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, _ ->
                trySend(
                    snapshot?.documents?.mapNotNull {
                        it.toObject(Review::class.java)?.copy(id = it.id)
                    } ?: emptyList(),
                )
            }
        awaitClose { registration.remove() }
    }

    /** Per-day rollups written by the aggregation function. */
    suspend fun series(applicationId: String, days: Long = 30): List<DailyStat> {
        val snapshot = FirestorePaths.analytics(applicationId)
            .orderBy(com.google.firebase.firestore.FieldPath.documentId(), Query.Direction.DESCENDING)
            .limit(days)
            .get()
            .await()
        return snapshot.documents.map { doc ->
            DailyStat(
                date = doc.id,
                views = (doc.getLong("views") ?: 0).toInt(),
                clicks = (doc.getLong("clicks") ?: 0).toInt(),
                downloads = (doc.getLong("downloads") ?: 0).toInt(),
            )
        }.sortedBy { it.date }
    }
}

enum class FeedSort(val label: String) {
    NEWEST("Newest"),
    TOP_RATED("Top rated"),
    MOST_VIEWED("Most viewed"),
    TRENDING("Trending"),
}

class BookmarkRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun watch(uid: String?, appId: String): Flow<Boolean> = callbackFlow {
        if (uid == null) {
            trySend(false)
            close()
            return@callbackFlow
        }
        val registration = FirestorePaths.bookmarks
            .document(FirestorePaths.bookmarkId(uid, appId))
            .addSnapshotListener { snapshot, _ -> trySend(snapshot?.exists() == true) }
        awaitClose { registration.remove() }
    }

    suspend fun toggle(uid: String, appId: String, appName: String): Boolean {
        val ref = FirestorePaths.bookmarks.document(FirestorePaths.bookmarkId(uid, appId))
        val exists = ref.get().await().exists()
        if (exists) {
            ref.delete().await()
            return false
        }
        ref.set(
            mapOf(
                "userId" to uid,
                "applicationId" to appId,
                "applicationName" to appName,
                "createdAt" to FieldValue.serverTimestamp(),
            ),
        ).await()
        return true
    }

    suspend fun saved(uid: String): List<AppListing> {
        val marks = FirestorePaths.bookmarks
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .await()
        val ids = marks.documents.mapNotNull { it.getString("applicationId") }
        if (ids.isEmpty()) return emptyList()
        val apps = ids.chunked(10).flatMap { chunk ->
            FirestorePaths.applications
                .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunk)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(AppListing::class.java)?.copy(id = it.id) }
        }
        val byId = apps.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }
}

class NotificationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    fun inbox(uid: String?, limit: Long = 50): Flow<List<AppNotification>> = callbackFlow {
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration = FirestorePaths.notifications
            .whereEqualTo("userId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, _ ->
                trySend(
                    snapshot?.documents?.mapNotNull {
                        it.toObject(AppNotification::class.java)?.copy(id = it.id)
                    } ?: emptyList(),
                )
            }
        awaitClose { registration.remove() }
    }

    fun unreadCount(uid: String?): Flow<Int> = callbackFlow {
        if (uid == null) {
            trySend(0)
            close()
            return@callbackFlow
        }
        val registration = FirestorePaths.notifications
            .whereEqualTo("userId", uid)
            .whereEqualTo("readAt", null)
            .addSnapshotListener { snapshot, _ -> trySend(snapshot?.size() ?: 0) }
        awaitClose { registration.remove() }
    }

    suspend fun markRead(id: String) {
        FirestorePaths.notification(id).update("readAt", FieldValue.serverTimestamp()).await()
    }
}

class PublishingRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("asia-south1"),
) {
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    /** Creates a draft. Counters are seeded at zero because the rules assert it. */
    suspend fun saveDraft(listing: AppListing, uid: String, ownerName: String): String {
        val payload = mutableMapOf<String, Any?>(
            "ownerId" to uid,
            "ownerName" to ownerName,
            "name" to listing.name.trim(),
            "tagline" to listing.tagline.trim(),
            "description" to listing.description.trim(),
            "categoryId" to listing.categoryId,
            "tags" to listing.tags,
            "iconUrl" to listing.iconUrl,
            "bannerUrl" to listing.bannerUrl,
            "screenshots" to listing.screenshots,
            "websiteUrl" to listing.websiteUrl,
            "launchUrl" to listing.launchUrl,
            "downloadUrl" to listing.downloadUrl,
            "platforms" to listing.platforms,
            "pricingModel" to listing.pricingModel,
            "priceLabel" to listing.priceLabel,
            "isOpenSource" to listing.isOpenSource,
            "features" to listing.features,
            "documentationUrl" to listing.documentationUrl,
            "supportEmail" to listing.supportEmail,
            "privacyPolicyUrl" to listing.privacyPolicyUrl,
            "termsUrl" to listing.termsUrl,
            "status" to "draft",
            "rating" to 0,
            "reviewCount" to 0,
            "viewCount" to 0,
            "uniqueViewerCount" to 0,
            "clickCount" to 0,
            "downloadCount" to 0,
            "bookmarkCount" to 0,
            "trendingScore" to 0,
            "sponsored" to false,
            "featured" to false,
            "search" to mapOf(
                "title" to listing.name.trim().lowercase(),
                "keywords" to listing.tags.map { it.lowercase() },
            ),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        payload.entries.removeAll { it.value == null }

        if (listing.id.isEmpty()) {
            val ref = FirestorePaths.applications.document()
            ref.set(payload).await()
            return ref.id
        }
        FirestorePaths.application(listing.id).set(payload).await()
        return listing.id
    }

    /**
     * Submits through the callable so the server runs the checks that matter:
     * ownership, required fields, link shape, duplicate names.
     */
    suspend fun submit(applicationId: String) {
        functions.getHttpsCallable("submitApplication")
            .call(mapOf("applicationId" to applicationId))
            .await()
    }

    suspend fun myApps(uid: String): List<AppListing> {
        val snapshot = FirestorePaths.applications
            .whereEqualTo("ownerId", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(60)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(AppListing::class.java)?.copy(id = it.id) }
    }
}
