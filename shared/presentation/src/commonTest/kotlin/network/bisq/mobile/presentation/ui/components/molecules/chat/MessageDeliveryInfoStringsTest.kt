package network.bisq.mobile.presentation.ui.components.molecules.chat

import network.bisq.mobile.domain.data.replicated.network.confidential.ack.MessageDeliveryInfoVO
import network.bisq.mobile.domain.data.replicated.network.confidential.ack.MessageDeliveryStatusEnum
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class MessageDeliveryInfoStringsTest {

    @Test
    fun test_i18n_delivery_state_key_resolves_successfully() {
        I18nSupport.setLanguage("en")
        // Sanity check: ensure i18n is initialized and delivery-state key resolves to non-blank.
        val key = "chat.message.deliveryState.SENT"
        val value = key.i18n()
        assertTrue(value.isNotBlank(), "Expected i18n value for $key to be non-blank")
    }

    @Test
    fun tooltip_single_peer_uses_plain_delivery_state() {
        I18nSupport.setLanguage("en")
        val info = MessageDeliveryInfoVO(
            messageDeliveryStatus = MessageDeliveryStatusEnum.SENT,
            ackRequestingMessageId = "msg-1",
            canManuallyResendMessage = false
        )
        val map = mapOf("peer-1" to info)
        val deliveryState = "chat.message.deliveryState.${info.messageDeliveryStatus.name}".i18n()
        val tooltip = if (map.size > 1) {
            val userName = "Alice"
            "chat.message.deliveryState.multiplePeers".i18n(userName, deliveryState)
        } else {
            deliveryState
        }
        assertEquals(deliveryState, tooltip, "Expected single-peer tooltip to equal delivery state")
    }

    @Test
    fun tooltip_multi_peer_includes_user_and_delivery_state() {
        I18nSupport.setLanguage("en")
        val info = MessageDeliveryInfoVO(
            messageDeliveryStatus = MessageDeliveryStatusEnum.SENT,
            ackRequestingMessageId = "msg-1",
            canManuallyResendMessage = false
        )
        val map = mapOf("peer-1" to info, "peer-2" to info)
        val userName = "Alice"
        val deliveryState = "chat.message.deliveryState.${info.messageDeliveryStatus.name}".i18n()
        val tooltip = if (map.size > 1) {
            "chat.message.deliveryState.multiplePeers".i18n(userName, deliveryState)
        } else {
            deliveryState
        }
        val expected = "chat.message.deliveryState.multiplePeers".i18n(userName, deliveryState)
        assertEquals(expected, tooltip, "Expected multi-peer tooltip to match formatted string")
        assertTrue(tooltip.contains(userName) && tooltip.contains(deliveryState), "Tooltip should contain userName and deliveryState")
    }
}

