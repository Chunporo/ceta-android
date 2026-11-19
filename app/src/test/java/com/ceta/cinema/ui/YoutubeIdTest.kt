package com.ceta.cinema.ui

import com.ceta.cinema.ui.screen.movie.youtubeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeIdTest {
    @Test fun readsWatchShortAndEmbedLinks() {
        assertEquals("dQw4w9WgXcQ", youtubeId("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", youtubeId("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10s"))
        assertEquals("dQw4w9WgXcQ", youtubeId("https://youtu.be/dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", youtubeId("https://www.youtube.com/embed/dQw4w9WgXcQ"))
    }

    @Test fun anythingElseIsNotAVideo() {
        assertNull(youtubeId(null))
        assertNull(youtubeId(""))
        assertNull(youtubeId("https://www.youtube.com/results?search_query=dune+trailer"))
        assertNull(youtubeId("https://example.com/watch?v=<script>"))
    }
}
