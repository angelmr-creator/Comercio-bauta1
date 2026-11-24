package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.PaymentConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.bautaDataStore: DataStore<Preferences> by preferencesDataStore(name = "comercio_bauta_prefs")

class LocalPreferencesStore(private val context: Context) {

    private object Keys {
        val BANK_NAME = stringPreferencesKey("payment_bank_name")
        val BANK_ACCOUNT = stringPreferencesKey("payment_bank_account")
        val ACCOUNT_HOLDER = stringPreferencesKey("payment_account_holder")
        val QR_PAYMENT_URL = stringPreferencesKey("payment_qr_url")
        val PAYMENT_UPDATED_AT = longPreferencesKey("payment_updated_at")
        val CLOUD_WORKSPACE_ID = stringPreferencesKey("cloud_workspace_id")
    }

    val paymentConfigFlow: Flow<PaymentConfig> = context.bautaDataStore.data.map { prefs ->
        PaymentConfig(
            id = "payment",
            bankName = prefs[Keys.BANK_NAME] ?: "",
            bankAccount = prefs[Keys.BANK_ACCOUNT] ?: PaymentConfig.DEFAULT_BAUTA_BANK_ACCOUNT,
            accountHolder = prefs[Keys.ACCOUNT_HOLDER] ?: "Empresa Municipal de Comercio Bauta",
            qrPaymentUrl = prefs[Keys.QR_PAYMENT_URL] ?: "",
            updatedAtMillis = prefs[Keys.PAYMENT_UPDATED_AT] ?: 0L
        )
    }

    val cloudWorkspaceIdFlow: Flow<String?> = context.bautaDataStore.data.map { prefs ->
        prefs[Keys.CLOUD_WORKSPACE_ID]?.takeIf { it.isNotBlank() }
    }

    suspend fun savePaymentConfig(
        bankName: String,
        bankAccount: String,
        accountHolder: String,
        qrPaymentUrl: String
    ) {
        context.bautaDataStore.edit { prefs ->
            prefs[Keys.BANK_NAME] = bankName.trim().take(80)
            prefs[Keys.BANK_ACCOUNT] = bankAccount.trim().take(80)
            prefs[Keys.ACCOUNT_HOLDER] = accountHolder.trim().take(120)
            prefs[Keys.QR_PAYMENT_URL] = qrPaymentUrl.trim().take(300)
            prefs[Keys.PAYMENT_UPDATED_AT] = System.currentTimeMillis()
        }
    }

    suspend fun saveCloudWorkspaceId(workspaceId: String) {
        context.bautaDataStore.edit { prefs ->
            prefs[Keys.CLOUD_WORKSPACE_ID] = workspaceId.trim().take(128)
        }
    }

    suspend fun clearCloudWorkspaceId() {
        context.bautaDataStore.edit { prefs ->
            prefs.remove(Keys.CLOUD_WORKSPACE_ID)
        }
    }
}
