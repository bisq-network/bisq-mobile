package network.bisq.mobile.presentation.common.ui.components.atoms

import kotlin.test.Test
import kotlin.test.assertEquals

class StarRatingTest {
    @Test
    fun `starRatingDisplayFor maps zero rating to empty stars`() {
        assertEquals(StarRatingDisplay(fullStars = 0, hasHalfStar = false), starRatingDisplayFor(0.0))
    }

    @Test
    fun `starRatingDisplayFor maps whole star ratings without half star`() {
        assertEquals(StarRatingDisplay(fullStars = 1, hasHalfStar = false), starRatingDisplayFor(1.0))
        assertEquals(StarRatingDisplay(fullStars = 3, hasHalfStar = false), starRatingDisplayFor(3.0))
        assertEquals(StarRatingDisplay(fullStars = 5, hasHalfStar = false), starRatingDisplayFor(5.0))
    }

    @Test
    fun `starRatingDisplayFor shows half star when fractional part is at least half`() {
        assertEquals(StarRatingDisplay(fullStars = 2, hasHalfStar = true), starRatingDisplayFor(2.5))
        assertEquals(StarRatingDisplay(fullStars = 2, hasHalfStar = true), starRatingDisplayFor(2.9))
        assertEquals(StarRatingDisplay(fullStars = 4, hasHalfStar = true), starRatingDisplayFor(4.5))
    }

    @Test
    fun `starRatingDisplayFor omits half star when fractional part is below half`() {
        assertEquals(StarRatingDisplay(fullStars = 2, hasHalfStar = false), starRatingDisplayFor(2.4))
        assertEquals(StarRatingDisplay(fullStars = 2, hasHalfStar = false), starRatingDisplayFor(2.49))
    }

    @Test
    fun `starRatingDisplayFor clamps out of range ratings to five full stars`() {
        assertEquals(StarRatingDisplay(fullStars = 5, hasHalfStar = false), starRatingDisplayFor(5.7))
        assertEquals(StarRatingDisplay(fullStars = 5, hasHalfStar = false), starRatingDisplayFor(6.0))
    }

    @Test
    fun `starRatingDisplayFor clamps negative ratings to zero stars`() {
        assertEquals(StarRatingDisplay(fullStars = 0, hasHalfStar = false), starRatingDisplayFor(-1.0))
    }

    @Test
    fun `starRatingDisplayFor never shows half star when all five stars are full`() {
        assertEquals(StarRatingDisplay(fullStars = 5, hasHalfStar = false), starRatingDisplayFor(5.0))
        assertEquals(StarRatingDisplay(fullStars = 5, hasHalfStar = false), starRatingDisplayFor(5.5))
    }
}
