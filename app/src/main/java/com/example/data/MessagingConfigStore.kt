package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.i18n.AppLanguage
import com.example.model.ReminderStage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.messagingDataStore: DataStore<Preferences> by preferencesDataStore(name = "gm_committee_prefs")

data class MessagingGatewayConfig(
    val whatsappPhoneNumberId: String = "",
    val whatsappAccessToken: String = "",
    val smsGatewayUrl: String = "",
    val smsGatewayApiKey: String = "",
    val smsSenderId: String = "GM HASMANI",
    val localLanguageCode: String = "en",
    val syncWebhookUrl: String = ""
) {
    val isWhatsAppApiConfigured: Boolean
        get() = whatsappPhoneNumberId.isNotBlank() && whatsappAccessToken.isNotBlank()

    val isSmsGatewayConfigured: Boolean
        get() = smsGatewayUrl.isNotBlank()

    val isSyncWebhookConfigured: Boolean
        get() = syncWebhookUrl.isNotBlank()
}

class MessagingConfigStore(private val context: Context) {

    private object Keys {
        val WA_PHONE_NUMBER_ID = stringPreferencesKey("wa_phone_number_id")
        val WA_ACCESS_TOKEN = stringPreferencesKey("wa_access_token")
        val SMS_GATEWAY_URL = stringPreferencesKey("sms_gateway_url")
        val SMS_GATEWAY_API_KEY = stringPreferencesKey("sms_gateway_api_key")
        val SMS_SENDER_ID = stringPreferencesKey("sms_sender_id")
        val LANGUAGE = stringPreferencesKey("app_language")
        val ACTIVE_COMMITTEE_ID = stringPreferencesKey("active_committee_id")
        val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
        val IS_LOGGED_OUT = booleanPreferencesKey("is_logged_out")
        val SYNC_WEBHOOK_URL = stringPreferencesKey("sync_webhook_url")
        fun templateKey(stage: ReminderStage, lang: AppLanguage) =
            stringPreferencesKey("tpl_${stage.name}_${lang.code}")
    }

    val activeCommitteeIdFlow: Flow<String?> = context.messagingDataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_COMMITTEE_ID]?.takeIf { it.isNotBlank() }
    }

    val currentUserIdFlow: Flow<String?> = context.messagingDataStore.data.map { prefs ->
        prefs[Keys.CURRENT_USER_ID]?.takeIf { it.isNotBlank() }
    }

    val isLoggedOutFlow: Flow<Boolean> = context.messagingDataStore.data.map { prefs ->
        prefs[Keys.IS_LOGGED_OUT] ?: false
    }

    val configFlow: Flow<MessagingGatewayConfig> = context.messagingDataStore.data.map { prefs ->
        MessagingGatewayConfig(
            whatsappPhoneNumberId = prefs[Keys.WA_PHONE_NUMBER_ID] ?: "",
            whatsappAccessToken = prefs[Keys.WA_ACCESS_TOKEN] ?: "",
            smsGatewayUrl = prefs[Keys.SMS_GATEWAY_URL] ?: "",
            smsGatewayApiKey = prefs[Keys.SMS_GATEWAY_API_KEY] ?: "",
            smsSenderId = prefs[Keys.SMS_SENDER_ID] ?: "GM HASMANI",
            localLanguageCode = prefs[Keys.LANGUAGE] ?: "en",
            syncWebhookUrl = prefs[Keys.SYNC_WEBHOOK_URL] ?: ""
        )
    }

    val templatesFlow: Flow<Map<Pair<ReminderStage, AppLanguage>, String>> =
        context.messagingDataStore.data.map { prefs ->
            val map = mutableMapOf<Pair<ReminderStage, AppLanguage>, String>()
            for (stage in ReminderStage.entries) {
                for (lang in AppLanguage.entries) {
                    val saved = prefs[Keys.templateKey(stage, lang)]
                    if (!saved.isNullOrBlank()) {
                        map[stage to lang] = saved
                    }
                }
            }
            map
        }

    suspend fun saveActiveCommitteeId(committeeId: String?) {
        context.messagingDataStore.edit { prefs ->
            if (committeeId.isNullOrBlank()) {
                prefs.remove(Keys.ACTIVE_COMMITTEE_ID)
            } else {
                prefs[Keys.ACTIVE_COMMITTEE_ID] = committeeId
            }
        }
    }

    suspend fun saveSession(userId: String?, isLoggedOut: Boolean) {
        context.messagingDataStore.edit { prefs ->
            if (userId != null) {
                prefs[Keys.CURRENT_USER_ID] = userId
            }
            prefs[Keys.IS_LOGGED_OUT] = isLoggedOut
        }
    }

    suspend fun saveSyncWebhookUrl(url: String) {
        context.messagingDataStore.edit { prefs ->
            prefs[Keys.SYNC_WEBHOOK_URL] = url.trim()
        }
    }

    suspend fun saveGatewayConfig(
        whatsappPhoneNumberId: String,
        whatsappAccessToken: String,
        smsGatewayUrl: String,
        smsGatewayApiKey: String,
        smsSenderId: String
    ) {
        context.messagingDataStore.edit { prefs ->
            prefs[Keys.WA_PHONE_NUMBER_ID] = whatsappPhoneNumberId.trim()
            prefs[Keys.WA_ACCESS_TOKEN] = whatsappAccessToken.trim()
            prefs[Keys.SMS_GATEWAY_URL] = smsGatewayUrl.trim()
            prefs[Keys.SMS_GATEWAY_API_KEY] = smsGatewayApiKey.trim()
            prefs[Keys.SMS_SENDER_ID] = smsSenderId.trim().ifEmpty { "GM HASMANI" }
        }
    }

    suspend fun saveLanguage(language: AppLanguage) {
        context.messagingDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    suspend fun saveCustomTemplate(stage: ReminderStage, language: AppLanguage, templateText: String) {
        context.messagingDataStore.edit { prefs ->
            if (templateText.isBlank()) {
                prefs.remove(Keys.templateKey(stage, language))
            } else {
                prefs[Keys.templateKey(stage, language)] = templateText
            }
        }
    }
}
