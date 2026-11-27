package com.example.data

import android.content.Context
import com.example.R
import com.example.model.AdminProfile
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
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class CommitteeRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Admin must sign in with Google before accessing Firestore.")
    }

    private fun userDoc(uid: String) = db.collection("users").document(uid)
    private fun committeesCol(uid: String) = userDoc(uid).collection("committees")
    private fun committeeDoc(uid: String, committeeId: String) = committeesCol(uid).document(committeeId)
    private fun membersCol(uid: String, committeeId: String) = committeeDoc(uid, committeeId).collection("members")
    private fun installmentsCol(uid: String, committeeId: String) = committeeDoc(uid, committeeId).collection("installments")
    private fun paymentLogsCol(uid: String, committeeId: String) = committeeDoc(uid, committeeId).collection("paymentLogs")
    private fun reminderLogsCol(uid: String, committeeId: String) = committeeDoc(uid, committeeId).collection("reminderLogs")

    private fun <T> Flow<T>.logFirestoreErrors(uid: String, operation: OperationType, path: String): Flow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, operation, path)
            }
            throw error
        }

    fun observeAdminProfile(userId: String): Flow<AdminProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            userDoc(userId).snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(AdminProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .logFirestoreErrors(userId, OperationType.GET, path)
        )
    }

    fun observeCommittees(userId: String): Flow<List<Committee>> = flow {
        val path = "users/$userId/committees"
        emitAll(
            committeesCol(userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap -> snap.toObjects(Committee::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    fun observeMembers(userId: String, committeeId: String): Flow<List<CommitteeMember>> = flow {
        val path = "users/$userId/committees/$committeeId/members"
        emitAll(
            membersCol(userId, committeeId)
                .orderBy("committeeNumber", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap -> snap.toObjects(CommitteeMember::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    fun observeInstallments(userId: String, committeeId: String): Flow<List<InstallmentRecord>> = flow {
        val path = "users/$userId/committees/$committeeId/installments"
        emitAll(
            installmentsCol(userId, committeeId)
                .snapshots()
                .map { snap -> snap.toObjects(InstallmentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    fun observePaymentLogs(userId: String, committeeId: String): Flow<List<PaymentHistoryLog>> = flow {
        val path = "users/$userId/committees/$committeeId/paymentLogs"
        emitAll(
            paymentLogsCol(userId, committeeId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200)
                .snapshots()
                .map { snap -> snap.toObjects(PaymentHistoryLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    fun observeReminderLogs(userId: String, committeeId: String): Flow<List<ReminderLog>> = flow {
        val path = "users/$userId/committees/$committeeId/reminderLogs"
        emitAll(
            reminderLogsCol(userId, committeeId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(200)
                .snapshots()
                .map { snap -> snap.toObjects(ReminderLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .logFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    suspend fun ensureAdminProfile(preferredLanguage: String): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not signed in"))
        val path = "users/$uid"
        return try {
            val snap = userDoc(uid).get().await()
            val currentUser = auth.currentUser!!
            val displayName = (currentUser.displayName?.takeIf { it.isNotBlank() } ?: "Committee Admin").take(80)
            val email = (currentUser.email ?: "").take(120)
            if (!snap.exists()) {
                val profile = AdminProfile(
                    userId = uid,
                    displayName = displayName,
                    email = email,
                    preferredLanguage = if (preferredLanguage == "ur") "ur" else "en"
                )
                userDoc(uid).set(profile.toCreateMap()).await()
            } else {
                userDoc(uid).update(
                    mapOf(
                        "displayName" to displayName,
                        "email" to email,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateProfilePreferences(preferredLanguage: String? = null, activeCommitteeId: String? = null): Result<Unit> {
        var path = "users/unknown"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (preferredLanguage != null) {
                updates["preferredLanguage"] = if (preferredLanguage == "ur") "ur" else "en"
            }
            if (activeCommitteeId != null && activeCommitteeId.isNotBlank()) {
                updates["activeCommitteeId"] = activeCommitteeId.take(128)
            }
            userDoc(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

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
        var path = "users/unknown/committees"
        return try {
            val uid = requireUserId()
            require(name.trim().isNotEmpty()) { "Committee name is required" }
            require(memberCount in 2L..200L) { "Member count must be between 2 and 200" }
            require(monthlyInstallment in 100L..10_000_000L) { "Installment must be between 100 and 10,000,000" }
            require(totalMonths in 2L..200L) { "Duration must be between 2 and 200 months" }
            require(MonthUtils.isValidMonthKey(startMonth)) { "Start month must be YYYY-MM" }

            val endMonth = MonthUtils.addMonths(startMonth, (totalMonths - 1).toInt())
            val docRef = committeesCol(uid).document()
            path = docRef.path
            val committee = Committee(
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
                notes = notes?.trim()?.take(500)?.takeIf { it.isNotEmpty() }
            )
            docRef.set(committee.toCreateMap()).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
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
        var path = "users/unknown/committees/${existing.id}"
        return try {
            val uid = requireUserId()
            require(existing.id.isNotBlank())
            require(newName.trim().isNotEmpty())
            require(newMemberCount in 2L..200L)
            require(newMonthlyInstallment in 100L..10_000_000L)
            require(newTotalMonths in 2L..200L)
            require(MonthUtils.isValidMonthKey(newStartMonth))

            val endMonth = MonthUtils.addMonths(newStartMonth, (newTotalMonths - 1).toInt())
            val cRef = committeeDoc(uid, existing.id)
            path = cRef.path

            val committeeUpdates = mutableMapOf<String, Any>(
                "name" to newName.trim().take(80),
                "memberCount" to newMemberCount,
                "monthlyInstallment" to newMonthlyInstallment,
                "totalMonths" to newTotalMonths,
                "collectionDayStart" to newCollectionDayStart.coerceIn(1L, 31L),
                "collectionDayEnd" to newCollectionDayEnd.coerceIn(1L, 31L),
                "payoutDay" to newPayoutDay.coerceIn(1L, 31L),
                "startMonth" to newStartMonth,
                "endMonth" to endMonth,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            newNotes?.trim()?.take(500)?.takeIf { it.isNotEmpty() }?.let { committeeUpdates["notes"] = it }

            cRef.update(committeeUpdates).await()

            // Recalculate ONLY future/unpaid records if installment or memberCount changed; NEVER alter past paid records.
            val newPayoutAmount = newMemberCount * newMonthlyInstallment
            val membersSnap = membersCol(uid, existing.id).get().await()
            val installmentsSnap = installmentsCol(uid, existing.id).get().await()

            val batch = db.batch()
            var ops = 0
            for (mDoc in membersSnap.documents) {
                val payoutStatus = PayoutStatus.fromRaw(mDoc.getString("payoutStatus"))
                if (payoutStatus == PayoutStatus.PENDING) {
                    val number = (mDoc.getLong("committeeNumber") ?: 1L).toInt()
                    val payoutMonth = MonthUtils.addMonths(newStartMonth, (number - 1).coerceAtLeast(0))
                    batch.update(
                        mDoc.reference,
                        mapOf(
                            "payoutAmount" to newPayoutAmount,
                            "payoutMonth" to payoutMonth,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    ops++
                    if (ops >= 400) break
                }
            }

            if (newMonthlyInstallment != existing.monthlyInstallment) {
                for (iDoc in installmentsSnap.documents) {
                    val st = InstallmentStatus.fromRaw(iDoc.getString("status"))
                    val paid = iDoc.getLong("paidAmount") ?: 0L
                    // Strictly preserve past PAID or partially paid records; only update untouched UNPAID records
                    if (st == InstallmentStatus.UNPAID && paid == 0L && ops < 440) {
                        batch.update(
                            iDoc.reference,
                            mapOf(
                                "dueAmount" to newMonthlyInstallment,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                        ops++
                    }
                }
            }
            if (ops > 0) batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCommitteeStatus(committeeId: String, status: CommitteeStatus): Result<Unit> {
        var path = "users/unknown/committees/$committeeId"
        return try {
            val uid = requireUserId()
            val docRef = committeeDoc(uid, committeeId)
            path = docRef.path
            docRef.update(
                mapOf(
                    "status" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addMember(
        committee: Committee,
        name: String,
        phone: String,
        committeeNumber: Long
    ): Result<String> {
        var path = "users/unknown/committees/${committee.id}/members"
        return try {
            val uid = requireUserId()
            require(name.trim().isNotEmpty()) { "Member name is required" }
            require(phone.trim().length >= 5) { "Valid mobile number is required" }
            require(committeeNumber in 1L..500L) { "Committee number must be >= 1" }

            val docRef = membersCol(uid, committee.id).document()
            path = docRef.path
            val payoutMonth = MonthUtils.addMonths(committee.startMonth, (committeeNumber - 1).toInt().coerceAtLeast(0))
            val member = CommitteeMember(
                name = name.trim().take(80),
                phone = phone.trim().take(25),
                committeeNumber = committeeNumber,
                payoutMonth = payoutMonth,
                payoutStatus = PayoutStatus.PENDING.name,
                payoutAmount = committee.eachPayoutAmount,
                status = MemberStatus.ACTIVE.name
            )
            docRef.set(member.toCreateMap()).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
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
        var path = "users/unknown/committees/${committee.id}/members/${member.id}"
        return try {
            val uid = requireUserId()
            require(newName.trim().isNotEmpty())
            require(newPhone.trim().length >= 5)
            require(newCommitteeNumber in 1L..500L)

            val docRef = membersCol(uid, committee.id).document(member.id)
            path = docRef.path
            val newPayoutMonth = MonthUtils.addMonths(committee.startMonth, (newCommitteeNumber - 1).toInt().coerceAtLeast(0))
            docRef.update(
                mapOf(
                    "name" to newName.trim().take(80),
                    "phone" to newPhone.trim().take(25),
                    "committeeNumber" to newCommitteeNumber,
                    "payoutMonth" to newPayoutMonth,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Soft-archives a member so historical installment and payout records are NEVER erased.
     */
    suspend fun archiveMember(committeeId: String, memberId: String): Result<Unit> {
        var path = "users/unknown/committees/$committeeId/members/$memberId"
        return try {
            val uid = requireUserId()
            val docRef = membersCol(uid, committeeId).document(memberId)
            path = docRef.path
            docRef.update(
                mapOf(
                    "status" to MemberStatus.ARCHIVED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun swapMemberPayoutOrder(
        committee: Committee,
        first: CommitteeMember,
        second: CommitteeMember
    ): Result<Unit> {
        var path = "users/unknown/committees/${committee.id}/members"
        return try {
            val uid = requireUserId()
            val col = membersCol(uid, committee.id)
            path = col.path
            val batch = db.batch()
            val firstNewMonth = MonthUtils.addMonths(committee.startMonth, (second.committeeNumber - 1).toInt().coerceAtLeast(0))
            val secondNewMonth = MonthUtils.addMonths(committee.startMonth, (first.committeeNumber - 1).toInt().coerceAtLeast(0))
            batch.update(
                col.document(first.id),
                mapOf(
                    "committeeNumber" to second.committeeNumber,
                    "payoutMonth" to firstNewMonth,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                col.document(second.id),
                mapOf(
                    "committeeNumber" to first.committeeNumber,
                    "payoutMonth" to secondNewMonth,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateMemberPayoutStatus(
        committee: Committee,
        member: CommitteeMember,
        payoutStatus: PayoutStatus,
        payoutDate: String?
    ): Result<Unit> {
        var path = "users/unknown/committees/${committee.id}/members/${member.id}"
        return try {
            val uid = requireUserId()
            val docRef = membersCol(uid, committee.id).document(member.id)
            path = docRef.path
            val payload = mutableMapOf<String, Any>(
                "payoutStatus" to payoutStatus.name,
                "payoutAmount" to (if (member.payoutAmount > 0L) member.payoutAmount else committee.eachPayoutAmount),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (payoutStatus == PayoutStatus.PAID) {
                payload["payoutDate"] = (payoutDate?.takeIf { it.isNotBlank() } ?: MonthUtils.todayDateString()).take(20)
            }
            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Automatically creates missing monthly installment records for all active members
     * without overwriting existing records.
     */
    suspend fun syncInstallmentsForMonth(
        committee: Committee,
        activeMembers: List<CommitteeMember>,
        existingInstallments: List<InstallmentRecord>,
        monthKey: String,
        monthIndex: Int
    ): Result<Int> {
        var path = "users/unknown/committees/${committee.id}/installments"
        return try {
            val uid = requireUserId()
            val col = installmentsCol(uid, committee.id)
            path = col.path

            val existingMemberIds = existingInstallments
                .filter { it.monthKey == monthKey }
                .map { it.memberId }
                .toSet()

            val missingMembers = activeMembers.filter { it.id !in existingMemberIds }
            if (missingMembers.isEmpty()) return Result.success(0)

            var created = 0
            val chunks = missingMembers.chunked(400)
            for (chunk in chunks) {
                val batch = db.batch()
                for (member in chunk) {
                    val safeId = "${member.id.replace(Regex("[^a-zA-Z0-9_\\-]"), "")}_$monthKey".take(128)
                    val record = InstallmentRecord(
                        memberId = member.id,
                        memberName = member.name,
                        memberPhone = member.phone,
                        committeeNumber = member.committeeNumber,
                        monthKey = monthKey,
                        monthIndex = monthIndex.toLong().coerceAtLeast(1L),
                        dueAmount = committee.monthlyInstallment,
                        paidAmount = 0L,
                        status = InstallmentStatus.UNPAID.name
                    )
                    batch.set(col.document(safeId), record.toCreateMap())
                    created++
                }
                batch.commit().await()
            }
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
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
        var path = "users/unknown/committees/$committeeId/installments/${record.id}"
        return try {
            val uid = requireUserId()
            require(record.id.isNotBlank())
            val clampedPaid = newPaidAmount.coerceIn(0L, 10_000_000L)
            val effectiveDate = when (newStatus) {
                InstallmentStatus.UNPAID -> null
                else -> (paymentDate?.takeIf { it.isNotBlank() } ?: MonthUtils.todayDateString()).take(20)
            }

            val instRef = installmentsCol(uid, committeeId).document(record.id)
            path = instRef.path
            val logRef = paymentLogsCol(uid, committeeId).document()

            val batch = db.batch()
            val updateMap = mutableMapOf<String, Any>(
                "paidAmount" to clampedPaid,
                "status" to newStatus.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (effectiveDate != null) updateMap["paymentDate"] = effectiveDate
            val cleanNote = paymentNote?.trim()?.take(200)?.takeIf { it.isNotEmpty() }
            if (cleanNote != null) updateMap["paymentNote"] = cleanNote

            batch.update(instRef, updateMap)

            val log = PaymentHistoryLog(
                installmentId = record.id,
                memberId = record.memberId,
                memberName = record.memberName,
                monthKey = record.monthKey,
                previousPaidAmount = record.paidAmount,
                newPaidAmount = clampedPaid,
                previousStatus = record.status,
                newStatus = newStatus.name,
                actionNote = cleanNote
            )
            batch.set(logRef, log.toCreateMap())
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
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
        var path = "users/unknown/committees/$committeeId/reminderLogs"
        return try {
            val uid = requireUserId()
            val docRef = reminderLogsCol(uid, committeeId).document()
            path = docRef.path
            val log = ReminderLog(
                memberId = member.id,
                memberName = member.name,
                memberPhone = member.phone,
                monthKey = monthKey,
                channel = channel.name,
                scheduleStage = stage.name,
                messagePreview = messagePreview.take(500),
                dispatchStatus = dispatchStatus.name,
                errorDetail = errorDetail?.take(300)
            )
            docRef.set(log.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Exports all committees, members, installments, payment logs, and reminder logs as JSON.
     */
    suspend fun exportFullBackupJson(): Result<String> {
        var path = "users/unknown/committees"
        return try {
            val uid = requireUserId()
            path = "users/$uid/committees"
            val root = JSONObject()
            root.put("brand", "GM HASMANI COMMITTEE MANAGER")
            root.put("version", 1)
            root.put("exportedAt", MonthUtils.todayDateString())

            val committeesArr = JSONArray()
            val cSnap = committeesCol(uid).get().await()
            for (cDoc in cSnap.documents) {
                val c = cDoc.toObject(Committee::class.java) ?: continue
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
                membersCol(uid, c.id).get().await().documents.forEach { mDoc ->
                    val m = mDoc.toObject(CommitteeMember::class.java) ?: return@forEach
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
                installmentsCol(uid, c.id).get().await().documents.forEach { iDoc ->
                    val i = iDoc.toObject(InstallmentRecord::class.java) ?: return@forEach
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
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    /**
     * Restores committees, members, and installments from a previously exported JSON backup.
     */
    suspend fun restoreFromBackupJson(jsonContent: String): Result<Int> {
        var path = "users/unknown/committees"
        return try {
            val uid = requireUserId()
            path = "users/$uid/committees"
            val root = JSONObject(jsonContent)
            val committeesArr = root.getJSONArray("committees")
            var restoredCount = 0

            for (idx in 0 until committeesArr.length()) {
                val cObj = committeesArr.getJSONObject(idx)
                val rawCid = cObj.optString("id").replace(Regex("[^a-zA-Z0-9_\\-]"), "")
                val cRef = if (rawCid.isNotBlank()) committeesCol(uid).document(rawCid.take(128))
                else committeesCol(uid).document()

                val committee = Committee(
                    name = cObj.getString("name").take(80),
                    memberCount = cObj.optLong("memberCount", 10L).coerceIn(2L, 200L),
                    monthlyInstallment = cObj.optLong("monthlyInstallment", 5000L).coerceIn(100L, 10_000_000L),
                    totalMonths = cObj.optLong("totalMonths", 10L).coerceIn(2L, 200L),
                    collectionDayStart = cObj.optLong("collectionDayStart", 28L).coerceIn(1L, 31L),
                    collectionDayEnd = cObj.optLong("collectionDayEnd", 8L).coerceIn(1L, 31L),
                    payoutDay = cObj.optLong("payoutDay", 10L).coerceIn(1L, 31L),
                    startMonth = cObj.optString("startMonth", MonthUtils.currentMonthKey()),
                    endMonth = cObj.optString("endMonth", MonthUtils.currentMonthKey()),
                    status = CommitteeStatus.fromRaw(cObj.optString("status")).name,
                    notes = cObj.optString("notes").takeIf { it.isNotBlank() }
                )

                val ops = mutableListOf<Pair<com.google.firebase.firestore.DocumentReference, Map<String, Any>>>()
                ops.add(cRef to committee.toCreateMap())

                val membersArr = cObj.optJSONArray("members") ?: JSONArray()
                for (mIdx in 0 until membersArr.length()) {
                    val mObj = membersArr.getJSONObject(mIdx)
                    val rawMid = mObj.optString("id").replace(Regex("[^a-zA-Z0-9_\\-]"), "")
                    val mRef = if (rawMid.isNotBlank()) membersCol(uid, cRef.id).document(rawMid.take(128))
                    else membersCol(uid, cRef.id).document()
                    val member = CommitteeMember(
                        name = mObj.getString("name").take(80),
                        phone = mObj.getString("phone").take(25),
                        committeeNumber = mObj.optLong("committeeNumber", 1L).coerceIn(1L, 500L),
                        payoutMonth = mObj.optString("payoutMonth", committee.startMonth),
                        payoutStatus = PayoutStatus.fromRaw(mObj.optString("payoutStatus")).name,
                        payoutDate = mObj.optString("payoutDate").takeIf { it.isNotBlank() },
                        payoutAmount = mObj.optLong("payoutAmount", committee.eachPayoutAmount).coerceAtLeast(0L),
                        status = MemberStatus.fromRaw(mObj.optString("status")).name
                    )
                    ops.add(mRef to member.toCreateMap())
                }

                val instArr = cObj.optJSONArray("installments") ?: JSONArray()
                for (iIdx in 0 until instArr.length()) {
                    val iObj = instArr.getJSONObject(iIdx)
                    val rawIid = iObj.optString("id").replace(Regex("[^a-zA-Z0-9_\\-]"), "")
                    val iRef = if (rawIid.isNotBlank()) installmentsCol(uid, cRef.id).document(rawIid.take(128))
                    else installmentsCol(uid, cRef.id).document()
                    val rec = InstallmentRecord(
                        memberId = iObj.getString("memberId").take(128),
                        memberName = iObj.getString("memberName").take(80),
                        memberPhone = iObj.getString("memberPhone").take(25),
                        committeeNumber = iObj.optLong("committeeNumber", 1L).coerceIn(1L, 500L),
                        monthKey = iObj.getString("monthKey"),
                        monthIndex = iObj.optLong("monthIndex", 1L).coerceIn(1L, 500L),
                        dueAmount = iObj.optLong("dueAmount", committee.monthlyInstallment).coerceIn(100L, 10_000_000L),
                        paidAmount = iObj.optLong("paidAmount", 0L).coerceIn(0L, 10_000_000L),
                        status = InstallmentStatus.fromRaw(iObj.optString("status")).name,
                        paymentDate = iObj.optString("paymentDate").takeIf { it.isNotBlank() },
                        paymentNote = iObj.optString("paymentNote").takeIf { it.isNotBlank() }
                    )
                    ops.add(iRef to rec.toCreateMap())
                }

                for (chunk in ops.chunked(400)) {
                    val batch = db.batch()
                    chunk.forEach { (ref, payload) -> batch.set(ref, payload) }
                    batch.commit().await()
                }
                restoredCount++
            }
            Result.success(restoredCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
