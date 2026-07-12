package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.test_utils.compose.BisqComposeUiTestBase
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import org.junit.Test

class StarRatingUiTest : BisqComposeUiTestBase() {
    private val testPainters =
        StarPainters(
            fill = ColorPainter(Color.Green),
            half = ColorPainter(Color.Yellow),
            empty = ColorPainter(Color.Gray),
        )

    @Test
    fun `when rendered then uses fixed five-star row dimensions`() {
        setTestContent {
            StarRating(
                rating = 4.5,
                modifier = Modifier.testTag(STAR_RATING_TAG),
                painters = testPainters,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag(STAR_RATING_TAG)
            .assertWidthIsEqualTo(STAR_SIZE * 5 + STAR_SPACING * 4)
            .assertHeightIsEqualTo(STAR_SIZE)
    }

    @Test
    fun `when rating is zero then composes without error`() {
        setTestContent {
            StarRating(
                rating = 0.0,
                modifier = Modifier.testTag(STAR_RATING_TAG),
                painters = testPainters,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithTag(STAR_RATING_TAG)
            .assertWidthIsEqualTo(STAR_SIZE * 5 + STAR_SPACING * 4)
            .assertHeightIsEqualTo(STAR_SIZE)
    }

    @Test
    fun `when rating has no half star then composes without error`() {
        setTestContent {
            StarRating(
                rating = 2.4,
                modifier = Modifier.testTag(STAR_RATING_TAG),
                painters = testPainters,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(STAR_RATING_TAG).assertHeightIsEqualTo(STAR_SIZE)
    }

    @Test
    fun `when rating has half star then composes without error`() {
        setTestContent {
            StarRating(
                rating = 2.5,
                modifier = Modifier.testTag(STAR_RATING_TAG),
                painters = testPainters,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(STAR_RATING_TAG).assertHeightIsEqualTo(STAR_SIZE)
    }

    @Test
    fun `when rating is maximum then composes without error`() {
        setTestContent {
            StarRating(
                rating = 5.0,
                modifier = Modifier.testTag(STAR_RATING_TAG),
                painters = testPainters,
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(STAR_RATING_TAG).assertHeightIsEqualTo(STAR_SIZE)
    }

    private companion object {
        const val STAR_RATING_TAG = "star_rating"
        val STAR_SIZE = BisqUIConstants.ScreenPadding
        val STAR_SPACING = 1.dp
    }
}
