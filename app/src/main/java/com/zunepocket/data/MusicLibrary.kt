package com.zunepocket.data

/** An immutable snapshot of the user's library. Lists are pre-sorted for display. */
class MusicLibrary(
    val artists: List<Artist>,
    val albums: List<Album>,
    val songs: List<Song>,
    val genres: List<Genre>,
    val playlists: List<Playlist>,
) {
    private val artistsById by lazy { artists.associateBy { it.id } }
    private val albumsById by lazy { albums.associateBy { it.id } }
    private val songsById by lazy { songs.associateBy { it.id } }
    private val songsByAlbum by lazy {
        songs.groupBy { it.albumId }.mapValues { (_, list) -> list.sortedWith(compareBy({ it.disc }, { it.track })) }
    }
    private val albumsByArtist by lazy {
        albums.groupBy { it.artistId }.mapValues { (_, list) -> list.sortedBy { it.year } }
    }

    fun artist(id: Long): Artist? = artistsById[id]
    fun album(id: Long): Album? = albumsById[id]
    fun song(id: Long): Song? = songsById[id]

    fun albumsBy(artistId: Long): List<Album> = albumsByArtist[artistId].orEmpty()
    fun songsOn(albumId: Long): List<Song> = songsByAlbum[albumId].orEmpty()
    fun songsBy(artistId: Long): List<Song> = albumsBy(artistId).flatMap { songsOn(it.id) }
    fun songsIn(genre: String): List<Song> = songs.filter { it.genre == genre }
    fun songsIn(playlist: Playlist): List<Song> = playlist.songIds.mapNotNull { songsById[it] }

    companion object {
        val Empty = MusicLibrary(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
    }
}
