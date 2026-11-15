package my.slotify.app.feature.logistics

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import my.slotify.app.core.data.currentAppStrings

/**
 * Periodic background geofence check.
 *
 * On Android 10+ a background worker receives no location fixes without
 * ACCESS_BACKGROUND_LOCATION, so the worker is a no-op until the user grants it.
 */
class GeofenceProximityWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (!GpsLocationHelper.hasLocationPermission(ctx) ||
            !hasBackgroundLocationPermission(ctx) ||
            !GpsLocationHelper.isLocationServiceEnabled(ctx)
        ) {
            return Result.success()
        }

        return try {
            val fix = GpsLocationHelper.getAccurateLocation(ctx, timeoutMs = 5000L)
            if (fix != null) {
                GeofenceManager.evaluateProximity(
                    context = ctx,
                    currentLat = fix.latitude,
                    currentLng = fix.longitude,
                    alertStrings = ctx.currentAppStrings().systemUi
                )
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    private fun hasBackgroundLocationPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
}
