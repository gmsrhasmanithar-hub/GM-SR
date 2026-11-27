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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommittee(committee: CommitteeEntity)

    @Update
    suspend fun updateCommittee(committee: CommitteeEntity)

    @Query("UPDATE committees SET status = :status, updatedAtMillis = :updatedAtMillis WHERE id = :committeeId")
    suspend fun updateCommitteeStatus(committeeId: String, status: String, updatedAtMillis: Long)

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

    @Query("SELECT * FROM installments WHERE committeeId = :committeeId ORDER BY monthIndex ASC, committeeNumber ASC")
    fun observeInstallments(committeeId: String): Flow<List<InstallmentEntity>>

    @Query("SELECT * FROM installments WHERE committeeId = :committeeId ORDER BY monthIndex ASC, committeeNumber ASC")
    suspend fun getInstallmentsOnce(committeeId: String): List<InstallmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallments(installments: List<InstallmentEntity>)

    @Update
    suspend fun updateInstallment(installment: InstallmentEntity)

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
}
