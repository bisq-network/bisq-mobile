package network.bisq.mobile.presentation.create_payment_account.account_review.ui.core

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountDetailDetailsSectionUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setTestContent(
        creationDate: String?,
        tradeLimitInfo: String?,
        tradeDuration: String?,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    AccountDetailDetailsSection(
                        creationDate = creationDate,
                        tradeLimitInfo = tradeLimitInfo,
                        tradeDuration = tradeDuration,
                    )
                }
            }
        }
    }

    @Test
    fun `when all details are null or blank then section is not rendered`() {
        // Given / When
        setTestContent(
            creationDate = null,
            tradeLimitInfo = "",
            tradeDuration = null,
        )

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.details".i18n().uppercase())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.accountCreationDate".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.tradeLimit".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.tradeDuration".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when creation date provided then header and creation date row are rendered`() {
        // Given / When
        setTestContent(
            creationDate = "Apr 3, 2026",
            tradeLimitInfo = null,
            tradeDuration = null,
        )

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.details".i18n().uppercase())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.accountCreationDate".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Apr 3, 2026")
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.tradeLimit".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.tradeDuration".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when trade limit and trade duration provided then both rows are rendered`() {
        // Given / When
        setTestContent(
            creationDate = null,
            tradeLimitInfo = "1000 EUR",
            tradeDuration = "8 days",
        )

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.details".i18n().uppercase())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.tradeLimit".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("1000 EUR")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.tradeDuration".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("8 days")
            .assertIsDisplayed()
    }

    @Test
    fun `when detail values are blank then corresponding rows are not rendered`() {
        // Given / When
        setTestContent(
            creationDate = "Apr 3, 2026",
            tradeLimitInfo = "",
            tradeDuration = "   ",
        )

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.details".i18n().uppercase())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.accountCreationDate".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Apr 3, 2026")
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.tradeLimit".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.tradeDuration".i18n())
            .assertCountEquals(0)
    }
}
