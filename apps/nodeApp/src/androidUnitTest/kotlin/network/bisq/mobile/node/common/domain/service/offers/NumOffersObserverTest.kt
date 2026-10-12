package network.bisq.mobile.node.common.domain.service.offers

import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage
import bisq.common.observable.collection.ObservableSet
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

/**
 * The message filter checks ban, ignore and reputation state per message and runs for every market
 * channel when the offers facade is activated on the main thread, so each pass over a channel counts.
 */
class NumOffersObserverTest {
    private val chatMessages = ObservableSet<BisqEasyOfferbookMessage>()
    private val channel: BisqEasyOfferbookChannel =
        mockk(relaxed = true) {
            every { chatMessages } returns this@NumOffersObserverTest.chatMessages
        }
    private var filterCalls = 0
    private val reportedCounts = mutableListOf<Int>()

    private fun offerMessage(): BisqEasyOfferbookMessage = mockk(relaxed = true) { every { hasBisqEasyOffer() } returns true }

    private fun createObserver() =
        NumOffersObserver(
            channel,
            messageFilter = {
                filterCalls++
                true
            },
            setNumOffers = { reportedCounts.add(it) },
        )

    @Test
    fun `offers of a channel are counted in one pass when the observer is created`() {
        repeat(3) { chatMessages.add(offerMessage()) }

        createObserver()

        assertEquals(listOf(3), reportedCounts)
        assertEquals(3, filterCalls)
    }

    @Test
    fun `offers are counted in one pass when the observer is resumed`() {
        repeat(3) { chatMessages.add(offerMessage()) }
        val observer = createObserver()
        filterCalls = 0

        observer.resume()

        assertEquals(3, filterCalls)
    }

    @Test
    fun `count is reported again when an offer is added`() {
        chatMessages.add(offerMessage())
        createObserver()

        chatMessages.add(offerMessage())

        assertEquals(listOf(1, 2), reportedCounts)
    }

    @Test
    fun `changes are not reported after dispose`() {
        val observer = createObserver()
        observer.dispose()

        chatMessages.add(offerMessage())

        assertEquals(emptyList(), reportedCounts)
    }
}
