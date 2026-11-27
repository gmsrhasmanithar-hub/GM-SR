package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
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

@Keep
@IgnoreExtraProperties
data class AdminProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val preferredLanguage: String = "en",
    val activeCommitteeId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to displayName.take(80),
        "email" to email.take(120),
        "preferredLanguage" to (if (preferredLanguage == "ur") "ur" else "en"),
        "activeCommitteeId" to activeCommitteeId?.takeIf { it.isNotBlank() },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class Committee(
    @DocumentId val id: String = "",
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
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val statusEnum: CommitteeStatus get() = CommitteeStatus.fromRaw(status)
    val monthlyCollectionTotal: Long get() = memberCount * monthlyInstallment
    val totalCommitteeAmount: Long get() = monthlyCollectionTotal * totalMonths
    val eachPayoutAmount: Long get() = monthlyCollectionTotal

    fun toCreateMap(): Map<String, Any> = mapOf(
        "name" to name.trim().take(80),
        "memberCount" to memberCount.coerceIn(2L, 200L),
        "monthlyInstallment" to monthlyInstallment.coerceIn(100L, 10000000L),
        "totalMonths" to totalMonths.coerceIn(2L, 200L),
        "collectionDayStart" to collectionDayStart.coerceIn(1L, 31L),
        "collectionDayEnd" to collectionDayEnd.coerceIn(1L, 31L),
        "payoutDay" to payoutDay.coerceIn(1L, 31L),
        "startMonth" to startMonth,
        "endMonth" to endMonth,
        "status" to statusEnum.name,
        "notes" to notes?.trim()?.take(500)?.takeIf { it.isNotEmpty() },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class CommitteeMember(
    @DocumentId val id: String = "",
    val name: String = "",
    val phone: String = "",
    val committeeNumber: Long = 1L,
    val payoutMonth: String = MonthUtils.currentMonthKey(),
    val payoutStatus: String = PayoutStatus.PENDING.name,
    val payoutDate: String? = null,
    val payoutAmount: Long = 0L,
    val status: String = MemberStatus.ACTIVE.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val payoutStatusEnum: PayoutStatus get() = PayoutStatus.fromRaw(payoutStatus)
    val memberStatusEnum: MemberStatus get() = MemberStatus.fromRaw(status)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "name" to name.trim().take(80),
        "phone" to phone.trim().take(25),
        "committeeNumber" to committeeNumber.coerceIn(1L, 500L),
        "payoutMonth" to payoutMonth,
        "payoutStatus" to payoutStatusEnum.name,
        "payoutDate" to payoutDate?.trim()?.take(20)?.takeIf { it.isNotEmpty() },
        "payoutAmount" to payoutAmount.coerceAtLeast(0L),
        "status" to memberStatusEnum.name,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class InstallmentRecord(
    @DocumentId val id: String = "",
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
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val statusEnum: InstallmentStatus get() = InstallmentStatus.fromRaw(status)
    val remainingAmount: Long get() = (dueAmount - paidAmount).coerceAtLeast(0L)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "memberId" to memberId,
        "memberName" to memberName.trim().take(80),
        "memberPhone" to memberPhone.trim().take(25),
        "committeeNumber" to committeeNumber.coerceIn(1L, 500L),
        "monthKey" to monthKey,
        "monthIndex" to monthIndex.coerceIn(1L, 500L),
        "dueAmount" to dueAmount.coerceIn(100L, 10000000L),
        "paidAmount" to paidAmount.coerceIn(0L, 10000000L),
        "status" to statusEnum.name,
        "paymentDate" to paymentDate?.trim()?.take(20)?.takeIf { it.isNotEmpty() },
        "paymentNote" to paymentNote?.trim()?.take(200)?.takeIf { it.isNotEmpty() },
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class PaymentHistoryLog(
    @DocumentId val id: String = "",
    val installmentId: String = "",
    val memberId: String = "",
    val memberName: String = "",
    val monthKey: String = "",
    val previousPaidAmount: Long = 0L,
    val newPaidAmount: Long = 0L,
    val previousStatus: String = InstallmentStatus.UNPAID.name,
    val newStatus: String = InstallmentStatus.PAID.name,
    val actionNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "installmentId" to installmentId,
        "memberId" to memberId,
        "memberName" to memberName.trim().take(80),
        "monthKey" to monthKey,
        "previousPaidAmount" to previousPaidAmount.coerceAtLeast(0L),
        "newPaidAmount" to newPaidAmount.coerceAtLeast(0L),
        "previousStatus" to InstallmentStatus.fromRaw(previousStatus).name,
        "newStatus" to InstallmentStatus.fromRaw(newStatus).name,
        "actionNote" to actionNote?.trim()?.take(200)?.takeIf { it.isNotEmpty() },
        "createdAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

@Keep
@IgnoreExtraProperties
data class ReminderLog(
    @DocumentId val id: String = "",
    val memberId: String = "",
    val memberName: String = "",
    val memberPhone: String = "",
    val monthKey: String = "",
    val channel: String = ReminderChannel.WHATSAPP_MANUAL.name,
    val scheduleStage: String = ReminderStage.FIRST_28.name,
    val messagePreview: String = "",
    val dispatchStatus: String = DispatchStatus.OPENED_APP.name,
    val errorDetail: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "memberId" to memberId,
        "memberName" to memberName.trim().take(80),
        "memberPhone" to memberPhone.trim().take(25),
        "monthKey" to monthKey,
        "channel" to ReminderChannel.fromRaw(channel).name,
        "scheduleStage" to ReminderStage.fromRaw(scheduleStage).name,
        "messagePreview" to messagePreview.trim().take(500).ifEmpty { "Reminder" },
        "dispatchStatus" to DispatchStatus.fromRaw(dispatchStatus).name,
        "errorDetail" to errorDetail?.trim()?.take(300)?.takeIf { it.isNotEmpty() },
        "createdAt" to FieldValue.serverTimestamp()
    ).filterValues { it != null }.mapValues { it.value!! }
}

data class MonthlyChartPoint(
    val monthKey: String,
    val shortLabel: String,
    val collected: Long,
    val target: Long
)

data class DashboardSummary(
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
        // If collection spans 28th to 8th of next month, payout is on payoutDay of the next month if payoutDay < startDay
        val targetMonth = if (payoutDay < startDay) addMonths(monthKey, 1) else monthKey
        return "$payoutDay ${formatMonthDisplay(targetMonth)}"
    }
}

object PhoneUtils {
    /**
     * Normalizes Pakistani and international mobile numbers to digits without '+' for wa.me & Cloud API.
     * Example: "0300-1234567" -> "923001234567", "+92 300 1234567" -> "923001234567".
     */
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
