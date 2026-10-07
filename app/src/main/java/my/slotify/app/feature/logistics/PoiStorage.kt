package my.slotify.app.feature.logistics

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object PoiStorage {
    private const val PREFS_NAME = "slotify_logistics_pois"
    private const val KEY_POIS_JSON = "pois_json_v1"

    val defaultPois: List<PoiItem> = listOf(
        PoiItem("poi-1", "Rumah Kediaman", PoiCategory.RUMAH, "🏠", 3.1390, 101.6869, 0.0, "Taman Contoh"),
        PoiItem("poi-2", "SK Taman Contoh (SK)", PoiCategory.SEKOLAH, "🏫", 3.1390, 101.6869, 2.4, "Jalan Contoh 1"),
        PoiItem("poi-3", "SRA Taman Contoh (SRA)", PoiCategory.SEKOLAH, "🕌", 3.1390, 101.6869, 1.8, "Jalan Contoh 2"),
        PoiItem("poi-4", "Taska Ceria (Transit)", PoiCategory.TRANSIT, "🎒", 3.1390, 101.6869, 1.2, "Taman Contoh"),
        PoiItem("poi-5", "Tadika Ceria (Danial)", PoiCategory.TRANSIT, "🧒", 3.1390, 101.6869, 2.0, "Taman Contoh"),
        PoiItem("poi-6", "SRAI Bukit Contoh (SRAI)", PoiCategory.SEKOLAH, "📚", 3.1390, 101.6869, 4.2, "Bukit Contoh, Bandar Contoh"),
        PoiItem("poi-7", "Pejabat Abah (Bandar Contoh)", PoiCategory.PEJABAT, "🏢", 3.1390, 101.6869, 7.5, "Pusat Bandar Contoh, Bandar Contoh")
    )

    fun loadPois(context: Context): List<PoiItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_POIS_JSON, null) ?: return defaultPois
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<PoiItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    PoiItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.getString("name"),
                        category = try {
                            PoiCategory.valueOf(obj.getString("category"))
                        } catch (e: Exception) {
                            PoiCategory.ALL
                        },
                        iconEmoji = obj.optString("iconEmoji", "📍"),
                        lat = obj.getDouble("lat"),
                        lng = obj.getDouble("lng"),
                        distanceKm = obj.optDouble("distanceKm", 0.0),
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
