package my.slotify.app.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies MIGRATION_5_6 against the exported schemas in app/schemas (wired as androidTest assets).
 * Requires a device or emulator: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class SlotifyMigrationTest {

    private val dbName = "migration-test.db"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        SlotifyDatabase::class.java
    )

    @Test
    fun migrate5To6_keepsData_andDerivesFeeSeries() {
        helper.createDatabase(dbName, 5).apply {
            execSQL(
                "INSERT INTO students (id, fullName, nickname, currentLevel, avatarEmoji, primarySchoolName, " +
                        "secondarySchoolName, sessionType, academicYearShift, vanDriverName, vanDriverPhone, starCoinsBalance, " +
                        "dailyCanteenBudget, myKid, heightCm, weightKg, age, className) VALUES " +
                        "('s1','A','A','D4','👦','SK',NULL,'DUAL_SRA_SK','Y4',NULL,NULL,42,5.0,'',0,0,10,'4A')"
            )
            // Seeded-style id "<series>_<month>" with a monthly label, and a one-off UUID-style fee
            execSQL(
                "INSERT INTO commitment_fees (id, title, childTarget, amountRm, dueDateLabel, billingMonth, isPaid, categoryEmoji) " +
                        "VALUES ('van_a_2026-09','Van','A',80.0,'1hb Setiap Bulan','2026-09',1,'🚌')"
            )
            execSQL(
                "INSERT INTO commitment_fees (id, title, childTarget, amountRm, dueDateLabel, billingMonth, isPaid, categoryEmoji) " +
                        "VALUES ('3f2a-uuid','PIBG','A',10.0,'15hb September','2026-09',0,'🏫')"
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 6, true, SlotifyMigrations.MIGRATION_5_6)

        db.query("SELECT starCoinsBalance FROM students WHERE id = 's1'").use {
            it.moveToFirst(); assertEquals(42, it.getInt(0))
        }
        db.query("SELECT seriesId, isRecurring FROM commitment_fees WHERE id = 'van_a_2026-09'").use {
            it.moveToFirst()
            assertEquals("van_a", it.getString(0))
            assertEquals(1, it.getInt(1))
        }
        db.query("SELECT seriesId, isRecurring FROM commitment_fees WHERE id = '3f2a-uuid'").use {
            it.moveToFirst()
            assertEquals("3f2a-uuid", it.getString(0))
            assertEquals(0, it.getInt(1))
        }
        db.query("SELECT COUNT(*) FROM reward_claims").use {
            it.moveToFirst(); assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun migrate6To7_convertsMoneyToSen_withoutLosingChildRows() {
        helper.createDatabase(dbName, 6).apply {
            execSQL(
                "INSERT INTO students (id, fullName, nickname, currentLevel, avatarEmoji, primarySchoolName, " +
                        "secondarySchoolName, sessionType, academicYearShift, vanDriverName, vanDriverPhone, starCoinsBalance, " +
                        "dailyCanteenBudget, myKid, heightCm, weightKg, age, className) VALUES " +
                        "('s1','A','A','D4','👦','SK',NULL,'DUAL_SRA_SK','Y4',NULL,NULL,42,4.5,'',0,0,10,'4A')"
            )
            execSQL(
                "INSERT INTO school_sessions (id, studentId, schoolName, className, sessionTimeType, startTime, endTime, fridayEndTime) " +
                        "VALUES ('sess1','s1','SK','4A','PETANG','13:00','18:00','11:30')"
            )
            execSQL(
                "INSERT INTO timetable_slots (id, sessionId, dayOfWeek, periodIndex, startTime, endTime, subjectCode, subjectName, " +
                        "teacherName, isRehat, requiresTextbook, requiresWorkbook) VALUES ('slot1','sess1',1,1,'13:00','14:00','BM','BM',NULL,0,1,1)"
            )
            execSQL(
                "INSERT INTO commitment_fees (id, title, childTarget, amountRm, dueDateLabel, billingMonth, isPaid, categoryEmoji, seriesId, isRecurring) " +
                        "VALUES ('van_2026-11','Van','A',80.1,'1hb Setiap Bulan','2026-11',0,'🚌','van',1)"
            )
            execSQL("INSERT INTO daily_spending (studentId, dateKey, spentAmountRm) VALUES ('s1','2026-11-14',3.2)")
            close()
        }

        val db = helper.runMigrationsAndValidate(dbName, 7, true, SlotifyMigrations.MIGRATION_6_7)

        db.query("SELECT dailyCanteenBudgetSen, starCoinsBalance FROM students WHERE id = 's1'").use {
            it.moveToFirst(); assertEquals(450L, it.getLong(0)); assertEquals(42, it.getInt(1))
        }
        // Rebuilding the parent table must not cascade-delete its children.
        db.query("SELECT COUNT(*) FROM school_sessions").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        db.query("SELECT COUNT(*) FROM timetable_slots").use { it.moveToFirst(); assertEquals(1, it.getInt(0)) }
        db.query("SELECT amountSen, seriesId FROM commitment_fees").use {
            it.moveToFirst(); assertEquals(8010L, it.getLong(0)); assertEquals("van", it.getString(1))
        }
        db.query("SELECT spentSen FROM daily_spending").use { it.moveToFirst(); assertEquals(320L, it.getLong(0)) }
    }
}
