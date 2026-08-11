package network.bisq.mobile.presentation.common.ui.components.molecules

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.backup
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

class IconTextRowUiTest : BisqComposeUiTestBase() {
    @Test
    fun `renders the icon and the text`() {
        setTestContent {
            IconTextRow(icon = Res.drawable.backup, text = "Encrypted backups")
        }
        composeTestRule.onNodeWithText("Encrypted backups").assertIsDisplayed()
    }
}
