package app.tab.store.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class CampaignStatus(val wire: String, val label: String) {
    DRAFT("draft", "Draft"),
    PENDING_APPROVAL("pending_approval", "In review"),
    ACTIVE("active", "Delivering"),
    PAUSED("paused", "Paused"),
    EXHAUSTED("exhausted", "Budget spent"),
    REJECTED("rejected", "Rejected");

    companion object {
        fun from(raw: String?): CampaignStatus = entries.firstOrNull { it.wire == raw } ?: DRAFT
    }
}

/**
 * A sponsored placement. Money and delivery counters are server-owned: the rules
 * reject client writes to `spend`, `impressions` and `clicks`, and `serveAds`
 * maintains them inside a transaction.
 */
data class Campaign(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val applicationId: String = "",
    val applicationName: String = "",
    val applicationIconUrl: String? = null,
    val name: String = "",
    val objective: String = "awareness",
    val budgetPaise: Long = 0,
    val spendPaise: Long = 0,
    val cpmPaise: Long = 0,
    val impressions: Int = 0,
    val clicks: Int = 0,
    val placements: List<String> = emptyList(),
    val categoryIds: List<String> = emptyList(),
    val headline: String = "",
    val status: String = "draft",
    val rejectionReason: String? = null,
    val startDate: Date? = null,
    val endDate: Date? = null,
    @ServerTimestamp val createdAt: Date? = null,
) {
    val campaignStatus: CampaignStatus get() = CampaignStatus.from(status)
    val budgetRupees: Long get() = budgetPaise / 100
    val spendRupees: Long get() = spendPaise / 100
    val remainingPaise: Long get() = (budgetPaise - spendPaise).coerceAtLeast(0)
    val ctr: Double get() = if (impressions == 0) 0.0 else clicks.toDouble() / impressions
    val progress: Float
        get() = if (budgetPaise <= 0) 0f else (spendPaise.toFloat() / budgetPaise).coerceIn(0f, 1f)
}
