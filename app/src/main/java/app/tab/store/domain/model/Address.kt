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

    fun isComplete(): Boolean =
        fullName.isNotBlank() && phone.length >= 10 &&
            line1.isNotBlank() && city.isNotBlank() &&
            state.isNotBlank() && pincode.length == 6
}
