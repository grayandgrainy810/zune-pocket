package com.zunepocket.data

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The device's music, from every storage volume MediaStore has indexed (internal storage and SD card).
 * Rescans automatically when audio files are added or removed.
 */
class MediaStoreMusicRepository(
    private val context: Context,
    private val scope: CoroutineScope,
) : MusicRepository {

    private val _library = MutableStateFlow(MusicLibrary.Empty)
    override val library: StateFlow<MusicLibrary> = _library.asStateFlow()

    private var scanJob: Job? = null
    private var observing = false

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            // MediaStore fires many changes while copying files; wait for them to settle.
            scan(delayMs = 2_000)
        }
    }

    /** Starts scanning. Call once the audio permission has been granted. */
    fun start() {
        if (!observing) {
            context.contentResolver.registerContentObserver(AUDIO_URI, true, observer)
            observing = true
        }
        scan()
    }

    private fun scan(delayMs: Long = 0) {
        scanJob?.cancel()
        scanJob = scope.launch {
            delay(delayMs)
            val rows = withContext(Dispatchers.IO) { queryRows() }
            _library.value = withContext(Dispatchers.Default) { MediaStoreMapper.build(rows) }
        }
    }

    private fun queryRows(): List<AudioRow> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.DATA,
        )
        val rows = mutableListOf<AudioRow>()
        try {
            context.contentResolver.query(
                AUDIO_URI,
                projection,
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null,
                null,
            )?.use { c ->
                val id = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val title = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val displayName = c.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val artist = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumArtist = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
                val album = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumId = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val track = c.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val duration = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val year = c.getColumnIndex(MediaStore.Audio.Media.YEAR)
                val genre = c.getColumnIndex(MediaStore.Audio.Media.GENRE)
                val data = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                while (c.moveToNext()) {
                    val songId = c.getLong(id)
                    rows += AudioRow(
                        id = songId,
                        uri = ContentUris.withAppendedId(AUDIO_URI, songId).toString(),
                        title = c.stringOrNull(title),
                        displayName = c.stringOrNull(displayName),
                        artist = c.stringOrNull(artist),
                        albumArtist = c.stringOrNull(albumArtist),
                        album = c.stringOrNull(album),
                        albumId = if (albumId >= 0) c.getLong(albumId) else 0L,
                        track = if (track >= 0) c.getInt(track) else 0,
                        durationMs = if (duration >= 0) c.getLong(duration) else 0L,
                        year = if (year >= 0) c.getInt(year) else 0,
                        genre = c.stringOrNull(genre),
                        path = c.stringOrNull(data),
                    )
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "No permission to read audio", e)
        }
        return rows
    }

    private fun Cursor.stringOrNull(index: Int): String? = if (index >= 0 && !isNull(index)) getString(index) else null

    private companion object {
        const val TAG = "MediaStoreMusic"
        val AUDIO_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }
}
