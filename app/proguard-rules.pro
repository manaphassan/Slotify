# Slotify — Production ProGuard / R8 Rules

# ── AndroidX Room Database ───────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.RoomDatabase$Callback { *; }
-dontwarn androidx.room.paging.**

# Keep all database entities
-keep class my.slotify.app.core.database.** { *; }

# ── Kotlin Coroutines & Flow ────────────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ── OSMDroid (OpenStreetMap) ────────────────────────────────────────────────
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
-keepattributes *Annotation*

# ── OkHttp3 & Networking ────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keepclassmembers class okhttp3.internal.publicsuffix.PublicSuffixDatabase {
    private static final byte[] WILDCARD_LABEL;
    private static final byte[][] EMPTY_RULE;
}
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# ── Jetpack Compose ─────────────────────────────────────────────────────────
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }

# ── Slotify App Models & Domain Objects ──────────────────────────────────────
-keep class my.slotify.app.core.model.** { *; }
-keep class my.slotify.app.core.takwim.** { *; }
-keep class my.slotify.app.core.routing.** { *; }
