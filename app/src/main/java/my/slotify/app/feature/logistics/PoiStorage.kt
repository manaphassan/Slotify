package my.slotify.app.feature.logistics

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object PoiStorage {
    private const val PREFS_NAME = "slotify_logistics_pois"
    private const val KEY_POIS_JSON = "pois_json_v2"
    private const val OLD_KEY_POIS_JSON = "pois_json_v1"

    val defaultPois: List<PoiItem> = listOf(
        PoiItem("poi-1", "Rumah Kediaman", PoiCategory.RUMAH, "🏠", 3.1390, 101.6869, 0.0, "Taman Contoh, 00000 Bandar Contoh"),
        PoiItem("poi-2", "SK Taman Contoh (SK)", PoiCategory.SEKOLAH, "🏫", 3.1390, 101.6869, 2.4, "Jalan Contoh 1, 00000 Bandar Contoh"),
        PoiItem("poi-3", "SRA Taman Contoh (SRA)", PoiCategory.SEKOLAH, "🕌", 3.1390, 101.6869, 2.5, "Jalan Contoh 2, Taman Contoh"),
        PoiItem("poi-4", "Taska Ceria (Transit)", PoiCategory.TRANSIT, "🎒", 3.1390, 101.6869, 0.6, "Taman Contoh, Bandar Contoh"),
        PoiItem("poi-5", "Tadika Ceria (Danial)", PoiCategory.TRANSIT, "🧒", 3.1390, 101.6869, 1.9, "Taman Contoh, Bandar Contoh"),
        PoiItem("poi-6", "SRAI Bukit Contoh (SRAI)", PoiCategory.SEKOLAH, "📚", 3.1390, 101.6869, 4.2, "Jalan Contoh 3, Bukit Contoh, Bandar Contoh"),
        PoiItem("poi-7", "Pejabat", PoiCategory.PEJABAT, "🏢", 3.1390, 101.6869, 7.5, "Pusat Bandar Contoh, Bandar Contoh")
    )

    fun loadPois(context: Context): List<PoiItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_POIS_JSON, null)
            ?: prefs.getString(OLD_KEY_POIS_JSON, null)
            ?: return defaultPois
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<PoiItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val rawLat = obj.getDouble("lat")
                val rawLng = obj.getDouble("lng")
                val poiId = obj.optString("id", UUID.randomUUID().toString())

                // Auto-migrate old inaccurate mock coordinates (e.g. 3.1390 or 3.1390) to verified coordinates
                val (migratedLat, migratedLng) = when {
                    poiId == "poi-1" && (kotlin.math.abs(rawLat - 3.1390) < 0.001 || kotlin.math.abs(rawLat - 3.1390) < 0.001) -> 3.1390 to 101.6869
                    poiId == "poi-2" && kotlin.math.abs(rawLat - 3.1390) < 0.001 -> 3.1390 to 101.6869
                    poiId == "poi-3" && kotlin.math.abs(rawLat - 3.1390) < 0.001 -> 3.1390 to 101.6869
                    poiId == "poi-4" && (kotlin.math.abs(rawLat - 3.1390) < 0.001 || kotlin.math.abs(rawLat - 3.1390) < 0.001) -> 3.1390 to 101.6869
                    poiId == "poi-5" && (kotlin.math.abs(rawLat - 3.1390) < 0.001 || kotlin.math.abs(rawLat - 3.1390) < 0.001) -> 3.1390 to 101.6869
                    poiId == "poi-6" && kotlin.math.abs(rawLat - 3.1390) < 0.001 -> 3.1390 to 101.6869
                    poiId == "poi-7" && kotlin.math.abs(rawLat - 3.1390) < 0.001 -> 3.1390 to 101.6869
                    else -> rawLat to rawLng
                }

                result.add(
                    PoiItem(
                        id = poiId,
                        name = obj.getString("name"),
                        category = try {
                            PoiCategory.valueOf(obj.getString("category"))
                        } catch (e: Exception) {
                            PoiCategory.ALL
                        },
                        iconEmoji = obj.optString("iconEmoji", "📍"),
                        lat = migratedLat,
                        lng = migratedLng,
                        distanceKm = if (poiId == "poi-1") 0.0 else obj.optDouble("distanceKm", 0.0),
                        address = obj.optString("address", "")
                    )
                )
            }
            if (result.isEmpty()) defaultPois else result
        } catch (e: Exception) {
            defaultPois
        }
    }

    fun savePois(context: Context, pois: List<PoiItem>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        pois.forEach { poi ->
            val obj = JSONObject().apply {
                put("id", poi.id)
                put("name", poi.name)
                put("category", poi.category.name)
                put("iconEmoji", poi.iconEmoji)
                put("lat", poi.lat)
                put("lng", poi.lng)
                put("distanceKm", poi.distanceKm)
                put("address", poi.address)
            }
            arr.put(obj)
        }
        prefs.edit().putString(KEY_POIS_JSON, arr.toString()).apply()
    }

    fun resetToDefaults(context: Context): List<PoiItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_POIS_JSON).apply()
        return defaultPois
    }
}
