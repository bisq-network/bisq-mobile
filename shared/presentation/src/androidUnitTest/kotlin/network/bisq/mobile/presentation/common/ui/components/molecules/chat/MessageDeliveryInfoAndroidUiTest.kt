package network.bisq.mobile.presentation.common.ui.components.molecules.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.domain.data.replicated.network.confidential.ack.MessageDeliveryInfoVO
import network.bisq.mobile.domain.data.replicated.network.confidential.ack.MessageDeliveryStatusEnum
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for MessageDeliveryInfo component using Robolectric.
 *
 * NOTE: This component is in commonMain but tests are in androidUnitTest to enable
 * fast CI without emulators. Robolectric provides the Android framework on JVM.
 */
@RunWith(AndroidJUnit4::class)
class MessageDeliveryInfoAndroidUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    @Test
    fun messageDeliveryInfo_composes_with_empty_map() {
        composeTestRule.setContent {
            BisqTheme {
                MessageDeliveryInfo(
                    map = emptyMap(),
                    userNameProvider = { _ -> "Alice" },
                )
            }
        }

        // Wait for composition to complete
        composeTestRule.waitForIdle()
        // Test passes if no crash occurs
    }

    @Test
    fun messageDeliveryInfo_composes_with_non_empty_map_and_shows_text() {
        val info =
            MessageDeliveryInfoVO(
                messageDeliveryStatus = MessageDeliveryStatusEnum.SENT,
                ackRequestingMessageId = "msg-1",
                canManuallyResendMessage = false,
            )
        val expected = "chat.message.deliveryState.${info.messageDeliveryStatus.name}".i18n()
        val map = mapOf("peer-1" to info)

        composeTestRule.setContent {
            BisqTheme {
                MessageDeliveryInfo(
                    map = map,
                    userNameProvider = { _ -> "Alice" },
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }
}
