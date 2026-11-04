package app.tab.store.domain.model

/**
 * One person's rating of an app.
 *
 * The average is computed from these documents rather than stored on the listing:
 * a listing must not be writable by the people rating it, and on this plan there
 * is no server to aggregate on.
 */
data class Review(
    val reviewerId: String = "",
    val reviewerName: String = "",
    val rating: Int = 0,
    val text: String = "",
    val createdAt: Long = 0L,
)

/** The summary shown on a listing. */
data class RatingSummary(
    val average: Double = 0.0,
    val count: Int = 0,
    val mine: Review? = null,
) {
    val hasRatings: Boolean get() = count > 0
    /** Rounded for display, e.g. 4.3. */
    val display: String get() = if (count == 0) "—" else String.format("%.1f", average)
}
