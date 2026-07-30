package network.bisq.mobile.presentation.common.ui.i18n

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.compose.BisqComposeUiTestBase
import network.bisq.mobile.presentation.common.ui.components.context.LocalLanguageCode
import org.junit.After
import org.junit.Test

class LocalLanguageCodeComposeTest : BisqComposeUiTestBase() {
    private class LanguageHolder {
        var code by mutableStateOf("en")
    }

    @After
    fun tearDown() {
        I18nSupport.setLanguage("en")
    }

    @Test
    fun `i18nText resolves Spanish after setLanguage then local advances`() {
        val holder = LanguageHolder()
        val key = "mobile.myTrades.tab.open"

        setTestContent {
            CompositionLocalProvider(LocalLanguageCode provides holder.code) {
                Text(
                    text = i18nText(key),
                    modifier = Modifier.testTag("label"),
                )
            }
        }

        composeTestRule.onNodeWithTag("label").assertTextEquals("Open trades")

        I18nSupport.setLanguage("es")
        holder.code = "es"
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("label").assertTextEquals("Comercios abiertos")
    }

    @Test
    fun `i18nText with args resolves after language change`() {
        val holder = LanguageHolder()
        val key = "validation.tooShort"

        setTestContent {
            CompositionLocalProvider(LocalLanguageCode provides holder.code) {
                Text(
                    text = i18nText(key, 5),
                    modifier = Modifier.testTag("label"),
                )
            }
        }

        composeTestRule.onNodeWithTag("label").assertTextEquals("Input text must have at least 5 characters")

        I18nSupport.setLanguage("es")
        holder.code = "es"
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag("label")
            .assertTextEquals("El texto de entrada debe tener al menos 5 caracteres")
    }

    @Test
    fun `unmigrated i18n call site refreshes when LocalLanguageCode changes`() {
        val holder = LanguageHolder()

        setTestContent {
            CompositionLocalProvider(LocalLanguageCode provides holder.code) {
                // Plain .i18n() — no i18nText / resolve — relies on staticCompositionLocalOf invalidation.
                Text(
                    text = "mobile.myTrades.tab.open".i18n(),
                    modifier = Modifier.testTag("label"),
                )
            }
        }

        composeTestRule.onNodeWithTag("label").assertTextEquals("Open trades")

        I18nSupport.setLanguage("es")
        holder.code = "es"
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("label").assertTextEquals("Comercios abiertos")
    }

    @Test
    fun `LocalLanguageCode follows I18nSupport currentLanguage like App wiring`() {
        I18nSupport.setLanguage("en")

        setTestContent {
            // Mirrors App: provide LocalLanguageCode from I18nSupport.currentLanguage only.
            val currentLanguageCode by I18nSupport.currentLanguage.collectAsState()
            CompositionLocalProvider(LocalLanguageCode provides currentLanguageCode) {
                Text(
                    text = i18nText("mobile.myTrades.tab.open"),
                    modifier = Modifier.testTag("label"),
                )
            }
        }

        composeTestRule.onNodeWithTag("label").assertTextEquals("Open trades")

        I18nSupport.setLanguage("es")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("label").assertTextEquals("Comercios abiertos")
    }
}
