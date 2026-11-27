package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CommitteeEntity::class,
        MemberEntity::class,
        CommitteeTurnEntity::class,
        OldCommitteeRecordEntity::class,
        InstallmentEntity::class,
        PaymentLogEntity::class,
        ReminderLogEntity::class,
        LocalUserEntity::class,
        AuditLogEntity::class,
        UploadQueueEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun committeeDao(): CommitteeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `local_users` (
                        `id` TEXT NOT NULL,
                        `username` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `passwordHash` TEXT NOT NULL,
                        `role` TEXT NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `audit_logs` (
                        `id` TEXT NOT NULL,
                        `actorName` TEXT NOT NULL,
                        `actionType` TEXT NOT NULL,
                        `targetType` TEXT NOT NULL,
                        `targetName` TEXT NOT NULL,
                        `details` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `upload_queue` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `payloadType` TEXT NOT NULL,
                        `payloadJson` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `errorMessage` TEXT,
                        `attempts` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `committees` ADD COLUMN `totalShares` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `members` ADD COLUMN `shares` REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE `installments` ADD COLUMN `paymentMethod` TEXT NOT NULL DEFAULT 'CASH'")
                db.execSQL("ALTER TABLE `installments` ADD COLUMN `transactionId` TEXT")
                db.execSQL("ALTER TABLE `payment_logs` ADD COLUMN `paymentMethod` TEXT")
                db.execSQL("ALTER TABLE `payment_logs` ADD COLUMN `transactionId` TEXT")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `committee_turns` (
                        `id` TEXT NOT NULL,
                        `committeeId` TEXT NOT NULL,
                        `turnNumber` INTEGER NOT NULL,
                        `recipientMemberId` TEXT,
                        `recipientName` TEXT NOT NULL,
                        `recipientPhone` TEXT,
                        `payoutAmount` INTEGER NOT NULL,
                        `scheduledMonth` TEXT NOT NULL,
                        `payoutDate` TEXT,
                        `status` TEXT NOT NULL,
                        `confirmedByAdmin` INTEGER NOT NULL,
                        `notes` TEXT,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`committeeId`) REFERENCES `committees`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_committee_turns_committeeId` ON `committee_turns` (`committeeId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `old_committee_records` (
                        `id` TEXT NOT NULL,
                        `committeeNumber` TEXT NOT NULL,
                        `committeeName` TEXT NOT NULL,
                        `startDate` TEXT NOT NULL,
                        `endDate` TEXT NOT NULL,
                        `memberNames` TEXT NOT NULL,
                        `contactNumbers` TEXT NOT NULL,
                        `numberOfShares` TEXT NOT NULL,
                        `monthlyContributions` TEXT NOT NULL,
                        `totalCollection` TEXT NOT NULL,
                        `paymentHistory` TEXT NOT NULL,
                        `payoutRecipient` TEXT NOT NULL,
                        `payoutAmount` TEXT NOT NULL,
                        `payoutDate` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gm_hasmani_committee.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
