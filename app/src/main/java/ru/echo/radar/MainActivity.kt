package ru.echo.radar

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ru.echo.radar.di.AppContainer
import ru.echo.radar.ui.navigation.EchoApp
import ru.echo.radar.ui.theme.EchoTheme

class MainActivity : ComponentActivity() {

    private val container: AppContainer
        get() = (application as EchoApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        handleShareIntent(intent)

        setContent {
            EchoTheme {
                EchoApp(
                    repository = container.repository,
                    settingsStore = container.settings,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    /** «Поделиться → ЭХО»: принимаем текст «Исполнитель — Трек». */
    private fun handleShareIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action != Intent.ACTION_SEND) return
        if (!intent.type.orEmpty().startsWith("text/")) return

        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        container.repository.publishManual(text)
        intent.action = null
    }
}
