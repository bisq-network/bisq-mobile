package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for [BisqSwitch], focused on the disabled-tap routing added for the greyed
 * animations toggle: a disabled switch should route taps (both on the label and on the
 * greyed switch itself) to [BisqSwitch.onDisabledTap] rather than silently swallowing them.
 */
@RunWith(AndroidJUnit4::class)
class SwitchUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val label = "Use animations"

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            BisqTheme {
                content()
            }
        }
    }

    @Test
    fun `when enabled and switch tapped then toggles`() {
        val onSwitch = mockk<(Boolean) -> Unit>(relaxed = true)
        val onDisabledTap = mockk<() -> Unit>(relaxed = true)

        setContent {
            BisqSwitch(
                checked = false,
                label = label,
                disabled = false,
                onSwitch = onSwitch,
                onDisabledTap = onDisabledTap,
            )
        }

        composeTestRule.onNode(isToggleable()).performClick()

        verify { onSwitch(true) }
        verify(exactly = 0) { onDisabledTap() }
    }

    @Test
    fun `when disabled and switch itself tapped then routes to onDisabledTap not onSwitch`() {
        val onSwitch = mockk<(Boolean) -> Unit>(relaxed = true)
        val onDisabledTap = mockk<() -> Unit>(relaxed = true)

        setContent {
            BisqSwitch(
                checked = false,
                label = label,
                disabled = true,
                onSwitch = onSwitch,
                onDisabledTap = onDisabledTap,
            )
        }

        // Tap the greyed Switch region (right edge of the row, not the label). When disabled the
        // Switch installs no toggleable modifier, so the tap must fall through to the row handler
        // rather than hitting a dead target.
        composeTestRule.onNode(hasClickAction()).performTouchInput { click(centerRight) }

        verify { onDisabledTap() }
        verify(exactly = 0) { onSwitch(any()) }
    }

    @Test
    fun `when disabled and label tapped then routes to onDisabledTap`() {
        val onSwitch = mockk<(Boolean) -> Unit>(relaxed = true)
        val onDisabledTap = mockk<() -> Unit>(relaxed = true)

        setContent {
            BisqSwitch(
                checked = false,
                label = label,
                disabled = true,
                onSwitch = onSwitch,
                onDisabledTap = onDisabledTap,
            )
        }

        composeTestRule.onNodeWithText(label).performClick()

        verify { onDisabledTap() }
        verify(exactly = 0) { onSwitch(any()) }
    }

    @Test
    fun `when disabled without handler then tap is inert`() {
        val onSwitch = mockk<(Boolean) -> Unit>(relaxed = true)

        setContent {
            BisqSwitch(
                checked = false,
                label = label,
                disabled = true,
                onSwitch = onSwitch,
            )
        }

        composeTestRule.onNodeWithText(label).performClick()

        verify(exactly = 0) { onSwitch(any()) }
    }
}
