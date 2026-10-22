package com.zunepocket.data

import kotlinx.coroutines.flow.StateFlow

/**
 * Source of the music library. The prototype uses [FakeMusicRepository]; phase 2 adds a
 * MediaStore-backed implementation behind this same interface.
 */
interface MusicRepository {
    val library: StateFlow<MusicLibrary>
}
