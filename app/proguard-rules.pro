# Sproutly release rules
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Firebase / Firestore model reflection
-keepclassmembers class app.tab.store.data.remote.dto.** { *; }
-keep class app.tab.store.data.remote.dto.** { *; }

# Keep enums used by Firestore serialization
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Kotlin serialization-free DTOs
-dontwarn kotlinx.parcelize.**

# Hilt
-keep,allowobfuscation @interface dagger.hilt.**
-keep class dagger.hilt.** { *; }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# Play services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
