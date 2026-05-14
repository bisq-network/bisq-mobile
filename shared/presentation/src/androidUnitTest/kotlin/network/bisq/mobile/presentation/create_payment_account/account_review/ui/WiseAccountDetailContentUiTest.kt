package network.bisq.mobile.presentation.create_payment_account.account_review.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.domain.model.account.fiat.WiseAccount
import network.bisq.mobile.domain.model.account.fiat.WiseAccountPayload
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WiseAccountDetailContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setTestContent(account: WiseAccount) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    WiseAccountDetailContent(account = account)
                }
            }
        }
    }

    @Test
    fun `when wise review renders then shows selected currencies`() {
        setTestContent(sampleAccount())

        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("Wise").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("USD, EUR").assertCountEquals(1)
    }

    private fun sampleAccount(): WiseAccount =
        WiseAccount(
            accountName = "Wise Main",
            accountPayload =
                WiseAccountPayload(
                    selectedCurrencyCodes = listOf("USD", "EUR"),
                    holderName = "Satoshi Nakamoto",
                    email = "satoshi@example.com",
                    paymentMethodName = "Wise",
                ),
        )
}
