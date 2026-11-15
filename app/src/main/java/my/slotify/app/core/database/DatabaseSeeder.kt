package my.slotify.app.core.database

import android.content.Context
import android.util.Log
import my.slotify.app.core.finance.FeeRepository
import my.slotify.app.core.profile.FamilyProfile
import my.slotify.app.core.profile.FamilyProfileData
import androidx.core.content.edit

/**
 * Seeds the database from the active [FamilyProfile].
 *
 * - Runs once per [FamilyProfile.seedVersion] (tracked in SharedPreferences), so rows the
 *   user deleted are not resurrected on every launch.
 * - Uses INSERT OR IGNORE everywhere, so it never overwrites user edits (e.g. completed
 *   homework, paid fees) and never triggers cascade deletes.
 */
object DatabaseSeeder {
    private const val TAG = "DatabaseSeeder"
    private const val PREFS = "slotify_seed"
    private const val KEY_VERSION = "seeded_version"
    private const val KEY_PROFILE = "seeded_profile"

    suspend fun seedIfNeeded(context: Context, dao: SlotifyDao, profile: FamilyProfile = FamilyProfileData) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val alreadySeeded = prefs.getInt(KEY_VERSION, 0) >= profile.seedVersion &&
                prefs.getString(KEY_PROFILE, null) == profile.profileName
        if (alreadySeeded) return

        seed(context, dao, profile)
        prefs.edit {
            putInt(KEY_VERSION, profile.seedVersion)
            putString(KEY_PROFILE, profile.profileName)
        }
    }

    suspend fun seed(context: Context?, dao: SlotifyDao, profile: FamilyProfile) {
        dao.seedStudents(profile.students)
        dao.seedSessions(profile.sessions)

        // Installs from v1.x stored slots/locations with random UUIDs, so INSERT OR IGNORE
        // alone would duplicate them. Only seed sessions/students that have none yet.
        profile.slots.groupBy { it.sessionId }.forEach { (sessionId, sessionSlots) ->
            if (dao.countSlotsForSession(sessionId) == 0) dao.seedSlots(sessionSlots)
        }

        if (context != null) {
            for (import in profile.icsImports) {
                // Only import into a session that has no slots yet, so user edits survive.
                if (dao.countSlotsForSession(import.sessionId) > 0) continue
                try {
                    context.assets.open(import.assetPath).use { stream ->
                        dao.seedSlots(IcsParser.parse(stream, import.sessionId))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "ICS import failed for ${import.assetPath}", e)
                }
            }
        }

        profile.locationNodes.groupBy { it.studentId }.forEach { (studentId, nodes) ->
            if (dao.getLocationNodesOnce(studentId).isEmpty()) dao.seedLocations(nodes)
        }
        dao.seedHomework(profile.homework)
        dao.seedExams(profile.exams)
        dao.seedAssessments(profile.pbdAssessments)

        // Fee templates only seed a brand-new install; afterwards FeeRepository rolls months over.
        val month = FeeRepository.monthKey()
        if (dao.countFeesForMonth(month) == 0 && dao.latestFeeMonthBefore(month) == null) {
            dao.seedFees(profile.feeTemplates.map { FeeRepository.fromTemplate(it, month) })
        }
    }
}
