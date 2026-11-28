package com.example.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.model.AppCompanySettings
import com.example.model.Committee
import com.example.model.CommitteeMember
import com.example.model.FormatUtils
import com.example.model.ForwardFinancialSummary
import com.example.model.InstallmentRecord
import com.example.model.InstallmentStatus
import com.example.model.MemberStatus
import com.example.model.MonthUtils
import com.example.model.PhoneUtils
import com.example.model.UrduMessageType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed class ApiSendResult {
    data class Success(val messageId: String) : ApiSendResult()
    data class Failure(val error: String) : ApiSendResult()
}

class MessagingGateway(
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    fun renderMessage(
        template: String,
        member: CommitteeMember,
        committee: Committee,
        monthKey: String,
        dueAmount: Long,
        settings: AppCompanySettings = AppCompanySettings()
    ): String {
        val window = MonthUtils.formatCollectionWindow(
            monthKey,
            committee.collectionDayStart,
            committee.collectionDayEnd
        )
        return template
            .replace("{name}", member.name)
            .replace("{amount}", FormatUtils.formatMoney(dueAmount, settings.currency))
            .replace("{committee}", committee.name)
            .replace("{month}", MonthUtils.formatMonthDisplay(monthKey))
            .replace("{window}", window)
            .replace("{number}", "#${member.committeeNumber}")
            .replace("{easypaisa}", settings.easypaisaNumber)
            .replace("{holder}", settings.easypaisaAccountHolder)
    }

    fun renderUrduMessage(
        type: UrduMessageType,
        memberName: String = "ممبر",
        amount: Long = 0L,
        date: String = MonthUtils.todayDateString(),
        remainingAmount: Long = 0L,
        adminMessage: String = "",
        customTemplate: String? = null,
        settings: AppCompanySettings = AppCompanySettings()
    ): String {
        val rawTemplate = customTemplate?.takeIf { it.isNotBlank() } ?: type.templateUr
        val formattedAmount = FormatUtils.formatMoney(amount.coerceAtLeast(0L), settings.currency)
        val formattedRemaining = FormatUtils.formatMoney(remainingAmount.coerceAtLeast(0L), settings.currency)
        val cleanName = memberName.ifBlank { "ممبر" }
        val cleanDate = date.ifBlank { MonthUtils.todayDateString() }
        val cleanAdminMsg = adminMessage.ifBlank { "اہم اطلاع کے لیے رابطہ کریں" }

        var rendered = rawTemplate
            .replace("[Member Name]", cleanName)
            .replace("[Amount]", formattedAmount)
            .replace("[Date]", cleanDate)
            .replace("[Remaining Amount]", formattedRemaining)
            .replace("[Admin Message]", cleanAdminMsg)
            .replace("{name}", cleanName)
            .replace("{amount}", formattedAmount)
            .replace("{date}", cleanDate)
            .replace("{remaining}", formattedRemaining)

        if (!rendered.contains("G M S R HASHMANI THAR", ignoreCase = true)) {
            rendered = "G M S R HASHMANI THAR\n$rendered"
        }
        val officialContact = settings.adminContactNumber.ifBlank { "03423498631" }
        if (!rendered.contains("03423498631") && !rendered.contains(officialContact)) {
            rendered = "$rendered\nرابطہ: 03423498631"
        }
        return rendered
    }

    fun buildPaymentRequestMessage(
        memberName: String,
        amount: Long,
        dueDate: String,
        committeeName: String,
        settings: AppCompanySettings
    ): String {
        val formattedAmount = FormatUtils.formatCompactNumber(amount)
        val epNumber = settings.easypaisaNumber.ifBlank { "03423498631" }
        val holder = settings.easypaisaAccountHolder.ifBlank { settings.adminName }
        val currency = settings.currency.ifBlank { "PKR" }
        val brand = settings.companyName.ifBlank { "G M S R HASHMANI THAR" }
        val contact = settings.adminContactNumber.ifBlank { "03423498631" }
        return buildString {
            appendLine("Assalam-o-Alaikum $memberName,")
            appendLine("Your monthly committee installment ($committeeName) of $currency $formattedAmount is due by $dueDate.")
            appendLine("Please send your payment via Easypaisa:")
            appendLine("Easypaisa Number: $epNumber")
            appendLine("Account Holder: $holder")
            appendLine("Contact: $contact")
            appendLine("JazakAllah.")
            append("- $brand Management")
        }
    }

    /**
     * Generates an all-Urdu financial collection & forward-payment summary for the Committee Organizer,
     * keeping the official organization name strictly in English (`G M S R HASHMANI THAR`) and official
     * contact (`03423498631`). Includes Paid members, Unpaid/Partial members, Total Forwarded, and Remaining Balance.
     */
    fun buildOrganizerUrduSummaryMessage(
        committeeName: String,
        organizerName: String,
        summary: ForwardFinancialSummary,
        members: List<CommitteeMember>,
        scopedInstallments: List<InstallmentRecord>,
        settings: AppCompanySettings = AppCompanySettings()
    ): String {
        val brand = "G M S R HASHMANI THAR"
        val contact = settings.adminContactNumber.ifBlank { "03423498631" }
        val currency = settings.currency.ifBlank { "PKR" }
        val periodText = if (summary.isMonthlyScope && !summary.monthKey.isNullOrBlank()) {
            MonthUtils.formatMonthDisplay(summary.monthKey)
        } else {
            "مجموعی کمیٹی حساب"
        }

        val activeMembers = members.filter { it.memberStatusEnum == MemberStatus.ACTIVE }
            .sortedBy { it.committeeNumber }
        val installmentByMember = scopedInstallments.groupBy { it.memberId }

        val paidLines = mutableListOf<String>()
        val unpaidLines = mutableListOf<String>()

        activeMembers.forEach { member ->
            val recs = installmentByMember[member.id].orEmpty()
            val paidSum = recs.sumOf { it.paidAmount.coerceAtLeast(0L) }
            val dueSum = recs.sumOf { it.dueAmount.coerceAtLeast(0L) }
                .takeIf { it > 0L } ?: 0L
            val remaining = (dueSum - paidSum).coerceAtLeast(0L)
            val allPaid = recs.isNotEmpty() && recs.all { it.statusEnum == InstallmentStatus.PAID }

            if (allPaid || (paidSum > 0L && remaining == 0L)) {
                paidLines.add("• #${member.committeeNumber} ${member.name}: ${FormatUtils.formatMoney(paidSum, currency)}")
            } else if (paidSum > 0L) {
                paidLines.add("• #${member.committeeNumber} ${member.name} (جزوی وصول): ${FormatUtils.formatMoney(paidSum, currency)}")
                unpaidLines.add("• #${member.committeeNumber} ${member.name} (بقایا): ${FormatUtils.formatMoney(remaining, currency)}")
            } else {
                val estDue = if (dueSum > 0L) FormatUtils.formatMoney(dueSum, currency) else "مکمل قسط بقایا"
                unpaidLines.add("• #${member.committeeNumber} ${member.name}: $estDue")
            }
        }

        return buildString {
            appendLine(brand)
            appendLine("السلام علیکم محترم ${organizerName.ifBlank { "آرگنائزر صاحب" }}،")
            appendLine("کمیٹی: $committeeName ($periodText)")
            appendLine("-------------------------")
            appendLine("متوقع کل رقم: ${FormatUtils.formatMoney(summary.expectedFromMembers, currency)}")
            appendLine("ممبرز سے وصول شدہ رقم: ${FormatUtils.formatMoney(summary.collectedFromMembers, currency)}")
            appendLine("ممبرز سے بقایا رقم: ${FormatUtils.formatMoney(summary.pendingMemberCollection, currency)}")
            appendLine("آگے ادا کی گئی رقم (فارورڈ): ${FormatUtils.formatMoney(summary.totalForwarded, currency)}")
            appendLine("فارورڈ کی بقایا رقم: ${FormatUtils.formatMoney(summary.pendingForwardPayment, currency)}")
            appendLine("ایڈمن کے پاس موجود رقم: ${FormatUtils.formatMoney(summary.amountCurrentlyHeldByAdmin, currency)}")
            appendLine("-------------------------")
            appendLine("ادائیگی کرنے والے ممبرز (${paidLines.size}):")
            if (paidLines.isEmpty()) {
                appendLine("• ابھی تک کوئی ادائیگی درج نہیں ہوئی")
            } else {
                paidLines.forEach { appendLine(it) }
            }
            appendLine()
            appendLine("بقایا / غیر ادا شدہ ممبرز (${unpaidLines.size}):")
            if (unpaidLines.isEmpty()) {
                appendLine("• تمام ممبرز نے قسط ادا کر دی ہے")
            } else {
                unpaidLines.forEach { appendLine(it) }
            }
            appendLine("-------------------------")
            append("رابطہ: $contact | $brand")
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: return false
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Real WhatsApp Business Cloud API dispatch (Meta Graph API v21.0).
     */
    suspend fun sendViaWhatsAppCloudApi(
        config: MessagingGatewayConfig,
        rawPhone: String,
        messageBody: String
    ): ApiSendResult = withContext(Dispatchers.IO) {
        if (!config.isWhatsAppApiConfigured) {
            return@withContext ApiSendResult.Failure("WhatsApp Business Cloud API credentials are not configured.")
        }
        val toDigits = PhoneUtils.toWhatsAppDigits(rawPhone)
        if (toDigits.length < 8) {
            return@withContext ApiSendResult.Failure("Invalid mobile number: $rawPhone")
        }

        val url = "https://graph.facebook.com/v21.0/${config.whatsappPhoneNumberId.trim()}/messages"
        val payload = JSONObject().apply {
            put("messaging_product", "whatsapp")
            put("recipient_type", "individual")
            put("to", toDigits)
            put("type", "text")
            put("text", JSONObject().apply {
                put("preview_url", false)
                put("body", messageBody)
            })
        }

        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer ${config.whatsappAccessToken.trim()}")
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    val msgId = try {
                        JSONObject(bodyString)
                            .optJSONArray("messages")
                            ?.optJSONObject(0)
                            ?.optString("id")
                            .orEmpty()
                    } catch (_: Exception) {
                        ""
                    }
                    ApiSendResult.Success(msgId.ifBlank { "HTTP ${response.code}" })
                } else {
                    val errorMsg = try {
                        JSONObject(bodyString).optJSONObject("error")?.optString("message")
                            ?: "HTTP ${response.code}: ${bodyString.take(150)}"
                    } catch (_: Exception) {
                        "HTTP ${response.code}: ${bodyString.take(150)}"
                    }
                    ApiSendResult.Failure(errorMsg)
                }
            }
        } catch (e: Exception) {
            ApiSendResult.Failure(e.localizedMessage ?: "Network error contacting WhatsApp Cloud API")
        }
    }

    /**
     * Real HTTP SMS Gateway dispatch.
     */
    suspend fun sendViaSmsGateway(
        config: MessagingGatewayConfig,
        rawPhone: String,
        messageBody: String
    ): ApiSendResult = withContext(Dispatchers.IO) {
        if (!config.isSmsGatewayConfigured) {
            return@withContext ApiSendResult.Failure("SMS Gateway URL is not configured.")
        }
        val toPhone = PhoneUtils.toE164(rawPhone)
        val payload = JSONObject().apply {
            put("to", toPhone)
            put("message", messageBody)
            put("senderId", config.smsSenderId.ifBlank { "G M S R HASHMANI THAR" })
        }

        try {
            val builder = Request.Builder()
                .url(config.smsGatewayUrl.trim())
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))

            if (config.smsGatewayApiKey.isNotBlank()) {
                builder.addHeader("Authorization", "Bearer ${config.smsGatewayApiKey.trim()}")
                builder.addHeader("X-API-Key", config.smsGatewayApiKey.trim())
            }

            httpClient.newCall(builder.build()).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    ApiSendResult.Success("HTTP ${response.code}")
                } else {
                    ApiSendResult.Failure("HTTP ${response.code}: ${bodyString.take(150)}")
                }
            }
        } catch (e: Exception) {
            ApiSendResult.Failure(e.localizedMessage ?: "Network error contacting SMS Gateway")
        }
    }

    fun openWhatsAppManual(context: Context, rawPhone: String, messageBody: String): Boolean {
        val digits = PhoneUtils.toWhatsAppDigits(rawPhone)
        val encoded = URLEncoder.encode(messageBody, "UTF-8")
        val uri = if (digits.length >= 7) {
            Uri.parse("https://wa.me/$digits?text=$encoded")
        } else {
            Uri.parse("https://wa.me/?text=$encoded")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openSmsManual(context: Context, rawPhone: String, messageBody: String): Boolean {
        val cleanedPhone = PhoneUtils.toE164(rawPhone)
        val uri = if (cleanedPhone.isNotBlank()) Uri.parse("smsto:$cleanedPhone") else Uri.parse("smsto:")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", messageBody)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }
}
