package app.tab.store.core.utils

object Validators {
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(value: String): Boolean = emailRegex.matches(value.trim())

    fun isValidPassword(value: String): Boolean = value.length >= 8

    fun passwordStrength(value: String): Int {
        var score = 0
        if (value.length >= 8) score++
        if (value.any { it.isDigit() }) score++
        if (value.any { it.isUpperCase() }) score++
        if (value.any { !it.isLetterOrDigit() }) score++
        return score
    }

    fun isValidPincode(value: String): Boolean = value.length == 6 && value.all { it.isDigit() }

    fun isValidPhone(value: String): Boolean {
        val digits = value.filter { it.isDigit() }
        return digits.length in 10..13
    }

    fun slugify(value: String): String = value.trim().lowercase()
        .replace(Regex("[^a-z0-9\\s-]"), "")
        .replace(Regex("\\s+"), "-")
        .trim('-')
}
