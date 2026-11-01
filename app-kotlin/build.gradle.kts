plugins {
    // AGP 9 has Kotlin built in, so org.jetbrains.kotlin.android must not be
    // applied. The Compose Compiler plugin is still required when
    // buildFeatures.compose is on.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
}
