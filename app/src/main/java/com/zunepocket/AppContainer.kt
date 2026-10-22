package com.zunepocket

import android.content.Context
import com.zunepocket.data.FakeMusicRepository
import com.zunepocket.data.MusicRepository
import com.zunepocket.input.InputRouter
import com.zunepocket.input.KeyBindings
import com.zunepocket.player.FakePlayerController
import com.zunepocket.player.PlayerController
import com.zunepocket.settings.AppSettings

/** Hand-wired dependencies. Swap the fakes here for the MediaStore/Media3 implementations in phase 2. */
class AppContainer(context: Context) {
    val settings = AppSettings(context)
    val repository: MusicRepository = FakeMusicRepository()
    val player: PlayerController = FakePlayerController()
    val keyBindings = KeyBindings { settings.swapConfirmBack }
    val inputRouter = InputRouter(keyBindings)
}
