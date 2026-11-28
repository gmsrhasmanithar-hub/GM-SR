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
import androidx.compose.material.icons.rounded.ForwardToInbox
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.FormatUtils
import com.example.model.ForwardOrganizer
import com.example.model.ForwardPaymentRecord
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.MemberStatus
import com.example.model.MonthUtils
import com.example.model.OldCommitteeRecord
import com.example.model.PaymentMethod
import com.example.model.PayoutStatus
import com.example.model.ReminderStage
import com.example.model.UploadQueueStatus
import com.example.model.UrduMessageType
import com.example.ui.components.AddManualPaymentDialog
import com.example.ui.components.AdminDeleteConfirmDialog
import com.example.ui.components.AppSettingsDialog
import com.example.ui.components.BackupRestoreQuickDialog
import com.example.ui.components.ChangePasswordDialog
import com.example.ui.components.CommitteeDetailDialog
import com.example.ui.components.CommitteeEditorDialog
import com.example.ui.components.CommitteeSwitcherDialog
import com.example.ui.components.DashboardListDetailDialog
import com.example.ui.components.ForwardOrganizerEditorDialog
import com.example.ui.components.InAppNotificationsDialog
import com.example.ui.components.MemberDetailDialog
import com.example.ui.components.MemberEditorDialog
import com.example.ui.components.MessagingGatewayConfigDialog
import com.example.ui.components.OldCommitteeRecordDetailDialog
import com.example.ui.components.OldCommitteeRecordEditorDialog
import com.example.ui.components.OrganizerUrduSummaryDialog
import com.example.ui.components.PaymentActivityHistoryDialog
import com.example.ui.components.PaymentReceiptDialog
import com.example.ui.components.RecordForwardPaymentDialog
import com.example.ui.components.RecordPaymentDialog
import com.example.ui.components.RecordPayoutDialog
import com.example.ui.components.ReverseForwardPaymentDialog
import com.example.ui.components.TurnEditorDialog
import com.example.ui.components.UploadQueueDialog
import com.example.ui.components.UserManagementDialog
import com.example.ui.screens.CommitteeTurnsScreen
import com.example.ui.screens.CommitteesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ForwardPaymentsScreen
import com.example.ui.screens.HistoryReportsSettingsScreen
import com.example.ui.screens.InstallmentsScreen
import com.example.ui.screens.MemberPortalScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.OldHistoryScreen
import com.example.ui.screens.PdfReportsScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.theme.CommitteeManagerTheme
import com.example.ui.theme.StatusUnpaidRed
import com.example.viewmodel.CommitteeViewModel
import com.example.viewmodel.InstallmentFilter
import com.example.viewmodel.UiState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

enum class AppSection(val icon: ImageVector, val testTag: String) {
    DASHBOARD(Icons.Rounded.Dashboard, "nav_dashboard"),
    COMMITTEES(Icons.Rounded.AccountBalance, "nav_committees"),
    MEMBERS(Icons.Rounded.Group, "nav_members"),
    PAYMENTS(Icons.Rounded.Payments, "nav_installments"),
    FORWARD_PAYMENTS(Icons.Rounded.ForwardToInbox, "nav_forward_payments"),
    COMMITTEE_TURNS(Icons.AutoMirrored.Rounded.ListAlt, "nav_turns"),
    MESSAGE_REQUESTS(Icons.Rounded.Sms, "nav_reminders"),
    PDF_REPORTS(Icons.Rounded.PictureAsPdf, "nav_pdf_reports"),
    OLD_RECORDS(Icons.Rounded.History, "nav_old_history"),
    SETTINGS(Icons.Rounded.Settings, "nav_settings")
}

private enum class DashboardModalType {
    PAID_MEMBERS,
    UNPAID_MEMBERS,
    UPCOMING_COMMITTEE
}

private val BOTTOM_BAR_SECTIONS = listOf(
    AppSection.DASHBOARD,
    AppSection.MEMBERS,
    AppSection.PAYMENTS,
    AppSection.MESSAGE_REQUESTS,
    AppSection.SETTINGS
)

private sealed interface PendingDeleteTarget {
    data class CommitteeTarget(val committee: Committee) : PendingDeleteTarget
    data class MemberTarget(val member: CommitteeMember) : PendingDeleteTarget
    data class InstallmentTarget(val record: InstallmentRecord) : PendingDeleteTarget
    data class UnpaidMonthTarget(val monthKey: String) : PendingDeleteTarget
    data class TurnTarget(val turn: CommitteeTurn) : PendingDeleteTarget
    data class OldRecordTarget(val record: OldCommitteeRecord) : PendingDeleteTarget
    data class ForwardOrganizerTarget(val organizer: ForwardOrganizer) : PendingDeleteTarget
    data class ForwardPaymentTarget(val payment: ForwardPaymentRecord) : PendingDeleteTarget
    data object ResetAllAppDataTarget : PendingDeleteTarget
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

    val memberSession by viewModel.memberSession.collectAsStateWithLifecycle()
    val memberNotifications by viewModel.memberNotifications.collectAsStateWithLifecycle()
    val memberCommittee by viewModel.memberCommittee.collectAsStateWithLifecycle()
    val memberInstallments by viewModel.memberInstallments.collectAsStateWithLifecycle()
    val memberTurns by viewModel.memberTurns.collectAsStateWithLifecycle()
    val allNotifications by viewModel.allNotifications.collectAsStateWithLifecycle()
    val forwardOrganizers by viewModel.forwardOrganizersState.collectAsStateWithLifecycle()
    val forwardPayments by viewModel.forwardPaymentsState.collectAsStateWithLifecycle()
    val forwardMonthlySummary by viewModel.forwardMonthlySummary.collectAsStateWithLifecycle()
    val forwardOverallSummary by viewModel.forwardOverallSummary.collectAsStateWithLifecycle()

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
    if (currentSection != AppSection.DASHBOARD && !isLoggedOut && memberSession == null) {
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
    var receiptTarget by remember { mutableStateOf<Pair<InstallmentRecord, CommitteeMember?>?>(null) }
    var dashboardModalType by remember { mutableStateOf<DashboardModalType?>(null) }
    var showBackupRestoreQuick by remember { mutableStateOf(false) }
    var showNotificationsInbox by remember { mutableStateOf(false) }
    var showForwardOrganizerDialog by remember { mutableStateOf(false) }
    var forwardOrganizerToEdit by remember { mutableStateOf<ForwardOrganizer?>(null) }
    var showRecordForwardDialog by remember { mutableStateOf(false) }
    var forwardPaymentToEdit by remember { mutableStateOf<ForwardPaymentRecord?>(null) }
    var initialForwardOrganizerId by remember { mutableStateOf<String?>(null) }
    var forwardPaymentToReverse by remember { mutableStateOf<ForwardPaymentRecord?>(null) }
    var organizerSummaryTarget by remember { mutableStateOf<Pair<ForwardOrganizer?, String>?>(null) }

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

    // SAF Excel (.xls) Export launcher
    var pendingExcelExport by remember { mutableStateOf<Pair<PdfReportType, CommitteeMember?>?>(null) }
    val downloadExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.ms-excel")
    ) { uri ->
        val req = pendingExcelExport
        pendingExcelExport = null
        if (uri != null && req != null) {
            viewModel.saveExcelToUri(context, req.first, req.second, uri)
        }
    }

    fun triggerDownloadExcel(reportType: PdfReportType, member: CommitteeMember? = null) {
        pendingExcelExport = reportType to member
        val prefix = reportType.name.lowercase()
        downloadExcelLauncher.launch("GMSR_${prefix}_${MonthUtils.todayDateString()}.xls")
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

    if (memberSession != null) {
        val loggedMember = memberSession!!
        MemberPortalScreen(
            strings = strings,
            currentLang = effectiveLang,
            companySettings = companySettings,
            member = loggedMember,
            committee = memberCommittee ?: activeCommittee,
            installments = memberInstallments,
            turns = memberTurns,
            notifications = memberNotifications,
            onLanguageToggle = {
                val next = if (effectiveLang == AppLanguage.EN) AppLanguage.UR else AppLanguage.EN
                viewModel.setLanguage(next)
            },
            onViewReceipt = { rec -> receiptTarget = rec to loggedMember },
            onMarkNotificationRead = viewModel::markNotificationRead,
            onMarkAllNotificationsRead = viewModel::markAllMemberNotificationsRead,
            onLogout = { viewModel.logoutMember() }
        )
        receiptTarget?.let { (inst, mem) ->
            PaymentReceiptDialog(
                strings = strings,
                companySettings = companySettings,
                committeeName = (memberCommittee ?: activeCommittee)?.name ?: "Committee",
                installment = inst,
                member = mem,
                onViewPdf = {
                    viewModel.generateAndHandleReceiptPdf(context, inst, mem, printMode = false, shareMode = false)
                },
                onPrintPdf = {
                    viewModel.generateAndHandleReceiptPdf(context, inst, mem, printMode = true, shareMode = false)
                },
                onDownloadPdf = {
                    viewModel.generateAndHandleReceiptPdf(context, inst, mem) { file ->
                        pendingPdfFile = file
                        downloadPdfLauncher.launch(file.name)
                    }
                },
                onDismiss = { receiptTarget = null }
            )
        }
        return
    }

    if (isLoggedOut) {
        LocalLoginScreen(
            strings = strings,
            companyContact = companySettings.easypaisaNumber.ifBlank { "03423498631" },
            showDefaultPinHint = isDefaultPinActive,
            isBusy = isBusy,
            onAdminSignIn = { username, pin, onErr ->
                viewModel.loginLocalUser(username, pin, onErr)
            },
            onMemberSignIn = { identifier, pin, onErr ->
                viewModel.loginMember(identifier, pin, onErr)
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
                                onClick = { showNotificationsInbox = true },
                                modifier = Modifier.testTag("top_notifications_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Notifications,
                                    contentDescription = strings.notificationsTitle
                                )
                            }
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
                                        forwardSummary = forwardMonthlySummary,
                                        unreadNotificationCount = allNotifications.count { !it.isRead },
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
                                        onGoToCommittees = { currentSection = AppSection.COMMITTEES },
                                        onGoToMembers = { currentSection = AppSection.MEMBERS },
                                        onGoToInstallments = {
                                            viewModel.setInstallmentFilter(InstallmentFilter.ALL)
                                            currentSection = AppSection.PAYMENTS
                                        },
                                        onGoToPendingInstallments = {
                                            viewModel.setInstallmentFilter(InstallmentFilter.UNPAID)
                                            currentSection = AppSection.PAYMENTS
                                        },
                                        onGoToForwardPayments = { currentSection = AppSection.FORWARD_PAYMENTS },
                                        onGoToTurns = { currentSection = AppSection.COMMITTEE_TURNS },
                                        onGoToOldHistory = { currentSection = AppSection.OLD_RECORDS },
                                        onGoToReminders = { currentSection = AppSection.MESSAGE_REQUESTS },
                                        onGoToReports = { currentSection = AppSection.PDF_REPORTS },
                                        onGoToSettings = { currentSection = AppSection.SETTINGS },
                                        onOpenPaidDetail = { dashboardModalType = DashboardModalType.PAID_MEMBERS },
                                        onOpenUnpaidDetail = { dashboardModalType = DashboardModalType.UNPAID_MEMBERS },
                                        onOpenUpcomingDetail = { dashboardModalType = DashboardModalType.UPCOMING_COMMITTEE },
                                        onOpenBackupRestore = { showBackupRestoreQuick = true },
                                        onOpenNotifications = { showNotificationsInbox = true },
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
                                        onToggleMemberActive = viewModel::toggleMemberActive,
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
                                        onViewReceipt = { rec, mem ->
                                            receiptTarget = rec to mem
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

                                    AppSection.FORWARD_PAYMENTS -> ForwardPaymentsScreen(
                                        strings = strings,
                                        companySettings = companySettings,
                                        committee = activeCommittee,
                                        selectedMonthKey = selectedMonthKey,
                                        monthlySummary = forwardMonthlySummary,
                                        overallSummary = forwardOverallSummary,
                                        organizers = forwardOrganizers,
                                        forwardPayments = forwardPayments,
                                        onSelectMonth = viewModel::selectMonth,
                                        onCreateCommittee = { showCreateCommittee = true },
                                        onAddOrganizer = {
                                            forwardOrganizerToEdit = null
                                            showForwardOrganizerDialog = true
                                        },
                                        onEditOrganizer = { org ->
                                            forwardOrganizerToEdit = org
                                            showForwardOrganizerDialog = true
                                        },
                                        onDeleteOrganizer = { org ->
                                            deleteErrorMsg = null
                                            pendingDeleteTarget = PendingDeleteTarget.ForwardOrganizerTarget(org)
                                        },
                                        onRecordForwardPayment = { orgId ->
                                            forwardPaymentToEdit = null
                                            initialForwardOrganizerId = orgId
                                            showRecordForwardDialog = true
                                        },
                                        onEditForwardPayment = { rec ->
                                            forwardPaymentToEdit = rec
                                            initialForwardOrganizerId = rec.organizerId
                                            showRecordForwardDialog = true
                                        },
                                        onReverseForwardPayment = { rec ->
                                            forwardPaymentToReverse = rec
                                        },
                                        onDeleteForwardPayment = { rec ->
                                            deleteErrorMsg = null
                                            pendingDeleteTarget = PendingDeleteTarget.ForwardPaymentTarget(rec)
                                        },
                                        onOpenUrduSummary = { org, isMonthlyScope ->
                                            val name = org?.organizerName ?: forwardOrganizers.firstOrNull()?.organizerName.orEmpty()
                                            val msg = viewModel.buildOrganizerUrduSummary(name, isMonthlyScope)
                                            organizerSummaryTarget = (org ?: forwardOrganizers.firstOrNull()) to msg
                                        },
                                        onExportForwardReportPdf = {
                                            viewModel.generateAndOpenPdf(context, PdfReportType.FORWARD_PAYMENTS_SUMMARY, null, false)
                                        },
                                        onShowProofMissingSnackbar = {
                                            scope.launch { snackbarHostState.showSnackbar("Unable to open attached proof file on this device.") }
                                        }
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
                                        onDrawRandomWinner = { viewModel.drawRandomEligibleWinner() },
                                        onConfirmRandomWinner = { winner, payoutAmt, payoutDate, alsoNotify ->
                                            val matchingPendingTurn = turns
                                                .filter { it.statusEnum == PayoutStatus.PENDING }
                                                .sortedBy { it.turnNumber }
                                                .firstOrNull {
                                                    it.recipientMemberId == winner.id ||
                                                        it.recipientName.trim().equals("Upcoming", ignoreCase = true)
                                                }
                                            viewModel.saveCommitteeTurn(
                                                existingTurn = matchingPendingTurn,
                                                turnNumber = matchingPendingTurn?.turnNumber ?: nextSuggestedTurnNumber,
                                                recipientMemberId = winner.id,
                                                recipientName = winner.name,
                                                recipientPhone = winner.phone,
                                                payoutAmount = payoutAmt,
                                                scheduledMonth = matchingPendingTurn?.scheduledMonth ?: selectedMonthKey,
                                                payoutDate = payoutDate,
                                                status = PayoutStatus.PAID,
                                                confirmedByAdmin = true,
                                                notes = "Confirmed Random Draw Winner"
                                            )
                                            if (alsoNotify) {
                                                viewModel.sendUrduMessageAndNotify(
                                                    context = context,
                                                    type = UrduMessageType.WINNER_ANNOUNCEMENT,
                                                    recipients = members.filter { it.memberStatusEnum == MemberStatus.ACTIVE },
                                                    installmentsForMonth = emptyList(),
                                                    customBodyOrTemplate = "",
                                                    adminMessage = "${winner.name} (${FormatUtils.formatMoney(payoutAmt, companySettings.currency)})",
                                                    winnerDate = payoutDate,
                                                    sendWhatsApp = false,
                                                    sendSms = false,
                                                    sendInApp = true,
                                                    preventDuplicateInApp = false
                                                )
                                            }
                                        }
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
                                        renderUrduPreview = viewModel::renderUrduPreview,
                                        onSendUrduMessages = { type, targets, insts, body, adminMsg, wDate, wa, sms, inApp, dedup ->
                                            viewModel.sendUrduMessageAndNotify(
                                                context = context,
                                                type = type,
                                                recipients = targets,
                                                installmentsForMonth = insts,
                                                customBodyOrTemplate = body,
                                                adminMessage = adminMsg,
                                                winnerDate = wDate,
                                                sendWhatsApp = wa,
                                                sendSms = sms,
                                                sendInApp = inApp,
                                                preventDuplicateInApp = dedup
                                            )
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
                                        },
                                        onDownloadExcel = { type, member ->
                                            triggerDownloadExcel(type, member)
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
                                        onResetAllData = {
                                            deleteErrorMsg = null
                                            pendingDeleteTarget = PendingDeleteTarget.ResetAllAppDataTarget
                                        },
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
            onSave = { name, phone, number, shares, memberCode, fatherName, address, joiningDate, accessPin ->
                viewModel.addMember(
                    name = name,
                    phone = phone,
                    committeeNumber = number,
                    shares = shares,
                    memberCode = memberCode,
                    fatherName = fatherName,
                    address = address,
                    joiningDate = joiningDate,
                    accessPin = accessPin
                )
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
            onSave = { name, phone, number, shares, memberCode, fatherName, address, joiningDate, accessPin ->
                viewModel.updateMember(
                    member = m,
                    newName = name,
                    newPhone = phone,
                    newCommitteeNumber = number,
                    newShares = shares,
                    newMemberCode = memberCode,
                    newFatherName = fatherName,
                    newAddress = address,
                    newJoiningDate = joiningDate,
                    newAccessPin = accessPin
                )
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
            },
            onToggleActive = {
                viewModel.toggleMemberActive(m)
                memberToDetail = null
            },
            onViewReceipt = { rec ->
                receiptTarget = rec to m
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
            is PendingDeleteTarget.ForwardOrganizerTarget ->
                strings.deleteLabel to
                    "Delete organizer \"${target.organizer.organizerName}\"? Organizers with confirmed forward payments cannot be removed until reversed."
            is PendingDeleteTarget.ForwardPaymentTarget ->
                strings.deleteLabel to
                    "Delete reversed forward payment entry (${target.payment.receiptNumber ?: target.payment.id})?"
            PendingDeleteTarget.ResetAllAppDataTarget ->
                "Clear All Data (Fresh Start)" to
                    "This will permanently delete all committees, members, turns, installments, forward payments, receipts, notifications, and old history records so you can start completely fresh. Admin PIN required."
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
                    is PendingDeleteTarget.ForwardOrganizerTarget ->
                        viewModel.deleteForwardOrganizer(target.organizer.id, pin) { ok, err ->
                            if (ok) pendingDeleteTarget = null else deleteErrorMsg = err
                        }
                    is PendingDeleteTarget.ForwardPaymentTarget ->
                        viewModel.deleteForwardPayment(target.payment.id, pin) { ok, err ->
                            if (ok) pendingDeleteTarget = null else deleteErrorMsg = err
                        }
                    PendingDeleteTarget.ResetAllAppDataTarget ->
                        viewModel.clearAllAppDataForFreshStart(context, pin) { ok, err ->
                            if (ok) pendingDeleteTarget = null else deleteErrorMsg = err
                        }
                }
            }
        )
    }

    if (showForwardOrganizerDialog || forwardOrganizerToEdit != null) {
        ForwardOrganizerEditorDialog(
            strings = strings,
            existingOrganizer = forwardOrganizerToEdit,
            defaultMonthKey = selectedMonthKey,
            defaultPayableAmount = activeCommittee?.monthlyCollectionTotal ?: 0L,
            onDismiss = {
                showForwardOrganizerDialog = false
                forwardOrganizerToEdit = null
            },
            onSave = { name, phone, payable, mKey, notes ->
                viewModel.saveForwardOrganizer(
                    existingId = forwardOrganizerToEdit?.id,
                    organizerName = name,
                    organizerPhone = phone,
                    totalPayableAmount = payable,
                    monthKey = mKey,
                    notes = notes
                )
                showForwardOrganizerDialog = false
                forwardOrganizerToEdit = null
            }
        )
    }

    if (showRecordForwardDialog || forwardPaymentToEdit != null) {
        RecordForwardPaymentDialog(
            strings = strings,
            currency = companySettings.currency,
            organizers = forwardOrganizers,
            existingPayment = forwardPaymentToEdit,
            initialOrganizerId = initialForwardOrganizerId,
            defaultMonthKey = selectedMonthKey,
            onImportProof = { uri, onResult ->
                viewModel.importProofFile(context, uri, onResult)
            },
            onDismiss = {
                showRecordForwardDialog = false
                forwardPaymentToEdit = null
                initialForwardOrganizerId = null
            },
            onSave = { orgId, mKey, amt, pDate, method, txId, proofUri, proofLabel, notes, confirmed ->
                viewModel.recordForwardPayment(
                    existingPaymentId = forwardPaymentToEdit?.id,
                    organizerId = orgId,
                    monthKey = mKey,
                    amountForwarded = amt,
                    paymentDate = pDate,
                    paymentMethod = method,
                    transactionId = txId,
                    proofUri = proofUri,
                    proofLabel = proofLabel,
                    notes = notes,
                    confirmedByAdmin = confirmed
                )
                showRecordForwardDialog = false
                forwardPaymentToEdit = null
                initialForwardOrganizerId = null
            }
        )
    }

    forwardPaymentToReverse?.let { revTarget ->
        ReverseForwardPaymentDialog(
            strings = strings,
            payment = revTarget,
            currency = companySettings.currency,
            onDismiss = { forwardPaymentToReverse = null },
            onConfirmReverse = { reason ->
                viewModel.reverseForwardPayment(revTarget.id, reason)
                forwardPaymentToReverse = null
            }
        )
    }

    organizerSummaryTarget?.let { (org, msg) ->
        OrganizerUrduSummaryDialog(
            strings = strings,
            organizer = org,
            initialMessage = msg,
            onCopyMessage = { text -> viewModel.copyMessageToClipboard(context, text) },
            onSendWhatsApp = { phone, text -> viewModel.shareRawMessageWhatsApp(context, phone, text) },
            onSendSms = { phone, text -> viewModel.shareRawMessageSms(context, phone, text) },
            onDismiss = { organizerSummaryTarget = null }
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

    receiptTarget?.let { (inst, mem) ->
        PaymentReceiptDialog(
            strings = strings,
            companySettings = companySettings,
            committeeName = activeCommittee?.name ?: "Committee",
            installment = inst,
            member = mem,
            onViewPdf = {
                viewModel.generateAndHandleReceiptPdf(context, inst, mem, printMode = false, shareMode = false)
            },
            onPrintPdf = {
                viewModel.generateAndHandleReceiptPdf(context, inst, mem, printMode = true, shareMode = false)
            },
            onDownloadPdf = {
                viewModel.generateAndHandleReceiptPdf(context, inst, mem) { file ->
                    pendingPdfFile = file
                    downloadPdfLauncher.launch(file.name)
                }
            },
            onDismiss = { receiptTarget = null }
        )
    }

    dashboardModalType?.let { modal ->
        val monthInsts = installments.filter { it.monthKey == selectedMonthKey }.sortedBy { it.committeeNumber }
        val colStart = activeCommittee?.collectionDayStart ?: companySettings.defaultCollectionStartDay
        val colEnd = activeCommittee?.collectionDayEnd ?: companySettings.defaultCollectionEndDay
        when (modal) {
            DashboardModalType.PAID_MEMBERS -> {
                val items = monthInsts.filter { it.statusEnum == InstallmentStatus.PAID }.map {
                    "#${it.committeeNumber} ${it.memberName}" to
                        "${FormatUtils.formatMoney(it.paidAmount, companySettings.currency)} • ${it.paymentMethodEnum.label}${it.receiptNumber?.let { r -> " • $r" } ?: ""}"
                }
                DashboardListDetailDialog(
                    strings = strings,
                    title = "${strings.paidMembers} (${MonthUtils.formatMonthDisplay(selectedMonthKey)})",
                    items = items,
                    emptyMessage = "No paid installments recorded yet for this month.",
                    primaryActionLabel = "Open Payments",
                    onPrimaryAction = {
                        viewModel.setInstallmentFilter(InstallmentFilter.PAID)
                        currentSection = AppSection.PAYMENTS
                    },
                    onDismiss = { dashboardModalType = null }
                )
            }
            DashboardModalType.UNPAID_MEMBERS -> {
                val items = monthInsts.filter { it.statusEnum != InstallmentStatus.PAID }.map {
                    val eff = it.effectiveStatus(colStart, colEnd)
                    "#${it.committeeNumber} ${it.memberName} (${eff.name})" to
                        "Remaining: ${FormatUtils.formatMoney(it.remainingAmount, companySettings.currency)} • ${it.memberPhone}"
                }
                DashboardListDetailDialog(
                    strings = strings,
                    title = "${strings.unpaidMembers} / ${strings.overdueLabel} (${MonthUtils.formatMonthDisplay(selectedMonthKey)})",
                    items = items,
                    emptyMessage = strings.noUnpaidMembers,
                    primaryActionLabel = "Open Unpaid Payments",
                    onPrimaryAction = {
                        viewModel.setInstallmentFilter(InstallmentFilter.UNPAID)
                        currentSection = AppSection.PAYMENTS
                    },
                    onDismiss = { dashboardModalType = null }
                )
            }
            DashboardModalType.UPCOMING_COMMITTEE -> {
                val pendingTurns = turns.filter { it.statusEnum == PayoutStatus.PENDING }.sortedBy { it.turnNumber }
                val items = pendingTurns.map {
                    "Turn #${it.turnNumber}: ${it.recipientName}" to
                        "${MonthUtils.formatCollectionWindowFull(it.scheduledMonth, activeCommittee?.collectionDayStart ?: 28L, colEnd)} • ${FormatUtils.formatMoney(it.payoutAmount, companySettings.currency)}"
                }
                DashboardListDetailDialog(
                    strings = strings,
                    title = "Upcoming Committee Schedule",
                    items = items,
                    emptyMessage = "No pending payout turns remaining.",
                    primaryActionLabel = "Open Winners & Turns",
                    onPrimaryAction = { currentSection = AppSection.COMMITTEE_TURNS },
                    onDismiss = { dashboardModalType = null }
                )
            }
        }
    }

    if (showBackupRestoreQuick) {
        BackupRestoreQuickDialog(
            strings = strings,
            committeesCount = committees.size,
            membersCount = members.count { it.memberStatusEnum == MemberStatus.ACTIVE },
            installmentsCount = installments.size,
            oldRecordsCount = oldRecords.size,
            onExportBackup = { startBackupExport(false) },
            onRestoreBackup = {
                restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
            },
            onDismiss = { showBackupRestoreQuick = false }
        )
    }

    if (showNotificationsInbox) {
        InAppNotificationsDialog(
            strings = strings,
            notifications = allNotifications,
            onMarkRead = viewModel::markNotificationRead,
            onDismiss = { showNotificationsInbox = false }
        )
    }
}

@Composable
private fun LocalLoginScreen(
    strings: UiStrings,
    companyContact: String,
    showDefaultPinHint: Boolean,
    isBusy: Boolean,
    onAdminSignIn: (username: String, pin: String, onError: (String) -> Unit) -> Unit,
    onMemberSignIn: (identifier: String, pin: String, onError: (String) -> Unit) -> Unit,
    onLanguageToggle: () -> Unit,
    currentLang: AppLanguage
) {
    var loginTab by remember { mutableIntStateOf(0) } // 0 = Admin, 1 = Member
    var username by remember { mutableStateOf("admin") }
    var memberIdentifier by remember { mutableStateOf("") }
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
                            contentDescription = "G M S R HASHMANI THAR Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(strings.loginTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Contact: $companyContact",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TabRow(selectedTabIndex = loginTab) {
                        Tab(
                            selected = loginTab == 0,
                            onClick = { loginTab = 0; errorMsg = null },
                            text = { Text(strings.adminLoginTab, maxLines = 1) },
                            modifier = Modifier.testTag("tab_admin_login")
                        )
                        Tab(
                            selected = loginTab == 1,
                            onClick = { loginTab = 1; errorMsg = null },
                            text = { Text(strings.memberLoginTab, maxLines = 1) },
                            modifier = Modifier.testTag("tab_member_login")
                        )
                    }

                    if (loginTab == 0) {
                        if (showDefaultPinHint) {
                            Text(
                                text = "Default Admin: admin / 1234",
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
                    } else {
                        Text(
                            text = "Sign in with your Member ID (e.g. GM-001) or Mobile Number & PIN (default 1234)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = memberIdentifier,
                            onValueChange = { memberIdentifier = it; errorMsg = null },
                            label = { Text("${strings.memberIdLabel} / ${strings.mobileNumber}") },
                            placeholder = { Text("GM-001 or 03001234567") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("member_login_id_input")
                        )
                    }

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
                    val canSubmit = if (loginTab == 0) {
                        !isBusy && username.isNotBlank() && password.isNotBlank()
                    } else {
                        !isBusy && memberIdentifier.isNotBlank() && password.isNotBlank()
                    }
                    Button(
                        onClick = {
                            if (loginTab == 0) {
                                onAdminSignIn(username, password) { errorMsg = it }
                            } else {
                                onMemberSignIn(memberIdentifier, password) { errorMsg = it }
                            }
                        },
                        enabled = canSubmit,
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
    AppSection.DASHBOARD -> strings.navHome
    AppSection.COMMITTEES -> strings.navCommittees
    AppSection.MEMBERS -> strings.navMembers
    AppSection.PAYMENTS -> strings.navInstallments
    AppSection.FORWARD_PAYMENTS -> strings.navForwardPayments
    AppSection.COMMITTEE_TURNS -> strings.navTurns
    AppSection.MESSAGE_REQUESTS -> strings.navMessages
    AppSection.PDF_REPORTS -> strings.navPdfReports
    AppSection.OLD_RECORDS -> strings.navOldHistory
    AppSection.SETTINGS -> strings.settingsSectionTitle
}
