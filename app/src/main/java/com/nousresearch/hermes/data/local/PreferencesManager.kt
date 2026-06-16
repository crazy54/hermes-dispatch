package com.nousresearch.hermes.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nousresearch.hermes.data.model.ConnectionConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hermes_prefs")

private object PrefKeys {
    val GATEWAY_URL = stringPreferencesKey("gateway_url")
    val PROFILE_NAME = stringPreferencesKey("profile_name")
}

private const val ENCRYPTED_PREFS_FILE = "hermes_secure_prefs"
private const val KEY_TOKEN = "auth_token"

/**
 * Stores connection config (URL + profile) in DataStore (plain),
 * and the auth token in EncryptedSharedPreferences backed by the Android Keystore.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        ENCRYPTED_PREFS_FILE,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveToken(token: String) {
        securePrefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun loadToken(): String? = securePrefs.getString(KEY_TOKEN, null)

    fun clearToken() {
        securePrefs.edit().remove(KEY_TOKEN).apply()
    }

    val connectionConfigFlow: Flow<ConnectionConfig?> = context.dataStore.data.map { prefs ->
        val url = prefs[PrefKeys.GATEWAY_URL] ?: return@map null
        val token = loadToken() ?: return@map null
        val profile = prefs[PrefKeys.PROFILE_NAME] ?: "default"
        ConnectionConfig(url, token, profile)
    }

    suspend fun saveConnectionConfig(config: ConnectionConfig) {
        saveToken(config.token)
        context.dataStore.edit { prefs ->
            prefs[PrefKeys.GATEWAY_URL] = config.gatewayUrl
            prefs[PrefKeys.PROFILE_NAME] = config.profileName
        }
    }

    suspend fun clearConnectionConfig() {
        clearToken()
        context.dataStore.edit { prefs ->
            prefs.remove(PrefKeys.GATEWAY_URL)
            prefs.remove(PrefKeys.PROFILE_NAME)
        }
    }
}
