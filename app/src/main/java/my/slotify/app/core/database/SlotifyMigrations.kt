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

    val ALL: Array<Migration> = arrayOf(MIGRATION_5_6)
}
