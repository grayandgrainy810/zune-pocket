package com.zunepocket.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaStoreMapperTest {

    private fun row(
        id: Long,
        title: String? = "Song $id",
        artist: String? = "Artist",
        albumArtist: String? = null,
        album: String? = "Album",
        albumId: Long = 1,
        track: Int = 0,
        genre: String? = null,
        displayName: String? = "file$id.mp3",
        path: String? = "/storage/emulated/0/Music/Album/file$id.mp3",
        year: Int = 0,
    ) = AudioRow(id, "content://media/external/audio/media/$id", title, displayName, artist, albumArtist, album, albumId, track, 180_000, year, genre, path)

    @Test
    fun `track numbers are split into disc and track`() {
        val library = MediaStoreMapper.build(listOf(row(1, track = 2003), row(2, track = 1012), row(3, track = 7)))
        val songs = library.songs.associateBy { it.id }
        assertEquals(2 to 3, songs.getValue(1).disc to songs.getValue(1).track)
        assertEquals(1 to 12, songs.getValue(2).disc to songs.getValue(2).track)
        assertEquals(0 to 7, songs.getValue(3).disc to songs.getValue(3).track)
        assertEquals(listOf(3L, 2L, 1L), library.songsOn(1).map { it.id })
    }

    @Test
    fun `album artist tag keeps a compilation together`() {
        val library = MediaStoreMapper.build(
            listOf(
                row(1, artist = "Band A", albumArtist = "DJ Mix"),
                row(2, artist = "Band B", albumArtist = "DJ Mix"),
            ),
        )
        assertEquals(listOf("DJ Mix"), library.artists.map { it.name })
        assertEquals(1, library.albums.size)
        assertEquals("Band A", library.songs.first { it.id == 1L }.artistName)
        assertEquals(library.artists.single().id, library.songs.first().artistId)
    }

    @Test
    fun `untagged compilations become various artists`() {
        val library = MediaStoreMapper.build(listOf(row(1, artist = "A"), row(2, artist = "B"), row(3, artist = "C")))
        assertEquals("Various Artists", library.albums.single().artistName)
    }

    @Test
    fun `missing tags fall back sensibly`() {
        val library = MediaStoreMapper.build(listOf(row(1, title = " ", artist = "<unknown>", album = null, displayName = "My Recording.flac")))
        val song = library.songs.single()
        assertEquals("My Recording", song.title)
        assertEquals("Unknown Artist", song.artistName)
        assertEquals("Unknown Album", song.albumTitle)
        assertTrue(library.genres.isEmpty())
    }

    @Test
    fun `art source points at the album folder`() {
        val album = MediaStoreMapper.build(listOf(row(5, albumId = 42))).albums.single()
        assertEquals(42L, album.art?.albumId)
        assertEquals("/storage/emulated/0/Music/Album", album.art?.folder)
    }

    @Test
    fun `genres are counted and lists are sorted`() {
        val library = MediaStoreMapper.build(
            listOf(
                row(1, title = "Zebra", genre = "Rock", albumId = 1, album = "B Side"),
                row(2, title = "Apple", genre = "Rock", albumId = 2, album = "The A Side", albumArtist = "Other"),
                row(3, title = "Mango", genre = "Jazz", albumId = 2, album = "The A Side", albumArtist = "Other"),
            ),
        )
        assertEquals(listOf("Apple", "Mango", "Zebra"), library.songs.map { it.title })
        assertEquals(listOf("The A Side", "B Side"), library.albums.map { it.title })
        assertEquals(listOf(Genre("Jazz", 1), Genre("Rock", 2)), library.genres)
    }
}
