package network.bisq.mobile.domain.usecase.trade

import io.mockk.every
import io.mockk.mockk
import network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.domain.model.trade.TradeRoleFilter
import network.bisq.mobile.domain.model.trade.TradeSort
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FilterOpenTradesUseCaseTest {
    private val useCase = FilterOpenTradesUseCase()

    private fun trade(
        tradeId: String = "trade-id-default",
        shortTradeId: String = "short-id",
        peersUserName: String = "alice",
        myUserName: String = "bob",
        market: String = "BTC/USD",
        formattedPrice: String = "65000",
        bitcoinSettlementMethodDisplayString: String = "Onchain",
        fiatPaymentMethodDisplayString: String = "SEPA",
        baseAmountWithCode: String = "0.05 BTC",
        quoteAmountWithCode: String = "3250 USD",
        directionalTitle: String = "BUY",
        isSeller: Boolean = false,
        takeOfferDate: Long = 0L,
        quoteAmount: Long = 0L,
    ): TradeItemPresentationModel {
        val tradeModel = mockk<BisqEasyTradeModel>(relaxed = true)
        every { tradeModel.isSeller } returns isSeller
        every { tradeModel.takeOfferDate } returns takeOfferDate

        val item = mockk<TradeItemPresentationModel>(relaxed = true)
        every { item.bisqEasyTradeModel } returns tradeModel
        every { item.tradeId } returns tradeId
        every { item.shortTradeId } returns shortTradeId
        every { item.peersUserName } returns peersUserName
        every { item.myUserName } returns myUserName
        every { item.market } returns market
        every { item.formattedPrice } returns formattedPrice
        every { item.bitcoinSettlementMethodDisplayString } returns bitcoinSettlementMethodDisplayString
        every { item.fiatPaymentMethodDisplayString } returns fiatPaymentMethodDisplayString
        every { item.baseAmountWithCode } returns baseAmountWithCode
        every { item.quoteAmountWithCode } returns quoteAmountWithCode
        every { item.directionalTitle } returns directionalTitle
        every { item.quoteAmount } returns quoteAmount
        return item
    }

    @Test
    fun emptyInput_returnsEmpty() {
        val result = useCase.invoke(items = emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun emptyQuery_returnsAllItems() {
        val a = trade(tradeId = "a", takeOfferDate = 1)
        val b = trade(tradeId = "b", takeOfferDate = 2)
        val result = useCase.invoke(items = listOf(a, b))
        assertEquals(2, result.size)
    }

    @Test
    fun whitespaceOnlyQuery_treatedAsEmpty() {
        val a = trade(tradeId = "a")
        val b = trade(tradeId = "b")
        val result = useCase.invoke(items = listOf(a, b), searchQuery = "   ")
        assertEquals(2, result.size)
    }

    @Test
    fun query_matchesPeerUserName_caseInsensitive() {
        val match = trade(tradeId = "a", peersUserName = "Alice")
        val miss = trade(tradeId = "b", peersUserName = "Bob")
        val result = useCase.invoke(items = listOf(match, miss), searchQuery = "ALICE")
        assertEquals(listOf("a"), result.map { it.tradeId })
    }

    @Test
    fun query_matchesTradeId_partial() {
        val match = trade(tradeId = "abc-123-xyz")
        val miss = trade(tradeId = "different")
        val result = useCase.invoke(items = listOf(match, miss), searchQuery = "123")
        assertEquals(listOf("abc-123-xyz"), result.map { it.tradeId })
    }

    @Test
    fun query_matchesShortTradeId_partial() {
        val match = trade(tradeId = "a", shortTradeId = "abc123")
        val miss = trade(tradeId = "b", shortTradeId = "zzz999")
        val result = useCase.invoke(items = listOf(match, miss), searchQuery = "abc")
        assertEquals(listOf("a"), result.map { it.tradeId })
    }

    @Test
    fun query_matchesMyUserName_partial() {
        val match = trade(tradeId = "a", myUserName = "Charlie")
        val miss = trade(tradeId = "b", myUserName = "Dave")
        val result = useCase.invoke(items = listOf(match, miss), searchQuery = "char")
        assertEquals(listOf("a"), result.map { it.tradeId })
    }

    @Test
    fun query_matchesMarket() {
        val usd = trade(tradeId = "usd", market = "BTC/USD")
        val eur = trade(tradeId = "eur", market = "BTC/EUR")
        val result = useCase.invoke(items = listOf(usd, eur), searchQuery = "eur")
        assertEquals(listOf("eur"), result.map { it.tradeId })
    }

    @Test
    fun query_noMatch_returnsEmpty() {
        val items = listOf(trade(tradeId = "a"), trade(tradeId = "b"))
        val result = useCase.invoke(items = items, searchQuery = "no-such-thing")
        assertTrue(result.isEmpty())
    }

    @Test
    fun roleBuyer_keepsBuyerItems_dropsSellerItems() {
        val buyer = trade(tradeId = "buy", isSeller = false)
        val seller = trade(tradeId = "sell", isSeller = true)
        val result = useCase.invoke(items = listOf(buyer, seller), roleFilter = TradeRoleFilter.BUYER)
        assertEquals(listOf("buy"), result.map { it.tradeId })
    }

    @Test
    fun roleSeller_keepsSellerItems_dropsBuyerItems() {
        val buyer = trade(tradeId = "buy", isSeller = false)
        val seller = trade(tradeId = "sell", isSeller = true)
        val result = useCase.invoke(items = listOf(buyer, seller), roleFilter = TradeRoleFilter.SELLER)
        assertEquals(listOf("sell"), result.map { it.tradeId })
    }

    @Test
    fun roleAll_keepsBothRoles() {
        val buyer = trade(tradeId = "buy", isSeller = false)
        val seller = trade(tradeId = "sell", isSeller = true)
        val result = useCase.invoke(items = listOf(buyer, seller), roleFilter = TradeRoleFilter.ALL)
        assertEquals(2, result.size)
    }

    @Test
    fun newestFirst_sortsByTakeOfferDateDescending() {
        val older = trade(tradeId = "older", takeOfferDate = 100L)
        val newer = trade(tradeId = "newer", takeOfferDate = 200L)
        val result = useCase.invoke(items = listOf(older, newer), sortBy = TradeSort.NEWEST_FIRST)
        assertEquals(listOf("newer", "older"), result.map { it.tradeId })
    }

    @Test
    fun oldestFirst_sortsByTakeOfferDateAscending() {
        val older = trade(tradeId = "older", takeOfferDate = 100L)
        val newer = trade(tradeId = "newer", takeOfferDate = 200L)
        val result = useCase.invoke(items = listOf(newer, older), sortBy = TradeSort.OLDEST_FIRST)
        assertEquals(listOf("older", "newer"), result.map { it.tradeId })
    }

    @Test
    fun amountHighLow_sortsByQuoteAmountDescending() {
        val small = trade(tradeId = "small", quoteAmount = 100L)
        val large = trade(tradeId = "large", quoteAmount = 1_000L)
        val result = useCase.invoke(items = listOf(small, large), sortBy = TradeSort.AMOUNT_HIGH_LOW)
        assertEquals(listOf("large", "small"), result.map { it.tradeId })
    }

    @Test
    fun amountLowHigh_sortsByQuoteAmountAscending() {
        val small = trade(tradeId = "small", quoteAmount = 100L)
        val large = trade(tradeId = "large", quoteAmount = 1_000L)
        val result = useCase.invoke(items = listOf(large, small), sortBy = TradeSort.AMOUNT_LOW_HIGH)
        assertEquals(listOf("small", "large"), result.map { it.tradeId })
    }

    @Test
    fun combinedQueryAndRoleAndSort_appliesAll() {
        // Two items match (alice + buyer): a (older) and d (newer). One has role mismatch (b),
        // one has query mismatch (c). With OLDEST_FIRST, a must precede d in the result.
        val a = trade(tradeId = "a", peersUserName = "Alice", isSeller = false, takeOfferDate = 100L)
        val b = trade(tradeId = "b", peersUserName = "Alice", isSeller = true, takeOfferDate = 200L)
        val c = trade(tradeId = "c", peersUserName = "Carol", isSeller = false, takeOfferDate = 300L)
        val d = trade(tradeId = "d", peersUserName = "Alice", isSeller = false, takeOfferDate = 400L)
        val result =
            useCase.invoke(
                items = listOf(a, b, c, d),
                searchQuery = "alice",
                sortBy = TradeSort.OLDEST_FIRST,
                roleFilter = TradeRoleFilter.BUYER,
            )
        assertEquals(listOf("a", "d"), result.map { it.tradeId })
    }
}
