package app.tab.store.core.utils

/**
 * Install counts are rounded on purpose.
 *
 * An exact number is a privacy leak (it moves every time anyone installs) and it
 * is noise: "1,284 installs" reads worse than "1k+" and tells you nothing more
 * useful. The store shows a bucket instead.
 */
object InstallCount {

    /** e.g. 0 → "New", 7 → "5+", 42 → "40+", 1_284 → "1k+", 2_500_000 → "2M+". */
    fun label(count: Int): String = when {
        count <= 0 -> "New"
        count < 10 -> "5+"
        count < 100 -> "${(count / 10) * 10}+"
        count < 1_000 -> "${(count / 100) * 100}+"
        count < 1_000_000 -> {
            val thousands = count / 1_000
            if (thousands >= 10) "${thousands}k+" else "${thousands}k+"
        }
        else -> "${count / 1_000_000}M+"
    }

    /** Long form for a listing: "1k+ installs". */
    fun phrase(count: Int): String {
        val label = label(count)
        return if (label == "New") "New" else "$label installs"
    }
}
