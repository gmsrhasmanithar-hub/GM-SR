package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.InstallmentRecord
import com.example.model.PaymentHistoryLog
import com.example.model.ReminderLog

@Entity(tableName = "committees")
data class CommitteeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val memberCount: Long,
    val monthlyInstallment: Long,
    val totalMonths: Long,
    val collectionDayStart: Long,
    val collectionDayEnd: Long,
    val payoutDay: Long,
    val startMonth: String,
    val endMonth: String,
    val status: String,
    val notes: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toModel(): Committee = Committee(
        id = id,
        name = name,
        memberCount = memberCount,
        monthlyInstallment = monthlyInstallment,
        totalMonths = totalMonths,
        collectionDayStart = collectionDayStart,
        collectionDayEnd = collectionDayEnd,
        payoutDay = payoutDay,
        startMonth = startMonth,
        endMonth = endMonth,
        status = status,
        notes = notes,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

fun Committee.toEntity(): CommitteeEntity = CommitteeEntity(
    id = id,
    name = name,
    memberCount = memberCount,
    monthlyInstallment = monthlyInstallment,
    totalMonths = totalMonths,
    collectionDayStart = collectionDayStart,
    collectionDayEnd = collectionDayEnd,
    payoutDay = payoutDay,
    startMonth = startMonth,
    endMonth = endMonth,
    status = status,
    notes = notes,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

@Entity(
    tableName = "members",
    foreignKeys = [
        ForeignKey(
            entity = CommitteeEntity::class,
            parentColumns = ["id"],
            childColumns = ["committeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("committeeId")]
)
data class MemberEntity(
    @PrimaryKey val id: String,
    val committeeId: String,
    val name: String,
    val phone: String,
    val committeeNumber: Long,
    val payoutMonth: String,
    val payoutStatus: String,
    val payoutDate: String?,
    val payoutAmount: Long,
    val status: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toModel(): CommitteeMember = CommitteeMember(
        id = id,
        committeeId = committeeId,
        name = name,
        phone = phone,
        committeeNumber = committeeNumber,
        payoutMonth = payoutMonth,
        payoutStatus = payoutStatus,
        payoutDate = payoutDate,
        payoutAmount = payoutAmount,
        status = status,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

fun CommitteeMember.toEntity(overrideCommitteeId: String = committeeId): MemberEntity = MemberEntity(
    id = id,
    committeeId = overrideCommitteeId,
    name = name,
    phone = phone,
    committeeNumber = committeeNumber,
    payoutMonth = payoutMonth,
    payoutStatus = payoutStatus,
    payoutDate = payoutDate,
    payoutAmount = payoutAmount,
    status = status,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

@Entity(
    tableName = "installments",
    foreignKeys = [
        ForeignKey(
            entity = CommitteeEntity::class,
            parentColumns = ["id"],
            childColumns = ["committeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("committeeId"), Index("monthKey"), Index("memberId")]
)
data class InstallmentEntity(
    @PrimaryKey val id: String,
    val committeeId: String,
    val memberId: String,
    val memberName: String,
    val memberPhone: String,
    val committeeNumber: Long,
    val monthKey: String,
    val monthIndex: Long,
    val dueAmount: Long,
    val paidAmount: Long,
    val status: String,
    val paymentDate: String?,
    val paymentNote: String?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toModel(): InstallmentRecord = InstallmentRecord(
        id = id,
        committeeId = committeeId,
        memberId = memberId,
        memberName = memberName,
        memberPhone = memberPhone,
        committeeNumber = committeeNumber,
        monthKey = monthKey,
        monthIndex = monthIndex,
        dueAmount = dueAmount,
        paidAmount = paidAmount,
        status = status,
        paymentDate = paymentDate,
        paymentNote = paymentNote,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}

fun InstallmentRecord.toEntity(overrideCommitteeId: String = committeeId): InstallmentEntity = InstallmentEntity(
    id = id,
    committeeId = overrideCommitteeId,
    memberId = memberId,
    memberName = memberName,
    memberPhone = memberPhone,
    committeeNumber = committeeNumber,
    monthKey = monthKey,
    monthIndex = monthIndex,
    dueAmount = dueAmount,
    paidAmount = paidAmount,
    status = status,
    paymentDate = paymentDate,
    paymentNote = paymentNote,
    createdAtMillis = createdAtMillis,
    updatedAtMillis = updatedAtMillis
)

@Entity(
    tableName = "payment_logs",
    foreignKeys = [
        ForeignKey(
            entity = CommitteeEntity::class,
            parentColumns = ["id"],
            childColumns = ["committeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("committeeId"), Index("installmentId")]
)
data class PaymentLogEntity(
    @PrimaryKey val id: String,
    val committeeId: String,
    val installmentId: String,
    val memberId: String,
    val memberName: String,
    val monthKey: String,
    val previousPaidAmount: Long,
    val newPaidAmount: Long,
    val previousStatus: String,
    val newStatus: String,
    val actionNote: String?,
    val createdAtMillis: Long
) {
    fun toModel(): PaymentHistoryLog = PaymentHistoryLog(
        id = id,
        committeeId = committeeId,
        installmentId = installmentId,
        memberId = memberId,
        memberName = memberName,
        monthKey = monthKey,
        previousPaidAmount = previousPaidAmount,
        newPaidAmount = newPaidAmount,
        previousStatus = previousStatus,
        newStatus = newStatus,
        actionNote = actionNote,
        createdAtMillis = createdAtMillis
    )
}

fun PaymentHistoryLog.toEntity(overrideCommitteeId: String = committeeId): PaymentLogEntity = PaymentLogEntity(
    id = id,
    committeeId = overrideCommitteeId,
    installmentId = installmentId,
    memberId = memberId,
    memberName = memberName,
    monthKey = monthKey,
    previousPaidAmount = previousPaidAmount,
    newPaidAmount = newPaidAmount,
    previousStatus = previousStatus,
    newStatus = newStatus,
    actionNote = actionNote,
    createdAtMillis = createdAtMillis
)

@Entity(
    tableName = "reminder_logs",
    foreignKeys = [
        ForeignKey(
            entity = CommitteeEntity::class,
            parentColumns = ["id"],
            childColumns = ["committeeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("committeeId")]
)
data class ReminderLogEntity(
    @PrimaryKey val id: String,
    val committeeId: String,
    val memberId: String,
    val memberName: String,
    val memberPhone: String,
    val monthKey: String,
    val channel: String,
    val scheduleStage: String,
    val messagePreview: String,
    val dispatchStatus: String,
    val errorDetail: String?,
    val createdAtMillis: Long
) {
    fun toModel(): ReminderLog = ReminderLog(
        id = id,
        committeeId = committeeId,
        memberId = memberId,
        memberName = memberName,
        memberPhone = memberPhone,
        monthKey = monthKey,
        channel = channel,
        scheduleStage = scheduleStage,
        messagePreview = messagePreview,
        dispatchStatus = dispatchStatus,
        errorDetail = errorDetail,
        createdAtMillis = createdAtMillis
    )
}

fun ReminderLog.toEntity(overrideCommitteeId: String = committeeId): ReminderLogEntity = ReminderLogEntity(
    id = id,
    committeeId = overrideCommitteeId,
    memberId = memberId,
    memberName = memberName,
    memberPhone = memberPhone,
    monthKey = monthKey,
    channel = channel,
    scheduleStage = scheduleStage,
    messagePreview = messagePreview,
    dispatchStatus = dispatchStatus,
    errorDetail = errorDetail,
    createdAtMillis = createdAtMillis
)
