package com.zunepocket

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.zunepocket.ui.screens.AppRoot

class MainActivity : ComponentActivity() {

    private val app get() = application as ZuneApp

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        app.container.refreshPermission()
        // If still not granted the user declined; Android may not show the dialog again.
        app.appState.permissionDenied = !app.container.hasAudioPermission
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        app.appState.requestMusicPermission = {
            // Images too, for cover.jpg files next to the music.
            permissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_IMAGES))
        }
        app.appState.openAppSettings = {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
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

    override fun onResume() {
        super.onResume()
        app.container.refreshPermission()
    }

    override fun onPause() {
        super.onPause()
        app.container.inputRouter.cancelRepeat()
    }
}
