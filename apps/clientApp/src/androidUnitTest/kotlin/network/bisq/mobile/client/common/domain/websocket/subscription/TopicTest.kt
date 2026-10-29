package network.bisq.mobile.client.common.domain.websocket.subscription

import network.bisq.mobile.client.common.data.model.alert.AuthorizedAlertDataDto
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import org.junit.Before
import org.junit.Test
import kotlin.reflect.typeOf
import kotlin.test.assertEquals

@OptIn(ExperimentalStdlibApi::class)
class TopicTest {
    @Before
    fun setUp() {
        I18nSupport.setLanguage()
    }

    @Test
    fun `alert notifications topic exposes expected wire metadata`() {
        assertEquals(typeOf<List<AuthorizedAlertDataDto>>(), Topic.ALERT_NOTIFICATIONS.typeOf)
        assertEquals(TopicImportance.CRITICAL, Topic.ALERT_NOTIFICATIONS.importance)
        assertEquals("mobile.client.topic.alert_notifications.title", Topic.ALERT_NOTIFICATIONS.titleKey)
        assertEquals("mobile.client.topic.alert_notifications.desc", Topic.ALERT_NOTIFICATIONS.descriptionKey)
        assertEquals(Topic.ALERT_NOTIFICATIONS.titleKey.i18n(), Topic.ALERT_NOTIFICATIONS.i18n())
    }

    /** bisq2's SimpleObservableWebSocketService subclasses and NetworkInfo send their whole state on every REPLACE. */
    @Test
    fun `only snapshot-style topics mark replace as full state`() {
        val expected =
            setOf(
                Topic.MARKET_PRICE,
                Topic.NUM_OFFERS,
                Topic.NUM_USER_PROFILES,
                Topic.REPUTATION,
                Topic.ALERT_NOTIFICATIONS,
                Topic.TRADE_RESTRICTING_ALERT,
                Topic.CONTACTS,
                Topic.NETWORK_INFO,
            )
        assertEquals(expected, Topic.entries.filter { it.replaceIsFullState }.toSet())
    }
}
