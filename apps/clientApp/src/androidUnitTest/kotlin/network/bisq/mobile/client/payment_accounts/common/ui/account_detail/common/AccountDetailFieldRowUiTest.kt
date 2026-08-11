package network.bisq.mobile.client.payment_accounts.common.ui.account_detail.common

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.AccountDetailFieldRow
import network.bisq.mobile.client.payment_accounts.presentation.common.ui.account_detail.common.ExpandableAccountDetailFieldRow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import org.robolectric.annotation.Config

@Config(application = TestApplication::class)
class AccountDetailFieldRowUiTest : BisqComposeUiTestBase() {
    private fun setExpandableFieldRowContent(
        label: String,
        value: String,
        dialogTitle: String = label,
        maxLines: Int = 4,
    ) {
        setTestContent {
            ExpandableAccountDetailFieldRow(
                label = label,
                value = value,
                dialogTitle = dialogTitle,
                maxLines = maxLines,
            )
        }
    }

    @Test
    fun `when field row renders then label and value are displayed`() {
        // Given / When
        setTestContent {
            AccountDetailFieldRow(
                label = "paymentAccounts.holderName".i18n(),
                value = "Alice Doe",
            )
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.holderName".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Alice Doe")
            .assertIsDisplayed()
    }

    @Test
    fun `when expandable row text does not overflow then dialog is not shown on click`() {
        // Given
        val label = "paymentAccounts.userDefined.accountData".i18n()
        val value = "Short account data"

        // When
        setExpandableFieldRowContent(
            label = label,
            value = value,
            maxLines = 4,
        )
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(value)
            .performClick()

        // Then
        composeTestRule
            .onAllNodesWithText("action.close".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText(label)
            .assertCountEquals(1)
    }

    @Test
    fun `when expandable row text overflows then clicking value opens and closes dialog`() {
        // Given
        val label = "paymentAccounts.userDefined.accountData".i18n()
        val dialogTitle = "paymentAccounts.details".i18n()
        val longValue =
            List(10) { "Use SWIFT: DEUTDEFF and reference code 1234567890." }.joinToString("\n")

        // When
        setExpandableFieldRowContent(
            label = label,
            value = longValue,
            dialogTitle = dialogTitle,
            maxLines = 1,
        )
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText(longValue, substring = true)
            .performClick()

        // Then
        composeTestRule
            .onNodeWithText(dialogTitle)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("action.close".i18n())
            .performClick()
        composeTestRule
            .onAllNodesWithText("action.close".i18n())
            .assertCountEquals(0)
    }
}
