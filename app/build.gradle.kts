import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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
    compileSdk = 35

    defaultConfig {
        applicationId = "my.slotify.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDir(familyProfileDir.resolve("java"))
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

    kotlinOptions {
        jvmTarget = "17"
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

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // AndroidX Core & Lifecycle
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Jetpack Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.11.00")
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
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Persisted settings (theme, language, selected student)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // JSON & Networking for OSRM Routing (org.json ships with Android; the artifact is only
    // needed so JVM unit tests get a real implementation instead of the android.jar stubs)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation("org.json:json:20240303")

    // OpenStreetMap (osmdroid) for zero-cost offline/online map rendering
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
}
