package com.zunepocket.ui.components

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.util.LruCache
import android.util.Size
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.zunepocket.data.ArtSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections

val LocalArtLoader = staticCompositionLocalOf<ArtLoader?> { null }

/**
 * Finds and caches real album artwork. Tries, in order: MediaStore's album thumbnail, art embedded in
 * the album's first track, then cover.jpg / folder.jpg / front.jpg next to the files.
 */
class ArtLoader(private val context: Context) {

    private val cache = object : LruCache<String, ImageBitmap>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }
    private val missing: MutableSet<Long> = Collections.synchronizedSet(HashSet())

    fun cached(source: ArtSource, large: Boolean): ImageBitmap? = cache.get(key(source, large))

    suspend fun load(source: ArtSource, large: Boolean): ImageBitmap? {
        cached(source, large)?.let { return it }
        if (source.albumId in missing) return null
        return withContext(Dispatchers.IO) {
            val target = if (large) LARGE_PX else SMALL_PX
            val bitmap = fromMediaStore(source, target) ?: fromEmbedded(source, target) ?: fromFolder(source, target)
            if (bitmap == null) {
                missing += source.albumId
                null
            } else {
                bitmap.asImageBitmap().also { cache.put(key(source, large), it) }
            }
        }
    }

    private fun fromMediaStore(source: ArtSource, target: Int): Bitmap? = runCatching {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, source.albumId)
        context.contentResolver.loadThumbnail(uri, Size(target, target), null)
    }.getOrNull()

    private fun fromEmbedded(source: ArtSource, target: Int): Bitmap? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(source.firstTrackUri))
            retriever.embeddedPicture?.let { bytes ->
                decodeSampled(target) { opts -> BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) }
            }
        } finally {
            retriever.release()
        }
    }.getOrNull()

    private fun fromFolder(source: ArtSource, target: Int): Bitmap? = runCatching {
        val folder = source.folder ?: return null
        val file = COVER_NAMES.asSequence().map { File(folder, it) }.firstOrNull { it.canRead() } ?: return null
        decodeSampled(target) { opts -> BitmapFactory.decodeFile(file.path, opts) }
    }.getOrNull()

    /** Decodes at the smallest power-of-two downsample that still covers [target] pixels. */
    private inline fun decodeSampled(target: Int, decode: (BitmapFactory.Options) -> Bitmap?): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decode(bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= target && bounds.outHeight / (sample * 2) >= target) sample *= 2
        return decode(BitmapFactory.Options().apply { inSampleSize = sample })
    }

    private fun key(source: ArtSource, large: Boolean) = "${source.albumId}:${if (large) "L" else "S"}"

    private companion object {
        const val CACHE_BYTES = 64 * 1024 * 1024
        const val SMALL_PX = 256
        const val LARGE_PX = 1080
        val COVER_NAMES = listOf("cover.jpg", "folder.jpg", "front.jpg", "cover.png", "folder.png", "Cover.jpg", "Folder.jpg", "AlbumArt.jpg")
    }
}
