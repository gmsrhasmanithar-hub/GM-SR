package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.AuditLogEntry
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.InstallmentRecord
import com.example.model.LocalUser
import com.example.model.PaymentHistoryLog
import com.example.model.ReminderLog
import com.example.model.UploadQueueItem

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

@Entity(tableName = "local_users")
data class LocalUserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String,
    val passwordHash: String,
    val role: String,
    val isActive: Boolean,
    val createdAtMillis: Long
) {
    fun toModel(): LocalUser = LocalUser(
        id = id,
        username = username,
        displayName = displayName,
        passwordHash = passwordHash,
        role = role,
        isActive = isActive,
        createdAtMillis = createdAtMillis
    )
}

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val actorName: String,
    val actionType: String,
    val targetType: String,
    val targetName: String,
    val details: String,
    val createdAtMillis: Long
) {
    fun toModel(): AuditLogEntry = AuditLogEntry(
        id = id,
        actorName = actorName,
        actionType = actionType,
        targetType = targetType,
        targetName = targetName,
        details = details,
        createdAtMillis = createdAtMillis
    )
}

@Entity(tableName = "upload_queue")
data class UploadQueueEntity(
    @PrimaryKey val id: String,
    val title: String,
    val payloadType: String,
    val payloadJson: String,
    val status: String,
    val errorMessage: String?,
    val attempts: Int,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
) {
    fun toModel(): UploadQueueItem = UploadQueueItem(
        id = id,
        title = title,
        payloadType = payloadType,
        payloadJson = payloadJson,
        status = status,
        errorMessage = errorMessage,
        attempts = attempts,
        createdAtMillis = createdAtMillis,
        updatedAtMillis = updatedAtMillis
    )
}
