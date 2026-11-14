# Slotify — Production ProGuard / R8 Rules
#
# AndroidX (Room, Compose, WorkManager, Lifecycle), Kotlin coroutines, OkHttp and osmdroid
# ship their own consumer rules, so blanket `-keep class androidx.compose.**` style rules are
# unnecessary and only defeat R8 shrinking. Keep this file limited to app-specific needs.

# Room entities are mapped by generated code; keep field names stable for readable crash
# reports and schema export parity.
-keepclassmembers @androidx.room.Entity class my.slotify.app.core.database.** { <fields>; }

# Widget/alarm/boot receivers and workers are instantiated reflectively from the manifest /
# WorkManager by class name (AGP keeps manifest components; workers need their constructor).
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Keep source/line info for useful stack traces; strip original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-dontwarn org.osmdroid.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
