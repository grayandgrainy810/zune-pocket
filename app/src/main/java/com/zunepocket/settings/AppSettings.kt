package com.zunepocket.settings

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zunepocket.ui.theme.Accent

/** How the library's albums tab is laid out. [columns] is 1 for the list. */
enum class AlbumView(val label: String, val columns: Int) {
    List("list", 1),
    LargeTiles("large tiles", 2),
    SmallTiles("small tiles", 3),
}

/** User preferences, persisted to SharedPreferences and observable from Compose. */
class AppSettings(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private var _accent by mutableStateOf(Accent.entries.getOrElse(prefs.getInt(KEY_ACCENT, 0)) { Accent.Magenta })
    var accent: Accent
        get() = _accent
        set(value) {
            _accent = value
            prefs.edit().putInt(KEY_ACCENT, value.ordinal).apply()
        }

    /** When true, B confirms and A goes back (matches Retroid's swapped-layout setting). */
    private var _swapConfirmBack by mutableStateOf(prefs.getBoolean(KEY_SWAP, false))
    var swapConfirmBack: Boolean
        get() = _swapConfirmBack
        set(value) {
            _swapConfirmBack = value
            prefs.edit().putBoolean(KEY_SWAP, value).apply()
        }

    private var _keepScreenOn by mutableStateOf(prefs.getBoolean(KEY_SCREEN_ON, false))
    var keepScreenOn: Boolean
        get() = _keepScreenOn
        set(value) {
            _keepScreenOn = value
            prefs.edit().putBoolean(KEY_SCREEN_ON, value).apply()
        }

    /** Sample music and a silent player, for trying the UI without a music library. */
    private var _useSampleLibrary by mutableStateOf(prefs.getBoolean(KEY_SAMPLE, false))
    var useSampleLibrary: Boolean
        get() = _useSampleLibrary
        set(value) {
            _useSampleLibrary = value
            prefs.edit().putBoolean(KEY_SAMPLE, value).apply()
        }

    private var _albumView by mutableStateOf(AlbumView.entries.getOrElse(prefs.getInt(KEY_ALBUM_VIEW, 0)) { AlbumView.List })
    var albumView: AlbumView
        get() = _albumView
        set(value) {
            _albumView = value
            prefs.edit().putInt(KEY_ALBUM_VIEW, value.ordinal).apply()
        }

    private companion object {
        const val KEY_ACCENT = "accent"
        const val KEY_SWAP = "swap_confirm_back"
        const val KEY_SCREEN_ON = "keep_screen_on"
        const val KEY_SAMPLE = "use_sample_library"
        const val KEY_ALBUM_VIEW = "album_view"
    }
}
