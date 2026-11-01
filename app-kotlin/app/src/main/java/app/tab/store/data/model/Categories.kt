package app.tab.store.data.model

/** The fixed launch taxonomy. Ids are stable and are what Firestore stores. */
data class AppCategory(val id: String, val label: String, val blurb: String)

val SproutlyCategories = listOf(
    AppCategory("ai_ml", "AI & Machine Learning", "Models, agents and intelligent tools"),
    AppCategory("developer_tools", "Developer Tools", "Build, test and ship faster"),
    AppCategory("productivity", "Productivity", "Plan, focus and get things done"),
    AppCategory("design", "Design", "Interfaces, type and creative craft"),
    AppCategory("education", "Education", "Learning for every stage"),
    AppCategory("finance", "Finance", "Money, markets and tracking"),
    AppCategory("business", "Business", "Teams, customers and operations"),
    AppCategory("entertainment", "Entertainment", "Watch, listen and play"),
    AppCategory("lifestyle", "Lifestyle", "Health, home and everyday life"),
    AppCategory("security", "Security", "Privacy and protection"),
    AppCategory("utilities", "Utilities", "Small tools that do one thing well"),
    AppCategory("open_source", "Open Source", "Community-built and freely licensed"),
    AppCategory("other", "Other", "Everything that resists a label"),
)

fun categoryById(id: String): AppCategory =
    SproutlyCategories.firstOrNull { it.id == id } ?: SproutlyCategories.last()
