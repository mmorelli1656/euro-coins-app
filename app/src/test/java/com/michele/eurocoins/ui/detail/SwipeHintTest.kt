package com.michele.eurocoins.ui.detail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeHintTest {
    @Test
    fun showsOnFirstOpeningWithSeveralPages() {
        assertTrue(shouldShowSwipeHint(timesShown = 0, hasSwiped = false, pageCount = 12))
    }

    @Test
    fun neverWithASinglePage() {
        assertFalse(shouldShowSwipeHint(timesShown = 0, hasSwiped = false, pageCount = 1))
    }

    @Test
    fun repeatsUntilThreeTimes() {
        assertTrue(shouldShowSwipeHint(timesShown = 2, hasSwiped = false, pageCount = 8))
        assertFalse(shouldShowSwipeHint(timesShown = SWIPE_HINT_MAX_SHOWS, hasSwiped = false, pageCount = 8))
    }

    @Test
    fun stopsForeverAfterARealSwipe() {
        assertFalse(shouldShowSwipeHint(timesShown = 0, hasSwiped = true, pageCount = 8))
    }

    @Test
    fun capIsThree() {
        assertEquals(3, SWIPE_HINT_MAX_SHOWS)
    }
}
