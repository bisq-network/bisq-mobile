package network.bisq.mobile.client.payment_accounts.common.ui.account_detail.common

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailDetailsSection
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import org.robolectric.annotation.Config

@Config(application = TestApplication::class)
class AccountDetailDetailsSectionUiTest : BisqComposeUiTestBase() {
    private fun setSectionContent(
        creationDate: String?,
        tradeLimitInfo: String?,
        tradeDuration: String?,
    ) {
        setTestContent {
            Column {
                AccountDetailDetailsSection(
                    creationDate = creationDate,
                    tradeLimitInfo = tradeLimitInfo,
                    tradeDuration = tradeDuration,
                )
            }
        }
    }

    @Test
    fun `when all details are null or blank then section is not rendered`() {
        // Given / When
        setSectionContent(
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
        setSectionContent(
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
        setSectionContent(
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
        setSectionContent(
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
