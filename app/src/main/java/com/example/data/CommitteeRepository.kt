package com.example.data

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.CommitteeDao
import com.example.data.local.CommitteeEntity
import com.example.data.local.InstallmentEntity
import com.example.data.local.MemberEntity
import com.example.data.local.PaymentLogEntity
import com.example.data.local.ReminderLogEntity
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.CommitteeStatus
import com.example.model.DispatchStatus
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.MemberStatus
import com.example.model.MonthUtils
import com.example.model.PaymentHistoryLog
import com.example.model.PayoutStatus
import com.example.model.ReminderChannel
import com.example.model.ReminderLog
import com.example.model.ReminderStage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class CommitteeRepository(
    private val db: AppDatabase,
    private val dao: CommitteeDao = db.committeeDao()
) {
    constructor(context: Context) : this(AppDatabase.getInstance(context))

    fun observeCommittees(): Flow<List<Committee>> =
        dao.observeCommittees().map { list -> list.map { it.toModel() } }

    fun observeMembers(committeeId: String): Flow<List<CommitteeMember>> =
        dao.observeMembers(committeeId).map { list -> list.map { it.toModel() } }

    fun observeInstallments(committeeId: String): Flow<List<InstallmentRecord>> =
        dao.observeInstallments(committeeId).map { list -> list.map { it.toModel() } }

    fun observePaymentLogs(committeeId: String): Flow<List<PaymentHistoryLog>> =
        dao.observePaymentLogs(committeeId).map { list -> list.map { it.toModel() } }

    fun observeReminderLogs(committeeId: String): Flow<List<ReminderLog>> =
        dao.observeReminderLogs(committeeId).map { list -> list.map { it.toModel() } }

    suspend fun createCommittee(
        name: String,
        memberCount: Long,
        monthlyInstallment: Long,
        totalMonths: Long,
        collectionDayStart: Long,
        collectionDayEnd: Long,
        payoutDay: Long,
        startMonth: String,
        notes: String?
    ): Result<String> {
        return try {
            require(name.trim().isNotEmpty()) { "Committee name is required" }
            require(memberCount in 2L..200L) { "Member count must be between 2 and 200" }
            require(monthlyInstallment in 100L..10_000_000L) { "Installment must be between 100 and 10,000,000" }
            require(totalMonths in 2L..200L) { "Duration must be between 2 and 200 months" }
            require(MonthUtils.isValidMonthKey(startMonth)) { "Start month must be YYYY-MM" }

            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()
            val endMonth = MonthUtils.addMonths(startMonth, (totalMonths - 1).toInt())
            val entity = CommitteeEntity(
                id = id,
                name = name.trim().take(80),
                memberCount = memberCount,
                monthlyInstallment = monthlyInstallment,
                totalMonths = totalMonths,
                collectionDayStart = collectionDayStart.coerceIn(1L, 31L),
                collectionDayEnd = collectionDayEnd.coerceIn(1L, 31L),
                payoutDay = payoutDay.coerceIn(1L, 31L),
                startMonth = startMonth,
                endMonth = endMonth,
                status = CommitteeStatus.ACTIVE.name,
                notes = notes?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
                createdAtMillis = now,
                updatedAtMillis = now
            )
            dao.insertCommittee(entity)
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCommitteeWithFutureRecalculation(
        existing: Committee,
        newName: String,
        newMemberCount: Long,
        newMonthlyInstallment: Long,
        newTotalMonths: Long,
        newCollectionDayStart: Long,
        newCollectionDayEnd: Long,
        newPayoutDay: Long,
        newStartMonth: String,
        newNotes: String?
    ): Result<Unit> {
        return try {
            require(existing.id.isNotBlank())
            require(newName.trim().isNotEmpty())
            require(newMemberCount in 2L..200L)
            require(newMonthlyInstallment in 100L..10_000_000L)
            require(newTotalMonths in 2L..200L)
            require(MonthUtils.isValidMonthKey(newStartMonth))

            val now = System.currentTimeMillis()
            val endMonth = MonthUtils.addMonths(newStartMonth, (newTotalMonths - 1).toInt())
            val newPayoutAmount = newMemberCount * newMonthlyInstallment

            db.withTransaction {
                dao.updateCommittee(
                    CommitteeEntity(
                        id = existing.id,
                        name = newName.trim().take(80),
                        memberCount = newMemberCount,
                        monthlyInstallment = newMonthlyInstallment,
                        totalMonths = newTotalMonths,
                        collectionDayStart = newCollectionDayStart.coerceIn(1L, 31L),
                        collectionDayEnd = newCollectionDayEnd.coerceIn(1L, 31L),
                        payoutDay = newPayoutDay.coerceIn(1L, 31L),
                        startMonth = newStartMonth,
                        endMonth = endMonth,
                        status = existing.status,
                        notes = newNotes?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
                        createdAtMillis = existing.createdAtMillis,
                        updatedAtMillis = now
                    )
                )

                val members = dao.getMembersOnce(existing.id)
                members.forEach { m ->
                    if (PayoutStatus.fromRaw(m.payoutStatus) == PayoutStatus.PENDING) {
                        val payoutMonth = MonthUtils.addMonths(newStartMonth, (m.committeeNumber - 1).toInt().coerceAtLeast(0))
                        dao.updateMember(
                            m.copy(
                                payoutAmount = newPayoutAmount,
                                payoutMonth = payoutMonth,
                                updatedAtMillis = now
                            )
                        )
                    }
                }

                if (newMonthlyInstallment != existing.monthlyInstallment) {
                    val installments = dao.getInstallmentsOnce(existing.id)
                    installments.forEach { inst ->
                        val st = InstallmentStatus.fromRaw(inst.status)
                        if (st == InstallmentStatus.UNPAID && inst.paidAmount == 0L) {
                            dao.updateInstallment(
                                inst.copy(
                                    dueAmount = newMonthlyInstallment,
                                    updatedAtMillis = now
                                )
                            )
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCommitteeStatus(committeeId: String, status: CommitteeStatus): Result<Unit> {
        return try {
            dao.updateCommitteeStatus(committeeId, status.name, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addMember(
        committee: Committee,
        name: String,
        phone: String,
        committeeNumber: Long
    ): Result<String> {
        return try {
            require(name.trim().isNotEmpty()) { "Member name is required" }
            require(phone.trim().length >= 5) { "Valid mobile number is required" }
            require(committeeNumber in 1L..500L) { "Committee number must be >= 1" }

            val now = System.currentTimeMillis()
            val memberId = UUID.randomUUID().toString()
            val payoutMonth = MonthUtils.addMonths(committee.startMonth, (committeeNumber - 1).toInt().coerceAtLeast(0))
            val entity = MemberEntity(
                id = memberId,
                committeeId = committee.id,
                name = name.trim().take(80),
                phone = phone.trim().take(25),
                committeeNumber = committeeNumber,
                payoutMonth = payoutMonth,
                payoutStatus = PayoutStatus.PENDING.name,
                payoutDate = null,
                payoutAmount = committee.eachPayoutAmount,
                status = MemberStatus.ACTIVE.name,
                createdAtMillis = now,
                updatedAtMillis = now
            )
            dao.insertMember(entity)
            Result.success(memberId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMember(
        committee: Committee,
        member: CommitteeMember,
        newName: String,
        newPhone: String,
        newCommitteeNumber: Long
    ): Result<Unit> {
        return try {
            require(newName.trim().isNotEmpty())
            require(newPhone.trim().length >= 5)
            require(newCommitteeNumber in 1L..500L)

            val now = System.currentTimeMillis()
            val cleanName = newName.trim().take(80)
            val cleanPhone = newPhone.trim().take(25)
            val newPayoutMonth = MonthUtils.addMonths(committee.startMonth, (newCommitteeNumber - 1).toInt().coerceAtLeast(0))
            db.withTransaction {
                dao.updateMember(
                    MemberEntity(
                        id = member.id,
                        committeeId = committee.id,
                        name = cleanName,
                        phone = cleanPhone,
                        committeeNumber = newCommitteeNumber,
                        payoutMonth = newPayoutMonth,
                        payoutStatus = member.payoutStatus,
                        payoutDate = member.payoutDate,
                        payoutAmount = member.payoutAmount,
                        status = member.status,
                        createdAtMillis = member.createdAtMillis,
                        updatedAtMillis = now
                    )
                )
                dao.syncMemberSnapshotInInstallments(
                    committeeId = committee.id,
                    memberId = member.id,
                    memberName = cleanName,
                    memberPhone = cleanPhone,
                    committeeNumber = newCommitteeNumber,
                    updatedAtMillis = now
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun archiveMember(committeeId: String, memberId: String): Result<Unit> {
        return try {
            dao.updateMemberStatus(memberId, MemberStatus.ARCHIVED.name, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun swapMemberPayoutOrder(
        committee: Committee,
        first: CommitteeMember,
        second: CommitteeMember
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val firstNewMonth = MonthUtils.addMonths(committee.startMonth, (second.committeeNumber - 1).toInt().coerceAtLeast(0))
            val secondNewMonth = MonthUtils.addMonths(committee.startMonth, (first.committeeNumber - 1).toInt().coerceAtLeast(0))
            db.withTransaction {
                dao.updateMember(
                    MemberEntity(
                        id = first.id,
                        committeeId = committee.id,
                        name = first.name,
                        phone = first.phone,
                        committeeNumber = second.committeeNumber,
                        payoutMonth = firstNewMonth,
                        payoutStatus = first.payoutStatus,
                        payoutDate = first.payoutDate,
                        payoutAmount = first.payoutAmount,
                        status = first.status,
                        createdAtMillis = first.createdAtMillis,
                        updatedAtMillis = now
                    )
                )
                dao.updateMember(
                    MemberEntity(
                        id = second.id,
                        committeeId = committee.id,
                        name = second.name,
                        phone = second.phone,
                        committeeNumber = first.committeeNumber,
                        payoutMonth = secondNewMonth,
                        payoutStatus = second.payoutStatus,
                        payoutDate = second.payoutDate,
                        payoutAmount = second.payoutAmount,
                        status = second.status,
                        createdAtMillis = second.createdAtMillis,
                        updatedAtMillis = now
                    )
                )
                dao.syncMemberSnapshotInInstallments(committee.id, first.id, first.name, first.phone, second.committeeNumber, now)
                dao.syncMemberSnapshotInInstallments(committee.id, second.id, second.name, second.phone, first.committeeNumber, now)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMemberPayoutStatus(
        committee: Committee,
        member: CommitteeMember,
        payoutStatus: PayoutStatus,
        payoutDate: String?,
        payoutAmount: Long? = null
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val effectiveAmount = (payoutAmount ?: member.payoutAmount)
                .takeIf { it > 0L } ?: committee.eachPayoutAmount
            val effectiveDate = if (payoutStatus == PayoutStatus.PAID) {
                (payoutDate?.takeIf { it.isNotBlank() } ?: MonthUtils.todayDateString()).take(20)
            } else {
                null
            }
            dao.updateMember(
                MemberEntity(
                    id = member.id,
                    committeeId = committee.id,
                    name = member.name,
                    phone = member.phone,
                    committeeNumber = member.committeeNumber,
                    payoutMonth = member.payoutMonth,
                    payoutStatus = payoutStatus.name,
                    payoutDate = effectiveDate,
                    payoutAmount = effectiveAmount.coerceIn(0L, 500_000_000L),
                    status = member.status,
                    createdAtMillis = member.createdAtMillis,
                    updatedAtMillis = now
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncInstallmentsForMonth(
        committee: Committee,
        activeMembers: List<CommitteeMember>,
        existingInstallments: List<InstallmentRecord>,
        monthKey: String,
        monthIndex: Int
    ): Result<Int> {
        return try {
            val existingMemberIds = existingInstallments
                .filter { it.monthKey == monthKey }
                .map { it.memberId }
                .toSet()

            val missingMembers = activeMembers.filter { it.id !in existingMemberIds }
            if (missingMembers.isEmpty()) return Result.success(0)

            val now = System.currentTimeMillis()
            val toInsert = missingMembers.map { member ->
                val safeId = "${committee.id}_${member.id}_$monthKey".take(128)
                InstallmentEntity(
                    id = safeId,
                    committeeId = committee.id,
                    memberId = member.id,
                    memberName = member.name,
                    memberPhone = member.phone,
                    committeeNumber = member.committeeNumber,
                    monthKey = monthKey,
                    monthIndex = monthIndex.toLong().coerceAtLeast(1L),
                    dueAmount = committee.monthlyInstallment,
                    paidAmount = 0L,
                    status = InstallmentStatus.UNPAID.name,
                    paymentDate = null,
                    paymentNote = null,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            }
            dao.insertInstallments(toInsert)
            Result.success(toInsert.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordInstallmentPayment(
        committeeId: String,
        record: InstallmentRecord,
        newPaidAmount: Long,
        newStatus: InstallmentStatus,
        paymentDate: String?,
        paymentNote: String?
    ): Result<Unit> {
        return try {
            require(record.id.isNotBlank())
            val now = System.currentTimeMillis()
            val clampedPaid = newPaidAmount.coerceIn(0L, 10_000_000L)
            val effectiveDate = when (newStatus) {
                InstallmentStatus.UNPAID -> null
                else -> (paymentDate?.takeIf { it.isNotBlank() } ?: MonthUtils.todayDateString()).take(20)
            }
            val cleanNote = paymentNote?.trim()?.take(200)?.takeIf { it.isNotEmpty() }

            db.withTransaction {
                dao.updateInstallment(
                    InstallmentEntity(
                        id = record.id,
                        committeeId = committeeId,
                        memberId = record.memberId,
                        memberName = record.memberName,
                        memberPhone = record.memberPhone,
                        committeeNumber = record.committeeNumber,
                        monthKey = record.monthKey,
                        monthIndex = record.monthIndex,
                        dueAmount = record.dueAmount,
                        paidAmount = clampedPaid,
                        status = newStatus.name,
                        paymentDate = effectiveDate,
                        paymentNote = cleanNote,
                        createdAtMillis = record.createdAtMillis,
                        updatedAtMillis = now
                    )
                )
                dao.insertPaymentLog(
                    PaymentLogEntity(
                        id = UUID.randomUUID().toString(),
                        committeeId = committeeId,
                        installmentId = record.id,
                        memberId = record.memberId,
                        memberName = record.memberName,
                        monthKey = record.monthKey,
                        previousPaidAmount = record.paidAmount,
                        newPaidAmount = clampedPaid,
                        previousStatus = record.status,
                        newStatus = newStatus.name,
                        actionNote = cleanNote,
                        createdAtMillis = now
                    )
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logReminderDispatch(
        committeeId: String,
        member: CommitteeMember,
        monthKey: String,
        channel: ReminderChannel,
        stage: ReminderStage,
        messagePreview: String,
        dispatchStatus: DispatchStatus,
        errorDetail: String? = null
    ): Result<Unit> {
        return try {
            dao.insertReminderLog(
                ReminderLogEntity(
                    id = UUID.randomUUID().toString(),
                    committeeId = committeeId,
                    memberId = member.id,
                    memberName = member.name,
                    memberPhone = member.phone,
                    monthKey = monthKey,
                    channel = channel.name,
                    scheduleStage = stage.name,
                    messagePreview = messagePreview.trim().take(500).ifEmpty { "Reminder" },
                    dispatchStatus = dispatchStatus.name,
                    errorDetail = errorDetail?.trim()?.take(300)?.takeIf { it.isNotEmpty() },
                    createdAtMillis = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportFullBackupJson(): Result<String> {
        return try {
            val root = JSONObject()
            root.put("brand", "GM HASMANI COMMITTEE MANAGER")
            root.put("version", 1)
            root.put("exportedAt", MonthUtils.todayDateString())

            val committeesArr = JSONArray()
            val committees = dao.getAllCommitteesOnce()
            for (c in committees) {
                val cJson = JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("memberCount", c.memberCount)
                    put("monthlyInstallment", c.monthlyInstallment)
                    put("totalMonths", c.totalMonths)
                    put("collectionDayStart", c.collectionDayStart)
                    put("collectionDayEnd", c.collectionDayEnd)
                    put("payoutDay", c.payoutDay)
                    put("startMonth", c.startMonth)
                    put("endMonth", c.endMonth)
                    put("status", c.status)
                    if (!c.notes.isNullOrBlank()) put("notes", c.notes)
                }

                val membersArr = JSONArray()
                dao.getMembersOnce(c.id).forEach { m ->
                    membersArr.put(JSONObject().apply {
                        put("id", m.id)
                        put("name", m.name)
                        put("phone", m.phone)
                        put("committeeNumber", m.committeeNumber)
                        put("payoutMonth", m.payoutMonth)
                        put("payoutStatus", m.payoutStatus)
                        if (!m.payoutDate.isNullOrBlank()) put("payoutDate", m.payoutDate)
                        put("payoutAmount", m.payoutAmount)
                        put("status", m.status)
                    })
                }
                cJson.put("members", membersArr)

                val instArr = JSONArray()
                dao.getInstallmentsOnce(c.id).forEach { i ->
                    instArr.put(JSONObject().apply {
                        put("id", i.id)
                        put("memberId", i.memberId)
                        put("memberName", i.memberName)
                        put("memberPhone", i.memberPhone)
                        put("committeeNumber", i.committeeNumber)
                        put("monthKey", i.monthKey)
                        put("monthIndex", i.monthIndex)
                        put("dueAmount", i.dueAmount)
                        put("paidAmount", i.paidAmount)
                        put("status", i.status)
                        if (!i.paymentDate.isNullOrBlank()) put("paymentDate", i.paymentDate)
                        if (!i.paymentNote.isNullOrBlank()) put("paymentNote", i.paymentNote)
                    })
                }
                cJson.put("installments", instArr)
                committeesArr.put(cJson)
            }
            root.put("committees", committeesArr)
            Result.success(root.toString(2))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromBackupJson(jsonContent: String): Result<Int> {
        return try {
            val root = JSONObject(jsonContent)
            val committeesArr = root.getJSONArray("committees")
            var restoredCount = 0
            val now = System.currentTimeMillis()

            db.withTransaction {
                for (idx in 0 until committeesArr.length()) {
                    val cObj = committeesArr.getJSONObject(idx)
                    val cid = cObj.optString("id").replace(Regex("[^a-zA-Z0-9_\\-]"), "")
                        .takeIf { it.isNotBlank() }?.take(128) ?: UUID.randomUUID().toString()
                    val memberCount = cObj.optLong("memberCount", 10L).coerceIn(2L, 200L)
                    val monthlyInstallment = cObj.optLong("monthlyInstallment", 5000L).coerceIn(100L, 10_000_000L)

                    val committeeEntity = CommitteeEntity(
                        id = cid,
                        name = cObj.getString("name").take(80),
                        memberCount = memberCount,
                        monthlyInstallment = monthlyInstallment,
                        totalMonths = cObj.optLong("totalMonths", 10L).coerceIn(2L, 200L),
                        collectionDayStart = cObj.optLong("collectionDayStart", 28L).coerceIn(1L, 31L),
                        collectionDayEnd = cObj.optLong("collectionDayEnd", 8L).coerceIn(1L, 31L),
                        payoutDay = cObj.optLong("payoutDay", 10L).coerceIn(1L, 31L),
                        startMonth = cObj.optString("startMonth", MonthUtils.currentMonthKey()),
                        endMonth = cObj.optString("endMonth", MonthUtils.currentMonthKey()),
                        status = CommitteeStatus.fromRaw(cObj.optString("status")).name,
                        notes = cObj.optString("notes").takeIf { it.isNotBlank() },
                        createdAtMillis = now,
                        updatedAtMillis = now
                    )
                    dao.insertCommittee(committeeEntity)

                    val membersArr = cObj.optJSONArray("members") ?: JSONArray()
                    val memberEntities = buildList {
                        for (mIdx in 0 until membersArr.length()) {
                            val mObj = membersArr.getJSONObject(mIdx)
                            val mid = mObj.optString("id").replace(Regex("[^a-zA-Z0-9_\\-]"), "")
                                .takeIf { it.isNotBlank() }?.take(128) ?: UUID.randomUUID().toString()
                            add(
                                MemberEntity(
                                    id = mid,
                                    committeeId = cid,
                                    name = mObj.getString("name").take(80),
                                    phone = mObj.getString("phone").take(25),
                                    committeeNumber = mObj.optLong("committeeNumber", 1L).coerceIn(1L, 500L),
                                    payoutMonth = mObj.optString("payoutMonth", committeeEntity.startMonth),
                                    payoutStatus = PayoutStatus.fromRaw(mObj.optString("payoutStatus")).name,
                                    payoutDate = mObj.optString("payoutDate").takeIf { it.isNotBlank() },
                                    payoutAmount = mObj.optLong("payoutAmount", memberCount * monthlyInstallment).coerceAtLeast(0L),
                                    status = MemberStatus.fromRaw(mObj.optString("status")).name,
                                    createdAtMillis = now,
                                    updatedAtMillis = now
                                )
                            )
                        }
                    }
                    if (memberEntities.isNotEmpty()) dao.insertMembers(memberEntities)

                    val instArr = cObj.optJSONArray("installments") ?: JSONArray()
                    val instEntities = buildList {
                        for (iIdx in 0 until instArr.length()) {
                            val iObj = instArr.getJSONObject(iIdx)
                            val iid = iObj.optString("id").replace(Regex("[^a-zA-Z0-9_\\-]"), "")
                                .takeIf { it.isNotBlank() }?.take(128) ?: UUID.randomUUID().toString()
                            add(
                                InstallmentEntity(
                                    id = iid,
                                    committeeId = cid,
                                    memberId = iObj.getString("memberId").take(128),
                                    memberName = iObj.getString("memberName").take(80),
                                    memberPhone = iObj.getString("memberPhone").take(25),
                                    committeeNumber = iObj.optLong("committeeNumber", 1L).coerceIn(1L, 500L),
                                    monthKey = iObj.getString("monthKey"),
                                    monthIndex = iObj.optLong("monthIndex", 1L).coerceIn(1L, 500L),
                                    dueAmount = iObj.optLong("dueAmount", monthlyInstallment).coerceIn(100L, 10_000_000L),
                                    paidAmount = iObj.optLong("paidAmount", 0L).coerceIn(0L, 10_000_000L),
                                    status = InstallmentStatus.fromRaw(iObj.optString("status")).name,
                                    paymentDate = iObj.optString("paymentDate").takeIf { it.isNotBlank() },
                                    paymentNote = iObj.optString("paymentNote").takeIf { it.isNotBlank() },
                                    createdAtMillis = now,
                                    updatedAtMillis = now
                                )
                            )
                        }
                    }
                    if (instEntities.isNotEmpty()) dao.insertInstallments(instEntities)
                    restoredCount++
                }
            }
            Result.success(restoredCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
