package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import com.example.i18n.UiStrings
import com.example.i18n.stringsFor
import com.example.model.BackupPreview
import com.example.model.BackupUtils
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.CommitteeTurn
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.MemberStatus
import com.example.model.MonthUtils
import com.example.model.OldCommitteeRecord
import com.example.model.PaymentMethod
import com.example.model.PayoutStatus
import com.example.model.ReminderStage
import com.example.model.UploadQueueStatus
import com.example.ui.components.AddManualPaymentDialog
import com.example.ui.components.AdminDeleteConfirmDialog
import com.example.ui.components.AppSettingsDialog
import com.example.ui.components.ChangePasswordDialog
import com.example.ui.components.CommitteeDetailDialog
import com.example.ui.components.CommitteeEditorDialog
import com.example.ui.components.CommitteeSwitcherDialog
import com.example.ui.components.MemberDetailDialog
import com.example.ui.components.MemberEditorDialog
import com.example.ui.components.MessagingGatewayConfigDialog
import com.example.ui.components.OldCommitteeRecordDetailDialog
import com.example.ui.components.OldCommitteeRecordEditorDialog
import com.example.ui.components.PaymentActivityHistoryDialog
import com.example.ui.components.RecordPaymentDialog
import com.example.ui.components.RecordPayoutDialog
import com.example.ui.components.TurnEditorDialog
import com.example.ui.components.UploadQueueDialog
import com.example.ui.components.UserManagementDialog
import com.example.ui.screens.CommitteeTurnsScreen
import com.example.ui.screens.CommitteesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HistoryReportsSettingsScreen
import com.example.ui.screens.InstallmentsScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.OldHistoryScreen
import com.example.ui.screens.PdfReportsScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.theme.CommitteeManagerTheme
import com.example.ui.theme.StatusUnpaidRed
import com.example.viewmodel.CommitteeViewModel
import com.example.viewmodel.UiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

enum class AppSection(val icon: ImageVector, val testTag: String) {
    DASHBOARD(Icons.Rounded.Dashboard, "nav_dashboard"),
    COMMITTEES(Icons.Rounded.AccountBalance, "nav_committees"),
    MEMBERS(Icons.Rounded.Group, "nav_members"),
    PAYMENTS(Icons.Rounded.Payments, "nav_installments"),
    COMMITTEE_TURNS(Icons.AutoMirrored.Rounded.ListAlt, "nav_turns"),
    MESSAGE_REQUESTS(Icons.Rounded.Sms, "nav_reminders"),
    PDF_REPORTS(Icons.Rounded.PictureAsPdf, "nav_pdf_reports"),
    OLD_RECORDS(Icons.Rounded.History, "nav_old_history"),
    SETTINGS(Icons.Rounded.Settings, "nav_settings")
}

private val BOTTOM_BAR_SECTIONS = listOf(
    AppSection.DASHBOARD,
    AppSection.COMMITTEES,
    AppSection.PAYMENTS,
    AppSection.COMMITTEE_TURNS,
    AppSection.SETTINGS
)

private sealed interface PendingDeleteTarget {
    data class CommitteeTarget(val committee: Committee) : PendingDeleteTarget
    data class MemberTarget(val member: CommitteeMember) : PendingDeleteTarget
    data class InstallmentTarget(val record: InstallmentRecord) : PendingDeleteTarget
    data class UnpaidMonthTarget(val monthKey: String) : PendingDeleteTarget
    data class TurnTarget(val turn: CommitteeTurn) : PendingDeleteTarget
    data class OldRecordTarget(val record: OldCommitteeRecord) : PendingDeleteTarget
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
    val turns by viewModel.turnsState.collectAsStateWithLifecycle()
    val oldRecords by viewModel.oldRecords.collectAsStateWithLifecycle()
    val paymentLogs by viewModel.paymentLogs.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val reminderLogs by viewModel.reminderLogs.collectAsStateWithLifecycle()
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val selectedMonthKey by viewModel.selectedMonthKey.collectAsStateWithLifecycle()
    val memberSearchQuery by viewModel.memberSearchQuery.collectAsStateWithLifecycle()
    val installmentFilter by viewModel.installmentFilter.collectAsStateWithLifecycle()
    val gatewayConfig by viewModel.messagingConfig.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val customTemplates by viewModel.customTemplates.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val localUsers by viewModel.localUsers.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isLoggedOut by viewModel.isLoggedOut.collectAsStateWithLifecycle()
    val isDefaultPinActive by viewModel.isDefaultPinActive.collectAsStateWithLifecycle()
    val pendingUploads by viewModel.pendingUploads.collectAsStateWithLifecycle()
    val completedUploads by viewModel.completedUploads.collectAsStateWithLifecycle()
    val failedUploads by viewModel.failedUploads.collectAsStateWithLifecycle()

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

    var currentSection by rememberSaveable { mutableStateOf(AppSection.DASHBOARD) }
    if (currentSection != AppSection.DASHBOARD && !isLoggedOut) {
        BackHandler { currentSection = AppSection.DASHBOARD }
    }

    // Dialog states
    var showCreateCommittee by remember { mutableStateOf(false) }
    var committeeToView by remember { mutableStateOf<Committee?>(null) }
    var committeeToEdit by remember { mutableStateOf<Committee?>(null) }
    var showCommitteeSwitcher by remember { mutableStateOf(false) }
    var showAddMember by remember { mutableStateOf(false) }
    var memberToEdit by remember { mutableStateOf<CommitteeMember?>(null) }
    var memberToDetail by remember { mutableStateOf<CommitteeMember?>(null) }
    var installmentToEdit by remember { mutableStateOf<InstallmentRecord?>(null) }
    var showAddManualPayment by remember { mutableStateOf(false) }
    var showAddTurnDialog by remember { mutableStateOf(false) }
    var turnToEdit by remember { mutableStateOf<CommitteeTurn?>(null) }
    var showAddOldRecord by remember { mutableStateOf(false) }
    var oldRecordToEdit by remember { mutableStateOf<OldCommitteeRecord?>(null) }
    var oldRecordToView by remember { mutableStateOf<OldCommitteeRecord?>(null) }
    var showGatewayConfig by remember { mutableStateOf(false) }
    var payoutTargetMember by remember { mutableStateOf<CommitteeMember?>(null) }
    var showActivityHistory by remember { mutableStateOf(false) }
    var pendingRestoreData by remember { mutableStateOf<Pair<String, BackupPreview>?>(null) }
    var pendingDeleteTarget by remember { mutableStateOf<PendingDeleteTarget?>(null) }
    var deleteErrorMsg by remember { mutableStateOf<String?>(null) }

    // Simple Settings dialogs
    var showAppSettings by remember { mutableStateOf(false) }
    var showUserManagement by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var uploadDialogStatus by remember { mutableStateOf<UploadQueueStatus?>(null) }
    var markUploadsCompleteOnBackup by remember { mutableStateOf(false) }

    // SAF PDF Download launcher
    var pendingPdfFile by remember { mutableStateOf<File?>(null) }
    val downloadPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        val file = pendingPdfFile
        pendingPdfFile = null
        if (uri != null && file != null) {
            viewModel.savePdfToUri(context, file, uri)
        }
    }

    fun triggerDownloadPdf(reportType: PdfReportType, member: CommitteeMember? = null) {
        viewModel.preparePdfForDownload(context, reportType, member) { generatedFile ->
            pendingPdfFile = generatedFile
            downloadPdfLauncher.launch(generatedFile.name)
        }
    }

    // SAF CSV Import / Export for Old Committee Records
    val importCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val csv = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!csv.isNullOrBlank()) {
                    viewModel.importOldRecordsCsv(csv)
                } else {
                    scope.launch { snackbarHostState.showSnackbar("Selected CSV file is empty.") }
                }
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("Could not read CSV: ${e.localizedMessage}") }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                val csvText = viewModel.exportOldRecordsCsv()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csvText.toByteArray(Charsets.UTF_8))
                }
                scope.launch { snackbarHostState.showSnackbar("Old committee records CSV exported.") }
            } catch (e: Exception) {
                scope.launch { snackbarHostState.showSnackbar("CSV export failed: ${e.localizedMessage}") }
            }
        }
    }

    // SAF Backup Export / Restore launchers
    var pendingBackupJson by remember { mutableStateOf<String?>(null) }
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val json = pendingBackupJson
        val shouldMarkComplete = markUploadsCompleteOnBackup
        pendingBackupJson = null
        markUploadsCompleteOnBackup = false
        if (uri != null && json != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                if (shouldMarkComplete) {
                    viewModel.markQueueCompletedLocally()
                } else {
                    scope.launch { snackbarHostState.showSnackbar("Backup exported successfully.") }
                }
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

    fun startBackupExport(markQueueCompleted: Boolean = false) {
        scope.launch {
            val res = viewModel.exportBackupJson()
            res.onSuccess { json ->
                pendingBackupJson = json
                markUploadsCompleteOnBackup = markQueueCompleted
                exportBackupLauncher.launch("GM_HASMANI_Backup_${MonthUtils.todayDateString()}.json")
            }.onFailure {
                snackbarHostState.showSnackbar("Backup export failed.")
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
    val nextSuggestedTurnNumber = remember(turns) {
        ((turns.maxOfOrNull { it.turnNumber } ?: 0L) + 1L)
    }

    if (isLoggedOut) {
        LocalLoginScreen(
            strings = strings,
            showDefaultPinHint = isDefaultPinActive,
            isBusy = isBusy,
            onSignIn = { username, pin, onErr ->
                viewModel.loginLocalUser(username, pin, onErr)
            },
            onLanguageToggle = {
                val next = if (effectiveLang == AppLanguage.EN) AppLanguage.UR else AppLanguage.EN
                viewModel.setLanguage(next)
            },
            currentLang = effectiveLang
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 700.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        navigationIcon = {
                            if (currentSection != AppSection.DASHBOARD) {
                                IconButton(
                                    onClick = { currentSection = AppSection.DASHBOARD },
                                    modifier = Modifier.testTag("top_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = strings.backLabel
                                    )
                                }
                            }
                        },
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
                                    text = companySettings.companyName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { viewModel.refreshAll() },
                                modifier = Modifier.testTag("top_refresh_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = strings.refreshLabel
                                )
                            }
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
                    // 9-section mobile quick-navigation bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppSection.entries.forEach { sec ->
                            val label = sectionLabel(sec, strings)
                            val selected = currentSection == sec
                            FilterChip(
                                selected = selected,
                                onClick = { currentSection = sec },
                                leadingIcon = {
                                    Icon(
                                        imageVector = sec.icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("chip_${sec.testTag}")
                            )
                        }
                    }
                    if (isBusy) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        BOTTOM_BAR_SECTIONS.forEach { sec ->
                            val label = sectionLabel(sec, strings)
                            NavigationBarItem(
                                selected = currentSection == sec,
                                onClick = { currentSection = sec },
                                icon = { Icon(sec.icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.testTag(sec.testTag)
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
                    NavigationRail(
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        AppSection.entries.forEach { sec ->
                            val label = sectionLabel(sec, strings)
                            NavigationRailItem(
                                selected = currentSection == sec,
                                onClick = { currentSection = sec },
                                icon = { Icon(sec.icon, contentDescription = label) },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag("rail_${sec.testTag}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 820.dp).fillMaxSize()) {
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
                                when (currentSection) {
                                    AppSection.DASHBOARD -> DashboardScreen(
                                        strings = strings,
                                        companySettings = companySettings,
                                        committee = activeCommittee,
                                        summary = summary,
                                        members = members,
                                        installments = installments,
                                        turns = turns,
                                        selectedMonthKey = selectedMonthKey,
                                        onSelectMonth = viewModel::selectMonth,
                                        onOpenCommitteeSwitcher = { showCommitteeSwitcher = true },
                                        onCreateCommittee = { showCreateCommittee = true },
                                        onViewCommittee = { committeeToView = it },
                                        onEditCommittee = { committeeToEdit = it },
                                        onDeleteCommittee = {
                                            deleteErrorMsg = null
                                            pendingDeleteTarget = PendingDeleteTarget.CommitteeTarget(it)
                                        },
                                        onQuickAddMember = { showAddMember = true },
                                        onGoToInstallments = { currentSection = AppSection.PAYMENTS },
                                        onGoToTurns = { currentSection = AppSection.COMMITTEE_TURNS },
                                        onGoToReminders = { currentSection = AppSection.MESSAGE_REQUESTS },
                                        onGoToReports = { currentSection = AppSection.PDF_REPORTS },
                                        onPrintSummary = { viewModel.printReport(context, PdfReportType.COMPLETE_FINANCIAL_SUMMARY) },
                                        onDownloadSummary = { triggerDownloadPdf(PdfReportType.COMPLETE_FINANCIAL_SUMMARY) },
                                        onOpenAuditHistory = { showActivityHistory = true },
                                        onRefresh = { viewModel.refreshAll() }
                                    )

                                    AppSection.COMMITTEES -> CommitteesScreen(
                                        strings = strings,
                                        committees = committees,
                                        activeCommittee = activeCommittee,
                                        currency = companySettings.currency,
                                        onSelectCommittee = { c -> viewModel.selectCommittee(c.id) },
                                        onCreateCommittee = { showCreateCommittee = true },
                                        onViewCommittee = { committeeToView = it },
                                        onEditCommittee = { committeeToEdit = it },
                                        onSetCommitteeStatus = { c, st -> viewModel.setCommitteeStatus(c.id, st) },
                                        onDeleteCommittee = { c ->
                                            deleteErrorMsg = null
                                            pendingDeleteTarget = PendingDeleteTarget.CommitteeTarget(c)
                                        }
                                    )

                                    AppSection.MEMBERS -> MembersScreen(
                                        strings = strings,
                                        committee = activeCommittee,
                                        members = members,
                                        installments = installments,
                                        currency = companySettings.currency,
                                        searchQuery = memberSearchQuery,
                                        onSearchQueryChange = viewModel::setMemberSearchQuery,
                                        onCreateCommittee = { showCreateCommittee = true },
                                        onAddMember = { showAddMember = true },
                                        onEditMember = { memberToEdit = it },
                                        onArchiveMember = viewModel::archiveMember,
                                        onDeleteMember = { m ->
                                            deleteErrorMsg = null
                                            pendingDeleteTarget = PendingDeleteTarget.MemberTarget(m)
                                        },
                                        onViewMemberDetail = { memberToDetail = it }
                                    )

                                    AppSection.PAYMENTS -> InstallmentsScreen(
                                        strings = strings,
                                        committee = activeCommittee,
                                        members = members,
                                        installments = installments,
                                        currency = companySettings.currency,
                                        selectedMonthKey = selectedMonthKey,
                                        activeFilter = installmentFilter,
                                        onSelectMonth = viewModel::selectMonth,
                                        onSelectFilter = viewModel::setInstallmentFilter,
                                        onAddManualPayment = { showAddManualPayment = true },
                                        onSyncMonth = viewModel::syncCurrentMonthInstallments,
                                        onDeleteUnpaidMonth = { monthKey ->
                                            deleteErrorMsg = null
                                            pendingDeleteTarget = PendingDeleteTarget.UnpaidMonthTarget(monthKey)
                                        },
                                        onQuickMarkPaid = { rec ->
                                            viewModel.recordPayment(
                                                record = rec,
                                                paidAmount = rec.dueAmount,
                                                status = InstallmentStatus.PAID,
                                                paymentDate = MonthUtils.todayDateString(),
                                                paymentMethod = rec.paymentMethodEnum,
                                                transactionId = rec.transactionId,
                                                note = null
                                            )
                                        },
                                        onEditPayment = { installmentToEdit = it },
                                        onDeleteInstallment = { rec ->
                                            deleteErrorMsg = null
                                            pendingDeleteTarget = PendingDeleteTarget.InstallmentTarget(rec)
                                        },
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

                                    AppSection.COMMITTEE_TURNS -> CommitteeTurnsScreen(
                                        strings = strings,
                                        committee = activeCommittee,
                                        turns = turns,
                                        currency = companySettings.currency,
                                        onCreateCommittee = { showCreateCommittee = true },
                                        onAddTurn = { showAddTurnDialog = true },
                                        onEditTurn = { turnToEdit = it },
                                        onSwapOrder = viewModel::swapTurnOrder,
                                        onMarkTurnPaid = { turn ->
                                            viewModel.saveCommitteeTurn(
                                                existingTurn = turn,
                                                turnNumber = turn.turnNumber,
                                                recipientMemberId = turn.recipientMemberId,
                                                recipientName = turn.recipientName,
                                                recipientPhone = turn.recipientPhone,
                                                payoutAmount = turn.payoutAmount,
                                                scheduledMonth = turn.scheduledMonth,
                                                payoutDate = turn.payoutDate ?: MonthUtils.todayDateString(),
                                                status = PayoutStatus.PAID,
                                                confirmedByAdmin = true,
                                                notes = turn.notes
                                            )
                                        },
                                        onRevertTurnPending = { turn ->
                                            viewModel.saveCommitteeTurn(
                                                existingTurn = turn,
                                                turnNumber = turn.turnNumber,
                                                recipientMemberId = turn.recipientMemberId,
                                                recipientName = turn.recipientName,
                                                recipientPhone = turn.recipientPhone,
                                                payoutAmount = turn.payoutAmount,
                                                scheduledMonth = turn.scheduledMonth,
                                                payoutDate = turn.payoutDate,
                                                status = PayoutStatus.PENDING,
                                                confirmedByAdmin = turn.confirmedByAdmin,
                                                notes = turn.notes
                                            )
                                        },
                                        onDeleteTurn = { turn ->
                                            deleteErrorMsg = null
                                            pendingDeleteTarget = PendingDeleteTarget.TurnTarget(turn)
                                        },
                                        onGenerateFromShares = viewModel::generateTurnsFromMembers,
                                        onLoadKnownTurns = viewModel::loadKnownHistoryTurns
                                    )

                                    AppSection.MESSAGE_REQUESTS -> RemindersScreen(
                                        strings = strings,
                                        language = effectiveLang,
                                        companySettings = companySettings,
                                        committee = activeCommittee,
                                        members = members,
                                        installments = installments,
                                        reminderLogs = reminderLogs,
                                        selectedMonthKey = selectedMonthKey,
                                        gatewayConfig = gatewayConfig,
                                        savedTemplates = customTemplates,
                                        buildPaymentRequestText = viewModel::buildPaymentRequestText,
                                        onCopyMessage = { body -> viewModel.copyMessageToClipboard(context, body) },
                                        onShareRawWhatsApp = { phone, body ->
                                            viewModel.shareRawMessageWhatsApp(context, phone, body)
                                        },
                                        onShareRawSms = { phone, body ->
                                            viewModel.shareRawMessageSms(context, phone, body)
                                        },
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

                                    AppSection.PDF_REPORTS -> PdfReportsScreen(
                                        strings = strings,
                                        companySettings = companySettings,
                                        activeCommittee = activeCommittee,
                                        members = members,
                                        onViewPdf = { type, member ->
                                            viewModel.generateAndOpenPdf(context, type, member, false)
                                        },
                                        onPrintPdf = { type, member ->
                                            viewModel.printReport(context, type, member)
                                        },
                                        onDownloadPdf = { type, member ->
                                            triggerDownloadPdf(type, member)
                                        },
                                        onSharePdf = { type, member ->
                                            viewModel.generateAndOpenPdf(context, type, member, true)
                                        }
                                    )

                                    AppSection.OLD_RECORDS -> OldHistoryScreen(
                                        strings = strings,
                                        records = oldRecords,
                                        onAddRecord = { showAddOldRecord = true },
                                        onViewRecord = { oldRecordToView = it },
                                        onEditRecord = { oldRecordToEdit = it },
                                        onDeleteRecord = { rec ->
                                            deleteErrorMsg = null
                                            pendingDeleteTarget = PendingDeleteTarget.OldRecordTarget(rec)
                                        },
                                        onImportCsv = {
                                            importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                                        },
                                        onExportCsv = {
                                            exportCsvLauncher.launch("GM_HASMANI_Old_Committee_History_${MonthUtils.todayDateString()}.csv")
                                        },
                                        onViewHistoryPdf = {
                                            viewModel.generateAndOpenPdf(context, PdfReportType.PREVIOUS_COMMITTEE_HISTORY, null, false)
                                        }
                                    )

                                    AppSection.SETTINGS -> HistoryReportsSettingsScreen(
                                        strings = strings,
                                        companySettings = companySettings,
                                        language = effectiveLang,
                                        currentUser = currentUser,
                                        pendingUploadCount = pendingUploads.size,
                                        completedUploadCount = completedUploads.size,
                                        failedUploadCount = failedUploads.size,
                                        onSaveCompanySettings = viewModel::saveCompanySettings,
                                        onLanguageChange = viewModel::setLanguage,
                                        onOpenAppSettings = { showAppSettings = true },
                                        onOpenUploadPending = { uploadDialogStatus = UploadQueueStatus.PENDING },
                                        onOpenUploadCompleted = { uploadDialogStatus = UploadQueueStatus.COMPLETED },
                                        onOpenFailedUploads = { uploadDialogStatus = UploadQueueStatus.FAILED },
                                        onRetryUpload = { viewModel.retryUploads() },
                                        onExportBackupJson = { startBackupExport(false) },
                                        onRestoreBackupJson = {
                                            restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
                                        },
                                        onOpenUserManagement = { showUserManagement = true },
                                        onOpenChangePassword = { showChangePassword = true },
                                        onOpenAuditLog = { showActivityHistory = true },
                                        onLogout = { viewModel.logout() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Dialogs
    // -------------------------------------------------------------------------

    if (showCreateCommittee) {
        CommitteeEditorDialog(
            strings = strings,
            existingCommittee = null,
            defaultSettings = companySettings,
            hasRecordedPayments = false,
            onDismiss = { showCreateCommittee = false },
            onSave = { name, mCount, totalShares, inst, months, sDay, eDay, pDay, startMonth, notes ->
                viewModel.createCommittee(name, mCount, totalShares, inst, months, sDay, eDay, pDay, startMonth, notes)
                showCreateCommittee = false
            }
        )
    }

    committeeToView?.let { viewing ->
        val activeMemberCount = if (viewing.id == activeCommittee?.id) {
            members.count { it.memberStatusEnum == MemberStatus.ACTIVE }
        } else {
            viewing.memberCount.toInt()
        }
        CommitteeDetailDialog(
            strings = strings,
            committee = viewing,
            activeMemberCount = activeMemberCount,
            onEdit = {
                committeeToView = null
                committeeToEdit = viewing
            },
            onPrint = {
                viewModel.selectCommittee(viewing.id)
                viewModel.printReport(context, PdfReportType.INDIVIDUAL_COMMITTEE)
            },
            onDownload = {
                viewModel.selectCommittee(viewing.id)
                triggerDownloadPdf(PdfReportType.INDIVIDUAL_COMMITTEE)
            },
            onDismiss = { committeeToView = null }
        )
    }

    committeeToEdit?.let { editing ->
        val hasPayments = editing.id == activeCommittee?.id && installments.any { it.paidAmount > 0L }
        CommitteeEditorDialog(
            strings = strings,
            existingCommittee = editing,
            defaultSettings = companySettings,
            hasRecordedPayments = hasPayments,
            onDismiss = { committeeToEdit = null },
            onSave = { name, mCount, totalShares, inst, months, sDay, eDay, pDay, startMonth, notes ->
                viewModel.updateCommittee(editing, name, mCount, totalShares, inst, months, sDay, eDay, pDay, startMonth, notes)
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
            monthlyShareContribution = activeCommittee?.monthlyInstallment ?: companySettings.defaultMonthlyContribution,
            existingMembers = members,
            onDismiss = { showAddMember = false },
            onSave = { name, phone, number, shares ->
                viewModel.addMember(name, phone, number, shares)
                showAddMember = false
            }
        )
    }

    memberToEdit?.let { m ->
        MemberEditorDialog(
            strings = strings,
            existingMember = m,
            suggestedCommitteeNumber = m.committeeNumber,
            monthlyShareContribution = activeCommittee?.monthlyInstallment ?: companySettings.defaultMonthlyContribution,
            existingMembers = members,
            onDismiss = { memberToEdit = null },
            onSave = { name, phone, number, shares ->
                viewModel.updateMember(m, name, phone, number, shares)
                memberToEdit = null
            }
        )
    }

    memberToDetail?.let { m ->
        val tpl = if (effectiveLang == AppLanguage.UR) ReminderStage.FIRST_28.defaultTemplateUr
        else ReminderStage.FIRST_28.defaultTemplateEn
        val perShare = activeCommittee?.monthlyInstallment ?: companySettings.defaultMonthlyContribution
        val amt = m.monthlyDue(perShare)
        MemberDetailDialog(
            strings = strings,
            member = m,
            monthlyShareContribution = perShare,
            installments = installments,
            turns = turns,
            onDismiss = { memberToDetail = null },
            onExportPdf = {
                viewModel.generateAndOpenPdf(context, PdfReportType.INDIVIDUAL_MEMBER, m, false)
            },
            onPrintPdf = {
                viewModel.printReport(context, PdfReportType.INDIVIDUAL_MEMBER, m)
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
            onReversePayment = { reason ->
                viewModel.reversePayment(rec, reason) {
                    installmentToEdit = null
                }
            },
            onSave = { paidAmount, status, paymentDate, method, txId, note ->
                viewModel.recordPayment(rec, paidAmount, status, paymentDate, method, txId, note)
                installmentToEdit = null
            }
        )
    }

    if (showAddManualPayment) {
        activeCommittee?.let { c ->
            AddManualPaymentDialog(
                strings = strings,
                committee = c,
                members = members,
                initialMonthKey = selectedMonthKey,
                onDismiss = { showAddManualPayment = false },
                onSave = { member, mKey, paidAmt, status, pDate, method, txId, note ->
                    viewModel.addOrUpdateManualPayment(member, mKey, paidAmt, status, pDate, method, txId, note)
                    showAddManualPayment = false
                }
            )
        }
    }

    if (showAddTurnDialog) {
        activeCommittee?.let { c ->
            TurnEditorDialog(
                strings = strings,
                committee = c,
                existingTurn = null,
                suggestedTurnNumber = nextSuggestedTurnNumber,
                members = members,
                onDismiss = { showAddTurnDialog = false },
                onSave = { tNum, recMemberId, recName, recPhone, pAmt, sMonth, pDate, st, confirmed, notes ->
                    viewModel.saveCommitteeTurn(
                        existingTurn = null,
                        turnNumber = tNum,
                        recipientMemberId = recMemberId,
                        recipientName = recName,
                        recipientPhone = recPhone,
                        payoutAmount = pAmt,
                        scheduledMonth = sMonth,
                        payoutDate = pDate,
                        status = st,
                        confirmedByAdmin = confirmed,
                        notes = notes
                    )
                    showAddTurnDialog = false
                }
            )
        }
    }

    turnToEdit?.let { editingTurn ->
        activeCommittee?.let { c ->
            TurnEditorDialog(
                strings = strings,
                committee = c,
                existingTurn = editingTurn,
                suggestedTurnNumber = editingTurn.turnNumber,
                members = members,
                onDismiss = { turnToEdit = null },
                onSave = { tNum, recMemberId, recName, recPhone, pAmt, sMonth, pDate, st, confirmed, notes ->
                    viewModel.saveCommitteeTurn(
                        existingTurn = editingTurn,
                        turnNumber = tNum,
                        recipientMemberId = recMemberId,
                        recipientName = recName,
                        recipientPhone = recPhone,
                        payoutAmount = pAmt,
                        scheduledMonth = sMonth,
                        payoutDate = pDate,
                        status = st,
                        confirmedByAdmin = confirmed,
                        notes = notes
                    )
                    turnToEdit = null
                }
            )
        }
    }

    if (showAddOldRecord) {
        OldCommitteeRecordEditorDialog(
            strings = strings,
            existingRecord = null,
            onDismiss = { showAddOldRecord = false },
            onSave = { rec ->
                viewModel.saveOldCommitteeRecord(rec)
                showAddOldRecord = false
            }
        )
    }

    oldRecordToEdit?.let { editing ->
        OldCommitteeRecordEditorDialog(
            strings = strings,
            existingRecord = editing,
            onDismiss = { oldRecordToEdit = null },
            onSave = { rec ->
                viewModel.saveOldCommitteeRecord(rec)
                oldRecordToEdit = null
            }
        )
    }

    oldRecordToView?.let { viewing ->
        OldCommitteeRecordDetailDialog(
            strings = strings,
            record = viewing,
            onEdit = {
                oldRecordToView = null
                oldRecordToEdit = viewing
            },
            onDelete = {
                oldRecordToView = null
                deleteErrorMsg = null
                pendingDeleteTarget = PendingDeleteTarget.OldRecordTarget(viewing)
            },
            onDismiss = { oldRecordToView = null }
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
            auditLogs = auditLogs,
            onDismiss = { showActivityHistory = false }
        )
    }

    pendingDeleteTarget?.let { target ->
        val (title, body) = when (target) {
            is PendingDeleteTarget.CommitteeTarget ->
                strings.deleteCommitteeLabel to
                    "Delete \"${target.committee.name}\"? Committees with recorded payments or payouts are protected and should be archived instead."
            is PendingDeleteTarget.MemberTarget ->
                strings.deleteMember to
                    "Delete member #${target.member.committeeNumber} ${target.member.name}? Members with recorded payments or payouts are protected."
            is PendingDeleteTarget.InstallmentTarget ->
                strings.deleteLabel to
                    "Remove unpaid installment entry for ${target.record.memberName} (${MonthUtils.formatMonthDisplay(target.record.monthKey)})?"
            is PendingDeleteTarget.UnpaidMonthTarget ->
                strings.deleteUnpaidMonthLabel to
                    "Delete all unpaid installment entries for ${MonthUtils.formatMonthDisplay(target.monthKey)}? Any recorded paid/partial entries will be preserved."
            is PendingDeleteTarget.TurnTarget ->
                strings.deleteLabel to
                    "Delete Turn #${target.turn.turnNumber} (${target.turn.recipientName})? Paid turns are protected."
            is PendingDeleteTarget.OldRecordTarget ->
                strings.deleteLabel to
                    "Delete old committee record \"${target.record.committeeName}\"?"
        }

        AdminDeleteConfirmDialog(
            strings = strings,
            title = title,
            message = body,
            currentUser = currentUser,
            showDefaultPinHint = isDefaultPinActive,
            errorMessage = deleteErrorMsg,
            onDismiss = {
                pendingDeleteTarget = null
                deleteErrorMsg = null
            },
            onConfirmDelete = { pin ->
                when (target) {
                    is PendingDeleteTarget.CommitteeTarget ->
                        viewModel.deleteCommittee(target.committee, pin) { ok, err ->
                            if (ok) pendingDeleteTarget = null else deleteErrorMsg = err
                        }
                    is PendingDeleteTarget.MemberTarget ->
                        viewModel.deleteMember(target.member, pin) { ok, err ->
                            if (ok) pendingDeleteTarget = null else deleteErrorMsg = err
                        }
                    is PendingDeleteTarget.InstallmentTarget ->
                        viewModel.deleteInstallment(target.record, pin) { ok, err ->
                            if (ok) pendingDeleteTarget = null else deleteErrorMsg = err
                        }
                    is PendingDeleteTarget.UnpaidMonthTarget ->
                        viewModel.deleteUnpaidMonthEntries(target.monthKey, pin) { ok, err ->
                            if (ok) pendingDeleteTarget = null else deleteErrorMsg = err
                        }
                    is PendingDeleteTarget.TurnTarget ->
                        viewModel.deleteCommitteeTurn(target.turn, pin) { ok, err ->
                            if (ok) pendingDeleteTarget = null else deleteErrorMsg = err
                        }
                    is PendingDeleteTarget.OldRecordTarget ->
                        viewModel.deleteOldCommitteeRecord(target.record, pin) { ok, err ->
                            if (ok) pendingDeleteTarget = null else deleteErrorMsg = err
                        }
                }
            }
        )
    }

    pendingRestoreData?.let { (json, preview) ->
        AlertDialog(
            onDismissRequest = { pendingRestoreData = null },
            title = { Text(strings.confirmRestoreTitle, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.confirmRestoreBody)
                    Text(
                        text = "• Committees: ${preview.committeeCount}\n• Members: ${preview.memberCount}\n• Installments: ${preview.installmentCount}\n• Turns: ${preview.turnCount}\n• Old Records: ${preview.oldRecordCount}" +
                            if (preview.exportedAt.isNotBlank()) "\n• Exported: ${preview.exportedAt}" else "",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
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
                TextButton(onClick = { pendingRestoreData = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showAppSettings) {
        AppSettingsDialog(
            strings = strings,
            language = effectiveLang,
            gatewayConfig = gatewayConfig,
            onLanguageChange = viewModel::setLanguage,
            onSaveSyncUrl = viewModel::saveSyncWebhookUrl,
            onOpenMessagingGateway = {
                showAppSettings = false
                showGatewayConfig = true
            },
            onDismiss = { showAppSettings = false }
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

    uploadDialogStatus?.let { status ->
        val (title, items) = when (status) {
            UploadQueueStatus.PENDING -> strings.uploadPendingTitle to pendingUploads
            UploadQueueStatus.COMPLETED -> strings.uploadCompletedTitle to completedUploads
            UploadQueueStatus.FAILED -> strings.failedUploadsTitle to failedUploads
        }
        UploadQueueDialog(
            strings = strings,
            title = title,
            items = items,
            filterStatus = status,
            onRetryAll = { viewModel.retryUploads() },
            onExportAndCompleteLocal = {
                uploadDialogStatus = null
                startBackupExport(markQueueCompleted = true)
            },
            onClearCompleted = { viewModel.clearCompletedUploads() },
            onDismiss = { uploadDialogStatus = null }
        )
    }

    if (showUserManagement) {
        UserManagementDialog(
            strings = strings,
            users = localUsers,
            currentUser = currentUser,
            onDismiss = { showUserManagement = false },
            onCreateUser = { uname, dname, pass, role, onErr ->
                viewModel.createUser(uname, dname, pass, role) { ok, err ->
                    if (!ok && err != null) onErr(err)
                }
            },
            onUpdateUser = viewModel::updateUserRoleAndActive,
            onDeleteUser = viewModel::deleteUser
        )
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            strings = strings,
            currentUser = currentUser,
            showDefaultPinHint = isDefaultPinActive,
            onDismiss = { showChangePassword = false },
            onSubmit = { curr, newPass, onErr ->
                viewModel.changePassword(curr, newPass) { ok, err ->
                    if (ok) showChangePassword = false else if (err != null) onErr(err)
                }
            }
        )
    }
}

@Composable
private fun LocalLoginScreen(
    strings: UiStrings,
    showDefaultPinHint: Boolean,
    isBusy: Boolean,
    onSignIn: (username: String, pin: String, onError: (String) -> Unit) -> Unit,
    onLanguageToggle: () -> Unit,
    currentLang: AppLanguage
) {
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilterChip(
                            selected = currentLang == AppLanguage.UR,
                            onClick = onLanguageToggle,
                            label = { Text(if (currentLang == AppLanguage.EN) "اردو" else "EN") }
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_gm_committee_fg),
                            contentDescription = "GM HASMANI Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(strings.loginTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    if (showDefaultPinHint) {
                        Text(
                            text = "Default: admin / 1234",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it; errorMsg = null },
                        label = { Text(strings.usernameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("login_username_input")
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; errorMsg = null },
                        label = { Text(strings.passwordPinLabel) },
                        visualTransformation = PasswordVisualTransformation(),
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("login_password_input")
                    )
                    if (!errorMsg.isNullOrBlank()) {
                        Text(errorMsg ?: "", color = StatusUnpaidRed, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { onSignIn(username, password) { errorMsg = it } },
                        enabled = !isBusy && username.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("login_submit_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(strings.loginButton, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun sectionLabel(section: AppSection, strings: UiStrings): String = when (section) {
    AppSection.DASHBOARD -> strings.navDashboard
    AppSection.COMMITTEES -> strings.navCommittees
    AppSection.MEMBERS -> strings.navMembers
    AppSection.PAYMENTS -> strings.navInstallments
    AppSection.COMMITTEE_TURNS -> strings.navTurns
    AppSection.MESSAGE_REQUESTS -> strings.navMessageRequests
    AppSection.PDF_REPORTS -> strings.navPdfReports
    AppSection.OLD_RECORDS -> strings.navOldHistory
    AppSection.SETTINGS -> strings.navReports
}
