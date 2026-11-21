package network.bisq.mobile.presentation.common.ui.components.molecules

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import kotlin.test.assertEquals

/**
 * UI tests for [PaymentMethodIcon]'s fallback letter: it belongs to the icon, so it keeps the
 * icon's proportions at any system font scale.
 */
class PaymentMethodIconUiTest : BisqComposeUiTestBase() {
    @Test
    fun `the fallback letter ignores the system font scale`() {
        setTestContent {
            Column {
                IconAtFontScale(fontScale = 1f, tag = DEFAULT_SCALE)
                IconAtFontScale(fontScale = 2f, tag = LARGE_SCALE)
            }
        }

        // The letter's box is clamped to the icon either way, so compare the laid-out glyph size.
        assertEquals(letterHeightPx(DEFAULT_SCALE), letterHeightPx(LARGE_SCALE))
    }

    @Composable
    private fun IconAtFontScale(
        fontScale: Float,
        tag: String,
    ) {
        CompositionLocalProvider(
            LocalDensity provides Density(density = LocalDensity.current.density, fontScale = fontScale),
        ) {
            Box(modifier = Modifier.testTag(tag)) {
                // No bundled icon for this id, so the fallback with its overlay letter renders.
                PaymentMethodIcon(methodId = "MYSTERY_BANK", isPaymentMethod = true, size = 20.dp)
            }
        }
    }

    private fun letterHeightPx(tag: String): Float {
        val node =
            composeTestRule
                .onNode(hasText("M") and hasAnyAncestor(hasTestTag(tag)), useUnmergedTree = true)
                .fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layouts)
        return layouts.single().multiParagraph.height
    }

    private companion object {
        const val DEFAULT_SCALE = "defaultScale"
        const val LARGE_SCALE = "largeScale"
    }
}
