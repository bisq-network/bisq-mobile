package network.bisq.mobile.presentation.common.ui.components.molecules

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.backup
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IconTextRowUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme { content() }
            }
        }
    }

    @Test
    fun `renders the icon and the text`() {
        setContent {
            IconTextRow(icon = Res.drawable.backup, text = "Encrypted backups")
        }
        composeTestRule.onNodeWithText("Encrypted backups").assertIsDisplayed()
    }
}
