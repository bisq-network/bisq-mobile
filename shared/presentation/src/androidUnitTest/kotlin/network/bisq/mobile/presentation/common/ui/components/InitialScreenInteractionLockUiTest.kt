package network.bisq.mobile.presentation.common.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.After
import org.junit.Test
import kotlin.test.assertEquals

class InitialScreenInteractionLockUiTest : BisqComposeUiTestBase() {
    @After
    fun tearDown() {
        composeTestRule.mainClock.autoAdvance = true
    }

    @Composable
    private fun TestLockContent(
        lockDurationMillis: Long,
        onClick: () -> Unit,
    ) {
        InitialScreenInteractionLock(lockDurationMillis = lockDurationMillis) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Button(onClick = onClick) {
                    Text(BUTTON_TEXT)
                }
            }
        }
    }

    @Test
    fun `when rendered then content is displayed`() {
        setTestContent {
            InitialScreenInteractionLock {
                Text("Locked content")
            }
        }

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Locked content").assertIsDisplayed()
    }

    @Test
    fun `when clicked before delay ends then click is blocked`() {
        var clickCount = 0
        composeTestRule.mainClock.autoAdvance = false

        setTestContent {
            TestLockContent(
                lockDurationMillis = 250L,
                onClick = { clickCount++ },
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(BUTTON_TEXT).performTouchInput { click() }
        composeTestRule.waitForIdle()

        assertEquals(0, clickCount)
    }

    @Test
    fun `when clicked after delay ends then click is handled`() {
        var clickCount = 0
        composeTestRule.mainClock.autoAdvance = false

        setTestContent {
            TestLockContent(
                lockDurationMillis = 250L,
                onClick = { clickCount++ },
            )
        }

        composeTestRule.mainClock.advanceTimeBy(251L)
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(BUTTON_TEXT).performTouchInput { click() }
        composeTestRule.waitForIdle()

        assertEquals(1, clickCount)
    }

    @Test
    fun `when delay is zero then click is handled immediately`() {
        var clickCount = 0
        composeTestRule.mainClock.autoAdvance = false

        setTestContent {
            TestLockContent(
                lockDurationMillis = 0L,
                onClick = { clickCount++ },
            )
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(BUTTON_TEXT).performTouchInput { click() }
        composeTestRule.waitForIdle()

        assertEquals(1, clickCount)
    }

    private companion object {
        const val BUTTON_TEXT = "Tap action"
    }
}
