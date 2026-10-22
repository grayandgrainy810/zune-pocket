package com.zunepocket.data

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.media.MediaScannerConnection
import android.database.Cursor
import android.os.Handler
import android.os.Looper
import android.os.storage.StorageManager
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

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
    private var rescanJob: Job? = null
    private var observing = false

    /** Paths MediaStore already knows, so a manual rescan only submits genuinely new files. */
    @Volatile
    private var knownPaths: Set<String> = emptySet()

    private val _rescanStatus = MutableStateFlow<String?>(null)

    /** Progress or result of the last manual rescan, for display in Settings. */
    val rescanStatus: StateFlow<String?> = _rescanStatus.asStateFlow()

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
            reload()
        }
    }

    private suspend fun reload() {
        val rows = withContext(Dispatchers.IO) { queryRows() }
        knownPaths = rows.mapNotNullTo(HashSet()) { it.path }
        _library.value = withContext(Dispatchers.Default) { MediaStoreMapper.build(rows) }
    }

    /**
     * Manual rescan: finds audio files on every storage volume that MediaStore hasn't indexed yet
     * (e.g. just copied over USB or on a newly inserted SD card), asks the system to index them,
     * then reloads the library.
     */
    fun rescan() {
        if (rescanJob?.isActive == true) return
        rescanJob = scope.launch {
            _rescanStatus.value = "looking for new music…"
            val before = _library.value.songs.size
            val newFiles = withContext(Dispatchers.IO) { findAudioFiles().filterNot { it in knownPaths } }
            if (newFiles.isNotEmpty()) {
                _rescanStatus.value = "adding ${newFiles.size} new files…"
                withTimeoutOrNull(SCAN_TIMEOUT_MS) { indexFiles(newFiles) }
            }
            scanJob?.cancel()
            reload()
            val change = _library.value.songs.size - before
            _rescanStatus.value = when {
                change > 0 -> "found $change new ${if (change == 1) "song" else "songs"}"
                change < 0 -> "${-change} songs no longer on the device"
                else -> "no new music found"
            }
        }
    }

    private fun findAudioFiles(): List<String> {
        val storage = context.getSystemService(StorageManager::class.java) ?: return emptyList()
        val roots = storage.storageVolumes.mapNotNull { it.directory }
        return roots.flatMap { root ->
            runCatching {
                root.walkTopDown()
                    // Skip hidden folders and Android/ (app data, unreadable and never music).
                    .onEnter { dir -> !dir.name.startsWith(".") && !(dir.parentFile == root && dir.name == "Android") }
                    .filter { it.isFile && it.extension.lowercase() in AUDIO_EXTENSIONS }
                    .map { it.path }
                    .toList()
            }.getOrDefault(emptyList())
        }
    }

    private suspend fun indexFiles(paths: List<String>) = suspendCancellableCoroutine { cont ->
        val remaining = AtomicInteger(paths.size)
        MediaScannerConnection.scanFile(context, paths.toTypedArray(), null) { _, _ ->
            if (remaining.decrementAndGet() == 0 && cont.isActive) cont.resume(Unit)
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
        const val SCAN_TIMEOUT_MS = 120_000L
        val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "flac", "ogg", "oga", "opus", "wav", "alac", "mp4", "3gp", "amr", "mka")
        val AUDIO_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    }
}
