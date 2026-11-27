package com.example.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApiSendResult
import com.example.data.CommitteeRepository
import com.example.data.MessagingConfigStore
import com.example.data.MessagingGateway
import com.example.data.MessagingGatewayConfig
import com.example.data.PdfReportGenerator
import com.example.data.PdfReportType
import com.example.i18n.AppLanguage
import com.example.model.AppCompanySettings
import com.example.model.AuditLogEntry
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.CommitteeStatus
import com.example.model.CommitteeTurn
import com.example.model.CommitteeUtils
import com.example.model.DashboardSummary
import com.example.model.DispatchStatus
import com.example.model.DuplicateConflictType
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.LocalUser
import com.example.model.MemberStatus
import com.example.model.MonthUtils
import com.example.model.MonthlyChartPoint
import com.example.model.OldCommitteeRecord
import com.example.model.OldHistoryCsvUtils
import com.example.model.PasswordHasher
import com.example.model.PaymentHistoryLog
import com.example.model.PaymentMethod
import com.example.model.PayoutStatus
import com.example.model.ReminderChannel
import com.example.model.ReminderLog
import com.example.model.ReminderStage
import com.example.model.UploadQueueItem
import com.example.model.UploadQueueStatus
import com.example.model.UserRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class InstallmentFilter { ALL, PAID, UNPAID, PARTIAL }

@OptIn(ExperimentalCoroutinesApi::class)
class CommitteeViewModel(
    private val repository: CommitteeRepository,
    private val configStore: MessagingConfigStore,
    private val gateway: MessagingGateway
) : ViewModel() {

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    private val _selectedCommitteeId = MutableStateFlow<String?>(null)
    private val _selectedMonthKey = MutableStateFlow(MonthUtils.currentMonthKey())
    val selectedMonthKey: StateFlow<String> = _selectedMonthKey.asStateFlow()

    private val _memberSearchQuery = MutableStateFlow("")
    val memberSearchQuery: StateFlow<String> = _memberSearchQuery.asStateFlow()

    private val _installmentFilter = MutableStateFlow(InstallmentFilter.ALL)
    val installmentFilter: StateFlow<InstallmentFilter> = _installmentFilter.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    // Tracks (committeeId, monthKey) intentionally deleted by Admin so auto-sync does not recreate it immediately
    private val _skipAutoSyncMonths = MutableStateFlow<Set<Pair<String, String>>>(emptySet())

    val messagingConfig: StateFlow<MessagingGatewayConfig> = configStore.configFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), MessagingGatewayConfig())

    val companySettings: StateFlow<AppCompanySettings> = configStore.companySettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AppCompanySettings())

    val customTemplates: StateFlow<Map<Pair<ReminderStage, AppLanguage>, String>> = configStore.templatesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val localUsers: StateFlow<List<LocalUser>> = repository.observeUsers()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val isLoggedOut: StateFlow<Boolean> = configStore.isLoggedOutFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val currentUser: StateFlow<LocalUser?> = combine(
        localUsers,
        configStore.currentUserIdFlow
    ) { users, savedId ->
        val chosen = if (!savedId.isNullOrBlank()) users.firstOrNull { it.id == savedId } else null
        chosen
            ?: users.firstOrNull { it.id == CommitteeRepository.DEFAULT_ADMIN_ID }
            ?: users.firstOrNull { it.isAdmin && it.isActive }
            ?: users.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val isDefaultPinActive: StateFlow<Boolean> = currentUser.map { user ->
        user != null && user.passwordHash == PasswordHasher.sha256(CommitteeRepository.DEFAULT_ADMIN_PIN)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val auditLogs: StateFlow<List<AuditLogEntry>> = repository.observeAuditLogs()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val uploadQueue: StateFlow<List<UploadQueueItem>> = repository.observeUploadQueue()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val pendingUploads: StateFlow<List<UploadQueueItem>> = uploadQueue
        .map { list -> list.filter { it.statusEnum == UploadQueueStatus.PENDING } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val completedUploads: StateFlow<List<UploadQueueItem>> = uploadQueue
        .map { list -> list.filter { it.statusEnum == UploadQueueStatus.COMPLETED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val failedUploads: StateFlow<List<UploadQueueItem>> = uploadQueue
        .map { list -> list.filter { it.statusEnum == UploadQueueStatus.FAILED } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val committeesState: StateFlow<UiState<List<Committee>>> = repository.observeCommittees()
        .map<List<Committee>, UiState<List<Committee>>> { UiState.Success(it) }
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing committees", error)
            emit(UiState.Error(error.localizedMessage ?: "Failed to load committees"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val activeCommittee: StateFlow<Committee?> = combine(
        committeesState,
        _selectedCommitteeId,
        configStore.activeCommitteeIdFlow
    ) { state, selectedId, storedId ->
        val list = (state as? UiState.Success)?.data ?: emptyList()
        val chosen = selectedId ?: storedId
        list.firstOrNull { it.id == chosen }
            ?: list.firstOrNull { it.statusEnum == CommitteeStatus.ACTIVE }
            ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val activeCommitteeIdFlow = activeCommittee
        .map { it?.id.orEmpty() }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "")

    val membersState: StateFlow<UiState<List<CommitteeMember>>> = activeCommitteeIdFlow
        .flatMapLatest { cid ->
            if (cid.isBlank()) flowOf<UiState<List<CommitteeMember>>>(UiState.Success(emptyList()))
            else repository.observeMembers(cid)
                .map<List<CommitteeMember>, UiState<List<CommitteeMember>>> { UiState.Success(it) }
                .catch { error ->
                    if (error is CancellationException) throw error
                    emit(UiState.Error(error.localizedMessage ?: "Failed to load members"))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val turnsState: StateFlow<List<CommitteeTurn>> = activeCommitteeIdFlow
        .flatMapLatest { cid ->
            if (cid.isBlank()) flowOf(emptyList())
            else repository.observeTurns(cid).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val oldRecords: StateFlow<List<OldCommitteeRecord>> = repository.observeOldRecords()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val installmentsState: StateFlow<UiState<List<InstallmentRecord>>> = activeCommitteeIdFlow
        .flatMapLatest { cid ->
            if (cid.isBlank()) flowOf<UiState<List<InstallmentRecord>>>(UiState.Success(emptyList()))
            else repository.observeInstallments(cid)
                .map<List<InstallmentRecord>, UiState<List<InstallmentRecord>>> { UiState.Success(it) }
                .catch { error ->
                    if (error is CancellationException) throw error
                    emit(UiState.Error(error.localizedMessage ?: "Failed to load installments"))
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val paymentLogs: StateFlow<List<PaymentHistoryLog>> = activeCommitteeIdFlow
        .flatMapLatest { cid ->
            if (cid.isBlank()) flowOf(emptyList())
            else repository.observePaymentLogs(cid).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val reminderLogs: StateFlow<List<ReminderLog>> = activeCommitteeIdFlow
        .flatMapLatest { cid ->
            if (cid.isBlank()) flowOf(emptyList())
            else repository.observeReminderLogs(cid).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val coreInputsFlow = combine(
        committeesState,
        activeCommittee,
        membersState,
        installmentsState,
        turnsState
    ) { allC, c, m, i, t ->
        SummaryInputs(allC, c, m, i, t)
    }

    val dashboardSummary: StateFlow<DashboardSummary?> = combine(
        coreInputsFlow,
        _selectedMonthKey
    ) { inputs, selectedMonth ->
        val committee = inputs.activeCommittee ?: return@combine null
        val allCommittees = (inputs.allCommittees as? UiState.Success)?.data ?: emptyList()
        val activeCommitteeCount = allCommittees.count { it.statusEnum == CommitteeStatus.ACTIVE }
        val completedCommitteeCount = allCommittees.count { it.statusEnum == CommitteeStatus.COMPLETED }
        val members = (inputs.members as? UiState.Success)?.data ?: emptyList()
        val installments = (inputs.installments as? UiState.Success)?.data ?: emptyList()
        computeSummary(
            totalActiveCommittees = activeCommitteeCount.coerceAtLeast(if (committee.statusEnum == CommitteeStatus.ACTIVE) 1 else 0),
            totalCompletedCommittees = completedCommitteeCount,
            committee = committee,
            members = members,
            installments = installments,
            turns = inputs.turns,
            selectedMonthKey = selectedMonth
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    init {
        viewModelScope.launch {
            val admin = repository.ensureDefaultAdmin()
            if (currentUser.value == null) {
                configStore.saveSession(admin.id, isLoggedOut = false)
            }
            val seededId = repository.ensureKnownInitialCommitteeIfEmpty()
            if (seededId != null && _selectedCommitteeId.value == null) {
                _selectedCommitteeId.value = seededId
                configStore.saveActiveCommitteeId(seededId)
            }
        }

        viewModelScope.launch {
            activeCommittee.collect { committee ->
                if (committee != null) {
                    val months = MonthUtils.monthKeysForCommittee(committee.startMonth, committee.totalMonths.toInt())
                    if (_selectedMonthKey.value !in months) {
                        val todayKey = MonthUtils.currentMonthKey()
                        _selectedMonthKey.value = if (todayKey in months) todayKey else months.firstOrNull() ?: committee.startMonth
                    }
                }
            }
        }

        viewModelScope.launch {
            combine(
                activeCommittee,
                membersState,
                installmentsState,
                _selectedMonthKey,
                _skipAutoSyncMonths
            ) { committee, mState, iState, monthKey, skipped ->
                if (committee == null || mState !is UiState.Success || iState !is UiState.Success) {
                    null
                } else if ((committee.id to monthKey) in skipped) {
                    null
                } else {
                    val activeMembers = mState.data.filter { it.memberStatusEnum == MemberStatus.ACTIVE }
                    val existing = iState.data.filter { it.committeeId == committee.id }
                    val existingIdsForMonth = existing.filter { it.monthKey == monthKey }.map { it.memberId }.toSet()
                    val missing = activeMembers.filter { it.id !in existingIdsForMonth }
                    if (missing.isEmpty()) null else Triple(committee, activeMembers to existing, monthKey)
                }
            }.collect { payload ->
                if (payload != null) {
                    val (committee, lists, monthKey) = payload
                    val (activeMembers, existing) = lists
                    val monthKeys = MonthUtils.monthKeysForCommittee(committee.startMonth, committee.totalMonths.toInt())
                    if (monthKey in monthKeys) {
                        val monthIndex = (monthKeys.indexOf(monthKey) + 1).coerceAtLeast(1)
                        repository.syncInstallmentsForMonth(committee, activeMembers, existing, monthKey, monthIndex)
                    }
                }
            }
        }
    }

    private fun actorName(): String = currentUser.value?.displayName ?: "Admin"

    private fun computeSummary(
        totalActiveCommittees: Int,
        totalCompletedCommittees: Int,
        committee: Committee,
        members: List<CommitteeMember>,
        installments: List<InstallmentRecord>,
        turns: List<CommitteeTurn>,
        selectedMonthKey: String
    ): DashboardSummary {
        val activeMembers = members.filter { it.memberStatusEnum == MemberStatus.ACTIVE }.sortedBy { it.committeeNumber }
        val monthKeys = MonthUtils.monthKeysForCommittee(committee.startMonth, committee.totalMonths.toInt())
        val effectiveMonthKey = if (selectedMonthKey in monthKeys) selectedMonthKey else monthKeys.firstOrNull() ?: committee.startMonth
        val monthIndex = (monthKeys.indexOf(effectiveMonthKey) + 1).coerceAtLeast(1)

        val monthInstallments = installments.filter { it.monthKey == effectiveMonthKey }
        val monthlyTarget = if (monthInstallments.isNotEmpty()) {
            monthInstallments.sumOf { it.dueAmount }
        } else {
            committee.monthlyCollectionTotal
        }
        val monthCollected = monthInstallments.sumOf { it.paidAmount }
        val monthPending = (monthlyTarget - monthCollected).coerceAtLeast(0L)

        val paidCount = monthInstallments.count { it.statusEnum == InstallmentStatus.PAID }
        val partialCount = monthInstallments.count { it.statusEnum == InstallmentStatus.PARTIAL }
        val unpaidCount = if (monthInstallments.isNotEmpty()) {
            monthInstallments.count { it.statusEnum == InstallmentStatus.UNPAID }
        } else {
            activeMembers.size
        }

        val sortedTurns = turns.sortedBy { it.turnNumber }
        val nextTurn = sortedTurns.firstOrNull { it.statusEnum == PayoutStatus.PENDING }
        val nextPayoutMemberFallback = activeMembers.firstOrNull { it.payoutStatusEnum == PayoutStatus.PENDING }

        val completedPayouts = if (sortedTurns.isNotEmpty()) {
            sortedTurns.count { it.statusEnum == PayoutStatus.PAID }
        } else {
            activeMembers.count { it.payoutStatusEnum == PayoutStatus.PAID }
        }

        val totalPayoutsAmt = if (sortedTurns.isNotEmpty()) {
            sortedTurns.filter { it.statusEnum == PayoutStatus.PAID }.sumOf { it.payoutAmount }
        } else {
            activeMembers.filter { it.payoutStatusEnum == PayoutStatus.PAID }
                .sumOf { if (it.payoutAmount > 0L) it.payoutAmount else committee.eachPayoutAmount }
        }

        val nextPayoutAmt = nextTurn?.payoutAmount
            ?: nextPayoutMemberFallback?.let { if (it.payoutAmount > 0L) it.payoutAmount else committee.eachPayoutAmount }
            ?: committee.eachPayoutAmount

        val upcomingPayoutDate = when {
            nextTurn != null -> MonthUtils.formatCollectionWindow(
                nextTurn.scheduledMonth,
                committee.collectionDayStart,
                committee.collectionDayEnd
            )
            nextPayoutMemberFallback != null -> MonthUtils.formatPayoutDate(
                nextPayoutMemberFallback.payoutMonth,
                committee.payoutDay,
                committee.collectionDayStart
            )
            else -> MonthUtils.formatPayoutDate(effectiveMonthKey, committee.payoutDay, committee.collectionDayStart)
        }

        val nextPaymentDueDate = MonthUtils.formatDueDate(
            nextTurn?.scheduledMonth ?: effectiveMonthKey,
            committee.collectionDayStart,
            committee.collectionDayEnd
        )

        val groupedByMonth = installments.groupBy { it.monthKey }
        val chartPoints = monthKeys.map { key ->
            val rows = groupedByMonth[key].orEmpty()
            val col = rows.sumOf { it.paidAmount }
            val tgt = if (rows.isNotEmpty()) rows.sumOf { it.dueAmount } else committee.monthlyCollectionTotal
            MonthlyChartPoint(
                monthKey = key,
                shortLabel = MonthUtils.formatMonthDisplay(key).take(3),
                collected = col,
                target = tgt
            )
        }

        val remainingMonths = (committee.totalMonths.toInt() - completedPayouts).coerceAtLeast(0)
        val totalMemberShares = activeMembers.sumOf { it.effectiveShares }

        return DashboardSummary(
            totalActiveCommittees = totalActiveCommittees,
            totalCompletedCommittees = totalCompletedCommittees,
            activeMemberCount = activeMembers.size,
            targetMemberCount = committee.memberCount.toInt(),
            totalSharesConfigured = committee.effectiveTotalShares,
            totalMemberShares = totalMemberShares,
            monthlyCollectionTarget = monthlyTarget,
            currentMonthCollected = monthCollected,
            currentMonthPending = monthPending,
            totalCommitteeCollectedAllMonths = installments.sumOf { it.paidAmount },
            totalCommitteeExpectedAllMonths = committee.totalCommitteeAmount,
            paidMembersCount = paidCount,
            partialMembersCount = partialCount,
            unpaidMembersCount = unpaidCount,
            currentMonthKey = effectiveMonthKey,
            currentMonthIndex = monthIndex,
            totalMonths = committee.totalMonths.toInt(),
            nextPayoutMember = nextPayoutMemberFallback,
            nextPayoutTurn = nextTurn,
            nextPayoutAmount = nextPayoutAmt,
            upcomingPayoutDate = upcomingPayoutDate,
            nextPaymentDueDate = nextPaymentDueDate,
            completedPayouts = completedPayouts,
            totalPayoutsAmount = totalPayoutsAmt,
            remainingMonths = remainingMonths,
            monthlyChartPoints = chartPoints
        )
    }

    fun selectCommittee(committeeId: String) {
        _selectedCommitteeId.value = committeeId
        viewModelScope.launch {
            configStore.saveActiveCommitteeId(committeeId)
        }
    }

    fun selectMonth(monthKey: String) {
        _selectedMonthKey.value = monthKey
    }

    fun setMemberSearchQuery(query: String) {
        _memberSearchQuery.value = query
    }

    fun setInstallmentFilter(filter: InstallmentFilter) {
        _installmentFilter.value = filter
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            configStore.saveLanguage(language)
        }
    }

    fun saveCompanySettings(settings: AppCompanySettings) {
        viewModelScope.launch {
            configStore.saveCompanySettings(settings)
            _snackbarEvents.tryEmit("Settings saved.")
        }
    }

    fun refreshAll() {
        val committee = activeCommittee.value
        val members = (membersState.value as? UiState.Success)?.data
            ?.filter { it.memberStatusEnum == MemberStatus.ACTIVE }
            ?: emptyList()
        val existing = (installmentsState.value as? UiState.Success)?.data ?: emptyList()

        viewModelScope.launch {
            _isBusy.value = true
            repository.ensureDefaultAdmin()
            if (committee != null && members.isNotEmpty()) {
                val monthKey = _selectedMonthKey.value
                _skipAutoSyncMonths.update { it - (committee.id to monthKey) }
                val monthKeys = MonthUtils.monthKeysForCommittee(committee.startMonth, committee.totalMonths.toInt())
                val monthIndex = (monthKeys.indexOf(monthKey) + 1).coerceAtLeast(1)
                repository.syncInstallmentsForMonth(committee, members, existing, monthKey, monthIndex)
            }
            _isBusy.value = false
            _snackbarEvents.tryEmit("Data refreshed.")
        }
    }

    // -------------------------------------------------------------------------
    // Committee Actions
    // -------------------------------------------------------------------------

    fun createCommittee(
        name: String,
        memberCount: Long,
        totalShares: Long,
        monthlyInstallment: Long,
        totalMonths: Long,
        collectionDayStart: Long,
        collectionDayEnd: Long,
        payoutDay: Long,
        startMonth: String,
        notes: String?
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.createCommittee(
                name, memberCount, totalShares, monthlyInstallment, totalMonths,
                collectionDayStart, collectionDayEnd, payoutDay, startMonth, notes, actorName()
            )
            _isBusy.value = false
            res.onSuccess { newId ->
                _selectedCommitteeId.value = newId
                _selectedMonthKey.value = startMonth
                configStore.saveActiveCommitteeId(newId)
                _snackbarEvents.tryEmit("Committee \"$name\" created.")
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to create committee.")
            }
        }
    }

    fun updateCommittee(
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
        newNotes: String?
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.updateCommitteeWithFutureRecalculation(
                existing, newName, newMemberCount, newTotalShares, newMonthlyInstallment, newTotalMonths,
                newCollectionDayStart, newCollectionDayEnd, newPayoutDay, newStartMonth, newNotes, actorName()
            )
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.tryEmit("Committee settings updated.")
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update committee.")
            }
        }
    }

    fun setCommitteeStatus(committeeId: String, status: CommitteeStatus) {
        viewModelScope.launch {
            repository.updateCommitteeStatus(committeeId, status, actorName())
                .onSuccess { _snackbarEvents.tryEmit("Committee marked as ${status.name}.") }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update status.") }
        }
    }

    fun deleteCommittee(
        committee: Committee,
        adminPassword: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.deleteCommitteeSafely(committee, currentUser.value, adminPassword)
            _isBusy.value = false
            res.onSuccess {
                if (_selectedCommitteeId.value == committee.id) {
                    _selectedCommitteeId.value = null
                    configStore.saveActiveCommitteeId(null)
                }
                _snackbarEvents.tryEmit("Committee \"${committee.name}\" deleted.")
                onResult(true, null)
            }.onFailure {
                val msg = it.localizedMessage ?: "Could not delete committee."
                _snackbarEvents.tryEmit(msg)
                onResult(false, msg)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Member Actions
    // -------------------------------------------------------------------------

    fun addMember(name: String, phone: String, committeeNumber: Long, shares: Double = 1.0) {
        val committee = activeCommittee.value ?: return
        val existingMembers = (membersState.value as? UiState.Success)?.data ?: emptyList()
        val conflict = CommitteeUtils.findDuplicateMember(existingMembers, phone, committeeNumber)
        if (conflict != null) {
            val msg = when (conflict.type) {
                DuplicateConflictType.PHONE ->
                    "Duplicate mobile number: already assigned to #${conflict.existingMember.committeeNumber} ${conflict.existingMember.name}."
                DuplicateConflictType.COMMITTEE_NUMBER ->
                    "Duplicate Committee #$committeeNumber: already assigned to ${conflict.existingMember.name}."
            }
            _snackbarEvents.tryEmit(msg)
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.addMember(committee, name, phone, committeeNumber, shares, actorName())
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.tryEmit("Member \"$name\" added (#$committeeNumber).")
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to add member.")
            }
        }
    }

    fun updateMember(
        member: CommitteeMember,
        newName: String,
        newPhone: String,
        newCommitteeNumber: Long,
        newShares: Double = member.effectiveShares
    ) {
        val committee = activeCommittee.value ?: return
        val existingMembers = (membersState.value as? UiState.Success)?.data ?: emptyList()
        val conflict = CommitteeUtils.findDuplicateMember(
            members = existingMembers,
            phone = newPhone,
            committeeNumber = newCommitteeNumber,
            excludeMemberId = member.id
        )
        if (conflict != null) {
            val msg = when (conflict.type) {
                DuplicateConflictType.PHONE ->
                    "Duplicate mobile number: already used by #${conflict.existingMember.committeeNumber} ${conflict.existingMember.name}."
                DuplicateConflictType.COMMITTEE_NUMBER ->
                    "Duplicate Committee #$newCommitteeNumber: already assigned to ${conflict.existingMember.name}."
            }
            _snackbarEvents.tryEmit(msg)
            return
        }
        viewModelScope.launch {
            repository.updateMember(committee, member, newName, newPhone, newCommitteeNumber, newShares, actorName())
                .onSuccess { _snackbarEvents.tryEmit("Member updated.") }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update member.") }
        }
    }

    fun archiveMember(member: CommitteeMember) {
        val committee = activeCommittee.value ?: return
        viewModelScope.launch {
            repository.archiveMember(committee.id, member, actorName())
                .onSuccess { _snackbarEvents.tryEmit("${member.name} archived (history preserved).") }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to archive member.") }
        }
    }

    fun deleteMember(
        member: CommitteeMember,
        adminPassword: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.deleteMemberSafely(member, currentUser.value, adminPassword)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.tryEmit("Member \"${member.name}\" deleted.")
                onResult(true, null)
            }.onFailure {
                val msg = it.localizedMessage ?: "Could not delete member."
                _snackbarEvents.tryEmit(msg)
                onResult(false, msg)
            }
        }
    }

    fun swapPayoutOrder(first: CommitteeMember, second: CommitteeMember) {
        val committee = activeCommittee.value ?: return
        viewModelScope.launch {
            repository.swapMemberPayoutOrder(committee, first, second, actorName())
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to reorder payout.") }
        }
    }

    fun setPayoutStatus(
        member: CommitteeMember,
        status: PayoutStatus,
        date: String? = null,
        payoutAmount: Long? = null
    ) {
        val committee = activeCommittee.value ?: return
        viewModelScope.launch {
            repository.updateMemberPayoutStatus(committee, member, status, date, payoutAmount, actorName())
                .onSuccess { _snackbarEvents.tryEmit("Payout for ${member.name}: ${status.name}") }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update payout.") }
        }
    }

    // -------------------------------------------------------------------------
    // Committee Turns Actions
    // -------------------------------------------------------------------------

    fun saveCommitteeTurn(
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
        notes: String?
    ) {
        val committee = activeCommittee.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveCommitteeTurn(
                committee = committee,
                existingTurn = existingTurn,
                turnNumber = turnNumber,
                recipientMemberId = recipientMemberId,
                recipientName = recipientName,
                recipientPhone = recipientPhone,
                payoutAmount = payoutAmount,
                scheduledMonth = scheduledMonth,
                payoutDate = payoutDate,
                status = status,
                confirmedByAdmin = confirmedByAdmin,
                notes = notes,
                actorName = actorName()
            )
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.tryEmit("Turn #$turnNumber ($recipientName) saved.")
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to save turn.")
            }
        }
    }

    fun swapTurnOrder(first: CommitteeTurn, second: CommitteeTurn) {
        val committee = activeCommittee.value ?: return
        viewModelScope.launch {
            repository.swapTurnOrder(committee, first, second, actorName())
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to swap turn order.") }
        }
    }

    fun generateTurnsFromMembers() {
        val committee = activeCommittee.value ?: return
        val members = (membersState.value as? UiState.Success)?.data ?: emptyList()
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.generateTurnsFromMembers(committee, members, actorName())
            _isBusy.value = false
            res.onSuccess { count ->
                _snackbarEvents.tryEmit(
                    if (count > 0) "Generated $count turn(s) based on member shares."
                    else "All share turns already exist."
                )
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Could not generate turns.")
            }
        }
    }

    fun loadKnownHistoryTurns() {
        val committee = activeCommittee.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.loadKnownHistoryTurnsForCommittee(committee.id, actorName())
            _isBusy.value = false
            res.onSuccess { added ->
                _snackbarEvents.tryEmit(
                    if (added > 0) "Loaded $added known turn record(s) (Turns 1–4)."
                    else "Known turns 1–4 already present."
                )
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to load known turns.")
            }
        }
    }

    fun deleteCommitteeTurn(
        turn: CommitteeTurn,
        adminPassword: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.deleteTurnSafely(turn, currentUser.value, adminPassword)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.tryEmit("Turn #${turn.turnNumber} deleted.")
                onResult(true, null)
            }.onFailure {
                val msg = it.localizedMessage ?: "Could not delete turn."
                _snackbarEvents.tryEmit(msg)
                onResult(false, msg)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Old Committee Records / History Actions
    // -------------------------------------------------------------------------

    fun saveOldCommitteeRecord(record: OldCommitteeRecord) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.saveOldCommitteeRecord(record, actorName())
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.tryEmit("Old committee record saved.")
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to save old record.")
            }
        }
    }

    fun importOldRecordsCsv(csvText: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.importOldRecordsFromCsv(csvText, actorName())
            _isBusy.value = false
            res.onSuccess { count ->
                _snackbarEvents.tryEmit("Imported $count old committee record(s) from CSV.")
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "CSV import failed.")
            }
        }
    }

    fun exportOldRecordsCsv(): String = OldHistoryCsvUtils.exportToCsv(oldRecords.value)

    fun deleteOldCommitteeRecord(
        record: OldCommitteeRecord,
        adminPassword: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.deleteOldCommitteeRecord(record, currentUser.value, adminPassword)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.tryEmit("Old committee record deleted.")
                onResult(true, null)
            }.onFailure {
                val msg = it.localizedMessage ?: "Could not delete old record."
                _snackbarEvents.tryEmit(msg)
                onResult(false, msg)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Installment & Safe Reversal / Delete Actions
    // -------------------------------------------------------------------------

    fun syncCurrentMonthInstallments() {
        val committee = activeCommittee.value ?: return
        val members = (membersState.value as? UiState.Success)?.data
            ?.filter { it.memberStatusEnum == MemberStatus.ACTIVE }
            ?: emptyList()
        if (members.isEmpty()) {
            _snackbarEvents.tryEmit("Add active members first.")
            return
        }
        val existing = (installmentsState.value as? UiState.Success)?.data ?: emptyList()
        val monthKeys = MonthUtils.monthKeysForCommittee(committee.startMonth, committee.totalMonths.toInt())
        val monthKey = _selectedMonthKey.value
        val monthIndex = (monthKeys.indexOf(monthKey) + 1).coerceAtLeast(1)

        _skipAutoSyncMonths.update { it - (committee.id to monthKey) }
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.syncInstallmentsForMonth(committee, members, existing, monthKey, monthIndex)
            _isBusy.value = false
            res.onSuccess { count ->
                if (count > 0) _snackbarEvents.tryEmit("Generated $count installment records for ${MonthUtils.formatMonthDisplay(monthKey)}.")
                else _snackbarEvents.tryEmit("All active members already have records for this month.")
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to sync installments.")
            }
        }
    }

    fun recordPayment(
        record: InstallmentRecord,
        paidAmount: Long,
        status: InstallmentStatus,
        paymentDate: String?,
        paymentMethod: PaymentMethod,
        transactionId: String?,
        note: String?
    ) {
        val committee = activeCommittee.value ?: return
        viewModelScope.launch {
            repository.recordInstallmentPayment(
                committeeId = committee.id,
                record = record,
                newPaidAmount = paidAmount,
                newStatus = status,
                paymentDate = paymentDate,
                paymentNote = note,
                paymentMethod = paymentMethod,
                transactionId = transactionId,
                actorName = actorName()
            ).onSuccess {
                _snackbarEvents.tryEmit("Saved ${record.memberName}: ${status.name}")
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to save payment.")
            }
        }
    }

    fun addOrUpdateManualPayment(
        member: CommitteeMember,
        monthKey: String,
        paidAmount: Long,
        status: InstallmentStatus,
        paymentDate: String?,
        paymentMethod: PaymentMethod,
        transactionId: String?,
        note: String?
    ) {
        val committee = activeCommittee.value ?: return
        val months = MonthUtils.monthKeysForCommittee(committee.startMonth, committee.totalMonths.toInt())
        val monthIndex = (months.indexOf(monthKey) + 1L).coerceAtLeast(1L)
        viewModelScope.launch {
            repository.recordOrAddManualPayment(
                committee = committee,
                member = member,
                monthKey = monthKey,
                monthIndex = monthIndex,
                paidAmount = paidAmount,
                status = status,
                paymentDate = paymentDate,
                paymentMethod = paymentMethod,
                transactionId = transactionId,
                paymentNote = note,
                actorName = actorName()
            ).onSuccess {
                _snackbarEvents.tryEmit("Payment recorded for ${member.name} (${MonthUtils.formatMonthDisplay(monthKey)}).")
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to record payment.")
            }
        }
    }

    fun reversePayment(
        record: InstallmentRecord,
        reason: String,
        onComplete: () -> Unit
    ) {
        val committee = activeCommittee.value ?: return
        viewModelScope.launch {
            repository.reverseInstallmentPayment(committee.id, record, reason, actorName())
                .onSuccess {
                    _snackbarEvents.tryEmit("Payment reversed for ${record.memberName} (audit saved).")
                    onComplete()
                }
                .onFailure {
                    _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to reverse payment.")
                }
        }
    }

    fun deleteInstallment(
        record: InstallmentRecord,
        adminPassword: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _skipAutoSyncMonths.update { it + (record.committeeId to record.monthKey) }
            val res = repository.deleteInstallmentSafely(record, currentUser.value, adminPassword)
            res.onSuccess {
                _snackbarEvents.tryEmit("Unpaid entry for ${record.memberName} deleted.")
                onResult(true, null)
            }.onFailure {
                _skipAutoSyncMonths.update { set -> set - (record.committeeId to record.monthKey) }
                val msg = it.localizedMessage ?: "Could not delete installment."
                _snackbarEvents.tryEmit(msg)
                onResult(false, msg)
            }
        }
    }

    fun deleteUnpaidMonthEntries(
        monthKey: String,
        adminPassword: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val committee = activeCommittee.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            _skipAutoSyncMonths.update { it + (committee.id to monthKey) }
            val res = repository.deleteUnpaidMonthEntriesSafely(committee, monthKey, currentUser.value, adminPassword)
            _isBusy.value = false
            res.onSuccess { (deleted, preservedPaid) ->
                val summaryMsg = "Deleted $deleted unpaid entries for ${MonthUtils.formatMonthDisplay(monthKey)}" +
                    if (preservedPaid > 0) " ($preservedPaid paid kept)." else "."
                _snackbarEvents.tryEmit(summaryMsg)
                onResult(true, null)
            }.onFailure {
                _skipAutoSyncMonths.update { set -> set - (committee.id to monthKey) }
                val msg = it.localizedMessage ?: "Could not delete month entries."
                _snackbarEvents.tryEmit(msg)
                onResult(false, msg)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Local User / Password / Session / Settings Actions
    // -------------------------------------------------------------------------

    fun loginLocalUser(username: String, password: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.authenticateUser(username, password)
            _isBusy.value = false
            res.onSuccess { user ->
                configStore.saveSession(user.id, isLoggedOut = false)
                _snackbarEvents.tryEmit("Welcome, ${user.displayName}")
            }.onFailure {
                onError(it.localizedMessage ?: "Login failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            configStore.saveSession(currentUser.value?.id, isLoggedOut = true)
            _snackbarEvents.tryEmit("Logged out.")
        }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val uid = currentUser.value?.id ?: CommitteeRepository.DEFAULT_ADMIN_ID
        viewModelScope.launch {
            val res = repository.changePassword(uid, currentPassword, newPassword)
            res.onSuccess {
                _snackbarEvents.tryEmit("Password / PIN updated.")
                onResult(true, null)
            }.onFailure {
                onResult(false, it.localizedMessage ?: "Could not change password.")
            }
        }
    }

    fun createUser(
        username: String,
        displayName: String,
        password: String,
        role: UserRole,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (currentUser.value?.isAdmin != true) {
            onResult(false, "Only Admin users can create users.")
            return
        }
        viewModelScope.launch {
            val res = repository.createUser(username, displayName, password, role, actorName())
            res.onSuccess {
                _snackbarEvents.tryEmit("User \"$displayName\" added.")
                onResult(true, null)
            }.onFailure {
                onResult(false, it.localizedMessage ?: "Failed to create user.")
            }
        }
    }

    fun updateUserRoleAndActive(target: LocalUser, newRole: UserRole, isActive: Boolean) {
        if (currentUser.value?.isAdmin != true) {
            _snackbarEvents.tryEmit("Only Admin users can modify users.")
            return
        }
        viewModelScope.launch {
            repository.updateUserRoleAndActive(target, newRole, isActive, actorName())
                .onSuccess { _snackbarEvents.tryEmit("Updated ${target.displayName}.") }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update user.") }
        }
    }

    fun deleteUser(target: LocalUser) {
        if (currentUser.value?.isAdmin != true) {
            _snackbarEvents.tryEmit("Only Admin users can delete users.")
            return
        }
        viewModelScope.launch {
            repository.deleteUser(target, currentUser.value?.id, actorName())
                .onSuccess { _snackbarEvents.tryEmit("Deleted user ${target.displayName}.") }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to delete user.") }
        }
    }

    // -------------------------------------------------------------------------
    // Upload Queue Actions
    // -------------------------------------------------------------------------

    fun saveSyncWebhookUrl(url: String) {
        viewModelScope.launch {
            configStore.saveSyncWebhookUrl(url)
            _snackbarEvents.tryEmit("Sync settings saved.")
        }
    }

    fun retryUploads() {
        viewModelScope.launch {
            _isBusy.value = true
            val summary = repository.retryPendingAndFailedUploads(messagingConfig.value.syncWebhookUrl, actorName())
            _isBusy.value = false
            _snackbarEvents.tryEmit(summary.message)
        }
    }

    fun markQueueCompletedLocally() {
        viewModelScope.launch {
            val count = repository.markAllRetryableUploadsCompletedLocally(actorName())
            _snackbarEvents.tryEmit(if (count > 0) "$count upload item(s) marked completed." else "No pending items.")
        }
    }

    fun clearCompletedUploads() {
        viewModelScope.launch {
            repository.clearCompletedUploads()
            _snackbarEvents.tryEmit("Cleared completed upload log.")
        }
    }

    // -------------------------------------------------------------------------
    // Payment Request Messages & Reminders
    // -------------------------------------------------------------------------

    fun buildPaymentRequestText(
        memberName: String,
        amount: Long,
        dueDate: String
    ): String {
        val committeeName = activeCommittee.value?.name ?: companySettings.value.companyName
        return gateway.buildPaymentRequestMessage(
            memberName = memberName,
            amount = amount,
            dueDate = dueDate,
            committeeName = committeeName,
            settings = companySettings.value
        )
    }

    fun copyMessageToClipboard(context: Context, text: String) {
        if (gateway.copyToClipboard(context, "GM Hasmani Payment Request", text)) {
            _snackbarEvents.tryEmit("Message copied to clipboard.")
        } else {
            _snackbarEvents.tryEmit("Could not copy message.")
        }
    }

    fun shareRawMessageWhatsApp(context: Context, rawPhone: String, messageBody: String) {
        if (!gateway.openWhatsAppManual(context, rawPhone, messageBody)) {
            _snackbarEvents.tryEmit("Could not open WhatsApp on this device.")
        }
    }

    fun shareRawMessageSms(context: Context, rawPhone: String, messageBody: String) {
        if (!gateway.openSmsManual(context, rawPhone, messageBody)) {
            _snackbarEvents.tryEmit("Could not open SMS app on this device.")
        }
    }

    fun saveCustomTemplate(stage: ReminderStage, language: AppLanguage, template: String) {
        viewModelScope.launch {
            configStore.saveCustomTemplate(stage, language, template)
            _snackbarEvents.tryEmit("Reminder template saved.")
        }
    }

    fun saveGatewayConfig(
        whatsappPhoneNumberId: String,
        whatsappAccessToken: String,
        smsGatewayUrl: String,
        smsGatewayApiKey: String,
        smsSenderId: String
    ) {
        viewModelScope.launch {
            configStore.saveGatewayConfig(
                whatsappPhoneNumberId, whatsappAccessToken, smsGatewayUrl, smsGatewayApiKey, smsSenderId
            )
            _snackbarEvents.tryEmit("Messaging gateway settings saved.")
        }
    }

    fun renderReminderForMember(
        template: String,
        member: CommitteeMember,
        dueAmount: Long
    ): String {
        val committee = activeCommittee.value ?: return template
        return gateway.renderMessage(template, member, committee, _selectedMonthKey.value, dueAmount, companySettings.value)
    }

    fun sendViaApiToSelected(
        recipients: List<Pair<CommitteeMember, Long>>,
        useWhatsApp: Boolean,
        stage: ReminderStage,
        template: String
    ) {
        val committee = activeCommittee.value ?: return
        val config = messagingConfig.value
        if (useWhatsApp && !config.isWhatsAppApiConfigured) {
            _snackbarEvents.tryEmit("WhatsApp Cloud API not configured. Use Direct WhatsApp mode.")
            return
        }
        if (!useWhatsApp && !config.isSmsGatewayConfigured) {
            _snackbarEvents.tryEmit("SMS Gateway URL not configured. Use Direct SMS mode.")
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            var sent = 0
            var failed = 0
            val monthKey = _selectedMonthKey.value
            for ((member, dueAmt) in recipients) {
                val message = gateway.renderMessage(template, member, committee, monthKey, dueAmt, companySettings.value)
                val result = if (useWhatsApp) {
                    gateway.sendViaWhatsAppCloudApi(config, member.phone, message)
                } else {
                    gateway.sendViaSmsGateway(config, member.phone, message)
                }
                val channel = if (useWhatsApp) ReminderChannel.WHATSAPP_API else ReminderChannel.SMS_API
                when (result) {
                    is ApiSendResult.Success -> {
                        sent++
                        repository.logReminderDispatch(
                            committee.id, member, monthKey, channel, stage, message, DispatchStatus.SENT_API
                        )
                    }
                    is ApiSendResult.Failure -> {
                        failed++
                        repository.logReminderDispatch(
                            committee.id, member, monthKey, channel, stage, message, DispatchStatus.FAILED, result.error
                        )
                    }
                }
            }
            _isBusy.value = false
            _snackbarEvents.tryEmit("API Dispatch complete: $sent sent, $failed failed.")
        }
    }

    fun launchManualDispatch(
        context: Context,
        member: CommitteeMember,
        dueAmount: Long,
        useWhatsApp: Boolean,
        stage: ReminderStage,
        template: String
    ): Boolean {
        val committee = activeCommittee.value ?: return false
        val monthKey = _selectedMonthKey.value
        val message = gateway.renderMessage(template, member, committee, monthKey, dueAmount, companySettings.value)
        val launched = if (useWhatsApp) {
            gateway.openWhatsAppManual(context, member.phone, message)
        } else {
            gateway.openSmsManual(context, member.phone, message)
        }
        val channel = if (useWhatsApp) ReminderChannel.WHATSAPP_MANUAL else ReminderChannel.SMS_MANUAL
        viewModelScope.launch {
            repository.logReminderDispatch(
                committeeId = committee.id,
                member = member,
                monthKey = monthKey,
                channel = channel,
                stage = stage,
                messagePreview = message,
                dispatchStatus = if (launched) DispatchStatus.OPENED_APP else DispatchStatus.FAILED,
                errorDetail = if (launched) null else "No compatible app found on device"
            )
        }
        if (!launched) {
            _snackbarEvents.tryEmit("Could not open ${if (useWhatsApp) "WhatsApp" else "SMS"} app on this device.")
        }
        return launched
    }

    // -------------------------------------------------------------------------
    // PDF View, Share, Print, Download & JSON Backup
    // -------------------------------------------------------------------------

    fun generateAndOpenPdf(
        context: Context,
        reportType: PdfReportType,
        selectedMember: CommitteeMember?,
        shareMode: Boolean
    ) {
        val committee = activeCommittee.value
        val requiresCommittee = reportType != PdfReportType.PREVIOUS_COMMITTEE_HISTORY &&
            reportType != PdfReportType.COMPLETE_FINANCIAL_SUMMARY
        if (requiresCommittee && committee == null) {
            _snackbarEvents.tryEmit("Create or select a committee first.")
            return
        }
        val allCommittees = (committeesState.value as? UiState.Success)?.data ?: emptyList()
        val members = (membersState.value as? UiState.Success)?.data ?: emptyList()
        val installments = (installmentsState.value as? UiState.Success)?.data ?: emptyList()

        viewModelScope.launch {
            _isBusy.value = true
            val res = PdfReportGenerator.generateReport(
                context = context,
                reportType = reportType,
                committee = committee,
                allCommittees = allCommittees,
                members = members,
                installments = installments,
                turns = turnsState.value,
                oldRecords = oldRecords.value,
                selectedMonthKey = _selectedMonthKey.value,
                selectedMember = selectedMember,
                summary = dashboardSummary.value,
                companySettings = companySettings.value
            )
            _isBusy.value = false
            res.onSuccess { file: File ->
                val opened = PdfReportGenerator.openOrSharePdf(context, file, shareMode)
                if (!opened) {
                    _snackbarEvents.tryEmit("PDF generated (${file.name}), use Download or Print.")
                }
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to generate PDF.")
            }
        }
    }

    fun printReport(
        context: Context,
        reportType: PdfReportType,
        selectedMember: CommitteeMember? = null
    ) {
        val committee = activeCommittee.value
        val requiresCommittee = reportType != PdfReportType.PREVIOUS_COMMITTEE_HISTORY &&
            reportType != PdfReportType.COMPLETE_FINANCIAL_SUMMARY
        if (requiresCommittee && committee == null) {
            _snackbarEvents.tryEmit("Create or select a committee first.")
            return
        }
        val allCommittees = (committeesState.value as? UiState.Success)?.data ?: emptyList()
        val members = (membersState.value as? UiState.Success)?.data ?: emptyList()
        val installments = (installmentsState.value as? UiState.Success)?.data ?: emptyList()

        viewModelScope.launch {
            _isBusy.value = true
            val res = PdfReportGenerator.generateReport(
                context = context,
                reportType = reportType,
                committee = committee,
                allCommittees = allCommittees,
                members = members,
                installments = installments,
                turns = turnsState.value,
                oldRecords = oldRecords.value,
                selectedMonthKey = _selectedMonthKey.value,
                selectedMember = selectedMember,
                summary = dashboardSummary.value,
                companySettings = companySettings.value
            )
            _isBusy.value = false
            res.onSuccess { file ->
                PdfReportGenerator.printPdf(context, file, "${companySettings.value.companyName} - ${committee?.name ?: "Report"}")
                    .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Print failed.") }
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to prepare print PDF.")
            }
        }
    }

    fun preparePdfForDownload(
        context: Context,
        reportType: PdfReportType,
        selectedMember: CommitteeMember? = null,
        onReady: (File) -> Unit
    ) {
        val committee = activeCommittee.value
        val requiresCommittee = reportType != PdfReportType.PREVIOUS_COMMITTEE_HISTORY &&
            reportType != PdfReportType.COMPLETE_FINANCIAL_SUMMARY
        if (requiresCommittee && committee == null) {
            _snackbarEvents.tryEmit("Create or select a committee first.")
            return
        }
        val allCommittees = (committeesState.value as? UiState.Success)?.data ?: emptyList()
        val members = (membersState.value as? UiState.Success)?.data ?: emptyList()
        val installments = (installmentsState.value as? UiState.Success)?.data ?: emptyList()

        viewModelScope.launch {
            _isBusy.value = true
            val res = PdfReportGenerator.generateReport(
                context = context,
                reportType = reportType,
                committee = committee,
                allCommittees = allCommittees,
                members = members,
                installments = installments,
                turns = turnsState.value,
                oldRecords = oldRecords.value,
                selectedMonthKey = _selectedMonthKey.value,
                selectedMember = selectedMember,
                summary = dashboardSummary.value,
                companySettings = companySettings.value
            )
            _isBusy.value = false
            res.onSuccess(onReady)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to create PDF.") }
        }
    }

    fun savePdfToUri(context: Context, file: File, targetUri: Uri) {
        viewModelScope.launch {
            PdfReportGenerator.savePdfToUri(context, file, targetUri)
                .onSuccess { _snackbarEvents.tryEmit("PDF downloaded successfully.") }
                .onFailure { _snackbarEvents.tryEmit("Download failed: ${it.localizedMessage}") }
        }
    }

    suspend fun exportBackupJson(): Result<String> {
        _isBusy.value = true
        val res = repository.exportFullBackupJson(companySettings.value)
        _isBusy.value = false
        return res
    }

    fun restoreBackupJson(json: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.restoreFromBackupJson(
                jsonContent = json,
                actorName = actorName(),
                onRestoreSettings = { restored -> configStore.saveCompanySettings(restored) }
            )
            _isBusy.value = false
            res.onSuccess { count ->
                _snackbarEvents.tryEmit("Restored $count committees from backup.")
            }.onFailure {
                _snackbarEvents.tryEmit("Restore failed: ${it.localizedMessage ?: "Invalid JSON"}")
            }
        }
    }

    private data class SummaryInputs(
        val allCommittees: UiState<List<Committee>>,
        val activeCommittee: Committee?,
        val members: UiState<List<CommitteeMember>>,
        val installments: UiState<List<InstallmentRecord>>,
        val turns: List<CommitteeTurn>
    )

    private companion object {
        const val TAG = "CommitteeVM"
    }
}
