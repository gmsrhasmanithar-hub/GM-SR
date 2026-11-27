package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
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
        dueAmount: Long
    ): String {
        val window = MonthUtils.formatCollectionWindow(
            monthKey,
            committee.collectionDayStart,
            committee.collectionDayEnd
        )
        return template
            .replace("{name}", member.name)
            .replace("{amount}", FormatUtils.formatPkr(dueAmount))
            .replace("{committee}", committee.name)
            .replace("{month}", MonthUtils.formatMonthDisplay(monthKey))
            .replace("{window}", window)
            .replace("{number}", "#${member.committeeNumber}")
    }

    /**
     * Real WhatsApp Business Cloud API dispatch (Meta Graph API v21.0).
     * Returns Success ONLY when Meta Cloud API returns HTTP 2xx with a message ID.
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
     * Posts JSON { to, message, senderId } with optional Bearer API key.
     * Returns Success ONLY when gateway responds HTTP 2xx.
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

    /**
     * Opens WhatsApp directly with pre-filled recipient number and prepared message.
     * Returns true if an app handled the intent, false if WhatsApp/browser could not be launched.
     */
    fun openWhatsAppManual(context: Context, rawPhone: String, messageBody: String): Boolean {
        val digits = PhoneUtils.toWhatsAppDigits(rawPhone)
        val encoded = URLEncoder.encode(messageBody, "UTF-8")
        val uri = Uri.parse("https://wa.me/$digits?text=$encoded")
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

    /**
     * Opens the device SMS app using zero-permission ACTION_SENDTO smsto: intent.
     */
    fun openSmsManual(context: Context, rawPhone: String, messageBody: String): Boolean {
        val cleanedPhone = PhoneUtils.toE164(rawPhone)
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanedPhone")).apply {
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
