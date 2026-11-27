package com.example.model

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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

data class Committee(
    val id: String = "",
    val name: String = "",
    val memberCount: Long = 10L,
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
    val monthlyCollectionTotal: Long get() = memberCount * monthlyInstallment
    val totalCommitteeAmount: Long get() = monthlyCollectionTotal * totalMonths
    val eachPayoutAmount: Long get() = monthlyCollectionTotal
}

data class CommitteeMember(
    val id: String = "",
    val committeeId: String = "",
    val name: String = "",
    val phone: String = "",
    val committeeNumber: Long = 1L,
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
}

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
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    val statusEnum: InstallmentStatus get() = InstallmentStatus.fromRaw(status)
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
    val activeMemberCount: Int,
    val targetMemberCount: Int,
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
    val upcomingPayoutDate: String,
    val completedPayouts: Int,
    val remainingMonths: Int,
    val monthlyChartPoints: List<MonthlyChartPoint>
)

object MonthUtils {
    private val keyFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    private val displayFormat = SimpleDateFormat("MMM yyyy", Locale.US)
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

    fun formatCollectionWindow(monthKey: String, startDay: Long, endDay: Long): String {
        val nextMonth = addMonths(monthKey, 1)
        return "${startDay}th ${formatMonthDisplay(monthKey).take(3)} – ${endDay}th ${formatMonthDisplay(nextMonth).take(3)}"
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

    fun formatPkr(amount: Long): String = "PKR ${numberFormat.format(amount)}"
    fun formatCompactNumber(amount: Long): String = numberFormat.format(amount)
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
        fallbackMonthlyInstallment: Long
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
                val due = rec?.remainingAmount?.takeIf { it > 0L } ?: fallbackMonthlyInstallment
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

data class BackupPreview(
    val committeeCount: Int,
    val memberCount: Int,
    val installmentCount: Int,
    val exportedAt: String
)

object BackupUtils {
    fun parsePreview(json: String): Result<BackupPreview> {
        return try {
            val root = org.json.JSONObject(json)
            val committees = root.getJSONArray("committees")
            var members = 0
            var installments = 0
            for (i in 0 until committees.length()) {
                val c = committees.getJSONObject(i)
                members += c.optJSONArray("members")?.length() ?: 0
                installments += c.optJSONArray("installments")?.length() ?: 0
            }
            Result.success(
                BackupPreview(
                    committeeCount = committees.length(),
                    memberCount = members,
                    installmentCount = installments,
                    exportedAt = root.optString("exportedAt", "")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
