package my.slotify.app.core.profile

import my.slotify.app.core.database.ExamEntity
import my.slotify.app.core.database.HomeworkTaskEntity
import my.slotify.app.core.database.LocationNodeEntity
import my.slotify.app.core.database.PbdAssessmentEntity
import my.slotify.app.core.database.SchoolSessionEntity
import my.slotify.app.core.database.StudentEntity
import my.slotify.app.core.database.TimetableSlotEntity
import my.slotify.app.feature.dashboard.BagItem
import my.slotify.app.feature.logistics.DriverContact
import my.slotify.app.feature.logistics.PoiItem
import my.slotify.app.feature.logistics.RouteSelection

/**
 * Everything that is specific to one family: children, schools, routines, places and contacts.
 *
 * The implementation, `FamilyProfileData`, is compiled from exactly one of:
 *  - `app/family-profile/private/` (git-ignored, the real family), or
 *  - `app/family-profile/demo/` (committed, fictional).
 *
 * App code must read family data only through [FamilyProfileData] — never hard-code it.
 * IDs (students, sessions, POIs) must stay stable across releases because existing
 * installs reference them.
 */
interface FamilyProfile {
    /** Short label for diagnostics, e.g. "demo". */
    val profileName: String

    /** Bump to re-run seeding for rows that do not exist yet (existing rows are never overwritten). */
    val seedVersion: Int

    // ── Seed data (inserted with INSERT OR IGNORE) ────────────────────────────
    val students: List<StudentEntity>
    val sessions: List<SchoolSessionEntity>
    val slots: List<TimetableSlotEntity>
    val icsImports: List<IcsImport>
    val locationNodes: List<LocationNodeEntity>
    val homework: List<HomeworkTaskEntity>
    val exams: List<ExamEntity>
    val pbdAssessments: List<PbdAssessmentEntity>
    val feeTemplates: List<FeeTemplate>

    // ── Logistics ─────────────────────────────────────────────────────────────
    /** Default map points of interest. Users can edit/delete these; ids are referenced by [routes]. */
    val defaultPois: List<PoiItem>

    /** Map centre used when no home POI exists. */
    val mapCenter: GeoCoordinate

    /** Offline reverse-geocoding hints (nearest landmark wins). */
    val addressLandmarks: List<AddressLandmark>
    val routes: Map<RouteSelection, RouteProfile>

    // ── Routines ──────────────────────────────────────────────────────────────
    /** studentId → ordered routine zones used when GPS is unavailable (school days only). */
    val routineZones: Map<String, List<RoutineZone>>

    /** studentId → extra non-textbook bag items. */
    val bagEssentials: Map<String, List<BagItem>>
    val defaultBagEssentials: List<BagItem>

    /** Body of the 06:30 transit-bag reminder; null disables it. */
    val morningTransitReminder: String?

    /**
     * Corrections for stale coordinates saved by older builds: a stored POI with [LegacyPoiCorrection.poiId]
     * whose latitude is within ~100 m of a stale value is moved to the corrected position.
     */
    val legacyPoiCorrections: List<LegacyPoiCorrection> get() = emptyList()

    /** Location used for prayer-time calculation (defaults to [mapCenter]). */
    val prayerLocation: GeoCoordinate get() = mapCenter

    /** Human-readable prayer zone shown on the dashboard. */
    val prayerZoneLabel: String get() = "Zon Solat Tempatan"
}

data class LegacyPoiCorrection(
    val poiId: String,
    val staleLatitudes: List<Double>,
    val correctedLat: Double,
    val correctedLng: Double
)

/** Parse [assetPath] (under `assets/`) as ICS and import its weekly slots into [sessionId]. */
data class IcsImport(val sessionId: String, val assetPath: String)

/** A fee created for the current month on first launch. */
data class FeeTemplate(
    val seriesId: String,
    val title: String,
    val childTarget: String,
    val amountRm: Double,
    val dueDateLabel: String,
    val categoryEmoji: String,
    val isRecurring: Boolean
)

data class GeoCoordinate(val lat: Double, val lng: Double)

data class AddressLandmark(val lat: Double, val lng: Double, val address: String)

data class RouteProfile(
    /** Route tab title, e.g. "Laluan Ibu Bapa". */
    val tabTitle: String,
    /** Vehicle label used in HUD / notifications, e.g. "Van Sekolah A". */
    val vehicleLabel: String,
    val vehicleEmoji: String,
    /** Who rides this route, shown under the tab title when no ETA is known. */
    val passengerLabel: String,
    /** Ordered POI ids from [FamilyProfile.defaultPois] forming the route. */
    val waypointPoiIds: List<String>,
    val driver: DriverContact?,
    /** One step per leg between consecutive waypoints. */
    val steps: List<RouteStep>
)

data class RouteStep(val title: String, val note: String)

/**
 * A routine zone lasts until [untilMinuteOfDay] (exclusive). The last zone of a list
 * applies for the rest of the day.
 */
data class RoutineZone(val untilMinuteOfDay: Int, val label: String, val shortLabel: String)

/** Resolves the routine zone for [minuteOfDay], or null when the student has no routine. */
fun List<RoutineZone>.zoneAt(minuteOfDay: Int): RoutineZone? =
    firstOrNull { minuteOfDay < it.untilMinuteOfDay } ?: lastOrNull()
