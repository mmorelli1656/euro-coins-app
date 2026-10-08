package com.michele.eurocoins.data.pro

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BannerAdTest {
    @Test
    fun retryDelayGrowsThenStaysAtOneMinute() {
        assertEquals(15_000L, bannerRetryDelayMs(1))
        assertEquals(30_000L, bannerRetryDelayMs(2))
        assertEquals(60_000L, bannerRetryDelayMs(3))
        assertEquals(60_000L, bannerRetryDelayMs(50))
    }

    @Test
    fun firstRetryIsQuickerThanTheOldFixedMinute() {
        assertTrue(bannerRetryDelayMs(1) < 60_000L)
    }
}
