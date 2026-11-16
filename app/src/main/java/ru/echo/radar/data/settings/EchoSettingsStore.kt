package ru.echo.radar.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import ru.echo.radar.domain.model.ECHO_ZONES
import ru.echo.radar.domain.model.EchoSettings

private val Context.echoDataStore: DataStore<Preferences> by preferencesDataStore(name = "echo_settings")

class EchoSettingsStore(private val context: Context) {

    private object Keys {
        val host = stringPreferencesKey("server_host")
        val token = stringPreferencesKey("echo_token")
        val zone = stringPreferencesKey("zone_name")
        val presence = booleanPreferencesKey("presence_enabled")
    }

    val settings: Flow<EchoSettings> = context.echoDataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            EchoSettings(
                host = prefs[Keys.host].orEmpty(),
                token = prefs[Keys.token].orEmpty(),
                zoneName = prefs[Keys.zone].orEmpty().takeIf { it in ECHO_ZONES } ?: ECHO_ZONES.first(),
                presenceEnabled = prefs[Keys.presence] ?: true,
            )
        }

    suspend fun saveConnection(host: String, token: String) {
        context.echoDataStore.edit { prefs ->
            prefs[Keys.host] = host.trim()
            prefs[Keys.token] = token.trim()
        }
    }

    suspend fun saveZone(zoneName: String) {
        context.echoDataStore.edit { prefs ->
            prefs[Keys.zone] = zoneName
        }
    }

    suspend fun savePresence(enabled: Boolean) {
        context.echoDataStore.edit { prefs ->
            prefs[Keys.presence] = enabled
        }
    }
}
