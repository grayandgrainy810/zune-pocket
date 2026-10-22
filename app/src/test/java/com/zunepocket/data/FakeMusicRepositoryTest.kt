package com.zunepocket.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeMusicRepositoryTest {
    private val library = FakeMusicRepository.generate(seed = 2006L)

    @Test
    fun `generates a library large enough to exercise scrolling`() {
        assertEquals(40, library.artists.size)
        assertTrue(library.albums.size >= 80)
        assertTrue(library.songs.size >= 800)
        assertTrue(library.playlists.isNotEmpty())
    }

    @Test
    fun `generation is deterministic`() {
        val again = FakeMusicRepository.generate(seed = 2006L)
        assertEquals(library.songs.map { it.title }, again.songs.map { it.title })
    }

    @Test
    fun `lists are sorted ignoring a leading The`() {
        val keys = library.artists.map { sortKey(it.name) }
        assertEquals(keys.sorted(), keys)
    }

    @Test
    fun `every album has tracks in order`() {
        for (album in library.albums) {
            val tracks = library.songsOn(album.id).map { it.track }
            assertTrue(tracks.isNotEmpty())
            assertEquals((1..tracks.size).toList(), tracks)
        }
    }

    @Test
    fun `jump letters file non-letters under hash`() {
        assertEquals('#', jumpLetter("4am Collective"))
        assertEquals('V', jumpLetter("The Velvet Static"))
        assertEquals('A', jumpLetter("arcade"))
    }
}
