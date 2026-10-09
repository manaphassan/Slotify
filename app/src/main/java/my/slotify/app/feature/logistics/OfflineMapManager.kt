package my.slotify.app.feature.logistics

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.tileprovider.cachemanager.CacheManager
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView

/**
 * Offline Map Cache Manager for zero-data high-performance tile rendering
 * Targets the Daerah Contoh & Bandar Contoh corridor (Taman Contoh, SRAI Bukit Contoh, Pekan Contoh)
 */
object OfflineMapManager {

    private const val PREFS_NAME = "slotify_offline_map_prefs"
    private const val KEY_CACHE_READY = "region_tiles_cached"
    private const val KEY_LAST_CACHED_TS = "region_tiles_timestamp"

    // Ground truth bounding box for Daerah Contoh school & home corridors
    val REGION_BOUNDING_BOX = BoundingBox(
        3.1390, // North: Taman Contoh & Kawasan Contoh border
        101.6200, // East: Taman Contoh / Nilai bypass
        3.1390, // South: Pekan Contoh / Kawasan Contoh junction
        101.6869 // West: Kawasan Contoh & Bukit Contoh
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isOfflineCacheReady(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_CACHE_READY, false)
    }

    fun setOfflineCacheReady(context: Context, ready: Boolean) {
        getPrefs(context).edit()
            .putBoolean(KEY_CACHE_READY, ready)
            .putLong(KEY_LAST_CACHED_TS, System.currentTimeMillis())
            .apply()
    }

    /**
     * Estimates number of tiles for zoom levels 12 to 16 within the bounding box
     */
    fun estimateTileCount(minZoom: Int = 12, maxZoom: Int = 16): Int {
        var count = 0
        for (z in minZoom..maxZoom) {
            // Rough projection calculation for bounding box tiles
            val tilesAtZoom = when (z) {
                12 -> 4
                13 -> 12
                14 -> 35
                15 -> 95
                16 -> 240
                else -> 50
            }
            count += tilesAtZoom
        }
        return count
    }

    /**
     * Pre-downloads Mapnik tiles asynchronously into osmdroid's local SQLite/file cache
     */
    fun downloadFamilyCorridorTiles(
        context: Context,
        mapView: MapView,
        minZoom: Int = 12,
        maxZoom: Int = 16,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
        onComplete: (totalTiles: Int) -> Unit = {},
        onError: (errorMsg: String) -> Unit = {}
    ) {
        try {
            val cacheManager = CacheManager(mapView)
            val estimatedTiles = estimateTileCount(minZoom, maxZoom)

            cacheManager.downloadAreaAsync(
                context,
                REGION_BOUNDING_BOX,
                minZoom,
                maxZoom,
                object : CacheManager.CacheManagerCallback {
                    override fun onTaskComplete() {
                        setOfflineCacheReady(context, true)
                        onComplete(estimatedTiles)
                    }

                    override fun onTaskFailed(errors: Int) {
                        onError("Gagal memuat turun sesetengah jubin peta ($errors ralat)")
                    }

                    override fun updateProgress(progress: Int, current: Int, total: Int, errors: Int) {
                        onProgress(current, total)
                    }

                    override fun downloadStarted() {}

                    override fun setPossibleTilesInArea(total: Int) {}
                }
            )
        } catch (e: Exception) {
            onError(e.message ?: "Ralat memulakan muat turun peta")
        }
    }
}
