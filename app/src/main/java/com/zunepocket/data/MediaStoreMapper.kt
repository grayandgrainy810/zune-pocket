package com.zunepocket.data

/** One audio file as read from MediaStore, before cleanup. */
data class AudioRow(
    val id: Long,
    val uri: String,
    val title: String?,
    val displayName: String?,
    val artist: String?,
    val albumArtist: String?,
    val album: String?,
    val albumId: Long,
    val track: Int,
    val durationMs: Long,
    val year: Int,
    val genre: String?,
    val path: String?,
)

/**
 * Turns raw MediaStore rows into a [MusicLibrary]. Pure, so tag-cleanup rules are unit-testable.
 *
 * Albums are filed under their album artist (falling back to the most common track artist, or
 * "Various Artists" for untagged compilations), so compilations don't split into one album per artist.
 */
object MediaStoreMapper {
    const val UNKNOWN_ARTIST = "Unknown Artist"
    const val UNKNOWN_ALBUM = "Unknown Album"
    const val VARIOUS_ARTISTS = "Various Artists"

    fun build(rows: List<AudioRow>): MusicLibrary {
        val rowsByAlbum = rows.groupBy { it.albumId }

        val albumArtist = rowsByAlbum.mapValues { (_, albumRows) -> albumArtistOf(albumRows) }
        val artists = albumArtist.values
            .distinctBy { it.lowercase() }
            .sortedBy { sortKey(it) }
            .mapIndexed { i, name -> Artist(i + 1L, name) }
        val artistIdByKey = artists.associate { it.name.lowercase() to it.id }

        val albums = rowsByAlbum.map { (albumId, albumRows) ->
            val artistName = albumArtist.getValue(albumId)
            Album(
                id = albumId,
                title = albumRows.firstNotNullOfOrNull { it.album.clean() } ?: UNKNOWN_ALBUM,
                artistId = artistIdByKey.getValue(artistName.lowercase()),
                artistName = artistName,
                year = albumRows.maxOf { it.year },
                genre = albumRows.mapNotNull { it.genre.clean() }.mostCommon() ?: "",
                artSeed = albumId,
                art = ArtSource(albumId, albumRows.first().uri, albumRows.firstNotNullOfOrNull { folderOf(it.path) }),
            )
        }
        val albumsById = albums.associateBy { it.id }

        val songs = rows.map { row ->
            val album = albumsById.getValue(row.albumId)
            Song(
                id = row.id,
                title = titleOf(row),
                albumId = album.id,
                albumTitle = album.title,
                artistId = album.artistId,
                artistName = row.artist.cleanArtist() ?: album.artistName,
                track = row.track % 1000,
                disc = row.track / 1000,
                durationMs = row.durationMs,
                genre = row.genre.clean() ?: "",
                artSeed = album.artSeed,
                uri = row.uri,
                art = album.art,
            )
        }

        val genres = songs.filter { it.genre.isNotEmpty() }
            .groupingBy { it.genre }.eachCount()
            .map { (name, count) -> Genre(name, count) }
            .sortedBy { sortKey(it.name) }

        return MusicLibrary(
            artists = artists,
            albums = albums.sortedBy { sortKey(it.title) },
            songs = songs.sortedBy { sortKey(it.title) },
            genres = genres,
            playlists = emptyList(),
        )
    }

    fun titleOf(row: AudioRow): String =
        row.title.clean() ?: row.displayName?.substringBeforeLast('.').clean() ?: "Untitled"

    private fun albumArtistOf(rows: List<AudioRow>): String {
        rows.mapNotNull { it.albumArtist.cleanArtist() }.mostCommon()?.let { return it }
        val trackArtists = rows.mapNotNull { it.artist.cleanArtist() }
        if (trackArtists.distinctBy { it.lowercase() }.size > 2) return VARIOUS_ARTISTS
        return trackArtists.mostCommon() ?: UNKNOWN_ARTIST
    }

    private fun folderOf(path: String?): String? = path?.substringBeforeLast('/', "").clean()

    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    /** MediaStore reports missing artist tags as "<unknown>". */
    private fun String?.cleanArtist(): String? = clean()?.takeUnless { it == "<unknown>" }

    private fun List<String>.mostCommon(): String? = groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
}
