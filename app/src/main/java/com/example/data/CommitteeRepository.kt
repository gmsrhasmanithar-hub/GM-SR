package com.example.data

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.local.AuditLogEntity
import com.example.data.local.CommitteeDao
import com.example.data.local.CommitteeEntity
import com.example.data.local.CommitteeTurnEntity
import com.example.data.local.InstallmentEntity
import com.example.data.local.LocalUserEntity
import com.example.data.local.MemberEntity
import com.example.data.local.OldCommitteeRecordEntity
import com.example.data.local.PaymentLogEntity
import com.example.data.local.ReminderLogEntity
import com.example.data.local.UploadQueueEntity
import com.example.model.AppCompanySettings
import com.example.model.AuditLogEntry
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.CommitteeStatus
import com.example.model.CommitteeTurn
import com.example.model.DispatchStatus
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.LocalUser
import com.example.model.MemberStatus
import com.example.model.MonthUtils
import com.example.model.OldCommitteeRecord
import com.example.model.OldHistoryCsvUtils
import com.example.model.PasswordHasher
import com.example.model.PaymentHistoryLog
import com.example.model.PaymentMethod
import com.example.model.PayoutStatus
import com.example.model.ReminderChannel
import com.example.model.ReminderLog
import com.example.model.ReminderStage
import com.example.model.UploadBatchSummary
import com.example.model.UploadQueueItem
import com.example.model.UploadQueueStatus
import com.example.model.UserRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class CommitteeRepository(
    private val db: AppDatabase,
    private val dao: CommitteeDao = db.committeeDao(),
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    constructor(context: Context) : this(AppDatabase.getInstance(context))

    fun observeCommittees(): Flow<List<Committee>> =
        dao.observeCommittees().map { list -> list.map { it.toModel() } }

    fun observeMembers(committeeId: String): Flow<List<CommitteeMember>> =
        dao.observeMembers(committeeId).map { list -> list.map { it.toModel() } }

    fun observeAllMembers(): Flow<List<CommitteeMember>> =
        dao.observeAllMembers().map { list -> list.map { it.toModel() } }

    fun observeTurns(committeeId: String): Flow<List<CommitteeTurn>> =
        dao.observeTurns(committeeId).map { list -> list.map { it.toModel() } }

    fun observeAllTurns(): Flow<List<CommitteeTurn>> =
        dao.observeAllTurns().map { list -> list.map { it.toModel() } }

    fun observeOldRecords(): Flow<List<OldCommitteeRecord>> =
        dao.observeOldRecords().map { list -> list.map { it.toModel() } }

    fun observeInstallments(committeeId: String): Flow<List<InstallmentRecord>> =
        dao.observeInstallments(committeeId).map { list -> list.map { it.toModel() } }

    fun observeAllInstallments(): Flow<List<InstallmentRecord>> =
        dao.observeAllInstallments().map { list -> list.map { it.toModel() } }

    fun observePaymentLogs(committeeId: String): Flow<List<PaymentHistoryLog>> =
        dao.observePaymentLogs(committeeId).map { list -> list.map { it.toModel() } }

    fun observeReminderLogs(committeeId: String): Flow<List<ReminderLog>> =
        dao.observeReminderLogs(committeeId).map { list -> list.map { it.toModel() } }

    fun observeUsers(): Flow<List<LocalUser>> =
        dao.observeUsers().map { list -> list.map { it.toModel() } }

    fun observeAuditLogs(): Flow<List<AuditLogEntry>> =
        dao.observeAuditLogs().map { list -> list.map { it.toModel() } }

    fun observeUploadQueue(): Flow<List<UploadQueueItem>> =
        dao.observeUploadQueue().map { list -> list.map { it.toModel() } }

    // -------------------------------------------------------------------------
    // Initial Non-Destructive Known History Seed
    // -------------------------------------------------------------------------

    /**
     * Seeds the user's known GM HASMANI Committee + Turns 1..4 ONLY when zero committees exist
     * on device, ensuring existing user records are never touched or overwritten.
     */
    suspend fun ensureKnownInitialCommitteeIfEmpty(): String? {
        val existing = dao.getAllCommitteesOnce()
        if (existing.isNotEmpty()) return null

        val now = System.currentTimeMillis()
        val cid = KNOWN_MAIN_COMMITTEE_ID
        val startMonth = "2026-08"
        val totalMonths = 10L
        val endMonth = MonthUtils.addMonths(startMonth, (totalMonths - 1).toInt())

        val committee = CommitteeEntity(
            id = cid,
            name = "GM Hasmani Committee",
            memberCount = 7L,
            totalShares = 10L,
            monthlyInstallment = 5000L,
            totalMonths = totalMonths,
            collectionDayStart = 28L,
            collectionDayEnd = 8L,
            payoutDay = 10L,
            startMonth = startMonth,
            endMonth = endMonth,
            status = CommitteeStatus.ACTIVE.name,
            notes = "7 Members • 10 Shares • Rs. 5,000/share • 28th to 8th collection",
            createdAtMillis = now,
            updatedAtMillis = now
        )

        val knownTurns = buildKnownTurnsEntities(cid, now)
        db.withTransaction {
            dao.insertCommittee(committee)
            dao.insertTurns(knownTurns)
            insertAuditInternal(
                "System",
                "INITIAL_HISTORY_SEED",
                "COMMITTEE",
                committee.name,
                "Loaded known committee turns 1–4 (dates kept Unknown where not provided)."
            )
        }
        return cid
    }

    suspend fun loadKnownHistoryTurnsForCommittee(committeeId: String, actorName: String = "Admin"): Result<Int> {
        return try {
            val existingTurns = dao.getTurnsOnce(committeeId)
            val existingNumbers = existingTurns.map { it.turnNumber }.toSet()
            val now = System.currentTimeMillis()
            val candidates = buildKnownTurnsEntities(committeeId, now).filter { it.turnNumber !in existingNumbers }
            if (candidates.isEmpty()) {
                return Result.success(0)
            }
            db.withTransaction {
                dao.insertTurns(candidates)
                insertAuditInternal(
                    actorName,
                    "LOAD_KNOWN_TURNS",
                    "TURN",
                    committeeId,
                    "Added ${candidates.size} known turn record(s) (Ghulam Mustafa, Abdul Aleem x2, Upcoming)."
                )
            }
            Result.success(candidates.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildKnownTurnsEntities(committeeId: String, now: Long): List<CommitteeTurnEntity> {
        return listOf(
            CommitteeTurnEntity(
                id = "${committeeId}_turn_1",
                committeeId = committeeId,
                turnNumber = 1L,
                recipientMemberId = null,
                recipientName = "Ghulam Mustafa",
                recipientPhone = null,
                payoutAmount = 50000L,
                scheduledMonth = "2026-08",
                payoutDate = null,
                status = PayoutStatus.PAID.name,
                confirmedByAdmin = true,
                notes = "First Committee (Date unknown — editable)",
                createdAtMillis = now,
                updatedAtMillis = now
            ),
            CommitteeTurnEntity(
                id = "${committeeId}_turn_2",
                committeeId = committeeId,
                turnNumber = 2L,
                recipientMemberId = null,
                recipientName = "Abdul Aleem",
                recipientPhone = null,
                payoutAmount = 50000L,
                scheduledMonth = "2026-09",
                payoutDate = null,
                status = PayoutStatus.PAID.name,
                confirmedByAdmin = true,
                notes = "Second Committee (Date unknown — editable)",
                createdAtMillis = now,
                updatedAtMillis = now
            ),
            CommitteeTurnEntity(
                id = "${committeeId}_turn_3",
                committeeId = committeeId,
                turnNumber = 3L,
                recipientMemberId = null,
                recipientName = "Abdul Aleem",
                recipientPhone = null,
                payoutAmount = 50000L,
                scheduledMonth = "2026-10",
                payoutDate = null,
                status = PayoutStatus.PAID.name,
                confirmedByAdmin = true,
                notes = "Third Committee (Date unknown — editable)",
                createdAtMillis = now,
                updatedAtMillis = now
            ),
            CommitteeTurnEntity(
                id = "${committeeId}_turn_4",
                committeeId = committeeId,
                turnNumber = 4L,
                recipientMemberId = null,
                recipientName = "Upcoming",
                recipientPhone = null,
                payoutAmount = 50000L,
                scheduledMonth = "2026-11",
                payoutDate = null,
                status = PayoutStatus.PENDING.name,
                confirmedByAdmin = false,
                notes = "Fourth Committee: 28 November 2026 to 8 December 2026 (Pending Admin Confirmation)",
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
    }

    // -------------------------------------------------------------------------
    // Local User Management & Admin Verification
    // -------------------------------------------------------------------------

    suspend fun ensureDefaultAdmin(): LocalUser {
        val existing = dao.getAllUsersOnce()
        val admin = existing.firstOrNull { it.role == UserRole.ADMIN.name && it.isActive }
        if (admin != null) return admin.toModel()

        val now = System.currentTimeMillis()
        val defaultAdmin = LocalUserEntity(
            id = DEFAULT_ADMIN_ID,
            username = "admin",
            displayName = "GM Hasmani Admin",
            passwordHash = PasswordHasher.sha256(DEFAULT_ADMIN_PIN),
            role = UserRole.ADMIN.name,
            isActive = true,
            createdAtMillis = now
        )
        dao.insertUser(defaultAdmin)
        return defaultAdmin.toModel()
    }

    suspend fun getUserById(userId: String): LocalUser? =
        dao.getUserByIdOnce(userId)?.toModel()

    suspend fun authenticateUser(username: String, password: String): Result<LocalUser> {
        return try {
            val cleanUser = username.trim()
            require(cleanUser.isNotEmpty()) { "Username is required" }
            require(password.isNotEmpty()) { "Password / PIN is required" }
            val user = dao.getUserByUsernameOnce(cleanUser)
                ?: return Result.failure(IllegalArgumentException("User not found"))
            if (!user.isActive) {
                return Result.failure(IllegalStateException("This user account is disabled"))
            }
            if (user.passwordHash != PasswordHasher.sha256(password)) {
                return Result.failure(IllegalArgumentException("Incorrect password / PIN"))
            }
            recordAudit(user.displayName, "LOGIN", "USER", user.username, "Local sign-in successful")
            Result.success(user.toModel())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyAdminAccess(currentUser: LocalUser?, adminPassword: String): Result<LocalUser> {
        return try {
            if (currentUser == null || !currentUser.isAdmin || !currentUser.isActive) {
                return Result.failure(SecurityException("Only authorized Admin users can delete records."))
            }
            if (adminPassword.isBlank()) {
                return Result.failure(IllegalArgumentException("Enter Admin Password / PIN to confirm deletion."))
            }
            val fresh = dao.getUserByIdOnce(currentUser.id)
                ?: return Result.failure(SecurityException("Authorized Admin account not found."))
            if (!fresh.isActive || fresh.role != UserRole.ADMIN.name) {
                return Result.failure(SecurityException("Only authorized Admin users can delete records."))
            }
            if (fresh.passwordHash != PasswordHasher.sha256(adminPassword)) {
                return Result.failure(SecurityException("Incorrect Admin Password / PIN."))
            }
            Result.success(fresh.toModel())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createUser(
        username: String,
        displayName: String,
        password: String,
        role: UserRole,
        actorName: String
    ): Result<LocalUser> {
        return try {
            val cleanUsername = username.trim().lowercase()
            val cleanDisplay = displayName.trim()
            require(cleanUsername.length >= 3) { "Username must be at least 3 characters" }
            require(cleanDisplay.isNotEmpty()) { "Display name is required" }
            require(password.length >= 4) { "Password / PIN must be at least 4 characters" }
            if (dao.getUserByUsernameOnce(cleanUsername) != null) {
                return Result.failure(IllegalStateException("Username \"$cleanUsername\" already exists"))
            }
            val entity = LocalUserEntity(
                id = UUID.randomUUID().toString(),
                username = cleanUsername,
                displayName = cleanDisplay.take(60),
                passwordHash = PasswordHasher.sha256(password),
                role = role.name,
                isActive = true,
                createdAtMillis = System.currentTimeMillis()
            )
            dao.insertUser(entity)
            recordAudit(actorName, "CREATE_USER", "USER", cleanUsername, "Created ${role.name} user $cleanDisplay")
            Result.success(entity.toModel())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRoleAndActive(
        target: LocalUser,
        newRole: UserRole,
        isActive: Boolean,
        actorName: String
    ): Result<Unit> {
        return try {
            val allUsers = dao.getAllUsersOnce()
            val activeAdmins = allUsers.count { it.role == UserRole.ADMIN.name && it.isActive && it.id != target.id }
            if ((newRole != UserRole.ADMIN || !isActive) && target.isAdmin && activeAdmins == 0) {
                return Result.failure(IllegalStateException("At least one active Admin user must remain."))
            }
            dao.updateUser(
                LocalUserEntity(
                    id = target.id,
                    username = target.username,
                    displayName = target.displayName,
                    passwordHash = target.passwordHash,
                    role = newRole.name,
                    isActive = isActive,
                    createdAtMillis = target.createdAtMillis
                )
            )
            recordAudit(actorName, "UPDATE_USER", "USER", target.username, "Role=${newRole.name}, Active=$isActive")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUser(target: LocalUser, currentUserId: String?, actorName: String): Result<Unit> {
        return try {
            if (target.id == currentUserId) {
                return Result.failure(IllegalStateException("Cannot delete the currently logged-in user."))
            }
            val allUsers = dao.getAllUsersOnce()
            val otherActiveAdmins = allUsers.count { it.role == UserRole.ADMIN.name && it.isActive && it.id != target.id }
            if (target.isAdmin && otherActiveAdmins == 0) {
                return Result.failure(IllegalStateException("Cannot delete the last Admin account."))
            }
            dao.deleteUserById(target.id)
            recordAudit(actorName, "DELETE_USER", "USER", target.username, "Removed local user account")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changePassword(
        userId: String,
        currentPassword: String,
        newPassword: String
    ): Result<Unit> {
        return try {
            require(newPassword.length >= 4) { "New password / PIN must be at least 4 characters" }
            val user = dao.getUserByIdOnce(userId)
                ?: return Result.failure(IllegalStateException("User not found"))
            if (user.passwordHash != PasswordHasher.sha256(currentPassword)) {
                return Result.failure(IllegalArgumentException("Current password / PIN is incorrect"))
            }
            dao.updateUser(user.copy(passwordHash = PasswordHasher.sha256(newPassword)))
            recordAudit(user.displayName, "CHANGE_PASSWORD", "USER", user.username, "Updated local password/PIN")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Committee CRUD + Financial-Protected Delete
    // -------------------------------------------------------------------------

    suspend fun createCommittee(
        name: String,
        memberCount: Long,
        totalShares: Long,
        monthlyInstallment: Long,
        totalMonths: Long,
        collectionDayStart: Long,
        collectionDayEnd: Long,
        payoutDay: Long,
        startMonth: String,
        notes: String?,
        actorName: String = "Admin"
    ): Result<String> {
        return try {
            require(name.trim().isNotEmpty()) { "Committee name is required" }
            require(memberCount in 2L..200L) { "Member count must be between 2 and 200" }
            val safeShares = totalShares.coerceIn(memberCount, 500L)
            require(monthlyInstallment in 100L..10_000_000L) { "Share amount must be between 100 and 10,000,000" }
            require(totalMonths in 2L..200L) { "Duration must be between 2 and 200 months" }
            require(MonthUtils.isValidMonthKey(startMonth)) { "Start month must be YYYY-MM" }

            val now = System.currentTimeMillis()
            val id = UUID.randomUUID().toString()
            val endMonth = MonthUtils.addMonths(startMonth, (totalMonths - 1).toInt())
            val cleanName = name.trim().take(80)
            val entity = CommitteeEntity(
                id = id,
                name = cleanName,
                memberCount = memberCount,
                totalShares = safeShares,
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
            db.withTransaction {
                dao.insertCommittee(entity)
                insertAuditInternal(
                    actorName, "CREATE_COMMITTEE", "COMMITTEE", cleanName,
                    "$memberCount members • $safeShares shares • PKR $monthlyInstallment/share • $totalMonths months ($startMonth)"
                )
                enqueueUploadInternal(
                    title = "Committee Created: $cleanName",
                    payloadType = "COMMITTEE",
                    payload = JSONObject().apply {
                        put("id", id)
                        put("name", cleanName)
                        put("memberCount", memberCount)
                        put("totalShares", safeShares)
                        put("monthlyInstallment", monthlyInstallment)
                        put("totalMonths", totalMonths)
                        put("startMonth", startMonth)
                    }
                )
            }
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
        newTotalShares: Long,
        newMonthlyInstallment: Long,
        newTotalMonths: Long,
        newCollectionDayStart: Long,
        newCollectionDayEnd: Long,
        newPayoutDay: Long,
        newStartMonth: String,
        newNotes: String?,
        actorName: String = "Admin"
    ): Result<Unit> {
        return try {
            require(existing.id.isNotBlank())
            require(newName.trim().isNotEmpty())
            require(newMemberCount in 2L..200L)
            val safeShares = newTotalShares.coerceIn(newMemberCount, 500L)
            require(newMonthlyInstallment in 100L..10_000_000L)
            require(newTotalMonths in 2L..200L)
            require(MonthUtils.isValidMonthKey(newStartMonth))

            val now = System.currentTimeMillis()
            val endMonth = MonthUtils.addMonths(newStartMonth, (newTotalMonths - 1).toInt())
            val newPayoutAmount = safeShares * newMonthlyInstallment
            val cleanName = newName.trim().take(80)

            db.withTransaction {
                dao.updateCommittee(
                    CommitteeEntity(
                        id = existing.id,
                        name = cleanName,
                        memberCount = newMemberCount,
                        totalShares = safeShares,
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
                val memberMap = members.associateBy { it.id }
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

                // Update pending turns payout amount if unpaid
                dao.getTurnsOnce(existing.id).forEach { turn ->
                    if (PayoutStatus.fromRaw(turn.status) == PayoutStatus.PENDING) {
                        val turnMonth = MonthUtils.addMonths(newStartMonth, (turn.turnNumber - 1).toInt().coerceAtLeast(0))
                        dao.updateTurn(
                            turn.copy(
                                payoutAmount = newPayoutAmount,
                                scheduledMonth = turnMonth,
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
                            val memberShares = memberMap[inst.memberId]?.toModel()?.effectiveShares ?: 1.0
                            val recalculatedDue = kotlin.math.round(memberShares * newMonthlyInstallment).toLong().coerceAtLeast(100L)
                            dao.updateInstallment(
                                inst.copy(
                                    dueAmount = recalculatedDue,
                                    updatedAtMillis = now
                                )
                            )
                        }
                    }
                }

                insertAuditInternal(
                    actorName, "EDIT_COMMITTEE", "COMMITTEE", cleanName,
                    "Updated: $newMemberCount members, $safeShares shares, PKR $newMonthlyInstallment/share, $newTotalMonths months"
                )
                enqueueUploadInternal(
                    title = "Committee Updated: $cleanName",
                    payloadType = "COMMITTEE_UPDATE",
                    payload = JSONObject().apply {
                        put("id", existing.id)
                        put("name", cleanName)
                        put("memberCount", newMemberCount)
                        put("totalShares", safeShares)
                        put("monthlyInstallment", newMonthlyInstallment)
                    }
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCommitteeStatus(
        committeeId: String,
        status: CommitteeStatus,
        actorName: String = "Admin"
    ): Result<Unit> {
        return try {
            val committee = dao.getCommitteeByIdOnce(committeeId)
            db.withTransaction {
                dao.updateCommitteeStatus(committeeId, status.name, System.currentTimeMillis())
                insertAuditInternal(
                    actorName, "COMMITTEE_STATUS", "COMMITTEE", committee?.name ?: committeeId,
                    "Changed status to ${status.name}"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteCommitteeSafely(
        committee: Committee,
        currentUser: LocalUser?,
        adminPassword: String
    ): Result<Unit> {
        val authRes = verifyAdminAccess(currentUser, adminPassword)
        if (authRes.isFailure) return Result.failure(authRes.exceptionOrNull()!!)
        val admin = authRes.getOrThrow()

        return try {
            val recordedPayments = dao.countRecordedPaymentsForCommittee(committee.id)
            val paidPayouts = dao.countPaidPayoutsForCommittee(committee.id) + dao.countPaidTurnsForCommittee(committee.id)
            if (recordedPayments > 0 || paidPayouts > 0) {
                return Result.failure(
                    IllegalStateException(
                        "Financial protection: \"${committee.name}\" has $recordedPayments recorded payment(s) and $paidPayouts payout(s). " +
                            "Use Archive Committee to preserve history, or reverse payments first."
                    )
                )
            }
            db.withTransaction {
                dao.deleteCommitteeById(committee.id)
                insertAuditInternal(
                    admin.displayName,
                    "DELETE_COMMITTEE",
                    "COMMITTEE",
                    committee.name,
                    "Deleted empty/unpaid committee (${committee.memberCount} members, ${committee.startMonth})"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Member CRUD (With Shares) + Safe Delete
    // -------------------------------------------------------------------------

    suspend fun addMember(
        committee: Committee,
        name: String,
        phone: String,
        committeeNumber: Long,
        shares: Double = 1.0,
        actorName: String = "Admin"
    ): Result<String> {
        return try {
            require(name.trim().isNotEmpty()) { "Member name is required" }
            require(phone.trim().length >= 5) { "Valid mobile number is required" }
            require(committeeNumber in 1L..500L) { "Committee number must be >= 1" }
            val safeShares = shares.coerceIn(0.5, 50.0)

            val now = System.currentTimeMillis()
            val memberId = UUID.randomUUID().toString()
            val cleanName = name.trim().take(80)
            val cleanPhone = phone.trim().take(25)
            val payoutMonth = MonthUtils.addMonths(committee.startMonth, (committeeNumber - 1).toInt().coerceAtLeast(0))
            val entity = MemberEntity(
                id = memberId,
                committeeId = committee.id,
                name = cleanName,
                phone = cleanPhone,
                committeeNumber = committeeNumber,
                shares = safeShares,
                payoutMonth = payoutMonth,
                payoutStatus = PayoutStatus.PENDING.name,
                payoutDate = null,
                payoutAmount = committee.eachPayoutAmount,
                status = MemberStatus.ACTIVE.name,
                createdAtMillis = now,
                updatedAtMillis = now
            )
            db.withTransaction {
                dao.insertMember(entity)
                insertAuditInternal(
                    actorName, "ADD_MEMBER", "MEMBER", cleanName,
                    "Added #$committeeNumber ($cleanPhone) • $safeShares share(s) to ${committee.name}"
                )
                enqueueUploadInternal(
                    title = "Member Added: $cleanName (#$committeeNumber)",
                    payloadType = "MEMBER",
                    payload = JSONObject().apply {
                        put("id", memberId)
                        put("committeeId", committee.id)
                        put("name", cleanName)
                        put("phone", cleanPhone)
                        put("committeeNumber", committeeNumber)
                        put("shares", safeShares)
                    }
                )
            }
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
        newCommitteeNumber: Long,
        newShares: Double = member.effectiveShares,
        actorName: String = "Admin"
    ): Result<Unit> {
        return try {
            require(newName.trim().isNotEmpty())
            require(newPhone.trim().length >= 5)
            require(newCommitteeNumber in 1L..500L)
            val safeShares = newShares.coerceIn(0.5, 50.0)

            val now = System.currentTimeMillis()
            val cleanName = newName.trim().take(80)
            val cleanPhone = newPhone.trim().take(25)
            val newPayoutMonth = MonthUtils.addMonths(committee.startMonth, (newCommitteeNumber - 1).toInt().coerceAtLeast(0))
            val updatedMemberModel = member.copy(
                name = cleanName,
                phone = cleanPhone,
                committeeNumber = newCommitteeNumber,
                shares = safeShares,
                payoutMonth = newPayoutMonth
            )

            db.withTransaction {
                dao.updateMember(
                    MemberEntity(
                        id = member.id,
                        committeeId = committee.id,
                        name = cleanName,
                        phone = cleanPhone,
                        committeeNumber = newCommitteeNumber,
                        shares = safeShares,
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
                // Recalculate unpaid installment due amounts if shares changed
                if (kotlin.math.abs(safeShares - member.effectiveShares) > 0.001) {
                    val newDue = updatedMemberModel.monthlyDue(committee.monthlyInstallment)
                    dao.getInstallmentsOnce(committee.id)
                        .filter { it.memberId == member.id && it.paidAmount == 0L && InstallmentStatus.fromRaw(it.status) == InstallmentStatus.UNPAID }
                        .forEach { inst ->
                            dao.updateInstallment(inst.copy(dueAmount = newDue, updatedAtMillis = now))
                        }
                }
                insertAuditInternal(
                    actorName, "EDIT_MEMBER", "MEMBER", cleanName,
                    "Updated #$newCommitteeNumber ($cleanPhone) • $safeShares share(s) in ${committee.name}"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun archiveMember(committeeId: String, member: CommitteeMember, actorName: String = "Admin"): Result<Unit> {
        return try {
            db.withTransaction {
                dao.updateMemberStatus(member.id, MemberStatus.ARCHIVED.name, System.currentTimeMillis())
                insertAuditInternal(
                    actorName, "ARCHIVE_MEMBER", "MEMBER", member.name,
                    "Archived member #${member.committeeNumber} (history preserved)"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMemberSafely(
        member: CommitteeMember,
        currentUser: LocalUser?,
        adminPassword: String
    ): Result<Unit> {
        val authRes = verifyAdminAccess(currentUser, adminPassword)
        if (authRes.isFailure) return Result.failure(authRes.exceptionOrNull()!!)
        val admin = authRes.getOrThrow()

        return try {
            val paidInstallments = dao.countRecordedPaymentsForMember(member.id)
            if (paidInstallments > 0 || member.payoutStatusEnum == PayoutStatus.PAID) {
                return Result.failure(
                    IllegalStateException(
                        "Financial protection: ${member.name} has recorded payment/payout history ($paidInstallments payment(s)). " +
                            "Use Archive Member to keep records intact, or reverse payments first."
                    )
                )
            }
            db.withTransaction {
                dao.deleteUnpaidInstallmentsForMember(member.id)
                dao.deleteMemberById(member.id)
                insertAuditInternal(
                    admin.displayName,
                    "DELETE_MEMBER",
                    "MEMBER",
                    member.name,
                    "Deleted unpaid member #${member.committeeNumber} (${member.phone})"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Committee Turns (Separate Payout Slots Supporting Multi-Share Members)
    // -------------------------------------------------------------------------

    suspend fun saveCommitteeTurn(
        committee: Committee,
        existingTurn: CommitteeTurn?,
        turnNumber: Long,
        recipientMemberId: String?,
        recipientName: String,
        recipientPhone: String?,
        payoutAmount: Long,
        scheduledMonth: String,
        payoutDate: String?,
        status: PayoutStatus,
        confirmedByAdmin: Boolean,
        notes: String?,
        actorName: String = "Admin"
    ): Result<String> {
        return try {
            require(recipientName.trim().isNotEmpty()) { "Recipient name is required" }
            require(turnNumber in 1L..500L) { "Turn number must be between 1 and 500" }
            require(payoutAmount >= 0L) { "Payout amount cannot be negative" }
            val safeMonth = if (MonthUtils.isValidMonthKey(scheduledMonth.trim())) scheduledMonth.trim()
            else MonthUtils.addMonths(committee.startMonth, (turnNumber - 1).toInt().coerceAtLeast(0))

            val now = System.currentTimeMillis()
            val turnId = existingTurn?.id ?: UUID.randomUUID().toString()
            val cleanName = recipientName.trim().take(80)
            val cleanDate = payoutDate?.trim()?.take(30)?.takeIf { it.isNotEmpty() && !it.equals("Unknown", ignoreCase = true) }

            val entity = CommitteeTurnEntity(
                id = turnId,
                committeeId = committee.id,
                turnNumber = turnNumber,
                recipientMemberId = recipientMemberId?.takeIf { it.isNotBlank() },
                recipientName = cleanName,
                recipientPhone = recipientPhone?.trim()?.take(25)?.takeIf { it.isNotEmpty() },
                payoutAmount = payoutAmount,
                scheduledMonth = safeMonth,
                payoutDate = cleanDate,
                status = status.name,
                confirmedByAdmin = if (status == PayoutStatus.PAID) confirmedByAdmin else false,
                notes = notes?.trim()?.take(300)?.takeIf { it.isNotEmpty() },
                createdAtMillis = existingTurn?.createdAtMillis ?: now,
                updatedAtMillis = now
            )

            db.withTransaction {
                dao.insertTurn(entity)
                insertAuditInternal(
                    actorName,
                    if (existingTurn == null) "ADD_TURN" else "EDIT_TURN",
                    "TURN",
                    "Turn #$turnNumber • $cleanName",
                    "${status.name} • PKR $payoutAmount • Month: $safeMonth" + (cleanDate?.let { " • Date: $it" } ?: "")
                )
                enqueueUploadInternal(
                    title = "Turn #$turnNumber ${status.name}: $cleanName",
                    payloadType = "COMMITTEE_TURN",
                    payload = JSONObject().apply {
                        put("id", turnId)
                        put("committeeId", committee.id)
                        put("turnNumber", turnNumber)
                        put("recipientName", cleanName)
                        put("payoutAmount", payoutAmount)
                        put("status", status.name)
                    }
                )
            }
            Result.success(turnId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun swapTurnOrder(
        committee: Committee,
        first: CommitteeTurn,
        second: CommitteeTurn,
        actorName: String = "Admin"
    ): Result<Unit> {
        return try {
            val now = System.currentTimeMillis()
            val firstMonth = MonthUtils.addMonths(committee.startMonth, (second.turnNumber - 1).toInt().coerceAtLeast(0))
            val secondMonth = MonthUtils.addMonths(committee.startMonth, (first.turnNumber - 1).toInt().coerceAtLeast(0))
            db.withTransaction {
                dao.updateTurn(
                    CommitteeTurnEntity(
                        id = first.id,
                        committeeId = committee.id,
                        turnNumber = second.turnNumber,
                        recipientMemberId = first.recipientMemberId,
                        recipientName = first.recipientName,
                        recipientPhone = first.recipientPhone,
                        payoutAmount = first.payoutAmount,
                        scheduledMonth = firstMonth,
                        payoutDate = first.payoutDate,
                        status = first.status,
                        confirmedByAdmin = first.confirmedByAdmin,
                        notes = first.notes,
                        createdAtMillis = first.createdAtMillis,
                        updatedAtMillis = now
                    )
                )
                dao.updateTurn(
                    CommitteeTurnEntity(
                        id = second.id,
                        committeeId = committee.id,
                        turnNumber = first.turnNumber,
                        recipientMemberId = second.recipientMemberId,
                        recipientName = second.recipientName,
                        recipientPhone = second.recipientPhone,
                        payoutAmount = second.payoutAmount,
                        scheduledMonth = secondMonth,
                        payoutDate = second.payoutDate,
                        status = second.status,
                        confirmedByAdmin = second.confirmedByAdmin,
                        notes = second.notes,
                        createdAtMillis = second.createdAtMillis,
                        updatedAtMillis = now
                    )
                )
                insertAuditInternal(
                    actorName,
                    "REORDER_TURN",
                    "TURN",
                    "Turn #${first.turnNumber} ↔ Turn #${second.turnNumber}",
                    "Swapped ${first.recipientName} and ${second.recipientName}"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateTurnsFromMembers(
        committee: Committee,
        members: List<CommitteeMember>,
        actorName: String = "Admin"
    ): Result<Int> {
        return try {
            val activeMembers = members.filter { it.memberStatusEnum == MemberStatus.ACTIVE }.sortedBy { it.committeeNumber }
            if (activeMembers.isEmpty()) {
                return Result.failure(IllegalStateException("Add members first before generating turns."))
            }
            val existing = dao.getTurnsOnce(committee.id)
            val existingNumbers = existing.map { it.turnNumber }.toMutableSet()
            val now = System.currentTimeMillis()
            val newTurns = mutableListOf<CommitteeTurnEntity>()
            var nextTurnNo = 1L

            activeMembers.forEach { member ->
                val shareSlots = member.effectiveShares.roundToInt().coerceIn(1, 20)
                repeat(shareSlots) {
                    while (nextTurnNo in existingNumbers) {
                        nextTurnNo++
                    }
                    val monthKey = MonthUtils.addMonths(committee.startMonth, (nextTurnNo - 1).toInt().coerceAtLeast(0))
                    newTurns.add(
                        CommitteeTurnEntity(
                            id = UUID.randomUUID().toString(),
                            committeeId = committee.id,
                            turnNumber = nextTurnNo,
                            recipientMemberId = member.id,
                            recipientName = member.name,
                            recipientPhone = member.phone,
                            payoutAmount = committee.eachPayoutAmount,
                            scheduledMonth = monthKey,
                            payoutDate = null,
                            status = PayoutStatus.PENDING.name,
                            confirmedByAdmin = false,
                            notes = if (shareSlots > 1) "Share slot ${it + 1}/$shareSlots" else null,
                            createdAtMillis = now,
                            updatedAtMillis = now
                        )
                    )
                    existingNumbers.add(nextTurnNo)
                    nextTurnNo++
                }
            }

            if (newTurns.isEmpty()) return Result.success(0)
            db.withTransaction {
                dao.insertTurns(newTurns)
                insertAuditInternal(
                    actorName,
                    "GENERATE_TURNS",
                    "TURN",
                    committee.name,
                    "Generated ${newTurns.size} payout turn(s) based on member shares."
                )
            }
            Result.success(newTurns.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTurnSafely(
        turn: CommitteeTurn,
        currentUser: LocalUser?,
        adminPassword: String
    ): Result<Unit> {
        val authRes = verifyAdminAccess(currentUser, adminPassword)
        if (authRes.isFailure) return Result.failure(authRes.exceptionOrNull()!!)
        val admin = authRes.getOrThrow()

        return try {
            if (turn.statusEnum == PayoutStatus.PAID) {
                return Result.failure(
                    IllegalStateException("Financial protection: Turn #${turn.turnNumber} is PAID. Set status to PENDING before deleting.")
                )
            }
            db.withTransaction {
                dao.deleteTurnById(turn.id)
                insertAuditInternal(
                    admin.displayName,
                    "DELETE_TURN",
                    "TURN",
                    "Turn #${turn.turnNumber} • ${turn.recipientName}",
                    "Deleted pending payout turn"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Old Committee Records / History (Isolated Historical Archive + CSV Import)
    // -------------------------------------------------------------------------

    suspend fun saveOldCommitteeRecord(
        record: OldCommitteeRecord,
        actorName: String = "Admin"
    ): Result<String> {
        return try {
            require(record.committeeName.trim().isNotEmpty() || record.committeeNumber.trim().isNotEmpty()) {
                "Enter Committee Number or Committee Name"
            }
            val now = System.currentTimeMillis()
            val id = record.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
            val entity = OldCommitteeRecordEntity(
                id = id,
                committeeNumber = record.committeeNumber.trim().take(40),
                committeeName = record.committeeName.trim().take(100),
                startDate = record.startDate.trim().take(40),
                endDate = record.endDate.trim().take(40),
                memberNames = record.memberNames.trim().take(1500),
                contactNumbers = record.contactNumbers.trim().take(1000),
                numberOfShares = record.numberOfShares.trim().take(40),
                monthlyContributions = record.monthlyContributions.trim().take(80),
                totalCollection = record.totalCollection.trim().take(80),
                paymentHistory = record.paymentHistory.trim().take(1500),
                payoutRecipient = record.payoutRecipient.trim().take(100),
                payoutAmount = record.payoutAmount.trim().take(60),
                payoutDate = record.payoutDate.trim().take(40),
                status = record.status.trim().ifEmpty { CommitteeStatus.COMPLETED.name },
                createdAtMillis = if (record.id.isNotBlank()) record.createdAtMillis else now,
                updatedAtMillis = now
            )
            db.withTransaction {
                dao.insertOldRecord(entity)
                insertAuditInternal(
                    actorName,
                    if (record.id.isBlank()) "ADD_OLD_RECORD" else "EDIT_OLD_RECORD",
                    "OLD_HISTORY",
                    "${entity.committeeNumber} ${entity.committeeName}".trim(),
                    "Recipient: ${entity.payoutRecipient.ifBlank { "Unknown" }} • Payout: ${entity.payoutAmount.ifBlank { "-" }}"
                )
            }
            Result.success(id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importOldRecordsFromCsv(csvText: String, actorName: String = "Admin"): Result<Int> {
        return try {
            val parsed = OldHistoryCsvUtils.parseCsvToRecords(csvText)
            if (parsed.isEmpty()) {
                return Result.failure(IllegalArgumentException("No valid records found in CSV file."))
            }
            val now = System.currentTimeMillis()
            val entities = parsed.mapIndexed { idx, r ->
                OldCommitteeRecordEntity(
                    id = UUID.randomUUID().toString(),
                    committeeNumber = r.committeeNumber,
                    committeeName = r.committeeName,
                    startDate = r.startDate,
                    endDate = r.endDate,
                    memberNames = r.memberNames,
                    contactNumbers = r.contactNumbers,
                    numberOfShares = r.numberOfShares,
                    monthlyContributions = r.monthlyContributions,
                    totalCollection = r.totalCollection,
                    paymentHistory = r.paymentHistory,
                    payoutRecipient = r.payoutRecipient,
                    payoutAmount = r.payoutAmount,
                    payoutDate = r.payoutDate,
                    status = r.status,
                    createdAtMillis = now + idx,
                    updatedAtMillis = now + idx
                )
            }
            db.withTransaction {
                dao.insertOldRecords(entities)
                insertAuditInternal(
                    actorName,
                    "IMPORT_OLD_CSV",
                    "OLD_HISTORY",
                    "${entities.size} record(s)",
                    "Imported historical committee records from CSV/Excel."
                )
            }
            Result.success(entities.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteOldCommitteeRecord(
        record: OldCommitteeRecord,
        currentUser: LocalUser?,
        adminPassword: String
    ): Result<Unit> {
        val authRes = verifyAdminAccess(currentUser, adminPassword)
        if (authRes.isFailure) return Result.failure(authRes.exceptionOrNull()!!)
        val admin = authRes.getOrThrow()

        return try {
            db.withTransaction {
                dao.deleteOldRecordById(record.id)
                insertAuditInternal(
                    admin.displayName,
                    "DELETE_OLD_RECORD",
                    "OLD_HISTORY",
                    "${record.committeeNumber} ${record.committeeName}".trim(),
                    "Removed old history record"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Legacy Member Payouts & Installments + Safe Delete / Reversal
    // -------------------------------------------------------------------------

    suspend fun swapMemberPayoutOrder(
        committee: Committee,
        first: CommitteeMember,
        second: CommitteeMember,
        actorName: String = "Admin"
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
                        shares = first.effectiveShares,
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
                        shares = second.effectiveShares,
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
                insertAuditInternal(
                    actorName, "REORDER_PAYOUT", "MEMBER", "${first.name} ↔ ${second.name}",
                    "Swapped committee numbers #${first.committeeNumber} and #${second.committeeNumber}"
                )
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
        payoutAmount: Long? = null,
        actorName: String = "Admin"
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
            db.withTransaction {
                dao.updateMember(
                    MemberEntity(
                        id = member.id,
                        committeeId = committee.id,
                        name = member.name,
                        phone = member.phone,
                        committeeNumber = member.committeeNumber,
                        shares = member.effectiveShares,
                        payoutMonth = member.payoutMonth,
                        payoutStatus = payoutStatus.name,
                        payoutDate = effectiveDate,
                        payoutAmount = effectiveAmount.coerceIn(0L, 500_000_000L),
                        status = member.status,
                        createdAtMillis = member.createdAtMillis,
                        updatedAtMillis = now
                    )
                )
                insertAuditInternal(
                    actorName,
                    if (payoutStatus == PayoutStatus.PAID) "PAYOUT_PAID" else "PAYOUT_REVERSED",
                    "PAYOUT",
                    member.name,
                    "${MonthUtils.formatMonthDisplay(member.payoutMonth)} • PKR $effectiveAmount • ${payoutStatus.name}"
                )
                enqueueUploadInternal(
                    title = "Payout ${payoutStatus.name}: ${member.name}",
                    payloadType = "PAYOUT",
                    payload = JSONObject().apply {
                        put("committeeId", committee.id)
                        put("memberId", member.id)
                        put("memberName", member.name)
                        put("payoutStatus", payoutStatus.name)
                        put("payoutAmount", effectiveAmount)
                        put("payoutDate", effectiveDate ?: "")
                    }
                )
            }
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
                val due = member.monthlyDue(committee.monthlyInstallment).coerceAtLeast(100L)
                InstallmentEntity(
                    id = safeId,
                    committeeId = committee.id,
                    memberId = member.id,
                    memberName = member.name,
                    memberPhone = member.phone,
                    committeeNumber = member.committeeNumber,
                    monthKey = monthKey,
                    monthIndex = monthIndex.toLong().coerceAtLeast(1L),
                    dueAmount = due,
                    paidAmount = 0L,
                    status = InstallmentStatus.UNPAID.name,
                    paymentDate = null,
                    paymentNote = null,
                    paymentMethod = PaymentMethod.CASH.name,
                    transactionId = null,
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

    suspend fun recordOrAddManualPayment(
        committee: Committee,
        member: CommitteeMember,
        monthKey: String,
        monthIndex: Long,
        paidAmount: Long,
        status: InstallmentStatus,
        paymentDate: String?,
        paymentMethod: PaymentMethod,
        transactionId: String?,
        paymentNote: String?,
        actorName: String = "Admin"
    ): Result<Unit> {
        val existing = dao.findInstallmentOnce(committee.id, member.id, monthKey)?.toModel()
            ?: InstallmentRecord(
                id = "${committee.id}_${member.id}_$monthKey".take(128),
                committeeId = committee.id,
                memberId = member.id,
                memberName = member.name,
                memberPhone = member.phone,
                committeeNumber = member.committeeNumber,
                monthKey = monthKey,
                monthIndex = monthIndex.coerceAtLeast(1L),
                dueAmount = member.monthlyDue(committee.monthlyInstallment).coerceAtLeast(100L),
                paidAmount = 0L,
                status = InstallmentStatus.UNPAID.name
            )
        return recordInstallmentPayment(
            committeeId = committee.id,
            record = existing,
            newPaidAmount = paidAmount,
            newStatus = status,
            paymentDate = paymentDate,
            paymentMethod = paymentMethod,
            transactionId = transactionId,
            paymentNote = paymentNote,
            actorName = actorName,
            upsert = true
        )
    }

    suspend fun recordInstallmentPayment(
        committeeId: String,
        record: InstallmentRecord,
        newPaidAmount: Long,
        newStatus: InstallmentStatus,
        paymentDate: String?,
        paymentNote: String?,
        paymentMethod: PaymentMethod = record.paymentMethodEnum,
        transactionId: String? = record.transactionId,
        actorName: String = "Admin",
        upsert: Boolean = false
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
            val cleanTxId = transactionId?.trim()?.take(80)?.takeIf { it.isNotEmpty() }

            val entity = InstallmentEntity(
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
                paymentMethod = paymentMethod.name,
                transactionId = cleanTxId,
                createdAtMillis = record.createdAtMillis,
                updatedAtMillis = now
            )

            db.withTransaction {
                if (upsert) {
                    dao.insertInstallment(entity)
                } else {
                    dao.updateInstallment(entity)
                }
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
                        paymentMethod = paymentMethod.name,
                        transactionId = cleanTxId,
                        actionNote = cleanNote,
                        createdAtMillis = now
                    )
                )
                insertAuditInternal(
                    actorName,
                    if (newStatus == InstallmentStatus.UNPAID && record.paidAmount > 0L) "PAYMENT_REVERSED" else "PAYMENT_SAVED",
                    "INSTALLMENT",
                    "${record.memberName} (${record.monthKey})",
                    "${record.status} (${record.paidAmount}) → ${newStatus.name} ($clampedPaid) via ${paymentMethod.label}" +
                        (cleanTxId?.let { " • Tx: $it" } ?: "") +
                        (cleanNote?.let { " • Note: $it" } ?: "")
                )
                enqueueUploadInternal(
                    title = "Payment ${newStatus.name}: ${record.memberName} (${record.monthKey})",
                    payloadType = "INSTALLMENT_PAYMENT",
                    payload = JSONObject().apply {
                        put("installmentId", record.id)
                        put("committeeId", committeeId)
                        put("memberId", record.memberId)
                        put("memberName", record.memberName)
                        put("monthKey", record.monthKey)
                        put("paidAmount", clampedPaid)
                        put("paymentMethod", paymentMethod.name)
                        if (cleanTxId != null) put("transactionId", cleanTxId)
                        put("status", newStatus.name)
                    }
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reverseInstallmentPayment(
        committeeId: String,
        record: InstallmentRecord,
        reversalReason: String,
        actorName: String = "Admin"
    ): Result<Unit> {
        val reason = reversalReason.trim().ifEmpty { "Payment reversed for correction" }
        return recordInstallmentPayment(
            committeeId = committeeId,
            record = record,
            newPaidAmount = 0L,
            newStatus = InstallmentStatus.UNPAID,
            paymentDate = null,
            paymentMethod = record.paymentMethodEnum,
            transactionId = null,
            paymentNote = "REVERSED: $reason",
            actorName = actorName
        )
    }

    suspend fun deleteInstallmentSafely(
        record: InstallmentRecord,
        currentUser: LocalUser?,
        adminPassword: String
    ): Result<Unit> {
        val authRes = verifyAdminAccess(currentUser, adminPassword)
        if (authRes.isFailure) return Result.failure(authRes.exceptionOrNull()!!)
        val admin = authRes.getOrThrow()

        return try {
            if (record.paidAmount > 0L || record.statusEnum != InstallmentStatus.UNPAID) {
                return Result.failure(
                    IllegalStateException(
                        "Cannot silently delete a recorded payment (${record.memberName}). Use Reverse Payment to reset & keep an audit record."
                    )
                )
            }
            db.withTransaction {
                dao.deleteInstallmentById(record.id)
                insertAuditInternal(
                    admin.displayName,
                    "DELETE_INSTALLMENT",
                    "INSTALLMENT",
                    "${record.memberName} (${record.monthKey})",
                    "Removed unpaid installment entry #${record.committeeNumber}"
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteUnpaidMonthEntriesSafely(
        committee: Committee,
        monthKey: String,
        currentUser: LocalUser?,
        adminPassword: String
    ): Result<Pair<Int, Int>> {
        val authRes = verifyAdminAccess(currentUser, adminPassword)
        if (authRes.isFailure) return Result.failure(authRes.exceptionOrNull()!!)
        val admin = authRes.getOrThrow()

        return try {
            val paidCount = dao.countRecordedPaymentsForMonth(committee.id, monthKey)
            var deletedCount = 0
            db.withTransaction {
                deletedCount = dao.deleteUnpaidInstallmentsForMonth(committee.id, monthKey)
                insertAuditInternal(
                    admin.displayName,
                    "DELETE_MONTH_ENTRIES",
                    "MONTH",
                    "${committee.name} ($monthKey)",
                    "Deleted $deletedCount unpaid entries; preserved $paidCount recorded payment(s)."
                )
            }
            Result.success(deletedCount to paidCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------------------
    // Reminder Logs
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Upload Queue (Pending / Completed / Failed / Retry)
    // -------------------------------------------------------------------------

    suspend fun retryPendingAndFailedUploads(syncWebhookUrl: String, actorName: String = "Admin"): UploadBatchSummary =
        withContext(Dispatchers.IO) {
            val items = dao.getRetryableUploadsOnce()
            if (items.isEmpty()) {
                return@withContext UploadBatchSummary(0, 0, 0, "No pending or failed uploads.")
            }

            val url = syncWebhookUrl.trim()
            if (url.isEmpty()) {
                val now = System.currentTimeMillis()
                items.forEach { item ->
                    dao.updateUploadItem(
                        item.copy(
                            status = UploadQueueStatus.FAILED.name,
                            attempts = item.attempts + 1,
                            errorMessage = "No Sync Server URL configured in App Settings (use Local Backup Export or enter Sync URL).",
                            updatedAtMillis = now
                        )
                    )
                }
                recordAudit(actorName, "RETRY_UPLOAD", "UPLOAD_QUEUE", "${items.size} item(s)", "Marked FAILED: No Sync URL configured")
                return@withContext UploadBatchSummary(
                    attempted = items.size,
                    succeeded = 0,
                    failed = items.size,
                    message = "No Sync URL configured. ${items.size} item(s) listed in Failed Uploads — export Local Backup to complete offline sync."
                )
            }

            var succeeded = 0
            var failed = 0
            for (item in items) {
                val now = System.currentTimeMillis()
                try {
                    val bodyJson = JSONObject().apply {
                        put("id", item.id)
                        put("title", item.title)
                        put("payloadType", item.payloadType)
                        put("payload", JSONObject(item.payloadJson))
                    }.toString()
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("Content-Type", "application/json")
                        .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                        .build()

                    httpClient.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            succeeded++
                            dao.updateUploadItem(
                                item.copy(
                                    status = UploadQueueStatus.COMPLETED.name,
                                    attempts = item.attempts + 1,
                                    errorMessage = null,
                                    updatedAtMillis = now
                                )
                            )
                        } else {
                            failed++
                            dao.updateUploadItem(
                                item.copy(
                                    status = UploadQueueStatus.FAILED.name,
                                    attempts = item.attempts + 1,
                                    errorMessage = "HTTP ${response.code}: ${response.message}",
                                    updatedAtMillis = now
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    failed++
                    dao.updateUploadItem(
                        item.copy(
                            status = UploadQueueStatus.FAILED.name,
                            attempts = item.attempts + 1,
                            errorMessage = e.localizedMessage ?: "Network error",
                            updatedAtMillis = now
                        )
                    )
                }
            }
            recordAudit(actorName, "RETRY_UPLOAD", "UPLOAD_QUEUE", "${items.size} item(s)", "Succeeded=$succeeded, Failed=$failed")
            UploadBatchSummary(
                attempted = items.size,
                succeeded = succeeded,
                failed = failed,
                message = "Upload retry finished: $succeeded completed, $failed failed."
            )
        }

    suspend fun markAllRetryableUploadsCompletedLocally(actorName: String = "Admin"): Int {
        val items = dao.getRetryableUploadsOnce()
        if (items.isEmpty()) return 0
        val now = System.currentTimeMillis()
        db.withTransaction {
            items.forEach { item ->
                dao.updateUploadItem(
                    item.copy(
                        status = UploadQueueStatus.COMPLETED.name,
                        errorMessage = null,
                        updatedAtMillis = now
                    )
                )
            }
            insertAuditInternal(
                actorName, "LOCAL_BACKUP_SYNC", "UPLOAD_QUEUE", "${items.size} item(s)",
                "Synced and marked completed via local backup export"
            )
        }
        return items.size
    }

    suspend fun clearCompletedUploads() {
        dao.clearCompletedUploads()
    }

    // -------------------------------------------------------------------------
    // JSON Backup & Restore (v3 full schema + v1/v2 backward-compatible)
    // -------------------------------------------------------------------------

    suspend fun exportFullBackupJson(companySettings: AppCompanySettings = AppCompanySettings()): Result<String> {
        return try {
            val root = JSONObject()
            root.put("brand", "GM HASMANI COMMITTEE")
            root.put("version", 3)
            root.put("exportedAt", MonthUtils.todayDateString())

            root.put("settings", JSONObject().apply {
                put("companyName", companySettings.companyName)
                put("applicationName", companySettings.applicationName)
                put("adminName", companySettings.adminName)
                put("adminContactNumber", companySettings.adminContactNumber)
                put("easypaisaNumber", companySettings.easypaisaNumber)
                put("easypaisaAccountHolder", companySettings.easypaisaAccountHolder)
                put("currency", companySettings.currency)
                put("defaultMonthlyContribution", companySettings.defaultMonthlyContribution)
                put("defaultTotalShares", companySettings.defaultTotalShares)
                put("defaultCollectionStartDay", companySettings.defaultCollectionStartDay)
                put("defaultCollectionEndDay", companySettings.defaultCollectionEndDay)
                put("defaultDurationMonths", companySettings.defaultDurationMonths)
                put("pdfFooterNote", companySettings.pdfFooterNote)
            })

            val committeesArr = JSONArray()
            val committees = dao.getAllCommitteesOnce()
            for (c in committees) {
                val cJson = JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("memberCount", c.memberCount)
                    put("totalShares", c.totalShares)
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
                        put("shares", m.shares)
                        put("payoutMonth", m.payoutMonth)
                        put("payoutStatus", m.payoutStatus)
                        if (!m.payoutDate.isNullOrBlank()) put("payoutDate", m.payoutDate)
                        put("payoutAmount", m.payoutAmount)
                        put("status", m.status)
                    })
                }
                cJson.put("members", membersArr)

                val turnsArr = JSONArray()
                dao.getTurnsOnce(c.id).forEach { t ->
                    turnsArr.put(JSONObject().apply {
                        put("id", t.id)
                        put("turnNumber", t.turnNumber)
                        if (!t.recipientMemberId.isNullOrBlank()) put("recipientMemberId", t.recipientMemberId)
                        put("recipientName", t.recipientName)
                        if (!t.recipientPhone.isNullOrBlank()) put("recipientPhone", t.recipientPhone)
                        put("payoutAmount", t.payoutAmount)
                        put("scheduledMonth", t.scheduledMonth)
                        if (!t.payoutDate.isNullOrBlank()) put("payoutDate", t.payoutDate)
                        put("status", t.status)
                        put("confirmedByAdmin", t.confirmedByAdmin)
                        if (!t.notes.isNullOrBlank()) put("notes", t.notes)
                    })
                }
                cJson.put("turns", turnsArr)

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
                        put("paymentMethod", i.paymentMethod)
                        if (!i.transactionId.isNullOrBlank()) put("transactionId", i.transactionId)
                    })
                }
                cJson.put("installments", instArr)
                committeesArr.put(cJson)
            }
            root.put("committees", committeesArr)

            val oldArr = JSONArray()
            dao.getAllOldRecordsOnce().forEach { r ->
                oldArr.put(JSONObject().apply {
                    put("id", r.id)
                    put("committeeNumber", r.committeeNumber)
                    put("committeeName", r.committeeName)
                    put("startDate", r.startDate)
                    put("endDate", r.endDate)
                    put("memberNames", r.memberNames)
                    put("contactNumbers", r.contactNumbers)
                    put("numberOfShares", r.numberOfShares)
                    put("monthlyContributions", r.monthlyContributions)
                    put("totalCollection", r.totalCollection)
                    put("paymentHistory", r.paymentHistory)
                    put("payoutRecipient", r.payoutRecipient)
                    put("payoutAmount", r.payoutAmount)
                    put("payoutDate", r.payoutDate)
                    put("status", r.status)
                })
            }
            root.put("oldCommitteeRecords", oldArr)

            Result.success(root.toString(2))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreFromBackupJson(
        jsonContent: String,
        actorName: String = "Admin",
        onRestoreSettings: (suspend (AppCompanySettings) -> Unit)? = null
    ): Result<Int> {
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
                    val totalShares = cObj.optLong("totalShares", 0L).coerceIn(0L, 500L)
                    val monthlyInstallment = cObj.optLong("monthlyInstallment", 5000L).coerceIn(100L, 10_000_000L)

                    val committeeEntity = CommitteeEntity(
                        id = cid,
                        name = cObj.getString("name").take(80),
                        memberCount = memberCount,
                        totalShares = totalShares,
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
                                    shares = mObj.optDouble("shares", 1.0).coerceIn(0.5, 50.0),
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

                    val turnsArr = cObj.optJSONArray("turns") ?: JSONArray()
                    val turnEntities = buildList {
                        for (tIdx in 0 until turnsArr.length()) {
                            val tObj = turnsArr.getJSONObject(tIdx)
                            val tid = tObj.optString("id").replace(Regex("[^a-zA-Z0-9_\\-]"), "")
                                .takeIf { it.isNotBlank() }?.take(128) ?: UUID.randomUUID().toString()
                            add(
                                CommitteeTurnEntity(
                                    id = tid,
                                    committeeId = cid,
                                    turnNumber = tObj.optLong("turnNumber", (tIdx + 1).toLong()).coerceIn(1L, 500L),
                                    recipientMemberId = tObj.optString("recipientMemberId").takeIf { it.isNotBlank() },
                                    recipientName = tObj.optString("recipientName", "Member").take(80),
                                    recipientPhone = tObj.optString("recipientPhone").takeIf { it.isNotBlank() },
                                    payoutAmount = tObj.optLong("payoutAmount", committeeEntity.monthlyInstallment).coerceAtLeast(0L),
                                    scheduledMonth = tObj.optString("scheduledMonth", committeeEntity.startMonth),
                                    payoutDate = tObj.optString("payoutDate").takeIf { it.isNotBlank() },
                                    status = PayoutStatus.fromRaw(tObj.optString("status")).name,
                                    confirmedByAdmin = tObj.optBoolean("confirmedByAdmin", false),
                                    notes = tObj.optString("notes").takeIf { it.isNotBlank() },
                                    createdAtMillis = now,
                                    updatedAtMillis = now
                                )
                            )
                        }
                    }
                    if (turnEntities.isNotEmpty()) dao.insertTurns(turnEntities)

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
                                    paymentMethod = PaymentMethod.fromRaw(iObj.optString("paymentMethod")).name,
                                    transactionId = iObj.optString("transactionId").takeIf { it.isNotBlank() },
                                    createdAtMillis = now,
                                    updatedAtMillis = now
                                )
                            )
                        }
                    }
                    if (instEntities.isNotEmpty()) dao.insertInstallments(instEntities)
                    restoredCount++
                }

                val oldArr = root.optJSONArray("oldCommitteeRecords") ?: JSONArray()
                val oldEntities = buildList {
                    for (oIdx in 0 until oldArr.length()) {
                        val oObj = oldArr.getJSONObject(oIdx)
                        val oid = oObj.optString("id").replace(Regex("[^a-zA-Z0-9_\\-]"), "")
                            .takeIf { it.isNotBlank() }?.take(128) ?: UUID.randomUUID().toString()
                        add(
                            OldCommitteeRecordEntity(
                                id = oid,
                                committeeNumber = oObj.optString("committeeNumber"),
                                committeeName = oObj.optString("committeeName"),
                                startDate = oObj.optString("startDate"),
                                endDate = oObj.optString("endDate"),
                                memberNames = oObj.optString("memberNames"),
                                contactNumbers = oObj.optString("contactNumbers"),
                                numberOfShares = oObj.optString("numberOfShares"),
                                monthlyContributions = oObj.optString("monthlyContributions"),
                                totalCollection = oObj.optString("totalCollection"),
                                paymentHistory = oObj.optString("paymentHistory"),
                                payoutRecipient = oObj.optString("payoutRecipient"),
                                payoutAmount = oObj.optString("payoutAmount"),
                                payoutDate = oObj.optString("payoutDate"),
                                status = oObj.optString("status", CommitteeStatus.COMPLETED.name),
                                createdAtMillis = now,
                                updatedAtMillis = now
                            )
                        )
                    }
                }
                if (oldEntities.isNotEmpty()) dao.insertOldRecords(oldEntities)

                insertAuditInternal(
                    actorName, "RESTORE_BACKUP", "BACKUP", "JSON Restore",
                    "Restored $restoredCount committee(s) and ${oldEntities.size} old record(s) from backup"
                )
            }

            root.optJSONObject("settings")?.let { sObj ->
                val restoredSettings = AppCompanySettings(
                    companyName = sObj.optString("companyName", "GM HASMANI COMMITTEE"),
                    applicationName = sObj.optString("applicationName", "GM Hasmani Committee Manager"),
                    adminName = sObj.optString("adminName", "GM Hasmani Admin"),
                    adminContactNumber = sObj.optString("adminContactNumber", ""),
                    easypaisaNumber = sObj.optString("easypaisaNumber", "03423498631"),
                    easypaisaAccountHolder = sObj.optString("easypaisaAccountHolder", "GM Hasmani Admin"),
                    currency = sObj.optString("currency", "PKR"),
                    defaultMonthlyContribution = sObj.optLong("defaultMonthlyContribution", 5000L),
                    defaultTotalShares = sObj.optLong("defaultTotalShares", 10L),
                    defaultCollectionStartDay = sObj.optLong("defaultCollectionStartDay", 28L),
                    defaultCollectionEndDay = sObj.optLong("defaultCollectionEndDay", 8L),
                    defaultDurationMonths = sObj.optLong("defaultDurationMonths", 10L),
                    pdfFooterNote = sObj.optString("pdfFooterNote", "Official GM HASMANI Committee Record")
                )
                onRestoreSettings?.invoke(restoredSettings)
            }

            Result.success(restoredCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun recordAudit(
        actorName: String,
        actionType: String,
        targetType: String,
        targetName: String,
        details: String
    ) {
        insertAuditInternal(actorName, actionType, targetType, targetName, details)
    }

    private suspend fun insertAuditInternal(
        actorName: String,
        actionType: String,
        targetType: String,
        targetName: String,
        details: String
    ) {
        dao.insertAuditLog(
            AuditLogEntity(
                id = UUID.randomUUID().toString(),
                actorName = actorName.ifBlank { "Admin" },
                actionType = actionType,
                targetType = targetType,
                targetName = targetName,
                details = details.take(300),
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    private suspend fun enqueueUploadInternal(
        title: String,
        payloadType: String,
        payload: JSONObject
    ) {
        val now = System.currentTimeMillis()
        dao.insertUploadItem(
            UploadQueueEntity(
                id = UUID.randomUUID().toString(),
                title = title.take(120),
                payloadType = payloadType,
                payloadJson = payload.toString(),
                status = UploadQueueStatus.PENDING.name,
                errorMessage = null,
                attempts = 0,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
    }

    companion object {
        const val DEFAULT_ADMIN_ID = "admin_default"
        const val DEFAULT_ADMIN_PIN = "1234"
        const val KNOWN_MAIN_COMMITTEE_ID = "gm_hasmani_main_committee"
    }
}
