package network.bisq.mobile.presentation.common.ui.components.context

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class LocalExternalUrlOpenerMissingUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `when LocalExternalUrlOpener not provided then composition throws`() {
        val thrown =
            assertFailsWith<IllegalStateException> {
                composeTestRule.setContent {
                    BisqTheme {
                        Box {
                            LocalExternalUrlOpener.current
                        }
                    }
                }
                composeTestRule.waitForIdle()
            }
        assertTrue(
            thrown.message.orEmpty().contains("LocalExternalUrlOpener"),
            "message=${thrown.message}",
        )
    }
}
