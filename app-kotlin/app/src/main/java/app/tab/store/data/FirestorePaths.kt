package app.tab.store.data

import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * Every collection and composite id in one place, mirroring the security rules.
 * The ids that encode two entities are built by helpers so the client and the
 * rules can never disagree about their shape.
 */
object FirestorePaths {
    const val USERS = "users"
    const val APPLICATIONS = "applications"
    const val REVIEWS = "reviews"
    const val BOOKMARKS = "bookmarks"
    const val FOLLOWS = "developer_follows"
    const val EVENTS = "application_events"
    const val CAMPAIGNS = "advertising_campaigns"
    const val NOTIFICATIONS = "notifications"
    const val REPORTS = "reports"

    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    val users: CollectionReference get() = db.collection(USERS)
    val applications: CollectionReference get() = db.collection(APPLICATIONS)
    val reviews: CollectionReference get() = db.collection(REVIEWS)
    val bookmarks: CollectionReference get() = db.collection(BOOKMARKS)
    val notifications: CollectionReference get() = db.collection(NOTIFICATIONS)
    val events: CollectionReference get() = db.collection(EVENTS)

    fun user(uid: String): DocumentReference = users.document(uid)
    fun application(id: String): DocumentReference = applications.document(id)
    fun notification(id: String): DocumentReference = notifications.document(id)

    fun analytics(appId: String): CollectionReference = application(appId).collection("analytics")
    fun appAnalyticsDay(appId: String, day: String): DocumentReference = analytics(appId).document(day)

    /** Published listings, newest first. The base query for every feed. */
    fun liveApplications(): Query = applications.whereEqualTo("status", "approved")

    fun bookmarkId(uid: String, appId: String) = "${uid}_$appId"
    fun reviewId(appId: String, uid: String) = "${appId}_$uid"
    fun followId(followerId: String, developerId: String) = "${followerId}_$developerId"
}
