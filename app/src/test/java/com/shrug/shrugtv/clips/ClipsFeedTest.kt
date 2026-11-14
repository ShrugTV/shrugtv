package com.shrug.shrugtv.clips

import com.shrug.shrugtv.model.Movie
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

/** The pure parts of the clips feed: period, channel choice, merging and sorting. */
class ClipsFeedTest {

    private fun clip(slug: String, views: Int, created: String = "2026-11-10T12:00:00Z") = Movie().apply {
        setClipSlug(slug)
        viewCount = views
        setPublishedAt(created)
    }

    private fun channel(id: Int, favorite: Boolean = false) = Movie().apply {
        userId = id
        login = "ch$id"
        isFavorite = favorite
    }

    @Test
    fun `period gives the start of the time frame`() {
        val now = Instant.parse("2026-11-14T10:20:30.987Z")
        assertEquals("2026-11-13T10:20:30Z", ClipsFeed.startedAt(ClipsPeriod.DAY, now))
        assertEquals("2026-11-07T10:20:30Z", ClipsFeed.startedAt(ClipsPeriod.WEEK, now))
        assertEquals("2026-10-15T10:20:30Z", ClipsFeed.startedAt(ClipsPeriod.MONTH, now))
        assertEquals("2025-11-14T10:20:30Z", ClipsFeed.startedAt(ClipsPeriod.YEAR, now))
        assertNull(ClipsFeed.startedAt(ClipsPeriod.ALL, now))
    }

    @Test
    fun `unknown setting values fall back to the defaults`() {
        assertEquals(ClipsPeriod.WEEK, ClipsPeriod.of(null))
        assertEquals(ClipsPeriod.WEEK, ClipsPeriod.of("nonsense"))
        assertEquals(ClipsPeriod.ALL, ClipsPeriod.of("all"))
        assertEquals(ClipsSource.FOLLOWED, ClipsSource.of("x"))
        assertEquals(ClipsSort.VIEWS, ClipsSort.of(null))
    }

    @Test
    fun `favorites only or everyone, favorites first`() {
        val followed = listOf(channel(1), channel(2, favorite = true), channel(3), channel(4, favorite = true), channel(0))
        assertEquals(listOf(2, 4), ClipsFeed.channels(followed, ClipsSource.FAVORITES).map { it.userId })
        // a channel without id cannot be asked; favorites come first
        assertEquals(listOf(2, 4, 1, 3), ClipsFeed.channels(followed, ClipsSource.FOLLOWED).map { it.userId })
    }

    @Test
    fun `not more channels than the limit are asked`() {
        val many = (1..150).map { channel(it, favorite = it > 140) }
        val asked = ClipsFeed.channels(many, ClipsSource.FOLLOWED)
        assertEquals(ClipsFeed.MAX_CHANNELS, asked.size)
        assertEquals((141..150).toList(), asked.take(10).map { it.userId })
    }

    @Test
    fun `merged list has every clip once, most viewed first, cut at the limit`() {
        val a = listOf(clip("a1", 50), clip("a2", 500))
        val b = listOf(clip("b1", 300), clip("a2", 500))
        val merged = ClipsFeed.merge(listOf(a, b), ClipsSort.VIEWS, 3)
        assertEquals(listOf("a2", "b1", "a1"), merged.map { it.clipSlug })
    }

    @Test
    fun `newest first sorts by creation time`() {
        val list = listOf(clip("old", 900, "2026-11-01T08:00:00Z"), clip("new", 1, "2026-11-13T08:00:00Z"), clip("mid", 5, "2026-11-07T08:00:00Z"))
        assertEquals(listOf("new", "mid", "old"), ClipsFeed.merge(listOf(list), ClipsSort.NEWEST, 10).map { it.clipSlug })
    }

    @Test
    fun `seen clips and other languages are left out`() {
        // setLanguage only works once the flag table exists (empty here: the code is stored without a flag)
        com.shrug.shrugtv.session.AppState.languageFlags = HashMap()
        val de = clip("de", 10).apply { setLanguage("DE") }
        val en = clip("en", 50).apply { setLanguage("EN") }
        val seen = clip("seen", 99).apply { setLanguage("DE") }
        val all = listOf(listOf(de, en, seen))
        assertEquals(listOf("en", "de"), ClipsFeed.merge(all, ClipsSort.VIEWS, 10, seen = setOf("seen")).map { it.clipSlug })
        assertEquals(listOf("seen", "de"), ClipsFeed.merge(all, ClipsSort.VIEWS, 10, language = "de").map { it.clipSlug })
        assertEquals(listOf("de"), ClipsFeed.merge(all, ClipsSort.VIEWS, 10, seen = setOf("seen"), language = "DE").map { it.clipSlug })
    }

    @Test
    fun `shortest first puts clips without a length last`() {
        val list = listOf(clip("long", 1).apply { durationSeconds = 55 }, clip("none", 1), clip("short", 1).apply { durationSeconds = 12 })
        assertEquals(listOf("short", "long", "none"), ClipsFeed.merge(listOf(list), ClipsSort.SHORTEST, 10).map { it.clipSlug })
    }

    @Test
    fun `seen list keeps the newest and moves repeats to the end`() {
        assertEquals(listOf("b", "a"), ClipsSeen.add(linkedSetOf("a", "b"), "a"))
        val many = (1..ClipsSeen.MAX).map { "c$it" }.toCollection(LinkedHashSet())
        val after = ClipsSeen.add(many, "new")
        assertEquals(ClipsSeen.MAX, after.size)
        assertEquals("new", after.last())
        assertEquals("c2", after.first())
        assertEquals(setOf("x", "y"), ClipsSeen.decode(ClipsSeen.encode(listOf("x", "y"))))
    }

    @Test
    fun `relative sort compares a clip with its own channel`() {
        // Big channel: usual clip has 10 000 views. Small channel: usual clip has 300.
        val big = listOf(clip("big-top", 20000), clip("b2", 11000), clip("b3", 10000), clip("b4", 9000), clip("b5", 8000))
        val small = listOf(clip("small-top", 3000), clip("s2", 400), clip("s3", 300), clip("s4", 250), clip("s5", 200))
        val byViews = ClipsFeed.merge(listOf(big, small), ClipsSort.VIEWS, 3).map { it.clipSlug }
        assertEquals(listOf("big-top", "b2", "b3"), byViews)
        val relative = ClipsFeed.merge(listOf(big, small), ClipsSort.RELATIVE, 3).map { it.clipSlug }
        // the small channel's best clip is 10 times its usual, the big channel's best only 2 times
        assertEquals(listOf("small-top", "big-top", "s2"), relative)
    }

    @Test
    fun `usual value is the middle one, or the average with few clips`() {
        val five = listOf("a" to 100, "b" to 200, "c" to 300, "d" to 400, "e" to 1000).map { clip(it.first, it.second) }
        assertEquals(1000 / 300.0, ClipsFeed.relativeScores(listOf(five)).getValue("e"), 0.0001)
        val two = listOf(clip("x", 100), clip("y", 300))
        assertEquals(300 / 200.0, ClipsFeed.relativeScores(listOf(two)).getValue("y"), 0.0001)
        assertEquals(emptyMap<String, Double>(), ClipsFeed.relativeScores(listOf(emptyList())))
    }

    @Test
    fun `unknown seen mode dims, the others are read`() {
        assertEquals(ClipsSeenMode.DIM, ClipsSeenMode.of(null))
        assertEquals(ClipsSeenMode.DIM, ClipsSeenMode.of("nonsense"))
        assertEquals(ClipsSeenMode.SHOW, ClipsSeenMode.of("show"))
        assertEquals(ClipsSeenMode.DIM, ClipsSeenMode.of("dim"))
        assertEquals(ClipsSeenMode.HIDE, ClipsSeenMode.of("hide"))
    }

    @Test
    fun `the page grows by one load and stops at the pool`() {
        assertEquals(40, ClipsFeed.grow(20, 20, 300))
        assertEquals(300, ClipsFeed.grow(290, 20, 300))
        assertEquals(300, ClipsFeed.grow(300, 20, 300))
        assertEquals(21, ClipsFeed.grow(20, 0, 300)) // a step of 0 still moves on
    }

    @Test
    fun `nothing in, nothing out`() {
        assertEquals(emptyList<Movie>(), ClipsFeed.merge(emptyList(), ClipsSort.VIEWS, 20))
        assertEquals(emptyList<Movie>(), ClipsFeed.channels(emptyList(), ClipsSource.FAVORITES))
    }
}
