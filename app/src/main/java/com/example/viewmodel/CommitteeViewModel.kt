package com.example.viewmodel

import android.content.Context
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
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.CommitteeStatus
import com.example.model.DashboardSummary
import com.example.model.DispatchStatus
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.MemberStatus
import com.example.model.MonthUtils
import com.example.model.MonthlyChartPoint
import com.example.model.PaymentHistoryLog
import com.example.model.PayoutStatus
import com.example.model.ReminderChannel
import com.example.model.ReminderLog
import com.example.model.ReminderStage
import com.google.firebase.firestore.FirebaseFirestoreException
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val gateway: MessagingGateway,
    private val currentUserId: String
) : ViewModel() {

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
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

    val messagingConfig: StateFlow<MessagingGatewayConfig> = configStore.configFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), MessagingGatewayConfig())

    val customTemplates: StateFlow<Map<Pair<ReminderStage, AppLanguage>, String>> = configStore.templatesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val adminProfile = repository.observeAdminProfile(currentUserId)
        .catch { error ->
            unwrapFirestoreError(error, "profile")
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val committeesState: StateFlow<UiState<List<Committee>>> = repository.observeCommittees(currentUserId)
        .map<List<Committee>, UiState<List<Committee>>> { UiState.Success(it) }
        .catch { error ->
            val msg = unwrapFirestoreError(error, "committees")
            emit(UiState.Error(msg))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val activeCommittee: StateFlow<Committee?> = combine(
        committeesState,
        _selectedCommitteeId,
        adminProfile
    ) { state, selectedId, profile ->
        val list = (state as? UiState.Success)?.data ?: emptyList()
        val chosen = selectedId ?: profile?.activeCommitteeId
        list.firstOrNull { it.id == chosen }
            ?: list.firstOrNull { it.statusEnum == CommitteeStatus.ACTIVE }
            ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    private val activeCommitteeIdFlow = activeCommittee
        .map { it?.id.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), "")

    val membersState: StateFlow<UiState<List<CommitteeMember>>> = activeCommitteeIdFlow
        .flatMapLatest { cid ->
            if (cid.isBlank()) flowOf<UiState<List<CommitteeMember>>>(UiState.Success(emptyList()))
            else repository.observeMembers(currentUserId, cid)
                .map<List<CommitteeMember>, UiState<List<CommitteeMember>>> { UiState.Success(it) }
                .catch { error -> emit(UiState.Error(unwrapFirestoreError(error, "members"))) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val installmentsState: StateFlow<UiState<List<InstallmentRecord>>> = activeCommitteeIdFlow
        .flatMapLatest { cid ->
            if (cid.isBlank()) flowOf<UiState<List<InstallmentRecord>>>(UiState.Success(emptyList()))
            else repository.observeInstallments(currentUserId, cid)
                .map<List<InstallmentRecord>, UiState<List<InstallmentRecord>>> { UiState.Success(it) }
                .catch { error -> emit(UiState.Error(unwrapFirestoreError(error, "installments"))) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val paymentLogs: StateFlow<List<PaymentHistoryLog>> = activeCommitteeIdFlow
        .flatMapLatest { cid ->
            if (cid.isBlank()) flowOf(emptyList())
            else repository.observePaymentLogs(currentUserId, cid)
                .catch { error ->
                    unwrapFirestoreError(error, "paymentLogs")
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val reminderLogs: StateFlow<List<ReminderLog>> = activeCommitteeIdFlow
        .flatMapLatest { cid ->
            if (cid.isBlank()) flowOf(emptyList())
            else repository.observeReminderLogs(currentUserId, cid)
                .catch { error ->
                    unwrapFirestoreError(error, "reminderLogs")
                    emit(emptyList())
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val dashboardSummary: StateFlow<DashboardSummary?> = combine(
        activeCommittee,
        membersState,
        installmentsState,
        _selectedMonthKey
    ) { committee, mState, iState, selectedMonth ->
        if (committee == null) return@combine null
        val members = (mState as? UiState.Success)?.data ?: emptyList()
        val installments = (iState as? UiState.Success)?.data ?: emptyList()
        computeSummary(committee, members, installments, selectedMonth)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    init {
        viewModelScope.launch {
            repository.ensureAdminProfile(messagingConfig.value.localLanguageCode)
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
    }

    private fun unwrapFirestoreError(error: Throwable, label: String): String {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w(TAG, "Error observing $label", rootError)
        return rootError.localizedMessage ?: "Failed to load $label"
    }

    private fun computeSummary(
        committee: Committee,
        members: List<CommitteeMember>,
        installments: List<InstallmentRecord>,
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

        val completedPayouts = activeMembers.count { it.payoutStatusEnum == PayoutStatus.PAID }
        val nextPayout = activeMembers.firstOrNull { it.payoutStatusEnum == PayoutStatus.PENDING }
        val upcomingPayoutDate = if (nextPayout != null) {
            MonthUtils.formatPayoutDate(nextPayout.payoutMonth, committee.payoutDay, committee.collectionDayStart)
        } else {
            MonthUtils.formatPayoutDate(effectiveMonthKey, committee.payoutDay, committee.collectionDayStart)
        }

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

        return DashboardSummary(
            activeMemberCount = activeMembers.size,
            targetMemberCount = committee.memberCount.toInt(),
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
            nextPayoutMember = nextPayout,
            upcomingPayoutDate = upcomingPayoutDate,
            completedPayouts = completedPayouts,
            remainingMonths = remainingMonths,
            monthlyChartPoints = chartPoints
        )
    }

    fun selectCommittee(committeeId: String) {
        _selectedCommitteeId.value = committeeId
        viewModelScope.launch {
            repository.updateProfilePreferences(activeCommitteeId = committeeId)
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
            repository.updateProfilePreferences(preferredLanguage = language.code)
        }
    }

    fun createCommittee(
        name: String,
        memberCount: Long,
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
                name, memberCount, monthlyInstallment, totalMonths,
                collectionDayStart, collectionDayEnd, payoutDay, startMonth, notes
            )
            _isBusy.value = false
            res.onSuccess { newId ->
                _selectedCommitteeId.value = newId
                _selectedMonthKey.value = startMonth
                repository.updateProfilePreferences(activeCommitteeId = newId)
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
                existing, newName, newMemberCount, newMonthlyInstallment, newTotalMonths,
                newCollectionDayStart, newCollectionDayEnd, newPayoutDay, newStartMonth, newNotes
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
            repository.updateCommitteeStatus(committeeId, status)
                .onSuccess { _snackbarEvents.tryEmit("Committee marked as ${status.name}.") }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update status.") }
        }
    }

    fun addMember(name: String, phone: String, committeeNumber: Long) {
        val committee = activeCommittee.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.addMember(committee, name, phone, committeeNumber)
            _isBusy.value = false
            res.onSuccess {
                _snackbarEvents.tryEmit("Member \"$name\" added (#$committeeNumber).")
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to add member.")
            }
        }
    }

    fun updateMember(member: CommitteeMember, newName: String, newPhone: String, newCommitteeNumber: Long) {
        val committee = activeCommittee.value ?: return
        viewModelScope.launch {
            repository.updateMember(committee, member, newName, newPhone, newCommitteeNumber)
                .onSuccess { _snackbarEvents.tryEmit("Member updated.") }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update member.") }
        }
    }

    fun archiveMember(member: CommitteeMember) {
        val committee = activeCommittee.value ?: return
        viewModelScope.launch {
            repository.archiveMember(committee.id, member.id)
                .onSuccess { _snackbarEvents.tryEmit("${member.name} archived (history preserved).") }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to archive member.") }
        }
    }

    fun swapPayoutOrder(first: CommitteeMember, second: CommitteeMember) {
        val committee = activeCommittee.value ?: return
        viewModelScope.launch {
            repository.swapMemberPayoutOrder(committee, first, second)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to reorder payout.") }
        }
    }

    fun setPayoutStatus(member: CommitteeMember, status: PayoutStatus, date: String? = null) {
        val committee = activeCommittee.value ?: return
        viewModelScope.launch {
            repository.updateMemberPayoutStatus(committee, member, status, date)
                .onSuccess { _snackbarEvents.tryEmit("Payout for ${member.name}: ${status.name}") }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update payout.") }
        }
    }

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
                paymentNote = note
            ).onSuccess {
                _snackbarEvents.tryEmit("Saved ${record.memberName}: ${status.name}")
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to save payment.")
            }
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
        return gateway.renderMessage(template, member, committee, _selectedMonthKey.value, dueAmount)
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
            _snackbarEvents.tryEmit("WhatsApp Cloud API not configured. Use Manual WhatsApp mode or configure API in Settings.")
            return
        }
        if (!useWhatsApp && !config.isSmsGatewayConfigured) {
            _snackbarEvents.tryEmit("SMS Gateway URL not configured. Use Manual SMS mode or configure Gateway in Settings.")
            return
        }

        viewModelScope.launch {
            _isBusy.value = true
            var sent = 0
            var failed = 0
            val monthKey = _selectedMonthKey.value
            for ((member, dueAmt) in recipients) {
                val message = gateway.renderMessage(template, member, committee, monthKey, dueAmt)
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
        val message = gateway.renderMessage(template, member, committee, monthKey, dueAmount)
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

    fun generateAndOpenPdf(
        context: Context,
        reportType: PdfReportType,
        selectedMember: CommitteeMember?,
        shareMode: Boolean
    ) {
        val committee = activeCommittee.value ?: return
        val members = (membersState.value as? UiState.Success)?.data ?: emptyList()
        val installments = (installmentsState.value as? UiState.Success)?.data ?: emptyList()
        val summary = dashboardSummary.value ?: return

        viewModelScope.launch {
            _isBusy.value = true
            val res = PdfReportGenerator.generateReport(
                context = context,
                reportType = reportType,
                committee = committee,
                members = members,
                installments = installments,
                selectedMonthKey = _selectedMonthKey.value,
                selectedMember = selectedMember,
                summary = summary
            )
            _isBusy.value = false
            res.onSuccess { file: File ->
                val opened = PdfReportGenerator.openOrSharePdf(context, file, shareMode)
                if (!opened) {
                    _snackbarEvents.tryEmit("PDF saved (${file.name}), no PDF viewer app installed.")
                }
            }.onFailure {
                _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to generate PDF.")
            }
        }
    }

    suspend fun exportBackupJson(): Result<String> {
        _isBusy.value = true
        val res = repository.exportFullBackupJson()
        _isBusy.value = false
        return res
    }

    fun restoreBackupJson(json: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.restoreFromBackupJson(json)
            _isBusy.value = false
            res.onSuccess { count ->
                _snackbarEvents.tryEmit("Restored $count committees from backup.")
            }.onFailure {
                _snackbarEvents.tryEmit("Restore failed: ${it.localizedMessage ?: "Invalid JSON"}")
            }
        }
    }

    private companion object {
        const val TAG = "CommitteeVM"
    }
}
