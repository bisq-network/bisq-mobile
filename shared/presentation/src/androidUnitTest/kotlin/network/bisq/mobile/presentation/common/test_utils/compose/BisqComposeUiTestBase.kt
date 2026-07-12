package network.bisq.mobile.presentation.common.test_utils.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.test_utils.compose.BisqComposeTestSupport.setBisqTestContent
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
abstract class BisqComposeUiTestBase {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    open fun setUpUiTest() {
        I18nSupport.setLanguage()
    }

    protected fun setTestContent(content: @Composable () -> Unit) {
        composeTestRule.setBisqTestContent(content)
    }
}
