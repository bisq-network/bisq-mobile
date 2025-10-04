package network.bisq.mobile.presentation.ui.components.molecules.chat

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import network.bisq.mobile.domain.data.replicated.network.confidential.ack.MessageDeliveryInfoVO
import network.bisq.mobile.domain.data.replicated.network.confidential.ack.MessageDeliveryStatusEnum
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

class MessageDeliveryInfoAndroidUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Ignore("Temporarily disabled due to Robolectric Compose idling NPE; will re-enable with runner setup")
    @Test
    fun messageDeliveryInfo_composes_with_empty_map() {
        I18nSupport.setLanguage("en")
        composeTestRule.setContent {
            network.bisq.mobile.presentation.ui.theme.BisqTheme {
                MessageDeliveryInfo(
                    map = emptyMap(),
                    userNameProvider = { _ -> "Alice" }
                )
            }
        }
        // No assertion needed; test passes if no crash.
        composeTestRule.waitForIdle()
    }

    @Ignore("Temporarily disabled due to Robolectric Compose idling NPE; will re-enable with runner setup")
    @Test
    fun messageDeliveryInfo_composes_with_non_empty_map_and_shows_text() {
        I18nSupport.setLanguage("en")
        val info = MessageDeliveryInfoVO(
            messageDeliveryStatus = MessageDeliveryStatusEnum.SENT,
            ackRequestingMessageId = "msg-1",
            canManuallyResendMessage = false
        )
        val expected = "chat.message.deliveryState.${info.messageDeliveryStatus.name}".i18n()
        val map = mapOf("peer-1" to info)

        composeTestRule.setContent {
            network.bisq.mobile.presentation.ui.theme.BisqTheme {
                MessageDeliveryInfo(
                    map = map,
                    userNameProvider = { _ -> "Alice" }
                )
            }
        }
        composeTestRule.onNodeWithText(expected).assertIsDisplayed()
    }
}

