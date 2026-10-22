package com.zunepocket

import android.app.Application
import com.zunepocket.ui.AppState

class ZuneApp : Application() {
    lateinit var container: AppContainer
        private set

    /** Held by the application so the back stack survives activity recreation. */
    val appState: AppState by lazy { AppState(container) }

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
