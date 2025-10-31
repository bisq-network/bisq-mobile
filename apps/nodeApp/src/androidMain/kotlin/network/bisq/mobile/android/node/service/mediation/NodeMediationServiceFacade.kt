package network.bisq.mobile.android.node.service.mediation

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService
import bisq.contract.bisq_easy.BisqEasyContract
import bisq.i18n.Res
import bisq.support.mediation.MediationRequestService
import network.bisq.mobile.android.node.AndroidApplicationService
import network.bisq.mobile.android.node.mapping.Mappings
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.offers.MediatorNotAvailableException

class NodeMediationServiceFacade(applicationService: AndroidApplicationService.Provider) : ServiceFacade(), MediationServiceFacade {
    // Dependencies
    private val channelService: BisqEasyOpenTradeChannelService by lazy { applicationService.chatService.get().bisqEasyOpenTradeChannelService }
    private val mediationRequestService: MediationRequestService by lazy { applicationService.supportService.get().mediationRequestService }

    override fun activate() {
        super<ServiceFacade>.activate()
    }

    override fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    override suspend fun reportToMediator(value: TradeItemPresentationModel): Result<Unit> {
        val tradeId = value.tradeId
        val optionalChannel = channelService.findChannelByTradeId(tradeId)
        if (optionalChannel.isPresent) {
            val channel = optionalChannel.get()
            val mediator = channel.mediator
            if (mediator != null) {
                val encoded = Res.encode("bisqEasy.mediation.requester.tradeLogMessage", channel.myUserIdentity.userName)
                channelService.sendTradeLogMessage(encoded, channel).join()
                channel.setIsInMediation(true)
                val contract: BisqEasyContract = Mappings.BisqEasyContractMapping.toBisq2Model(value.bisqEasyTradeModel.contract)
                mediationRequestService.requestMediation(channel, contract)
                return Result.success(Unit)
            } else {
                return Result.failure(MediatorNotAvailableException("No mediator found"))
            }
        } else {
            return Result.failure(RuntimeException("No channel found for trade ID $tradeId"))
        }
    }
}