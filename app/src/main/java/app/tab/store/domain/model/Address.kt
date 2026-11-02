package app.tab.store.domain.model

data class Address(
    val id: String = "",
    val label: String = "Home",
    val fullName: String = "",
    val phone: String = "",
    val line1: String = "",
    val line2: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val isDefault: Boolean = false,
) {
    val oneLine: String
        get() = listOf(line1, line2, city, state, pincode)
            .filter { it.isNotBlank() }
            .joinToString(", ")

    /**
     * Phone is deliberately not required: an app store has nothing to deliver,
     * so billing details are collected only where they are actually needed
     * (paid apps and tax invoices) and never to browse or install.
     */
    fun isComplete(): Boolean =
        fullName.isNotBlank() &&
            line1.isNotBlank() && city.isNotBlank() &&
            state.isNotBlank() && pincode.length == 6
}
