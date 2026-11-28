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
        UploadQueueEntity::class,
        InAppNotificationEntity::class,
        ForwardOrganizerEntity::class,
        ForwardPaymentEntity::class
    ],
    version = 6,
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `members` ADD COLUMN `memberCode` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `members` ADD COLUMN `fatherName` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `members` ADD COLUMN `address` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `members` ADD COLUMN `joiningDate` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `members` ADD COLUMN `accessPin` TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE `installments` ADD COLUMN `receiptNumber` TEXT")
                db.execSQL("ALTER TABLE `payment_logs` ADD COLUMN `receiptNumber` TEXT")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `in_app_notifications` (
                        `id` TEXT NOT NULL,
                        `committeeId` TEXT NOT NULL,
                        `memberId` TEXT,
                        `memberName` TEXT,
                        `title` TEXT NOT NULL,
                        `message` TEXT NOT NULL,
                        `notificationType` TEXT NOT NULL,
                        `monthKey` TEXT NOT NULL,
                        `isRead` INTEGER NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_in_app_notifications_committeeId` ON `in_app_notifications` (`committeeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_in_app_notifications_memberId` ON `in_app_notifications` (`memberId`)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `forward_organizers` (
                        `id` TEXT NOT NULL,
                        `committeeId` TEXT NOT NULL,
                        `organizerName` TEXT NOT NULL,
                        `organizerPhone` TEXT NOT NULL,
                        `totalPayableAmount` INTEGER NOT NULL,
                        `monthKey` TEXT NOT NULL,
                        `notes` TEXT,
                        `status` TEXT NOT NULL,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`committeeId`) REFERENCES `committees`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_forward_organizers_committeeId` ON `forward_organizers` (`committeeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_forward_organizers_monthKey` ON `forward_organizers` (`monthKey`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `forward_payments` (
                        `id` TEXT NOT NULL,
                        `committeeId` TEXT NOT NULL,
                        `organizerId` TEXT NOT NULL,
                        `organizerName` TEXT NOT NULL,
                        `organizerPhone` TEXT NOT NULL,
                        `monthKey` TEXT NOT NULL,
                        `amountForwarded` INTEGER NOT NULL,
                        `paymentDate` TEXT NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `transactionId` TEXT,
                        `receiptNumber` TEXT,
                        `proofUri` TEXT,
                        `proofLabel` TEXT,
                        `notes` TEXT,
                        `confirmedByAdmin` INTEGER NOT NULL,
                        `isReversed` INTEGER NOT NULL,
                        `reversedReason` TEXT,
                        `createdAtMillis` INTEGER NOT NULL,
                        `updatedAtMillis` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`committeeId`) REFERENCES `committees`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`organizerId`) REFERENCES `forward_organizers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_forward_payments_committeeId` ON `forward_payments` (`committeeId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_forward_payments_organizerId` ON `forward_payments` (`organizerId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_forward_payments_monthKey` ON `forward_payments` (`monthKey`)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM `forward_payments`")
                db.execSQL("DELETE FROM `forward_organizers`")
                db.execSQL("DELETE FROM `payment_logs`")
                db.execSQL("DELETE FROM `reminder_logs`")
                db.execSQL("DELETE FROM `installments`")
                db.execSQL("DELETE FROM `committee_turns`")
                db.execSQL("DELETE FROM `members`")
                db.execSQL("DELETE FROM `committees`")
                db.execSQL("DELETE FROM `old_committee_records`")
                db.execSQL("DELETE FROM `in_app_notifications`")
                db.execSQL("DELETE FROM `audit_logs`")
                db.execSQL("DELETE FROM `upload_queue`")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "gm_hasmani_committee.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
