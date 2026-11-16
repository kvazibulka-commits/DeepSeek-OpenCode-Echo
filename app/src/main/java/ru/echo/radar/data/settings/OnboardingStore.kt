package ru.echo.radar.data.settings

import android.content.Context

/** Первый вход: показываем знакомство один раз, пока пользователь не пройдёт его. */
class OnboardingStore(context: Context) {

    private val prefs = context.getSharedPreferences("echo_onboarding", Context.MODE_PRIVATE)

    fun isDone(): Boolean = prefs.getBoolean(KEY_DONE, false)

    fun markDone() {
        prefs.edit().putBoolean(KEY_DONE, true).apply()
    }

    fun reset() {
        prefs.edit().putBoolean(KEY_DONE, false).apply()
    }

    private companion object {
        const val KEY_DONE = "done"
    }
}
