package com.zunepocket

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.zunepocket.ui.screens.AppRoot

class MainActivity : ComponentActivity() {

    private val app get() = application as ZuneApp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            val keepScreenOn = app.container.settings.keepScreenOn
            LaunchedEffect(keepScreenOn) {
                if (keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
            // Back on the home screen backgrounds the app instead of closing it, so music keeps going.
            AppRoot(app.appState, onExit = { moveTaskToBack(true) })
        }
    }

    /** Every physical key goes through the InputRouter before Compose sees it. */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        app.container.inputRouter.onKeyEvent(event) || super.dispatchKeyEvent(event)

    override fun onPause() {
        super.onPause()
        app.container.inputRouter.cancelRepeat()
    }
}
