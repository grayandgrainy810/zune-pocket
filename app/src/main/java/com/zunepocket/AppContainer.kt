package com.zunepocket

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import com.zunepocket.data.FakeMusicRepository
import com.zunepocket.data.MediaStoreMusicRepository
import com.zunepocket.data.MusicRepository
import com.zunepocket.input.InputRouter
import com.zunepocket.input.KeyBindings
import com.zunepocket.player.FakePlayerController
import com.zunepocket.player.Media3PlayerController
import com.zunepocket.player.PlayerController
import com.zunepocket.settings.AppSettings
import com.zunepocket.ui.components.ArtLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Hand-wired dependencies. The library and player come from the device by default, or from the
 * sample set when Settings → music source says so; switching takes effect immediately.
 */
class AppContainer(private val context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val settings = AppSettings(context)
    val artLoader = ArtLoader(context)
    val keyBindings = KeyBindings { settings.swapConfirmBack }
    val inputRouter = InputRouter(keyBindings)

    var hasAudioPermission by mutableStateOf(checkAudioPermission())
        private set

    private val sampleRepository by lazy { FakeMusicRepository() }
    private val samplePlayerLazy = lazy { FakePlayerController(scope) }
    private val deviceRepository by lazy { MediaStoreMusicRepository(context, scope) }
    private val devicePlayerLazy = lazy {
        Media3PlayerController(context, scope, context.getSharedPreferences("playback", Context.MODE_PRIVATE))
    }

    val repository: MusicRepository
        get() = if (settings.useSampleLibrary) sampleRepository else deviceRepository

    val player: PlayerController
        get() = if (settings.useSampleLibrary) samplePlayerLazy.value else devicePlayerLazy.value

    init {
        scope.launch {
            snapshotFlow { settings.useSampleLibrary to hasAudioPermission }.collect { (sample, permitted) ->
                // Only one source plays at a time.
                if (sample && devicePlayerLazy.isInitialized()) devicePlayerLazy.value.pause()
                if (!sample && samplePlayerLazy.isInitialized()) samplePlayerLazy.value.pause()
                if (!sample && permitted) startDeviceLibrary()
            }
        }
    }

    private var deviceStarted = false

    private fun startDeviceLibrary() {
        if (deviceStarted) return
        deviceStarted = true
        deviceRepository.start()
        scope.launch {
            val library = deviceRepository.library.first { it.songs.isNotEmpty() }
            devicePlayerLazy.value.restore(library)
        }
    }

    /** Progress/result of the last manual rescan (device music only). */
    val rescanStatus: StateFlow<String?> get() = deviceRepository.rescanStatus

    fun rescanMusic() {
        if (settings.useSampleLibrary || !hasAudioPermission) return
        artLoader.forgetMissing()
        startDeviceLibrary()
        deviceRepository.rescan()
    }

    /** Re-checks the permission, e.g. after the system dialog or returning from system settings. */
    fun refreshPermission() {
        hasAudioPermission = checkAudioPermission()
    }

    private fun checkAudioPermission() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
}
