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
import com.example.model.MonthUtils
import com.example.model.PhoneUtils
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
        return buildString {
            appendLine("Assalam-o-Alaikum $memberName,")
            appendLine("Your monthly committee installment ($committeeName) of $currency $formattedAmount is due by $dueDate.")
            appendLine("Please send your payment via Easypaisa:")
            appendLine("Easypaisa Number: $epNumber")
            appendLine("Account Holder: $holder")
            appendLine("JazakAllah.")
            append("- ${settings.companyName} Management")
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
            put("senderId", config.smsSenderId.ifBlank { "GM HASMANI" })
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
