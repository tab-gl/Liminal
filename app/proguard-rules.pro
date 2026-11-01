# ---------------------------------------------------------------------------
# Sproutly R8 rules
#
# Deliberately minimal. Hilt, Room, Firebase, OkHttp and Compose all ship
# consumer rules inside their AARs, so repeating them here only adds risk —
# over-broad keeps are a common source of R8 failures and of APK bloat.
# ---------------------------------------------------------------------------

# Domain models are read field-by-field from Firestore maps and their enum
# names are persisted as strings, so names must survive obfuscation.
-keep class app.tab.store.domain.model.** { <fields>; }

# Enum names are written to Firestore ("ACTIVE", "PENDING", ...).
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# OkHttp / Okio optional platform integrations.
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# Firebase and Play Services reference optional classes that are not bundled.
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**
