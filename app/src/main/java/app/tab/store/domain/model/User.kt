package app.tab.store.domain.model

enum class UserRole { CUSTOMER, SELLER, ADMIN;

    /** How the role is described to a person. */
    val label: String
        get() = when (this) {
            CUSTOMER -> "Shopper"
            SELLER -> "Developer"
            ADMIN -> "Admin"
        }

    companion object {
        fun from(value: String?): UserRole =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: CUSTOMER
    }
}

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val phone: String? = null,
    val role: UserRole = UserRole.CUSTOMER,
    val storeId: String? = null,
    val createdAt: Long = 0L,
) {
    val isSeller: Boolean get() = role == UserRole.SELLER || role == UserRole.ADMIN
    val isAdmin: Boolean get() = role == UserRole.ADMIN
    val initials: String
        get() = displayName.trim().split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { email.take(1).uppercase() }
}
