package com.zunepocket.data

data class Artist(val id: Long, val name: String)

/** [artSeed] drives generated placeholder art; real artwork arrives with the MediaStore repository. */
data class Album(
    val id: Long,
    val title: String,
    val artistId: Long,
    val artistName: String,
    val year: Int,
    val genre: String,
    val artSeed: Long,
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
)

data class Genre(val name: String, val songCount: Int)

data class Playlist(val id: Long, val name: String, val songIds: List<Long>)
