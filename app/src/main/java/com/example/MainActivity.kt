package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.CommitteeRepository
import com.example.data.MessagingConfigStore
import com.example.data.MessagingGateway
import com.example.data.MessagingGatewayConfig
import com.example.data.PdfReportType
import com.example.i18n.AppLanguage
import com.example.i18n.stringsFor
import com.example.model.BackupPreview
import com.example.model.BackupUtils
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.MemberStatus
import com.example.model.MonthUtils
import com.example.model.PayoutStatus
import com.example.model.ReminderStage
import com.example.ui.components.CommitteeEditorDialog
import com.example.ui.components.CommitteeSwitcherDialog
import com.example.ui.components.MemberDetailDialog
import com.example.ui.components.MemberEditorDialog
import com.example.ui.components.MessagingGatewayConfigDialog
import com.example.ui.components.PaymentActivityHistoryDialog
import com.example.ui.components.RecordPaymentDialog
import com.example.ui.components.RecordPayoutDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryReportsSettingsScreen
import com.example.ui.screens.InstallmentsScreen
import com.example.ui.screens.MembersAndPayoutsScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.theme.CommitteeManagerTheme
import com.example.viewmodel.CommitteeViewModel
import com.example.viewmodel.UiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class MainTab(val icon: ImageVector, val testTag: String) {
    DASHBOARD(Icons.Rounded.Dashboard, "nav_dashboard"),
    MEMBERS(Icons.Rounded.Group, "nav_members"),
    INSTALLMENTS(Icons.Rounded.Payments, "nav_installments"),
    REMINDERS(Icons.Rounded.Sms, "nav_reminders"),
    REPORTS(Icons.Rounded.Assessment, "nav_reports")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val configStore = remember(context) { MessagingConfigStore(context.applicationContext) }
            val config by configStore.configFlow.collectAsStateWithLifecycle(initialValue = MessagingGatewayConfig())
            val localLanguage = if (config.localLanguageCode == "ur") AppLanguage.UR else AppLanguage.EN

            CommitteeManagerTheme(isUrdu = localLanguage == AppLanguage.UR) {
                MainFeatureShell(initialLanguage = localLanguage)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainFeatureShell(
    initialLanguage: AppLanguage
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val viewModel: CommitteeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                CommitteeViewModel(
                    repository = CommitteeRepository(app),
                    configStore = MessagingConfigStore(app),
                    gateway = MessagingGateway()
                )
            }
        }
    )

    val committeesState by viewModel.committeesState.collectAsStateWithLifecycle()
    val activeCommittee by viewModel.activeCommittee.collectAsStateWithLifecycle()
    val membersState by viewModel.membersState.collectAsStateWithLifecycle()
    val installmentsState by viewModel.installmentsState.collectAsStateWithLifecycle()
    val paymentLogs by viewModel.paymentLogs.collectAsStateWithLifecycle()
    val reminderLogs by viewModel.reminderLogs.collectAsStateWithLifecycle()
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val selectedMonthKey by viewModel.selectedMonthKey.collectAsStateWithLifecycle()
    val memberSearchQuery by viewModel.memberSearchQuery.collectAsStateWithLifecycle()
    val installmentFilter by viewModel.installmentFilter.collectAsStateWithLifecycle()
    val gatewayConfig by viewModel.messagingConfig.collectAsStateWithLifecycle()
    val customTemplates by viewModel.customTemplates.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    val effectiveLang = when (gatewayConfig.localLanguageCode) {
        "ur" -> AppLanguage.UR
        else -> initialLanguage
    }
    val strings = stringsFor(effectiveLang)

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var currentTab by rememberSaveable { mutableStateOf(MainTab.DASHBOARD) }
    if (currentTab != MainTab.DASHBOARD) {
        BackHandler { currentTab = MainTab.DASHBOARD }
    }

    // Dialog states
    var showCreateCommittee by remember { mutableStateOf(false) }
    var committeeToEdit by remember { mutableStateOf<Committee?>(null) }
    var showCommitteeSwitcher by remember { mutableStateOf(false) }
    var showAddMember by remember { mutableStateOf(false) }
    var memberToEdit by remember { mutableStateOf<CommitteeMember?>(null) }
    var memberToDetail by remember { mutableStateOf<CommitteeMember?>(null) }
    var installmentToEdit by remember { mutableStateOf<InstallmentRecord?>(null) }
    var showGatewayConfig by remember { mutableStateOf(false) }
    var payoutTargetMember by remember { mutableStateOf<CommitteeMember?>(null) }
    var showActivityHistory by remember { mutableStateOf(false) }
    var pendingRestoreData by remember { mutableStateOf<Pair<String, BackupPreview>?>(null) }

    // SAF Backup Export / Restore launchers
    var pendingBackupJson by remember { mutableStateOf<String?>(null) }
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingBackupJson
        pendingBackupJson = null
        if (uri != null && json != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                scope.launch { snackbarHostState.showSnackbar("Backup exported successfully.") }
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Export failed: ${e.localizedMessage}") }
            }
        }
    }
    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!json.isNullOrBlank()) {
                    val previewRes = BackupUtils.parsePreview(json)
                    previewRes.onSuccess { preview ->
                        pendingRestoreData = json to preview
                    }.onFailure {
                        scope.launch { snackbarHostState.showSnackbar("Invalid GM HASMANI backup JSON file.") }
                    }
                }
            } catch (_: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Could not read backup file.") }
            }
        }
    }

    val committees = (committeesState as? UiState.Success)?.data ?: emptyList()
    val members = (membersState as? UiState.Success)?.data ?: emptyList()
    val installments = (installmentsState as? UiState.Success)?.data ?: emptyList()

    val nextSuggestedNumber = remember(members) {
        val activeNums = members.filter { it.memberStatusEnum == MemberStatus.ACTIVE }.map { it.committeeNumber }
        ((activeNums.maxOrNull() ?: 0L) + 1L)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_gm_committee_fg),
                                        contentDescription = "GM HASMANI Logo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.appTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        actions = {
                            FilterChip(
                                selected = effectiveLang == AppLanguage.UR,
                                onClick = {
                                    val next = if (effectiveLang == AppLanguage.EN) AppLanguage.UR else AppLanguage.EN
                                    viewModel.setLanguage(next)
                                },
                                label = {
                                    Text(
                                        text = if (effectiveLang == AppLanguage.EN) "اردو" else "EN",
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .testTag("top_language_toggle")
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    if (isBusy) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        MainTab.entries.forEach { tab ->
                            val label = tabLabel(tab, strings)
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail {
                        MainTab.entries.forEach { tab ->
                            val label = tabLabel(tab, strings)
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
                        when (val cState = committeesState) {
                            is UiState.Loading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                            is UiState.Error -> {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(cState.message, color = MaterialTheme.colorScheme.error)
                                }
                            }
                            is UiState.Success -> {
                                when (currentTab) {
                                    MainTab.DASHBOARD -> DashboardScreen(
                                        strings = strings,
                                        committee = activeCommittee,
                                        summary = summary,
                                        selectedMonthKey = selectedMonthKey,
                                        onSelectMonth = viewModel::selectMonth,
                                        onOpenCommitteeSwitcher = { showCommitteeSwitcher = true },
                                        onCreateCommittee = { showCreateCommittee = true },
                                        onEditCommittee = { committeeToEdit = it },
                                        onQuickAddMember = { showAddMember = true },
                                        onGoToInstallments = { currentTab = MainTab.INSTALLMENTS },
                                        onGoToReminders = { currentTab = MainTab.REMINDERS },
                                        onGoToReports = { currentTab = MainTab.REPORTS }
                                    )

                                    MainTab.MEMBERS -> MembersAndPayoutsScreen(
                                        strings = strings,
                                        committee = activeCommittee,
                                        members = members,
                                        searchQuery = memberSearchQuery,
                                        onSearchQueryChange = viewModel::setMemberSearchQuery,
                                        onCreateCommittee = { showCreateCommittee = true },
                                        onAddMember = { showAddMember = true },
                                        onEditMember = { memberToEdit = it },
                                        onArchiveMember = viewModel::archiveMember,
                                        onViewMemberDetail = { memberToDetail = it },
                                        onSwapOrder = viewModel::swapPayoutOrder,
                                        onRequestMarkPayoutPaid = { payoutTargetMember = it },
                                        onRevertPayoutPending = { m -> viewModel.setPayoutStatus(m, PayoutStatus.PENDING) }
                                    )

                                    MainTab.INSTALLMENTS -> InstallmentsScreen(
                                        strings = strings,
                                        committee = activeCommittee,
                                        members = members,
                                        installments = installments,
                                        selectedMonthKey = selectedMonthKey,
                                        activeFilter = installmentFilter,
                                        onSelectMonth = viewModel::selectMonth,
                                        onSelectFilter = viewModel::setInstallmentFilter,
                                        onSyncMonth = viewModel::syncCurrentMonthInstallments,
                                        onQuickMarkPaid = { rec ->
                                            viewModel.recordPayment(
                                                record = rec,
                                                paidAmount = rec.dueAmount,
                                                status = InstallmentStatus.PAID,
                                                paymentDate = MonthUtils.todayDateString(),
                                                note = null
                                            )
                                        },
                                        onEditPayment = { installmentToEdit = it },
                                        onQuickWhatsApp = { m, amt ->
                                            val tpl = if (effectiveLang == AppLanguage.UR) ReminderStage.FIRST_28.defaultTemplateUr
                                            else ReminderStage.FIRST_28.defaultTemplateEn
                                            if (gatewayConfig.isWhatsAppApiConfigured) {
                                                viewModel.sendViaApiToSelected(listOf(m to amt), true, ReminderStage.FIRST_28, tpl)
                                            } else {
                                                viewModel.launchManualDispatch(context, m, amt, true, ReminderStage.FIRST_28, tpl)
                                            }
                                        },
                                        onQuickSms = { m, amt ->
                                            val tpl = if (effectiveLang == AppLanguage.UR) ReminderStage.FIRST_28.defaultTemplateUr
                                            else ReminderStage.FIRST_28.defaultTemplateEn
                                            if (gatewayConfig.isSmsGatewayConfigured) {
                                                viewModel.sendViaApiToSelected(listOf(m to amt), false, ReminderStage.FIRST_28, tpl)
                                            } else {
                                                viewModel.launchManualDispatch(context, m, amt, false, ReminderStage.FIRST_28, tpl)
                                            }
                                        },
                                        onOpenActivityHistory = { showActivityHistory = true },
                                        onCreateCommittee = { showCreateCommittee = true }
                                    )

                                    MainTab.REMINDERS -> RemindersScreen(
                                        strings = strings,
                                        language = effectiveLang,
                                        committee = activeCommittee,
                                        members = members,
                                        installments = installments,
                                        reminderLogs = reminderLogs,
                                        selectedMonthKey = selectedMonthKey,
                                        gatewayConfig = gatewayConfig,
                                        savedTemplates = customTemplates,
                                        renderPreview = viewModel::renderReminderForMember,
                                        onSelectMonth = viewModel::selectMonth,
                                        onSaveTemplate = viewModel::saveCustomTemplate,
                                        onSendViaApi = viewModel::sendViaApiToSelected,
                                        onOpenManualApp = { m, amt, useWa, stage, tpl ->
                                            viewModel.launchManualDispatch(context, m, amt, useWa, stage, tpl)
                                        },
                                        onOpenGatewaySettings = { showGatewayConfig = true },
                                        onCreateCommittee = { showCreateCommittee = true }
                                    )

                                    MainTab.REPORTS -> HistoryReportsSettingsScreen(
                                        strings = strings,
                                        language = effectiveLang,
                                        committees = committees,
                                        activeCommittee = activeCommittee,
                                        members = members,
                                        gatewayConfig = gatewayConfig,
                                        onLanguageChange = viewModel::setLanguage,
                                        onSelectCommittee = { c ->
                                            viewModel.selectCommittee(c.id)
                                            currentTab = MainTab.DASHBOARD
                                        },
                                        onCreateCommittee = { showCreateCommittee = true },
                                        onSetCommitteeStatus = { c, st -> viewModel.setCommitteeStatus(c.id, st) },
                                        onExportPdf = { type, member, share ->
                                            viewModel.generateAndOpenPdf(context, type, member, share)
                                        },
                                        onExportBackupJson = {
                                            scope.launch {
                                                val res = viewModel.exportBackupJson()
                                                res.onSuccess { json ->
                                                    pendingBackupJson = json
                                                    exportBackupLauncher.launch("GM_HASMANI_Backup_${MonthUtils.todayDateString()}.json")
                                                }.onFailure {
                                                    snackbarHostState.showSnackbar("Backup export failed.")
                                                }
                                            }
                                        },
                                        onRestoreBackupJson = {
                                            restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
                                        },
                                        onOpenGatewayConfig = { showGatewayConfig = true }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateCommittee) {
        CommitteeEditorDialog(
            strings = strings,
            existingCommittee = null,
            hasRecordedPayments = false,
            onDismiss = { showCreateCommittee = false },
            onSave = { name, mCount, inst, months, sDay, eDay, pDay, startMonth, notes ->
                viewModel.createCommittee(name, mCount, inst, months, sDay, eDay, pDay, startMonth, notes)
                showCreateCommittee = false
            }
        )
    }

    committeeToEdit?.let { editing ->
        val hasPayments = installments.any { it.paidAmount > 0L }
        CommitteeEditorDialog(
            strings = strings,
            existingCommittee = editing,
            hasRecordedPayments = hasPayments,
            onDismiss = { committeeToEdit = null },
            onSave = { name, mCount, inst, months, sDay, eDay, pDay, startMonth, notes ->
                viewModel.updateCommittee(editing, name, mCount, inst, months, sDay, eDay, pDay, startMonth, notes)
                committeeToEdit = null
            }
        )
    }

    if (showCommitteeSwitcher) {
        CommitteeSwitcherDialog(
            strings = strings,
            committees = committees,
            activeCommitteeId = activeCommittee?.id,
            onSelect = {
                viewModel.selectCommittee(it.id)
                showCommitteeSwitcher = false
            },
            onCreateNew = {
                showCommitteeSwitcher = false
                showCreateCommittee = true
            },
            onDismiss = { showCommitteeSwitcher = false }
        )
    }

    if (showAddMember) {
        MemberEditorDialog(
            strings = strings,
            existingMember = null,
            suggestedCommitteeNumber = nextSuggestedNumber,
            existingMembers = members,
            onDismiss = { showAddMember = false },
            onSave = { name, phone, number ->
                viewModel.addMember(name, phone, number)
                showAddMember = false
            }
        )
    }

    memberToEdit?.let { m ->
        MemberEditorDialog(
            strings = strings,
            existingMember = m,
            suggestedCommitteeNumber = m.committeeNumber,
            existingMembers = members,
            onDismiss = { memberToEdit = null },
            onSave = { name, phone, number ->
                viewModel.updateMember(m, name, phone, number)
                memberToEdit = null
            }
        )
    }

    memberToDetail?.let { m ->
        val tpl = if (effectiveLang == AppLanguage.UR) ReminderStage.FIRST_28.defaultTemplateUr
        else ReminderStage.FIRST_28.defaultTemplateEn
        val amt = activeCommittee?.monthlyInstallment ?: 5000L
        MemberDetailDialog(
            strings = strings,
            member = m,
            installments = installments,
            onDismiss = { memberToDetail = null },
            onExportPdf = {
                viewModel.generateAndOpenPdf(context, PdfReportType.INDIVIDUAL_HISTORY, m, false)
            },
            onOpenWhatsApp = {
                viewModel.launchManualDispatch(context, m, amt, true, ReminderStage.CUSTOM, tpl)
            },
            onOpenSms = {
                viewModel.launchManualDispatch(context, m, amt, false, ReminderStage.CUSTOM, tpl)
            }
        )
    }

    installmentToEdit?.let { rec ->
        RecordPaymentDialog(
            strings = strings,
            record = rec,
            logs = paymentLogs,
            onDismiss = { installmentToEdit = null },
            onSave = { paidAmount, status, paymentDate, note ->
                viewModel.recordPayment(rec, paidAmount, status, paymentDate, note)
                installmentToEdit = null
            }
        )
    }

    payoutTargetMember?.let { member ->
        activeCommittee?.let { committee ->
            RecordPayoutDialog(
                strings = strings,
                committee = committee,
                member = member,
                onDismiss = { payoutTargetMember = null },
                onConfirm = { amount, date ->
                    viewModel.setPayoutStatus(member, PayoutStatus.PAID, date = date, payoutAmount = amount)
                    payoutTargetMember = null
                }
            )
        }
    }

    if (showActivityHistory) {
        PaymentActivityHistoryDialog(
            strings = strings,
            logs = paymentLogs,
            onDismiss = { showActivityHistory = false }
        )
    }

    pendingRestoreData?.let { (json, preview) ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingRestoreData = null },
            title = { Text(strings.confirmRestoreTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.confirmRestoreBody)
                    Text(
                        text = "• Committees: ${preview.committeeCount}\n• Members: ${preview.memberCount}\n• Installments: ${preview.installmentCount}" +
                            if (preview.exportedAt.isNotBlank()) "\n• Exported: ${preview.exportedAt}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        viewModel.restoreBackupJson(json)
                        pendingRestoreData = null
                    },
                    modifier = Modifier.testTag("confirm_restore_btn")
                ) {
                    Text(strings.confirm)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingRestoreData = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showGatewayConfig) {
        MessagingGatewayConfigDialog(
            strings = strings,
            currentConfig = gatewayConfig,
            onDismiss = { showGatewayConfig = false },
            onSave = { waPhone, waToken, smsUrl, smsKey, smsSender ->
                viewModel.saveGatewayConfig(waPhone, waToken, smsUrl, smsKey, smsSender)
                showGatewayConfig = false
            }
        )
    }
}

private fun tabLabel(tab: MainTab, strings: com.example.i18n.UiStrings): String = when (tab) {
    MainTab.DASHBOARD -> strings.navDashboard
    MainTab.MEMBERS -> strings.navMembers
    MainTab.INSTALLMENTS -> strings.navInstallments
    MainTab.REMINDERS -> strings.navReminders
    MainTab.REPORTS -> strings.navReports
}
