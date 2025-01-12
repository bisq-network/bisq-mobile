package network.bisq.mobile.android.node.service.mediation

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService
import bisq.contract.bisq_easy.BisqEasyContract
import bisq.i18n.Res
import bisq.support.mediation.MediationRequestService
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.data.replicated.trade.bisq_easy.TradeItemPresentationModel
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.utils.Logging

class NodeMediationServiceFacade(applicationService: AndroidApplicationService.Provider) : MediationServiceFacade, Logging {

    // Dependencies
    private val channelService: BisqEasyOpenTradeChannelService by lazy { applicationService.chatService.get().bisqEasyOpenTradeChannelService }
    private val mediationRequestService: MediationRequestService by lazy { applicationService.supportService.get().mediationRequestService }

    // API
    override fun activate() {
    }

    override fun deactivate() {
    }

    // API
    override suspend fun reportToMediator(value: TradeItemPresentationModel): Result<Unit> {
        val channel: BisqEasyOpenTradeChannel =
            Mappings.BisqEasyOpenTradeChannelModelMapping.toBisq2Model(value.bisqEasyOpenTradeChannelModel)
        val mediator = channel.mediator
        if (mediator.isPresent) {
            val encoded =
                Res.encode("bisqEasy.mediation.requester.tradeLogMessage", channel.myUserIdentity.userName)
            channelService.sendTradeLogMessage(encoded, channel).join()
            channel.setIsInMediation(true)
            val contract: BisqEasyContract = Mappings.BisqEasyContractMapping.toBisq2Model(value.bisqEasyTradeModel.contract)
            mediationRequestService.requestMediation(channel, contract)
        }
        return Result.success(Unit)
    }
}