package network.bisq.mobile.domain.data.replicated.presentation.open_trades

import io.mockk.every
import io.mockk.mockk
import network.bisq.mobile.domain.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelModel
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.BisqEasyTradeModel
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class TradeItemPresentationModelDirectionTest {

    @BeforeTest
    fun initI18n() {
        I18nSupport.initialize("en")
    }

    @Test
    fun directionalTitle_usesIsSeller_whenSeller() {
        val dto = mockk<TradeItemPresentationDto>(relaxed = true)
        val channelModel = mockk<BisqEasyOpenTradeChannelModel>(relaxed = true)
        val tradeModel = mockk<BisqEasyTradeModel>(relaxed = true)
        every { tradeModel.isSeller } returns true

        val model = TradeItemPresentationModel(
            tradeItemPresentationDto = dto,
            bisqEasyOpenTradeChannelModel = channelModel,
            bisqEasyTradeModel = tradeModel,
        )

        assertEquals(
            "bisqEasy.openTrades.table.direction.seller".i18n().uppercase(),
            model.directionalTitle
        )
    }

    @Test
    fun directionalTitle_usesIsSeller_whenBuyer() {
        val dto = mockk<TradeItemPresentationDto>(relaxed = true)
        val channelModel = mockk<BisqEasyOpenTradeChannelModel>(relaxed = true)
        val tradeModel = mockk<BisqEasyTradeModel>(relaxed = true)
        every { tradeModel.isSeller } returns false

        val model = TradeItemPresentationModel(
            tradeItemPresentationDto = dto,
            bisqEasyOpenTradeChannelModel = channelModel,
            bisqEasyTradeModel = tradeModel,
        )

        assertEquals(
            "bisqEasy.openTrades.table.direction.buyer".i18n().uppercase(),
            model.directionalTitle
        )
    }
}

