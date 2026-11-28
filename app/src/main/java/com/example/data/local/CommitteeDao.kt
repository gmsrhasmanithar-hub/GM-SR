package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CommitteeDao {

    @Query("SELECT * FROM committees ORDER BY createdAtMillis DESC")
    fun observeCommittees(): Flow<List<CommitteeEntity>>

    @Query("SELECT * FROM committees ORDER BY createdAtMillis DESC")
    suspend fun getAllCommitteesOnce(): List<CommitteeEntity>

    @Query("SELECT * FROM committees WHERE id = :committeeId LIMIT 1")
    suspend fun getCommitteeByIdOnce(committeeId: String): CommitteeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommittee(committee: CommitteeEntity)

    @Update
    suspend fun updateCommittee(committee: CommitteeEntity)

    @Query("UPDATE committees SET status = :status, updatedAtMillis = :updatedAtMillis WHERE id = :committeeId")
    suspend fun updateCommitteeStatus(committeeId: String, status: String, updatedAtMillis: Long)

    @Query("DELETE FROM committees WHERE id = :committeeId")
    suspend fun deleteCommitteeById(committeeId: String)

    // Members
    @Query("SELECT * FROM members WHERE committeeId = :committeeId ORDER BY committeeNumber ASC")
    fun observeMembers(committeeId: String): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members ORDER BY committeeNumber ASC")
    fun observeAllMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE committeeId = :committeeId ORDER BY committeeNumber ASC")
    suspend fun getMembersOnce(committeeId: String): List<MemberEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: MemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<MemberEntity>)

    @Update
    suspend fun updateMember(member: MemberEntity)

    @Query("UPDATE members SET status = :status, updatedAtMillis = :updatedAtMillis WHERE id = :memberId")
    suspend fun updateMemberStatus(memberId: String, status: String, updatedAtMillis: Long)

    @Query("DELETE FROM members WHERE id = :memberId")
    suspend fun deleteMemberById(memberId: String)

    @Query("SELECT COUNT(*) FROM members WHERE committeeId = :committeeId AND payoutStatus = 'PAID'")
    suspend fun countPaidPayoutsForCommittee(committeeId: String): Int

    // Committee Turns (Individual payout slots, independent of member count)
    @Query("SELECT * FROM committee_turns WHERE committeeId = :committeeId ORDER BY turnNumber ASC")
    fun observeTurns(committeeId: String): Flow<List<CommitteeTurnEntity>>

    @Query("SELECT * FROM committee_turns ORDER BY turnNumber ASC")
    fun observeAllTurns(): Flow<List<CommitteeTurnEntity>>

    @Query("SELECT * FROM committee_turns WHERE committeeId = :committeeId ORDER BY turnNumber ASC")
    suspend fun getTurnsOnce(committeeId: String): List<CommitteeTurnEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTurn(turn: CommitteeTurnEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTurns(turns: List<CommitteeTurnEntity>)

    @Update
    suspend fun updateTurn(turn: CommitteeTurnEntity)

    @Query("DELETE FROM committee_turns WHERE id = :turnId")
    suspend fun deleteTurnById(turnId: String)

    @Query("SELECT COUNT(*) FROM committee_turns WHERE committeeId = :committeeId AND status = 'PAID'")
    suspend fun countPaidTurnsForCommittee(committeeId: String): Int

    // Old Committee Records / History (Isolated historical archive)
    @Query("SELECT * FROM old_committee_records ORDER BY createdAtMillis DESC")
    fun observeOldRecords(): Flow<List<OldCommitteeRecordEntity>>

    @Query("SELECT * FROM old_committee_records ORDER BY createdAtMillis DESC")
    suspend fun getAllOldRecordsOnce(): List<OldCommitteeRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOldRecord(record: OldCommitteeRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOldRecords(records: List<OldCommitteeRecordEntity>)

    @Update
    suspend fun updateOldRecord(record: OldCommitteeRecordEntity)

    @Query("DELETE FROM old_committee_records WHERE id = :recordId")
    suspend fun deleteOldRecordById(recordId: String)

    // Installments / Payments
    @Query("SELECT * FROM installments WHERE committeeId = :committeeId ORDER BY monthIndex ASC, committeeNumber ASC")
    fun observeInstallments(committeeId: String): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments ORDER BY monthIndex ASC, committeeNumber ASC")
    fun observeAllInstallments(): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE committeeId = :committeeId ORDER BY monthIndex ASC, committeeNumber ASC")
    suspend fun getInstallmentsOnce(committeeId: String): List<InstallmentEntity>

    @Query("SELECT * FROM installments WHERE committeeId = :committeeId AND memberId = :memberId AND monthKey = :monthKey LIMIT 1")
    suspend fun findInstallmentOnce(committeeId: String, memberId: String, monthKey: String): InstallmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallment(installment: InstallmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(installments: List<InstallmentEntity>)

    @Update
    suspend fun updateInstallment(installment: InstallmentEntity)

    @Query("DELETE FROM installments WHERE id = :installmentId")
    suspend fun deleteInstallmentById(installmentId: String)

    @Query("DELETE FROM installments WHERE committeeId = :committeeId AND monthKey = :monthKey AND paidAmount = 0")
    suspend fun deleteUnpaidInstallmentsForMonth(committeeId: String, monthKey: String): Int

    @Query("SELECT COUNT(*) FROM installments WHERE committeeId = :committeeId AND paidAmount > 0")
    suspend fun countRecordedPaymentsForCommittee(committeeId: String): Int

    @Query("SELECT COUNT(*) FROM installments WHERE committeeId = :committeeId AND monthKey = :monthKey AND paidAmount > 0")
    suspend fun countRecordedPaymentsForMonth(committeeId: String, monthKey: String): Int

    @Query("SELECT COUNT(*) FROM installments WHERE memberId = :memberId AND paidAmount > 0")
    suspend fun countRecordedPaymentsForMember(memberId: String): Int

    @Query("DELETE FROM installments WHERE memberId = :memberId AND paidAmount = 0")
    suspend fun deleteUnpaidInstallmentsForMember(memberId: String)

    @Query("UPDATE installments SET memberName = :memberName, memberPhone = :memberPhone, committeeNumber = :committeeNumber, updatedAtMillis = :updatedAtMillis WHERE committeeId = :committeeId AND memberId = :memberId")
    suspend fun syncMemberSnapshotInInstallments(
        committeeId: String,
        memberId: String,
        memberName: String,
        memberPhone: String,
        committeeNumber: Long,
        updatedAtMillis: Long
    )

    @Query("SELECT * FROM payment_logs WHERE committeeId = :committeeId ORDER BY createdAtMillis DESC LIMIT 200")
    fun observePaymentLogs(committeeId: String): Flow<List<PaymentLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaymentLog(log: PaymentLogEntity)

    @Query("SELECT * FROM reminder_logs WHERE committeeId = :committeeId ORDER BY createdAtMillis DESC LIMIT 200")
    fun observeReminderLogs(committeeId: String): Flow<List<ReminderLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderLog(log: ReminderLogEntity)

    // Local Users
    @Query("SELECT * FROM local_users ORDER BY createdAtMillis ASC")
    fun observeUsers(): Flow<List<LocalUserEntity>>

    @Query("SELECT * FROM local_users ORDER BY createdAtMillis ASC")
    suspend fun getAllUsersOnce(): List<LocalUserEntity>

    @Query("SELECT * FROM local_users WHERE id = :userId LIMIT 1")
    suspend fun getUserByIdOnce(userId: String): LocalUserEntity?

    @Query("SELECT * FROM local_users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsernameOnce(username: String): LocalUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: LocalUserEntity)

    @Update
    suspend fun updateUser(user: LocalUserEntity)

    @Query("DELETE FROM local_users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY createdAtMillis DESC LIMIT 300")
    fun observeAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    // Upload Queue
    @Query("SELECT * FROM upload_queue ORDER BY createdAtMillis DESC LIMIT 250")
    fun observeUploadQueue(): Flow<List<UploadQueueEntity>>

    @Query("SELECT * FROM upload_queue WHERE status IN ('PENDING', 'FAILED') ORDER BY createdAtMillis ASC")
    suspend fun getRetryableUploadsOnce(): List<UploadQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUploadItem(item: UploadQueueEntity)

    @Update
    suspend fun updateUploadItem(item: UploadQueueEntity)

    @Query("DELETE FROM upload_queue WHERE status = 'COMPLETED'")
    suspend fun clearCompletedUploads()

    // Members cross-committee lookup for Member Portal and ID generation
    @Query("SELECT * FROM members ORDER BY committeeNumber ASC, createdAtMillis ASC")
    suspend fun getAllMembersOnce(): List<MemberEntity>

    @Query("SELECT COUNT(*) FROM installments WHERE receiptNumber IS NOT NULL")
    suspend fun countReceiptsOnce(): Int

    // Persistent In-App Notifications
    @Query("SELECT * FROM in_app_notifications ORDER BY createdAtMillis DESC LIMIT 300")
    fun observeNotifications(): Flow<List<InAppNotificationEntity>>

    @Query("SELECT * FROM in_app_notifications WHERE memberId IS NULL OR memberId = :memberId ORDER BY createdAtMillis DESC LIMIT 200")
    fun observeNotificationsForMember(memberId: String): Flow<List<InAppNotificationEntity>>

    @Query("SELECT * FROM in_app_notifications ORDER BY createdAtMillis DESC")
    suspend fun getAllNotificationsOnce(): List<InAppNotificationEntity>

    @Query(
        "SELECT * FROM in_app_notifications WHERE committeeId = :committeeId " +
            "AND ((:memberId IS NULL AND memberId IS NULL) OR memberId = :memberId) " +
            "AND notificationType = :notificationType AND monthKey = :monthKey LIMIT 1"
    )
    suspend fun findDuplicateNotificationOnce(
        committeeId: String,
        memberId: String?,
        notificationType: String,
        monthKey: String
    ): InAppNotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: InAppNotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<InAppNotificationEntity>)

    @Query("UPDATE in_app_notifications SET isRead = 1 WHERE id = :notificationId")
    suspend fun markNotificationRead(notificationId: String)

    @Query("UPDATE in_app_notifications SET isRead = 1 WHERE memberId IS NULL OR memberId = :memberId")
    suspend fun markAllNotificationsReadForMember(memberId: String)

    // Forward Committee Organizers & Forward Payments
    @Query("SELECT * FROM forward_organizers WHERE committeeId = :committeeId ORDER BY createdAtMillis DESC")
    fun observeForwardOrganizers(committeeId: String): Flow<List<ForwardOrganizerEntity>>

    @Query("SELECT * FROM forward_organizers ORDER BY createdAtMillis DESC")
    fun observeAllForwardOrganizers(): Flow<List<ForwardOrganizerEntity>>

    @Query("SELECT * FROM forward_organizers WHERE committeeId = :committeeId ORDER BY createdAtMillis DESC")
    suspend fun getForwardOrganizersOnce(committeeId: String): List<ForwardOrganizerEntity>

    @Query("SELECT * FROM forward_organizers WHERE id = :organizerId LIMIT 1")
    suspend fun getForwardOrganizerByIdOnce(organizerId: String): ForwardOrganizerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForwardOrganizer(organizer: ForwardOrganizerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForwardOrganizers(organizers: List<ForwardOrganizerEntity>)

    @Update
    suspend fun updateForwardOrganizer(organizer: ForwardOrganizerEntity)

    @Query("DELETE FROM forward_organizers WHERE id = :organizerId")
    suspend fun deleteForwardOrganizerById(organizerId: String)

    @Query("SELECT * FROM forward_payments WHERE committeeId = :committeeId ORDER BY paymentDate DESC, createdAtMillis DESC")
    fun observeForwardPayments(committeeId: String): Flow<List<ForwardPaymentEntity>>

    @Query("SELECT * FROM forward_payments ORDER BY paymentDate DESC, createdAtMillis DESC")
    fun observeAllForwardPayments(): Flow<List<ForwardPaymentEntity>>

    @Query("SELECT * FROM forward_payments WHERE committeeId = :committeeId ORDER BY paymentDate DESC, createdAtMillis DESC")
    suspend fun getForwardPaymentsOnce(committeeId: String): List<ForwardPaymentEntity>

    @Query("SELECT * FROM forward_payments WHERE id = :paymentId LIMIT 1")
    suspend fun getForwardPaymentByIdOnce(paymentId: String): ForwardPaymentEntity?

    @Query("SELECT COUNT(*) FROM forward_payments WHERE organizerId = :organizerId AND confirmedByAdmin = 1 AND isReversed = 0")
    suspend fun countActiveForwardPaymentsForOrganizer(organizerId: String): Int

    @Query("SELECT COUNT(*) FROM forward_payments WHERE receiptNumber IS NOT NULL")
    suspend fun countForwardReceiptsOnce(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForwardPayment(payment: ForwardPaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForwardPayments(payments: List<ForwardPaymentEntity>)

    @Update
    suspend fun updateForwardPayment(payment: ForwardPaymentEntity)

    @Query("DELETE FROM forward_payments WHERE id = :paymentId")
    suspend fun deleteForwardPaymentById(paymentId: String)

    // Fresh Start Reset Helpers
    @Query("DELETE FROM forward_payments")
    suspend fun deleteAllForwardPayments()

    @Query("DELETE FROM forward_organizers")
    suspend fun deleteAllForwardOrganizers()

    @Query("DELETE FROM payment_logs")
    suspend fun deleteAllPaymentLogs()

    @Query("DELETE FROM reminder_logs")
    suspend fun deleteAllReminderLogs()

    @Query("DELETE FROM installments")
    suspend fun deleteAllInstallments()

    @Query("DELETE FROM committee_turns")
    suspend fun deleteAllTurns()

    @Query("DELETE FROM members")
    suspend fun deleteAllMembers()

    @Query("DELETE FROM committees")
    suspend fun deleteAllCommittees()

    @Query("DELETE FROM old_committee_records")
    suspend fun deleteAllOldRecords()

    @Query("DELETE FROM in_app_notifications")
    suspend fun deleteAllNotifications()

    @Query("DELETE FROM audit_logs")
    suspend fun deleteAllAuditLogs()

    @Query("DELETE FROM upload_queue")
    suspend fun deleteAllUploadQueue()
}
