package network.bisq.mobile.presentation.create_payment_account.account_review.ui.core

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
class AccountDetailFieldRowUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setExpandableFieldRowContent(
        label: String,
        value: String,
        dialogTitle: String = label,
        maxLines: Int = 4,
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    ExpandableAccountDetailFieldRow(
                        label = label,
                        value = value,
                        dialogTitle = dialogTitle,
                        maxLines = maxLines,
                    )
                }
            }
        }
    }

    @Test
    fun `when field row renders then label and value are displayed`() {
        // Given / When
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    AccountDetailFieldRow(
                        label = "paymentAccounts.holderName".i18n(),
                        value = "Alice Doe",
                    )
                }
            }
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
            "Use SWIFT: DEUTDEFF and reference code 1234567890. ".repeat(30)

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
