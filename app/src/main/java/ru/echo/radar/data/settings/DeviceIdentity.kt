package ru.echo.radar.data.settings

import android.content.Context
import java.util.UUID

/**
 * Стабильный идентификатор устройства. Хранится отдельно от настроек,
 * чтобы первый же запрос к серверу был уже подписан правильным deviceId.
 */
object DeviceIdentity {

    private const val PREFS = "echo_identity"
    private const val KEY_DEVICE_ID = "device_id"

    fun getOrCreate(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val created = "android_" + UUID.randomUUID().toString().replace("-", "").take(12)
        prefs.edit().putString(KEY_DEVICE_ID, created).apply()
        return created
    }
}
