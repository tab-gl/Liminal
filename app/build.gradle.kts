import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

fun secret(key: String, env: String): String? =
    (keystoreProps.getProperty(key) ?: System.getenv(env))?.takeIf { it.isNotBlank() }

android {
    namespace = "app.tab.store"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.tab.store"
        minSdk = 24
        targetSdk = 36
        versionCode = 70
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Public OAuth client id used by Google Sign-In (not a secret).
        // Cloudinary: the cloud name is public (it appears in every asset URL).
        // The upload preset is public too, but should be locked to the
        // "sproutly" folder with a size cap in the Cloudinary console.
        buildConfigField(
            "String",
            "CLOUDINARY_CLOUD_NAME",
            "\"${secret("cloudinaryCloudName", "SPROUTLY_CLOUDINARY_CLOUD_NAME") ?: "wjoegjfk"}\"",
        )
        // The unsigned preset always had a fallback, so the signed path could never
        // be reached and no build ever said which mode it was in. The mode is now
        // explicit and reported by the app; the fallback stays so an existing
        // deployment keeps uploading.
        buildConfigField(
            "String",
            "CLOUDINARY_UPLOAD_PRESET",
            "\"${secret("cloudinaryUploadPreset", "SPROUTLY_CLOUDINARY_UPLOAD_PRESET") ?: "sproutly_unsigned"}\"",
        )
        buildConfigField(
            "String",
            "CLOUDINARY_MODE",
            "\"${if (secret("cloudinaryUploadPreset", "SPROUTLY_CLOUDINARY_UPLOAD_PRESET") != null) "unsigned" else "unsigned-default"}\"",
        )

        // Where builds are uploaded: the Sproutly R2 uploader Worker. Public URL,
        // no secret — the Worker authenticates the caller by Firebase ID token.
        // AdMob. The defaults are Google's official test ids, so the banner works
        // before you have an account; replace them with yours to serve real ads.
        buildConfigField(
            "String",
            "ADMOB_BANNER_UNIT",
            "\"${secret("admobBannerUnit", "SPROUTLY_ADMOB_BANNER_UNIT") ?: "ca-app-pub-3940256099942544/6300978111"}\"",
        )
        manifestPlaceholders["admobAppId"] =
            secret("admobAppId", "SPROUTLY_ADMOB_APP_ID")
                ?: "ca-app-pub-3940256099942544~3347511713"

        buildConfigField(
            "String",
            "APK_UPLOAD_URL",
            "\"${secret("apkUploadUrl", "SPROUTLY_APK_UPLOAD_URL") ?: ""}\"",
        )

        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${secret("googleWebClientId", "SPROUTLY_GOOGLE_WEB_CLIENT_ID") ?: "919591212641-haoe16l24oej5pbvrdmc5csceumduv5l.apps.googleusercontent.com"}\"",
        )
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        create("release") {
            val storePath = secret("storeFile", "SPROUTLY_KEYSTORE_PATH")
            if (storePath != null && file(storePath).exists()) {
                storeFile = file(storePath)
                storePassword = secret("storePassword", "SPROUTLY_KEYSTORE_PASSWORD")
                keyAlias = secret("keyAlias", "SPROUTLY_KEY_ALIAS")
                keyPassword = secret("keyPassword", "SPROUTLY_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Same applicationId as release: the Firebase project only has one
            // registered Android app (app.tab.store), and a ".debug" suffix
            // would need a second registration in the console.
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // A release build must never be signed with the debug key: that APK
            // cannot be updated over an installed one, and it used to happen
            // silently when the keystore was missing.
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // ABI filters live on the flavours, not on defaultConfig: a filter set in
    // defaultConfig is merged into every variant and would leak arm64-v8a into
    // the arm32 APK.
    flavorDimensions += "abi"
    productFlavors {
        create("universal") {
            dimension = "abi"
            ndk { abiFilters += listOf("armeabi-v7a", "arm64-v8a") }
        }
        create("arm32") {
            dimension = "abi"
            ndk { abiFilters += listOf("armeabi-v7a") }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/DEPENDENCIES",
        )
    }

    lint {
        abortOnError = false
        warningsAsErrors = false
        checkReleaseBuilds = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.navigation.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)
    implementation(libs.firebase.messaging)
    implementation(libs.play.services.ads)
    // Shizuku: install builds without the system prompt on devices that grant it.
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // The payload test reflects over the model to catch a forgotten field.
    testImplementation(kotlin("reflect"))
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
