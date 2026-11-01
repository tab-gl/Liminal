package app.tab.store.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateFormatters {
    private val dayFormat = SimpleDateFormat("d MMM yyyy", Locale("en", "IN"))
    private val dayShort = SimpleDateFormat("d MMM", Locale("en", "IN"))
    private val timeFormat = SimpleDateFormat("h:mm a", Locale("en", "IN"))

    fun date(epochMillis: Long): String =
        if (epochMillis <= 0) "—" else dayFormat.format(Date(epochMillis))

    fun dayShort(epochMillis: Long): String = dayShort.format(Date(epochMillis))

    fun dateTime(epochMillis: Long): String =
        if (epochMillis <= 0) "—" else "${dayFormat.format(Date(epochMillis))}, ${timeFormat.format(Date(epochMillis))}"

    fun relative(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
        if (epochMillis <= 0) return "—"
        val diff = now - epochMillis
        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> "just now"
            diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
            diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
            else -> dayShort.format(Date(epochMillis))
        }
    }
}

object Money {
    /** All prices are stored and moved around as paise (Long) to avoid float drift. */
    fun toPaise(rupees: String): Long? =
        rupees.trim().toDoubleOrNull()?.let { Math.round(it * 100) }

    fun toRupeeString(paise: Long): String = "%.2f".format(paise / 100.0)
}
