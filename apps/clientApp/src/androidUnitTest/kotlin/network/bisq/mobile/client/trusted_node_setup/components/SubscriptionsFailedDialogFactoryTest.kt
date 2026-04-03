package network.bisq.mobile.client.trusted_node_setup.components

import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.TopicImportance
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class SubscriptionsFailedDialogFactoryTest {
    @Before
    fun setUp() {
        I18nSupport.setLanguage()
    }

    @Test
    fun `preview helper states expose expected topics`() {
        assertEquals(
            listOf(Topic.MARKET_PRICE),
            invokeStateFactory("singleCriticalFailure").failedTopics,
        )
        assertEquals(
            listOf(Topic.MARKET_PRICE, Topic.TRADE_PROPERTIES, Topic.NUM_OFFERS),
            invokeStateFactory("mixedFailures").failedTopics,
        )
        assertEquals(
            Topic.entries.filter { it.importance == TopicImportance.CRITICAL },
            invokeStateFactory("allCriticalFailures").failedTopics,
        )
        assertEquals(Topic.MARKET_PRICE.titleKey.i18n(), Topic.MARKET_PRICE.i18n())
        assertEquals(
            listOf(Topic.NUM_OFFERS),
            SubscriptionsFailedDialogUiState(listOf(Topic.NUM_OFFERS)).failedTopics,
        )
    }

    private fun invokeStateFactory(methodName: String): SubscriptionsFailedDialogUiState {
        val clazz =
            Class.forName("network.bisq.mobile.client.trusted_node_setup.components.SubscriptionsFailedDialogKt")
        val method = clazz.getDeclaredMethod(methodName)
        method.isAccessible = true
        return method.invoke(null) as SubscriptionsFailedDialogUiState
    }
}
