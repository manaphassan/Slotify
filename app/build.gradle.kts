import java.util.Properties

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    // No org.jetbrains.kotlin.android: AGP 9 provides built-in Kotlin compilation.
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

// Family-specific seed data lives in a git-ignored directory. Fresh clones and CI
// fall back to the committed, fictional demo profile. See MASTER_DOCUMENTATION §11.
val privateProfileDir = file("family-profile/private")
// `-PslotifyProfile=demo` forces the demo profile (e.g. to verify it builds/tests locally).
val forceDemoProfile = (findProperty("slotifyProfile") as String?) == "demo"
val familyProfileDir =
    if (privateProfileDir.exists() && !forceDemoProfile) privateProfileDir else file("family-profile/demo")

// Release signing credentials: keystore.properties (git-ignored) or environment variables.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun signingValue(propKey: String, envKey: String): String? =
    keystoreProps.getProperty(propKey) ?: System.getenv(envKey)

val hasReleaseSigning = listOf(
    "storeFile" to "SLOTIFY_KEYSTORE_FILE",
    "storePassword" to "SLOTIFY_KEYSTORE_PASSWORD",
    "keyAlias" to "SLOTIFY_KEY_ALIAS",
    "keyPassword" to "SLOTIFY_KEY_PASSWORD"
).all { (prop, env) -> signingValue(prop, env) != null }

android {
    namespace = "my.slotify.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "my.slotify.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    sourceSets {
        getByName("main") {
            // AGP 9 built-in Kotlin only compiles .kt files from kotlin source dirs.
            kotlin.srcDir(familyProfileDir.resolve("java"))
            assets.srcDir(familyProfileDir.resolve("assets"))
        }
        getByName("androidTest") {
            assets.srcDir(file("schemas"))
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(signingValue("storeFile", "SLOTIFY_KEYSTORE_FILE")!!)
                storePassword = signingValue("storePassword", "SLOTIFY_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "SLOTIFY_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "SLOTIFY_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Without credentials the release build is left unsigned (app-release-unsigned.apk):
            // nothing publishable is produced by accident, and CI needs no keystore.
            signingConfig = if (hasReleaseSigning) signingConfigs.getByName("release") else null
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // UI copy is Bahasa Melayu; the English "Typos" checker flags valid Malay words.
        disable += "Typos"
        warningsAsErrors = false
        abortOnError = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // AndroidX Core & Lifecycle
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.activity:activity-compose:1.13.0")

    // Jetpack Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.10.2")

    // Room Database
    val roomVersion = "2.8.5"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.12.0")

    // Persisted settings (theme, language, selected student)
    implementation("androidx.datastore:datastore-preferences:1.3.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")

    // JSON & Networking for OSRM Routing (org.json ships with Android; the artifact is only
    // needed so JVM unit tests get a real implementation instead of the android.jar stubs)
    implementation("com.squareup.okhttp3:okhttp:5.5.1")
    testImplementation("org.json:json:20260814")

    // OpenStreetMap (osmdroid) for zero-cost offline/online map rendering
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")

    constraints {
        // room-testing 2.8 (schema JSON) needs serialization 1.8+, but Navigation pulls 1.7.3 into the
        // app and AGP aligns the test APK to the app's versions, mixing core 1.7.3 with json 1.8.1.
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    }
}
