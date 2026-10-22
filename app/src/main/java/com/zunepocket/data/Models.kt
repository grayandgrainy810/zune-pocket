package com.zunepocket.data

data class Artist(val id: Long, val name: String)

/** Where to look for an album's real artwork. Null for sample music, which uses generated art. */
data class ArtSource(val albumId: Long, val firstTrackUri: String, val folder: String?)

/** [artSeed] drives the generated placeholder art shown until (or instead of) real artwork. */
data class Album(
    val id: Long,
    val title: String,
    val artistId: Long,
    val artistName: String,
    val year: Int,
    val genre: String,
    val artSeed: Long,
    val art: ArtSource? = null,
)

data class Song(
    val id: Long,
    val title: String,
    val albumId: Long,
    val albumTitle: String,
    val artistId: Long,
    val artistName: String,
    val track: Int,
    val durationMs: Long,
    val genre: String,
    val artSeed: Long,
    val disc: Int = 0,
    /** Content URI to play; empty for sample music. */
    val uri: String = "",
    val art: ArtSource? = null,
)

data class Genre(val name: String, val songCount: Int)

data class Playlist(val id: Long, val name: String, val songIds: List<Long>)
