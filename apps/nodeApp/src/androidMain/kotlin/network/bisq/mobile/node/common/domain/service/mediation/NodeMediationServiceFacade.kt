package network.bisq.mobile.node.common.domain.service.mediation

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService
import bisq.contract.bisq_easy.BisqEasyContract
import bisq.i18n.Res
import bisq.support.mediation.MediationRequestService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.mediation.MediationServiceFacade
import network.bisq.mobile.domain.service.offers.MediatorNotAvailableException
import network.bisq.mobile.node.common.domain.mapping.Mappings
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService

class NodeMediationServiceFacade(
    applicationService: AndroidApplicationService.Provider,
) : ServiceFacade(),
    MediationServiceFacade {
    // Dependencies
    private val channelService: BisqEasyOpenTradeChannelService by lazy { applicationService.chatService.get().bisqEasyOpenTradeChannelService }
    private val mediationRequestService: MediationRequestService by lazy { applicationService.supportService.get().mediationRequestService }

    override suspend fun activate() {
        super<ServiceFacade>.activate()
    }

    override suspend fun deactivate() {
        super<ServiceFacade>.deactivate()
    }

    override suspend fun reportToMediator(value: TradeItemPresentationModel): Result<Unit> =
        withContext(Dispatchers.IO) {
            val tradeId = value.tradeId
            val optionalChannel = channelService.findChannelByTradeId(tradeId)
            if (optionalChannel.isPresent) {
                val channel = optionalChannel.get()
                val mediator = channel.mediator
                if (mediator != null) {
                    val encoded =
                        Res.encode(
                            "bisqEasy.mediation.requester.tradeLogMessage",
                            channel.myUserIdentity.userName,
                        )
                    channelService.sendTradeLogMessage(encoded, channel).await()
                    channel.setIsInMediation(true)
                    val contract: BisqEasyContract =
                        Mappings.BisqEasyContractMapping.toBisq2Model(value.bisqEasyTradeModel.contract)
                    // requestMediation has synchronize call in confidentialSend's first ifPresent branch
                    mediationRequestService.requestMediation(channel, contract)
                    Result.success(Unit)
                } else {
                    Result.failure(MediatorNotAvailableException())
                }
            } else {
                Result.failure(RuntimeException("No channel found for trade ID $tradeId"))
            }
        }
}
