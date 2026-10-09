package my.slotify.app.feature.logistics

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Android WorkManager CoroutineWorker for checking geofence proximity in the background
 */
class GeofenceProximityWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!GpsLocationHelper.hasLocationPermission(applicationContext) ||
            !GpsLocationHelper.isLocationServiceEnabled(applicationContext)
        ) {
            return Result.success()
        }

        return try {
            val fix = GpsLocationHelper.getAccurateLocation(applicationContext, timeoutMs = 5000L)
            if (fix != null) {
                GeofenceManager.evaluateProximity(
                    context = applicationContext,
                    currentLat = fix.latitude,
                    currentLng = fix.longitude
                )
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
