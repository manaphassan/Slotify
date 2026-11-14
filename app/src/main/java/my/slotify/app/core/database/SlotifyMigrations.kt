package my.slotify.app.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Hand-written migrations. Each must produce exactly the schema in
 * `app/schemas/my.slotify.app.core.database.SlotifyDatabase/<version>.json`.
 */
object SlotifyMigrations {

    /** v5 → v6: idempotent reward claims + recurring fee series. */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `reward_claims` (" +
                        "`claimKey` TEXT NOT NULL, `studentId` TEXT NOT NULL, `stars` INTEGER NOT NULL, " +
                        "`claimedAt` INTEGER NOT NULL, PRIMARY KEY(`claimKey`))"
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reward_claims_studentId` ON `reward_claims` (`studentId`)")

            db.execSQL("ALTER TABLE `commitment_fees` ADD COLUMN `seriesId` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `commitment_fees` ADD COLUMN `isRecurring` INTEGER NOT NULL DEFAULT 0")

            // Seeded ids look like "<series>_<yyyy-MM>"; user-added fees use a UUID id.
            db.execSQL(
                "UPDATE `commitment_fees` SET `seriesId` = CASE " +
                        "WHEN substr(`id`, -length(`billingMonth`) - 1) = '_' || `billingMonth` " +
                        "THEN substr(`id`, 1, length(`id`) - length(`billingMonth`) - 1) " +
                        "ELSE `id` END"
            )
            db.execSQL(
                "UPDATE `commitment_fees` SET `isRecurring` = 1 " +
                        "WHERE `dueDateLabel` LIKE '%Setiap Bulan%' OR `dueDateLabel` LIKE '%Hujung Bulan%'"
            )
        }
    }

    /**
     * v6 → v7: money columns become whole sen (`INTEGER`).
     *
     * SQLite on minSdk 26 cannot change a column type, so the three tables are rebuilt with the
     * documented create-copy-drop-rename procedure. Room runs migrations before enabling
     * foreign keys (and inside a transaction), so dropping `students` does not cascade. As a
     * safety net, child row counts are compared before/after and the migration throws — rolling
     * the transaction back with data intact — if anything would be lost.
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val guarded = listOf(
                "students", "school_sessions", "timetable_slots", "location_nodes",
                "homework_tasks", "exams", "pbd_assessments", "commitment_fees", "daily_spending", "reward_claims"
            )

            fun count(table: String): Long =
                db.query("SELECT COUNT(*) FROM `$table`").use { it.moveToFirst(); it.getLong(0) }

            val before = guarded.associateWith(::count)

            db.execSQL(
                "CREATE TABLE `students_new` (`id` TEXT NOT NULL, `fullName` TEXT NOT NULL, `nickname` TEXT NOT NULL, " +
                        "`currentLevel` TEXT NOT NULL, `avatarEmoji` TEXT NOT NULL, `primarySchoolName` TEXT NOT NULL, " +
                        "`secondarySchoolName` TEXT, `sessionType` TEXT NOT NULL, `academicYearShift` TEXT NOT NULL, " +
                        "`vanDriverName` TEXT, `vanDriverPhone` TEXT, `starCoinsBalance` INTEGER NOT NULL, " +
                        "`dailyCanteenBudgetSen` INTEGER NOT NULL, `myKid` TEXT NOT NULL, `heightCm` REAL NOT NULL, " +
                        "`weightKg` REAL NOT NULL, `age` INTEGER NOT NULL, `className` TEXT NOT NULL, PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "INSERT INTO `students_new` SELECT `id`, `fullName`, `nickname`, `currentLevel`, `avatarEmoji`, " +
                        "`primarySchoolName`, `secondarySchoolName`, `sessionType`, `academicYearShift`, `vanDriverName`, " +
                        "`vanDriverPhone`, `starCoinsBalance`, CAST(ROUND(`dailyCanteenBudget` * 100) AS INTEGER), `myKid`, " +
                        "`heightCm`, `weightKg`, `age`, `className` FROM `students`"
            )
            db.execSQL("DROP TABLE `students`")
            db.execSQL("ALTER TABLE `students_new` RENAME TO `students`")

            db.execSQL(
                "CREATE TABLE `commitment_fees_new` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `childTarget` TEXT NOT NULL, " +
                        "`amountSen` INTEGER NOT NULL, `dueDateLabel` TEXT NOT NULL, `billingMonth` TEXT NOT NULL, " +
                        "`isPaid` INTEGER NOT NULL, `categoryEmoji` TEXT NOT NULL, `seriesId` TEXT NOT NULL DEFAULT '', " +
                        "`isRecurring` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`id`))"
            )
            db.execSQL(
                "INSERT INTO `commitment_fees_new` SELECT `id`, `title`, `childTarget`, CAST(ROUND(`amountRm` * 100) AS INTEGER), " +
                        "`dueDateLabel`, `billingMonth`, `isPaid`, `categoryEmoji`, `seriesId`, `isRecurring` FROM `commitment_fees`"
            )
            db.execSQL("DROP TABLE `commitment_fees`")
            db.execSQL("ALTER TABLE `commitment_fees_new` RENAME TO `commitment_fees`")

            db.execSQL(
                "CREATE TABLE `daily_spending_new` (`studentId` TEXT NOT NULL, `dateKey` TEXT NOT NULL, " +
                        "`spentSen` INTEGER NOT NULL, PRIMARY KEY(`studentId`, `dateKey`))"
            )
            db.execSQL(
                "INSERT INTO `daily_spending_new` SELECT `studentId`, `dateKey`, CAST(ROUND(`spentAmountRm` * 100) AS INTEGER) " +
                        "FROM `daily_spending`"
            )
            db.execSQL("DROP TABLE `daily_spending`")
            db.execSQL("ALTER TABLE `daily_spending_new` RENAME TO `daily_spending`")

            val after = guarded.associateWith(::count)
            check(after == before) { "MIGRATION_6_7 aborted: row counts changed $before -> $after" }
            db.query("PRAGMA foreign_key_check").use { c ->
                check(c.count == 0) { "MIGRATION_6_7 aborted: foreign key violations" }
            }
        }
    }

    val ALL: Array<Migration> = arrayOf(MIGRATION_5_6, MIGRATION_6_7)
}
