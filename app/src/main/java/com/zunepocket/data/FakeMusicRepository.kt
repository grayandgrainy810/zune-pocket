package com.zunepocket.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

/** Deterministic mock library (~40 artists, ~120 albums, ~1,400 songs) for UI work. All names are invented. */
class FakeMusicRepository(seed: Long = 2006L) : MusicRepository {
    override val library: StateFlow<MusicLibrary> = MutableStateFlow(generate(seed))

    companion object {
        fun generate(seed: Long): MusicLibrary {
            val rnd = Random(seed)
            val artists = ARTIST_NAMES.mapIndexed { i, name -> Artist(i + 1L, name) }
            val artistGenre = artists.associate { it.id to GENRES[rnd.nextInt(GENRES.size)] }

            val albums = mutableListOf<Album>()
            val songs = mutableListOf<Song>()
            val usedAlbumTitles = mutableSetOf<String>()
            var albumId = 1L
            var songId = 1L
            for (artist in artists) {
                repeat(rnd.nextInt(2, 5)) {
                    var title: String
                    do title = albumTitle(rnd) while (!usedAlbumTitles.add(title))
                    val album = Album(
                        id = albumId++,
                        title = title,
                        artistId = artist.id,
                        artistName = artist.name,
                        year = rnd.nextInt(1979, 2025),
                        genre = artistGenre.getValue(artist.id),
                        artSeed = rnd.nextLong(),
                    )
                    albums += album
                    for (track in 1..rnd.nextInt(8, 15)) {
                        songs += Song(
                            id = songId++,
                            title = songTitle(rnd),
                            albumId = album.id,
                            albumTitle = album.title,
                            artistId = artist.id,
                            artistName = artist.name,
                            track = track,
                            durationMs = rnd.nextLong(130_000, 390_000),
                            genre = album.genre,
                            artSeed = album.artSeed,
                        )
                    }
                }
            }

            val genres = songs.groupingBy { it.genre }.eachCount()
                .map { (name, count) -> Genre(name, count) }
                .sortedBy { sortKey(it.name) }

            val playlists = PLAYLISTS.mapIndexed { i, (name, size) ->
                Playlist(i + 1L, name, songs.shuffled(rnd).take(size).map { it.id })
            }

            return MusicLibrary(
                artists = artists.sortedBy { sortKey(it.name) },
                albums = albums.sortedBy { sortKey(it.title) },
                songs = songs.sortedBy { sortKey(it.title) },
                genres = genres,
                playlists = playlists,
            )
        }

        private fun albumTitle(rnd: Random): String = when (rnd.nextInt(4)) {
            0 -> "${ADJECTIVES.pick(rnd)} ${NOUNS.pick(rnd)}"
            1 -> "${NOUNS.pick(rnd)} ${NOUNS.pick(rnd)}"
            2 -> "The ${ADJECTIVES.pick(rnd)} ${NOUNS.pick(rnd)}"
            else -> "${NOUNS.pick(rnd)} of ${NOUNS.pick(rnd)}"
        }

        private fun songTitle(rnd: Random): String = when (rnd.nextInt(6)) {
            0 -> NOUNS.pick(rnd)
            1 -> "${ADJECTIVES.pick(rnd)} ${NOUNS.pick(rnd)}"
            2 -> "${VERBS.pick(rnd)} the ${NOUNS.pick(rnd)}"
            3 -> "${NOUNS.pick(rnd)} of ${NOUNS.pick(rnd)}"
            4 -> "${VERBS.pick(rnd)} ${ADVERBS.pick(rnd)}"
            else -> "${rnd.nextInt(2, 100)} ${NOUNS.pick(rnd)}s"
        }

        private fun List<String>.pick(rnd: Random) = this[rnd.nextInt(size)]

        private val ARTIST_NAMES = listOf(
            "Arcade Lights", "The Velvet Static", "Neon Harbor", "Glass Pilots", "Midnight Cartography",
            "Saturn Youth", "The Paper Kites Club", "Low Orbit", "Copper Tides", "Echo Valley Drive",
            "Silver Thread", "Hollow Pines", "Kite Theory", "The Northern Exits", "Amber Signal",
            "Quiet Machines", "Polar Radio", "Fable Coast", "Static Bloom", "The Wild Frequencies",
            "Lantern Days", "Moth & Mirror", "Cobalt Avenue", "Ivory Circuit", "Yesterday's Satellites",
            "Juniper Run", "The Slow Comets", "Electric Meadow", "Paper Planets", "Ghost Atlas",
            "Rooftop Choir", "Orchid Engine", "Vapor Lanes", "Winter Arcade", "Xylo Park",
            "Zenith Club", "Brass Lagoon", "Hazel & the Hours", "Underpass", "4am Collective",
        )
        private val GENRES = listOf(
            "Electronic", "Rock", "Indie", "Synthwave", "Jazz", "Hip-Hop", "Ambient", "Pop", "Folk", "Soul",
        )
        private val ADJECTIVES = listOf(
            "Golden", "Silent", "Electric", "Broken", "Endless", "Neon", "Hidden", "Wild", "Paper", "Velvet",
            "Crystal", "Midnight", "Northern", "Distant", "Burning", "Frozen", "Little", "Lucky", "Restless",
            "Violet", "Hollow", "Bright", "Lonely", "Secret", "Analog", "Faded", "Second", "Last", "Open",
        )
        private val NOUNS = listOf(
            "Satellite", "Ocean", "Highway", "Signal", "Garden", "Mirror", "Summer", "Static", "River",
            "Engine", "Lantern", "Echo", "Horizon", "Machine", "Window", "Island", "Thunder", "Feather",
            "Cassette", "Skyline", "Harbor", "Comet", "Carousel", "Desert", "Postcard", "Rocket", "Dream",
            "Avenue", "Season", "Wire", "Orbit", "Daylight", "Weekend", "Telephone", "Lightning", "Atlas",
        )
        private val VERBS = listOf(
            "Chasing", "Calling", "Running", "Holding", "Waiting", "Falling", "Dancing", "Breaking",
            "Finding", "Leaving", "Burning", "Drifting", "Counting", "Wake", "Follow", "Carry",
        )
        private val ADVERBS = listOf("Slowly", "Home", "Away", "Tonight", "Again", "Forever", "Under", "Out Loud")
        private val PLAYLISTS = listOf(
            "Zune Favorites" to 25,
            "Late Night Drive" to 18,
            "Workout" to 30,
            "Rainy Day" to 20,
            "On the Go" to 15,
        )
    }
}
