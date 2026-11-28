package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.i18n.AppLanguage
import com.example.model.AppCompanySettings
import com.example.model.ReminderStage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.messagingDataStore: DataStore<Preferences> by preferencesDataStore(name = "gm_committee_prefs")

data class MessagingGatewayConfig(
    val whatsappPhoneNumberId: String = "",
    val whatsappAccessToken: String = "",
    val smsGatewayUrl: String = "",
    val smsGatewayApiKey: String = "",
    val smsSenderId: String = "G M S R HASHMANI THAR",
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

        // Company, Easypaisa, Committee Defaults, Application Settings
        val COMPANY_NAME = stringPreferencesKey("company_name")
        val APPLICATION_NAME = stringPreferencesKey("application_name")
        val ADMIN_NAME = stringPreferencesKey("admin_name")
        val ADMIN_CONTACT = stringPreferencesKey("admin_contact")
        val EASYPAISA_NUMBER = stringPreferencesKey("easypaisa_number")
        val EASYPAISA_HOLDER = stringPreferencesKey("easypaisa_holder")
        val CURRENCY = stringPreferencesKey("currency")
        val DEFAULT_CONTRIBUTION = longPreferencesKey("default_monthly_contribution")
        val DEFAULT_SHARES = longPreferencesKey("default_total_shares")
        val DEFAULT_START_DAY = longPreferencesKey("default_collection_start_day")
        val DEFAULT_END_DAY = longPreferencesKey("default_collection_end_day")
        val DEFAULT_DURATION_MONTHS = longPreferencesKey("default_duration_months")
        val PDF_FOOTER_NOTE = stringPreferencesKey("pdf_footer_note")

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
            smsSenderId = prefs[Keys.SMS_SENDER_ID]?.takeIf {
                it.isNotBlank() && it != "GM HASMANI"
            } ?: "G M S R HASHMANI THAR",
            localLanguageCode = prefs[Keys.LANGUAGE] ?: "en",
            syncWebhookUrl = prefs[Keys.SYNC_WEBHOOK_URL] ?: ""
        )
    }

    val companySettingsFlow: Flow<AppCompanySettings> = context.messagingDataStore.data.map { prefs ->
        val rawCompany = prefs[Keys.COMPANY_NAME]?.trim().orEmpty()
        val migratedCompany = when {
            rawCompany.isEmpty() ||
                rawCompany.equals("GM HASMANI COMMITTEE", ignoreCase = true) ||
                rawCompany.equals("GM HASMANI COMMITTEE MANAGER", ignoreCase = true) ||
                rawCompany.equals("GM Hasmani Committee Manager", ignoreCase = true) -> "G M S R HASHMANI THAR"
            else -> rawCompany
        }
        val rawAppName = prefs[Keys.APPLICATION_NAME]?.trim().orEmpty()
        val migratedAppName = when {
            rawAppName.isEmpty() ||
                rawAppName.equals("GM Hasmani Committee Manager", ignoreCase = true) ||
                rawAppName.equals("GM HASMANI COMMITTEE", ignoreCase = true) -> "G M S R HASHMANI THAR"
            else -> rawAppName
        }
        val rawFooter = prefs[Keys.PDF_FOOTER_NOTE]?.trim().orEmpty()
        val migratedFooter = when {
            rawFooter.isEmpty() ||
                rawFooter.equals("Official GM HASMANI Committee Record", ignoreCase = true) ->
                "Official G M S R HASHMANI THAR Committee Record • 03423498631"
            else -> rawFooter
        }
        AppCompanySettings(
            companyName = migratedCompany,
            applicationName = migratedAppName,
            adminName = prefs[Keys.ADMIN_NAME]?.takeIf { it.isNotBlank() } ?: "G M S R HASHMANI THAR Admin",
            adminContactNumber = prefs[Keys.ADMIN_CONTACT]?.takeIf { it.isNotBlank() } ?: "03423498631",
            easypaisaNumber = prefs[Keys.EASYPAISA_NUMBER]?.takeIf { it.isNotBlank() } ?: "03423498631",
            easypaisaAccountHolder = prefs[Keys.EASYPAISA_HOLDER]?.takeIf { it.isNotBlank() } ?: "G M S R HASHMANI THAR",
            currency = prefs[Keys.CURRENCY]?.takeIf { it.isNotBlank() } ?: "PKR",
            defaultMonthlyContribution = prefs[Keys.DEFAULT_CONTRIBUTION] ?: 5000L,
            defaultTotalShares = prefs[Keys.DEFAULT_SHARES] ?: 10L,
            defaultCollectionStartDay = prefs[Keys.DEFAULT_START_DAY] ?: 28L,
            defaultCollectionEndDay = prefs[Keys.DEFAULT_END_DAY] ?: 8L,
            defaultDurationMonths = prefs[Keys.DEFAULT_DURATION_MONTHS] ?: 10L,
            pdfFooterNote = migratedFooter
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

    suspend fun saveCompanySettings(settings: AppCompanySettings) {
        context.messagingDataStore.edit { prefs ->
            prefs[Keys.COMPANY_NAME] = settings.companyName.trim().ifEmpty { "G M S R HASHMANI THAR" }
            prefs[Keys.APPLICATION_NAME] = settings.applicationName.trim().ifEmpty { "G M S R HASHMANI THAR" }
            prefs[Keys.ADMIN_NAME] = settings.adminName.trim().ifEmpty { "G M S R HASHMANI THAR Admin" }
            prefs[Keys.ADMIN_CONTACT] = settings.adminContactNumber.trim().ifEmpty { "03423498631" }
            prefs[Keys.EASYPAISA_NUMBER] = settings.easypaisaNumber.trim().ifEmpty { "03423498631" }
            prefs[Keys.EASYPAISA_HOLDER] = settings.easypaisaAccountHolder.trim().ifEmpty { "G M S R HASHMANI THAR" }
            prefs[Keys.CURRENCY] = settings.currency.trim().ifEmpty { "PKR" }
            prefs[Keys.DEFAULT_CONTRIBUTION] = settings.defaultMonthlyContribution.coerceIn(100L, 10_000_000L)
            prefs[Keys.DEFAULT_SHARES] = settings.defaultTotalShares.coerceIn(2L, 500L)
            prefs[Keys.DEFAULT_START_DAY] = settings.defaultCollectionStartDay.coerceIn(1L, 31L)
            prefs[Keys.DEFAULT_END_DAY] = settings.defaultCollectionEndDay.coerceIn(1L, 31L)
            prefs[Keys.DEFAULT_DURATION_MONTHS] = settings.defaultDurationMonths.coerceIn(2L, 200L)
            prefs[Keys.PDF_FOOTER_NOTE] = settings.pdfFooterNote.trim().ifEmpty {
                "Official G M S R HASHMANI THAR Committee Record • 03423498631"
            }
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
            prefs[Keys.SMS_SENDER_ID] = smsSenderId.trim().ifEmpty { "G M S R HASHMANI THAR" }
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
