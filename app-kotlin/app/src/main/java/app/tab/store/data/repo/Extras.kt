package app.tab.store.data.repo

import android.content.Context
import android.net.Uri
import app.tab.store.data.FirestorePaths
import app.tab.store.data.model.AppListing
import app.tab.store.data.model.Campaign
import app.tab.store.data.model.Review
import app.tab.store.data.model.UserProfile
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Cloud Storage uploads.
 *
 * Before a draft exists there is no application id, so uploads stage under the
 * owner's uid - `storage.rules` allows that via `ownsStaging()`, which is what
 * makes the first-time icon upload work at all.
 */
class MediaRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
) {
    private fun folder(kind: String) = when (kind) {
        "icon" -> "app_icons"
        "banner" -> "banners"
        else -> "screenshots"
    }

    suspend fun upload(
        context: Context,
        uri: Uri,
        ownerId: String,
        kind: String = "icon",
    ): String {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("That file could not be read.")
        require(bytes.size <= 8 * 1024 * 1024) { "Images must be 8 MB or smaller." }

        val extension = context.contentResolver.getType(uri)?.substringAfterLast('/') ?: "jpg"
        val path = "${folder(kind)}/$ownerId/${UUID.randomUUID()}.$extension"
        val ref = storage.reference.child(path)
        ref.putBytes(bytes).await()
        return ref.downloadUrl.await().toString()
    }
}

class ReviewRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("asia-south1"),
) {
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    /** One review per person per app; the document id enforces it. */
    suspend fun upsert(
        applicationId: String,
        uid: String,
        userName: String,
        photoUrl: String?,
        rating: Int,
        body: String,
    ) {
        val payload = mapOf(
            "applicationId" to applicationId,
            "userId" to uid,
            "userName" to userName,
            "userPhotoUrl" to photoUrl,
            "rating" to rating.coerceIn(1, 5),
            "body" to body.trim(),
            "helpfulCount" to 0,
            "status" to "published",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        db.collection(FirestorePaths.REVIEWS)
            .document(FirestorePaths.reviewId(applicationId, uid))
            .set(payload)
            .await()
    }

    suspend fun delete(applicationId: String, uid: String) {
        db.collection(FirestorePaths.REVIEWS)
            .document(FirestorePaths.reviewId(applicationId, uid))
            .delete()
            .await()
    }

    /** Developer reply - privileged, so it goes through a callable. */
    suspend fun respond(applicationId: String, reviewId: String, body: String) {
        functions.getHttpsCallable("respondToReview").call(
            mapOf(
                "applicationId" to applicationId,
                "reviewId" to reviewId,
                "body" to body,
            ),
        ).await()
    }
}

class CampaignRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("asia-south1"),
) {
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    suspend fun mine(uid: String): List<Campaign> {
        val snapshot = db.collection(FirestorePaths.CAMPAIGNS)
            .whereEqualTo("ownerId", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(Campaign::class.java)?.copy(id = it.id) }
    }

    suspend fun create(
        uid: String,
        listing: AppListing,
        name: String,
        budgetPaise: Long,
        placements: List<String>,
        categoryIds: List<String>,
        headline: String,
    ): String {
        val payload = mapOf(
            "ownerId" to uid,
            "applicationId" to listing.id,
            "applicationName" to listing.name,
            "applicationIconUrl" to listing.iconUrl,
            "name" to name,
            "objective" to "awareness",
            "budgetPaise" to budgetPaise,
            "spendPaise" to 0,
            "cpmPaise" to 25_000,
            "impressions" to 0,
            "clicks" to 0,
            "placements" to placements,
            "categoryIds" to categoryIds,
            "headline" to headline,
            "status" to "pending_approval",
            "createdAt" to FieldValue.serverTimestamp(),
        )
        val ref = db.collection(FirestorePaths.CAMPAIGNS).document()
        ref.set(payload).await()
        return ref.id
    }

    suspend fun setStatus(campaignId: String, status: String) {
        db.collection(FirestorePaths.CAMPAIGNS).document(campaignId)
            .update("status", status)
            .await()
    }

    /** Funding is verified server-side before the budget is credited. */
    suspend fun startFunding(campaignId: String, amountPaise: Long): Map<String, Any> {
        val result = functions.getHttpsCallable("createRazorpayOrder").call(
            mapOf("campaignId" to campaignId, "amountPaise" to amountPaise),
        ).await()
        @Suppress("UNCHECKED_CAST")
        return result.data as? Map<String, Any> ?: emptyMap()
    }

    suspend fun confirmFunding(paymentId: String, signature: String, orderId: String) {
        functions.getHttpsCallable("verifyRazorpayPayment").call(
            mapOf(
                "razorpayPaymentId" to paymentId,
                "razorpaySignature" to signature,
                "razorpayOrderId" to orderId,
            ),
        ).await()
    }
}

class AdminRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("asia-south1"),
) {
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    suspend fun pendingApplications(limit: Long = 40): List<AppListing> {
        val snapshot = db.collection(FirestorePaths.APPLICATIONS)
            .whereEqualTo("status", "pending_review")
            .orderBy("updatedAt", Query.Direction.ASCENDING)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(AppListing::class.java)?.copy(id = it.id) }
    }

    suspend fun pendingCampaigns(limit: Long = 40): List<Campaign> {
        val snapshot = db.collection(FirestorePaths.CAMPAIGNS)
            .whereEqualTo("status", "pending_approval")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(Campaign::class.java)?.copy(id = it.id) }
    }

    suspend fun openReports(limit: Long = 40): List<Report> {
        val snapshot = db.collection(FirestorePaths.REPORTS)
            .whereEqualTo("status", "open")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(Report::class.java)?.copy(id = it.id) }
    }

    suspend fun moderateApplication(
        applicationId: String,
        approve: Boolean,
        note: String? = null,
    ) {
        functions.getHttpsCallable("moderateApplication").call(
            mapOf(
                "applicationId" to applicationId,
                "decision" to if (approve) "approve" else "reject",
                "note" to note,
            ),
        ).await()
    }

    suspend fun moderateCampaign(campaignId: String, approve: Boolean, note: String? = null) {
        functions.getHttpsCallable("moderateCampaign").call(
            mapOf(
                "campaignId" to campaignId,
                "decision" to if (approve) "approve" else "reject",
                "note" to note,
            ),
        ).await()
    }

    suspend fun resolveReport(reportId: String, outcome: String, note: String? = null) {
        functions.getHttpsCallable("resolveReport").call(
            mapOf("reportId" to reportId, "outcome" to outcome, "note" to note),
        ).await()
    }

    suspend fun recentUsers(limit: Long = 40): List<UserProfile> {
        val snapshot = db.collection(FirestorePaths.USERS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        return snapshot.documents.mapNotNull { it.toObject(UserProfile::class.java)?.copy(uid = it.id) }
    }

    suspend fun setRole(uid: String, role: String) {
        functions.getHttpsCallable("setUserRole").call(
            mapOf("uid" to uid, "role" to role),
        ).await()
    }
}

/** A user-filed report against a listing, a review or a developer. */
data class Report(
    val id: String = "",
    val reporterId: String = "",
    val targetType: String = "application",
    val targetId: String = "",
    val reason: String = "",
    val details: String? = null,
    val status: String = "open",
)
