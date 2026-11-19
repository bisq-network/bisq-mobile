package network.bisq.mobile.presentation.common.ui.components.atoms

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * UI tests for [AutoResizeText]: both overloads expose the complete text, also when hiding is
 * allowed. The font size steps are covered by [AutoResizeStepTest]: Robolectric's text
 * measurement reports every line with an explicit line height as overflowing, so sizes measured
 * here would not reflect a device.
 */
class AutoResizeTextUiTest : BisqComposeUiTestBase() {
    @Test
    fun `the string overload exposes the complete text`() {
        setTestContent {
            AutoResizeText(text = TEXT, modifier = Modifier.testTag(TAG))
        }

        composeTestRule.onNodeWithTag(TAG).assertTextEquals(TEXT)
    }

    @Test
    fun `the annotated overload exposes the complete text`() {
        setTestContent {
            AutoResizeText(text = AnnotatedString(TEXT), modifier = Modifier.testTag(TAG))
        }

        composeTestRule.onNodeWithTag(TAG).assertTextEquals(TEXT)
    }

    @Test
    fun `a label hidden for overflowing stays readable to screen readers`() {
        setTestContent {
            AutoResizeText(text = TEXT, hideWhenOverflowingAtMinimum = true, modifier = Modifier.testTag(TAG))
        }

        composeTestRule.onNodeWithTag(TAG).assertTextEquals(TEXT)
    }

    private companion object {
        const val TAG = "autoResizeText"
        const val TEXT = "812 days"
    }
}
