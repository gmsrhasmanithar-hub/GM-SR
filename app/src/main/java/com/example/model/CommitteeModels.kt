package com.example.model

import java.security.MessageDigest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToLong

enum class CommitteeStatus {
    ACTIVE, COMPLETED, ARCHIVED;

    companion object {
        fun fromRaw(raw: String?): CommitteeStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class MemberStatus {
    ACTIVE, ARCHIVED;

    companion object {
        fun fromRaw(raw: String?): MemberStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class PayoutStatus {
    PENDING, PAID;

    companion object {
        fun fromRaw(raw: String?): PayoutStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class InstallmentStatus {
    PAID, UNPAID, PARTIAL;

    companion object {
        fun fromRaw(raw: String?): InstallmentStatus =
            entries.firstOrNull { it.name == raw } ?: UNPAID

        fun computeStatus(paidAmount: Long, dueAmount: Long): InstallmentStatus = when {
            paidAmount <= 0L -> UNPAID
            paidAmount >= dueAmount -> PAID
            else -> PARTIAL
        }
    }
}

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    EASYPAISA("Easypaisa"),
    BANK_TRANSFER("Bank Transfer"),
    OTHER("Other");

    companion object {
        fun fromRaw(raw: String?): PaymentMethod =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) || it.label.equals(raw, ignoreCase = true) } ?: CASH
    }
}

enum class ReminderChannel {
    WHATSAPP_API, WHATSAPP_MANUAL, SMS_API, SMS_MANUAL;

    companion object {
        fun fromRaw(raw: String?): ReminderChannel =
            entries.firstOrNull { it.name == raw } ?: WHATSAPP_MANUAL
    }
}

enum class ReminderStage(
    val dayHint: String,
    val defaultTemplateEn: String,
    val defaultTemplateUr: String
) {
    FIRST_28(
        "28th",
        "Assalam-o-Alaikum {name}, your monthly installment of {amount} for {committee} ({month}) is now open for collection ({window}). JazakAllah.",
        "السلام علیکم {name}، {committee} ({month}) کی ماہانہ قسط {amount} جمع کرانے کا وقت ({window}) شروع ہو گیا ہے۔ براہِ کرم قسط بروقت ادا کریں۔ جزاک اللہ۔"
    ),
    SECOND_3_4(
        "3rd / 4th",
        "Assalam-o-Alaikum {name}, gentle 2nd reminder for your {committee} installment of {amount} for {month}. Collection window: {window}.",
        "السلام علیکم {name}، {committee} ({month}) کی ماہانہ قسط {amount} کے لیے دوسری یاددہانی۔ وصولی کا وقت: {window}۔"
    ),
    THIRD_5(
        "5th",
        "Assalam-o-Alaikum {name}, 3rd reminder: {amount} installment for {committee} ({month}) is due by the {window}. Please share payment confirmation.",
        "السلام علیکم {name}، تیسری یاددہانی: {committee} ({month}) کی قسط {amount} مقررہ وقت ({window}) تک جمع کرا دیں۔ شکریہ۔"
    ),
    FINAL_7(
        "7th",
        "Assalam-o-Alaikum {name}, FINAL reminder: Only 1 day left for {committee} ({month}) installment of {amount}. Please submit before payout.",
        "السلام علیکم {name}، آخری یاددہانی: {committee} ({month}) کی قسط {amount} جمع کرانے میں صرف 1 دن باقی ہے۔ براہِ کرم آج ادا کر دیں۔"
    ),
    LAST_8(
        "8th",
        "Assalam-o-Alaikum {name}, TODAY is the LAST due date (8th) for {committee} ({month}) installment of {amount}. Please pay today so payout stays on schedule.",
        "السلام علیکم {name}، آج {committee} ({month}) کی قسط {amount} جمع کرانے کی آخری تاریخ (8 تاریخ) ہے۔ براہِ کرم آج لازمی ادا کریں۔"
    ),
    CUSTOM(
        "Custom",
        "Assalam-o-Alaikum {name}, update regarding {committee} ({month}) installment of {amount}.",
        "السلام علیکم {name}، {committee} ({month}) کی قسط {amount} کے حوالے سے ضروری پیغام۔"
    );

    companion object {
        fun fromRaw(raw: String?): ReminderStage =
            entries.firstOrNull { it.name == raw } ?: FIRST_28
    }
}

enum class DispatchStatus {
    SENT_API, OPENED_APP, FAILED;

    companion object {
        fun fromRaw(raw: String?): DispatchStatus =
            entries.firstOrNull { it.name == raw } ?: OPENED_APP
    }
}

enum class UserRole {
    ADMIN, STAFF;

    companion object {
        fun fromRaw(raw: String?): UserRole =
            entries.firstOrNull { it.name == raw } ?: STAFF
    }
}

enum class UploadQueueStatus {
    PENDING, COMPLETED, FAILED;

    companion object {
        fun fromRaw(raw: String?): UploadQueueStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

data class LocalUser(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val passwordHash: String = "",
    val role: String = UserRole.ADMIN.name,
    val isActive: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val roleEnum: UserRole get() = UserRole.fromRaw(role)
    val isAdmin: Boolean get() = roleEnum == UserRole.ADMIN
}

data class AuditLogEntry(
    val id: String = "",
    val actorName: String = "",
    val actionType: String = "",
    val targetType: String = "",
    val targetName: String = "",
    val details: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class UploadQueueItem(
    val id: String = "",
    val title: String = "",
    val payloadType: String = "",
    val payloadJson: String = "",
    val status: String = UploadQueueStatus.PENDING.name,
    val errorMessage: String? = null,
    val attempts: Int = 0,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val statusEnum: UploadQueueStatus get() = UploadQueueStatus.fromRaw(status)
}

data class UploadBatchSummary(
    val attempted: Int,
    val succeeded: Int,
    val failed: Int,
    val message: String
)

object PasswordHasher {
    fun sha256(raw: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

data class AppCompanySettings(
    val companyName: String = "GM HASMANI COMMITTEE",
    val applicationName: String = "GM Hasmani Committee Manager",
    val adminName: String = "GM Hasmani Admin",
    val adminContactNumber: String = "",
    val easypaisaNumber: String = "03423498631",
    val easypaisaAccountHolder: String = "GM Hasmani Admin",
    val currency: String = "PKR",
    val defaultMonthlyContribution: Long = 5000L,
    val defaultTotalShares: Long = 10L,
    val defaultCollectionStartDay: Long = 28L,
    val defaultCollectionEndDay: Long = 8L,
    val defaultDurationMonths: Long = 10L,
    val pdfFooterNote: String = "Official GM HASMANI Committee Record"
)

data class Committee(
    val id: String = "",
    val name: String = "",
    val memberCount: Long = 10L,
    val totalShares: Long = 0L,
    val monthlyInstallment: Long = 5000L,
    val totalMonths: Long = 10L,
    val collectionDayStart: Long = 28L,
    val collectionDayEnd: Long = 8L,
    val payoutDay: Long = 10L,
    val startMonth: String = MonthUtils.currentMonthKey(),
    val endMonth: String = MonthUtils.addMonths(MonthUtils.currentMonthKey(), 9),
    val status: String = CommitteeStatus.ACTIVE.name,
    val notes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val statusEnum: CommitteeStatus get() = CommitteeStatus.fromRaw(status)
    val effectiveTotalShares: Long get() = if (totalShares > 0L) totalShares else memberCount
    val monthlyCollectionTotal: Long get() = effectiveTotalShares * monthlyInstallment
    val totalCommitteeAmount: Long get() = monthlyCollectionTotal * totalMonths
    val eachPayoutAmount: Long get() = monthlyCollectionTotal
}

data class CommitteeMember(
    val id: String = "",
    val committeeId: String = "",
    val name: String = "",
    val phone: String = "",
    val committeeNumber: Long = 1L,
    val shares: Double = 1.0,
    val payoutMonth: String = MonthUtils.currentMonthKey(),
    val payoutStatus: String = PayoutStatus.PENDING.name,
    val payoutDate: String? = null,
    val payoutAmount: Long = 0L,
    val status: String = MemberStatus.ACTIVE.name,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val payoutStatusEnum: PayoutStatus get() = PayoutStatus.fromRaw(payoutStatus)
    val memberStatusEnum: MemberStatus get() = MemberStatus.fromRaw(status)
    val effectiveShares: Double get() = if (shares > 0.0) shares else 1.0

    fun monthlyDue(shareContribution: Long): Long =
        (effectiveShares * shareContribution.toDouble()).roundToLong().coerceAtLeast(0L)
}

data class CommitteeTurn(
    val id: String = "",
    val committeeId: String = "",
    val turnNumber: Long = 1L,
    val recipientMemberId: String? = null,
    val recipientName: String = "",
    val recipientPhone: String? = null,
    val payoutAmount: Long = 50000L,
    val scheduledMonth: String = MonthUtils.currentMonthKey(),
    val payoutDate: String? = null,
    val status: String = PayoutStatus.PENDING.name,
    val confirmedByAdmin: Boolean = false,
    val notes: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val statusEnum: PayoutStatus get() = PayoutStatus.fromRaw(status)
}

data class OldCommitteeRecord(
    val id: String = "",
    val committeeNumber: String = "",
    val committeeName: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val memberNames: String = "",
    val contactNumbers: String = "",
    val numberOfShares: String = "",
    val monthlyContributions: String = "",
    val totalCollection: String = "",
    val paymentHistory: String = "",
    val payoutRecipient: String = "",
    val payoutAmount: String = "",
    val payoutDate: String = "",
    val status: String = CommitteeStatus.COMPLETED.name,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
)

data class InstallmentRecord(
    val id: String = "",
    val committeeId: String = "",
    val memberId: String = "",
    val memberName: String = "",
    val memberPhone: String = "",
    val committeeNumber: Long = 1L,
    val monthKey: String = MonthUtils.currentMonthKey(),
    val monthIndex: Long = 1L,
    val dueAmount: Long = 5000L,
    val paidAmount: Long = 0L,
    val status: String = InstallmentStatus.UNPAID.name,
    val paymentDate: String? = null,
    val paymentNote: String? = null,
    val paymentMethod: String = PaymentMethod.CASH.name,
    val transactionId: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val statusEnum: InstallmentStatus get() = InstallmentStatus.fromRaw(status)
    val paymentMethodEnum: PaymentMethod get() = PaymentMethod.fromRaw(paymentMethod)
    val remainingAmount: Long get() = (dueAmount - paidAmount).coerceAtLeast(0L)
}

data class PaymentHistoryLog(
    val id: String = "",
    val committeeId: String = "",
    val installmentId: String = "",
    val memberId: String = "",
    val memberName: String = "",
    val monthKey: String = "",
    val previousPaidAmount: Long = 0L,
    val newPaidAmount: Long = 0L,
    val previousStatus: String = InstallmentStatus.UNPAID.name,
    val newStatus: String = InstallmentStatus.PAID.name,
    val paymentMethod: String? = null,
    val transactionId: String? = null,
    val actionNote: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class ReminderLog(
    val id: String = "",
    val committeeId: String = "",
    val memberId: String = "",
    val memberName: String = "",
    val memberPhone: String = "",
    val monthKey: String = "",
    val channel: String = ReminderChannel.WHATSAPP_MANUAL.name,
    val scheduleStage: String = ReminderStage.FIRST_28.name,
    val messagePreview: String = "",
    val dispatchStatus: String = DispatchStatus.OPENED_APP.name,
    val errorDetail: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class MonthlyChartPoint(
    val monthKey: String,
    val shortLabel: String,
    val collected: Long,
    val target: Long
)

data class DashboardSummary(
    val totalActiveCommittees: Int = 1,
    val totalCompletedCommittees: Int = 0,
    val activeMemberCount: Int,
    val targetMemberCount: Int,
    val totalSharesConfigured: Long = 10L,
    val totalMemberShares: Double = 0.0,
    val monthlyCollectionTarget: Long,
    val currentMonthCollected: Long,
    val currentMonthPending: Long,
    val totalCommitteeCollectedAllMonths: Long,
    val totalCommitteeExpectedAllMonths: Long,
    val paidMembersCount: Int,
    val partialMembersCount: Int,
    val unpaidMembersCount: Int,
    val currentMonthKey: String,
    val currentMonthIndex: Int,
    val totalMonths: Int,
    val nextPayoutMember: CommitteeMember?,
    val nextPayoutTurn: CommitteeTurn? = null,
    val nextPayoutAmount: Long = 0L,
    val upcomingPayoutDate: String,
    val nextPaymentDueDate: String = "",
    val completedPayouts: Int,
    val totalPayoutsAmount: Long = 0L,
    val remainingMonths: Int,
    val monthlyChartPoints: List<MonthlyChartPoint>
)

object MonthUtils {
    private val keyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    private val displayFormat = SimpleDateFormat("MMM yyyy", Locale.US)
    private val fullMonthFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun currentMonthKey(): String = keyFormat.format(Calendar.getInstance().time)
    fun todayDateString(): String = dateFormat.format(Calendar.getInstance().time)

    fun isValidMonthKey(key: String): Boolean =
        Regex("^[0-9]{4}-(0[1-9]|1[0-2])$").matches(key.trim())

    fun addMonths(startMonthKey: String, offsetMonths: Int): String {
        val parts = startMonthKey.split("-")
        if (parts.size != 2) return currentMonthKey()
        val year = parts[0].toIntOrNull() ?: return currentMonthKey()
        val month = (parts[1].toIntOrNull() ?: 1) - 1
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.add(Calendar.MONTH, offsetMonths)
        return keyFormat.format(cal.time)
    }

    fun monthKeysForCommittee(startMonth: String, totalMonths: Int): List<String> =
        (0 until totalMonths.coerceAtLeast(1)).map { addMonths(startMonth, it) }

    fun formatMonthDisplay(monthKey: String): String {
        val parts = monthKey.split("-")
        if (parts.size != 2) return monthKey
        val year = parts[0].toIntOrNull() ?: return monthKey
        val month = (parts[1].toIntOrNull() ?: 1) - 1
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return displayFormat.format(cal.time)
    }

    fun formatMonthFullDisplay(monthKey: String): String {
        val parts = monthKey.split("-")
        if (parts.size != 2) return monthKey
        val year = parts[0].toIntOrNull() ?: return monthKey
        val month = (parts[1].toIntOrNull() ?: 1) - 1
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return fullMonthFormat.format(cal.time)
    }

    fun formatCollectionWindow(monthKey: String, startDay: Long, endDay: Long): String {
        val nextMonth = addMonths(monthKey, 1)
        return "$startDay ${formatMonthDisplay(monthKey)} – $endDay ${formatMonthDisplay(nextMonth)}"
    }

    fun formatCollectionWindowFull(monthKey: String, startDay: Long, endDay: Long): String {
        val nextMonth = addMonths(monthKey, 1)
        return "$startDay ${formatMonthFullDisplay(monthKey)} to $endDay ${formatMonthFullDisplay(nextMonth)}"
    }

    fun formatDueDate(monthKey: String, startDay: Long, endDay: Long): String {
        val targetMonth = if (endDay < startDay) addMonths(monthKey, 1) else monthKey
        return "$endDay ${formatMonthDisplay(targetMonth)}"
    }

    fun formatPayoutDate(monthKey: String, payoutDay: Long, startDay: Long): String {
        val targetMonth = if (payoutDay < startDay) addMonths(monthKey, 1) else monthKey
        return "$payoutDay ${formatMonthDisplay(targetMonth)}"
    }
}

object PhoneUtils {
    fun toWhatsAppDigits(rawPhone: String): String {
        val digits = rawPhone.filter { it.isDigit() }
        return when {
            digits.startsWith("00") -> digits.drop(2)
            digits.startsWith("03") && digits.length == 11 -> "92" + digits.drop(1)
            digits.startsWith("3") && digits.length == 10 -> "92$digits"
            else -> digits
        }
    }

    fun toE164(rawPhone: String): String {
        val digits = toWhatsAppDigits(rawPhone)
        return if (digits.isNotEmpty()) "+$digits" else rawPhone.trim()
    }
}

object FormatUtils {
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    fun formatMoney(amount: Long, currency: String = "PKR"): String =
        if (currency.equals("Rs.", ignoreCase = true) || currency.equals("Rs", ignoreCase = true)) {
            "Rs. ${numberFormat.format(amount)}"
        } else {
            "$currency ${numberFormat.format(amount)}"
        }

    fun formatPkr(amount: Long): String = "PKR ${numberFormat.format(amount)}"
    fun formatCompactNumber(amount: Long): String = numberFormat.format(amount)

    fun formatShares(shares: Double): String {
        val rounded = shares.roundToLong().toDouble()
        return if (kotlin.math.abs(shares - rounded) < 0.001) {
            rounded.toLong().toString()
        } else {
            String.format(Locale.US, "%.1f", shares)
        }
    }
}

enum class DuplicateConflictType {
    PHONE, COMMITTEE_NUMBER
}

data class DuplicateConflict(
    val type: DuplicateConflictType,
    val existingMember: CommitteeMember
)

data class ReminderEligibility(
    val excludedPaidMembers: List<CommitteeMember>,
    val eligibleUnpaidOrPartial: List<Pair<CommitteeMember, Long>>
)

object CommitteeUtils {
    fun filterUnpaidForReminders(
        members: List<CommitteeMember>,
        monthInstallments: List<InstallmentRecord>,
        fallbackMonthlyShareAmount: Long
    ): ReminderEligibility {
        val activeSorted = members
            .filter { it.memberStatusEnum == MemberStatus.ACTIVE }
            .sortedBy { it.committeeNumber }
        val byMember = monthInstallments.associateBy { it.memberId }
        val excluded = mutableListOf<CommitteeMember>()
        val eligible = mutableListOf<Pair<CommitteeMember, Long>>()
        for (member in activeSorted) {
            val rec = byMember[member.id]
            if (rec?.statusEnum == InstallmentStatus.PAID) {
                excluded.add(member)
            } else {
                val due = rec?.remainingAmount?.takeIf { it > 0L }
                    ?: member.monthlyDue(fallbackMonthlyShareAmount)
                eligible.add(member to due)
            }
        }
        return ReminderEligibility(excludedPaidMembers = excluded, eligibleUnpaidOrPartial = eligible)
    }

    fun findDuplicateMember(
        members: List<CommitteeMember>,
        phone: String,
        committeeNumber: Long,
        excludeMemberId: String? = null
    ): DuplicateConflict? {
        val normalizedPhone = PhoneUtils.toWhatsAppDigits(phone)
        val active = members.filter {
            it.memberStatusEnum == MemberStatus.ACTIVE && it.id != excludeMemberId
        }
        val phoneMatch = active.firstOrNull {
            normalizedPhone.isNotEmpty() && PhoneUtils.toWhatsAppDigits(it.phone) == normalizedPhone
        }
        if (phoneMatch != null) {
            return DuplicateConflict(DuplicateConflictType.PHONE, phoneMatch)
        }
        val numMatch = active.firstOrNull { it.committeeNumber == committeeNumber }
        if (numMatch != null) {
            return DuplicateConflict(DuplicateConflictType.COMMITTEE_NUMBER, numMatch)
        }
        return null
    }
}

object OldHistoryCsvUtils {
    fun parseCsvToRecords(csvText: String): List<OldCommitteeRecord> {
        val lines = csvText.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val firstCols = parseCsvLine(lines.first()).map { it.trim().lowercase() }
        val hasHeader = firstCols.any {
            it.contains("committee") || it.contains("name") || it.contains("recipient") || it.contains("payout")
        }
        val dataLines = if (hasHeader) lines.drop(1) else lines

        return dataLines.mapIndexedNotNull { idx, line ->
            val cols = parseCsvLine(line)
            if (cols.all { it.isBlank() }) return@mapIndexedNotNull null
            fun col(i: Int): String = cols.getOrNull(i)?.trim().orEmpty()
            val cNum = col(0).ifBlank { "Old #${idx + 1}" }
            val cName = col(1).ifBlank { "Historical Committee $cNum" }
            val statusRaw = col(13).uppercase()
            val cleanStatus = when {
                statusRaw.contains("PENDING") -> CommitteeStatus.ACTIVE.name
                statusRaw.contains("ARCHIVE") -> CommitteeStatus.ARCHIVED.name
                else -> CommitteeStatus.COMPLETED.name
            }
            OldCommitteeRecord(
                committeeNumber = cNum,
                committeeName = cName,
                startDate = col(2),
                endDate = col(3),
                memberNames = col(4),
                contactNumbers = col(5),
                numberOfShares = col(6),
                monthlyContributions = col(7),
                totalCollection = col(8),
                paymentHistory = col(9),
                payoutRecipient = col(10),
                payoutAmount = col(11),
                payoutDate = col(12),
                status = cleanStatus
            )
        }
    }

    fun exportToCsv(records: List<OldCommitteeRecord>): String {
        val sb = StringBuilder()
        sb.appendLine(
            "Committee Number,Committee Name,Start Date,End Date,Member Names,Contact Numbers," +
                "Number of Shares,Monthly Contributions,Total Collection,Payment History," +
                "Payout Recipient,Payout Amount,Payout Date,Status"
        )
        fun esc(v: String): String {
            val cleaned = v.replace("\r", " ").replace("\n", "; ")
            return if (cleaned.contains(",") || cleaned.contains("\"")) {
                "\"${cleaned.replace("\"", "\"\"")}\""
            } else cleaned
        }
        records.forEach { r ->
            sb.appendLine(
                listOf(
                    r.committeeNumber, r.committeeName, r.startDate, r.endDate,
                    r.memberNames, r.contactNumbers, r.numberOfShares,
                    r.monthlyContributions, r.totalCollection, r.paymentHistory,
                    r.payoutRecipient, r.payoutAmount, r.payoutDate, r.status
                ).joinToString(",") { esc(it) }
            )
        }
        return sb.toString()
    }

    fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}

data class BackupPreview(
    val committeeCount: Int,
    val memberCount: Int,
    val installmentCount: Int,
    val turnCount: Int = 0,
    val oldRecordCount: Int = 0,
    val exportedAt: String
)

object BackupUtils {
    fun parsePreview(json: String): Result<BackupPreview> {
        return try {
            val root = org.json.JSONObject(json)
            val committees = root.getJSONArray("committees")
            var members = 0
            var installments = 0
            var turns = 0
            for (i in 0 until committees.length()) {
                val c = committees.getJSONObject(i)
                members += c.optJSONArray("members")?.length() ?: 0
                installments += c.optJSONArray("installments")?.length() ?: 0
                turns += c.optJSONArray("turns")?.length() ?: 0
            }
            val oldCount = root.optJSONArray("oldCommitteeRecords")?.length() ?: 0
            Result.success(
                BackupPreview(
                    committeeCount = committees.length(),
                    memberCount = members,
                    installmentCount = installments,
                    turnCount = turns,
                    oldRecordCount = oldCount,
                    exportedAt = root.optString("exportedAt", "")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
