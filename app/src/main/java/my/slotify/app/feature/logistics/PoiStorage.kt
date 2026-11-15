package my.slotify.app.feature.logistics

import android.content.Context
import my.slotify.app.core.profile.FamilyProfileData
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.math.abs
import androidx.core.content.edit

object PoiStorage {
    private const val PREFS_NAME = "slotify_logistics_pois"
    private const val KEY_POIS_JSON = "pois_json_v2"
    private const val OLD_KEY_POIS_JSON = "pois_json_v1"

    val defaultPois: List<PoiItem>
        get() = FamilyProfileData.defaultPois

    fun loadPois(context: Context): List<PoiItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_POIS_JSON, null)
            ?: prefs.getString(OLD_KEY_POIS_JSON, null)
            ?: return defaultPois
        return try {
            val arr = JSONArray(json)
            val result = mutableListOf<PoiItem>()
            var corrected = false
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val rawLat = obj.getDouble("lat")
                val rawLng = obj.getDouble("lng")
                val poiId = obj.optString("id", UUID.randomUUID().toString())

                // Profile-supplied fixes for stale coordinates written by older builds.
                val fix = FamilyProfileData.legacyPoiCorrections.firstOrNull { c ->
                    c.poiId == poiId && c.staleLatitudes.any { abs(rawLat - it) < 0.001 }
                }
                if (fix != null) corrected = true
                val migratedLat = fix?.correctedLat ?: rawLat
                val migratedLng = fix?.correctedLng ?: rawLng

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
            if (result.isEmpty()) {
                defaultPois
            } else {
                // Persist migrations so they are applied once and cannot clobber later edits.
                if (corrected || !prefs.contains(KEY_POIS_JSON)) savePois(context, result)
                result
            }
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
        prefs.edit { putString(KEY_POIS_JSON, arr.toString()) }
    }

    fun resetToDefaults(context: Context): List<PoiItem> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { remove(KEY_POIS_JSON); remove(OLD_KEY_POIS_JSON) }
        return defaultPois
    }
}
