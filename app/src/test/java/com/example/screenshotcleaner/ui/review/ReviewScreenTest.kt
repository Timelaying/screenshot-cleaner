package com.example.screenshotcleaner.ui.review

import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewScreenTest {
    @Test
    fun rightSwipePastThresholdKeepsScreenshot() {
        assertEquals(
            ReviewSwipeDecision.KEEP,
            reviewDecisionForDrag(dragAmount = 181f)
        )
    }

    @Test
    fun leftSwipePastThresholdDeletesScreenshot() {
        assertEquals(
            ReviewSwipeDecision.DELETE,
            reviewDecisionForDrag(dragAmount = -181f)
        )
    }

    @Test
    fun shortSwipeDoesNothing() {
        assertEquals(
            ReviewSwipeDecision.NONE,
            reviewDecisionForDrag(dragAmount = 180f)
        )
        assertEquals(
            ReviewSwipeDecision.NONE,
            reviewDecisionForDrag(dragAmount = -180f)
        )
    }
}
