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

    @Query("SELECT * FROM members WHERE committeeId = :committeeId ORDER BY committeeNumber ASC")
    fun observeMembers(committeeId: String): Flow<List<MemberEntity>>

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

    @Query("SELECT * FROM installments WHERE committeeId = :committeeId ORDER BY monthIndex ASC, committeeNumber ASC")
    fun observeInstallments(committeeId: String): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE committeeId = :committeeId ORDER BY monthIndex ASC, committeeNumber ASC")
    suspend fun getInstallmentsOnce(committeeId: String): List<InstallmentEntity>

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
}
