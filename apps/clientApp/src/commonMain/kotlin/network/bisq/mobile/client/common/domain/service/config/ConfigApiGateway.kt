package network.bisq.mobile.client.common.domain.service.config

import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.data.replicated.config.TradeAmountLimitsVO
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * API Gateway for static bisq2 config served by the trusted node's `/config` endpoint.
 *
 * - GET /config/trade-amount-limits — [TradeAmountLimitsVO]
 * - GET /config/capabilities — [ApiCapabilitiesDto]
 *
 * Older nodes predate this endpoint; the caller degrades gracefully on failure (see
 * [ClientConfigServiceFacade]), so this gateway only forwards the request.
 *
 * Coverage exclusion rationale — [WebSocketApiClient.get] is an inline reified function that
 * cannot be mocked in unit tests; the facade is covered with a fake gateway instead.
 */
@ExcludeFromCoverage
class ConfigApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
) : Logging {
    private val basePath = "config"

    suspend fun getTradeAmountLimits(): Result<TradeAmountLimitsVO> = webSocketApiClient.get("$basePath/trade-amount-limits")

    suspend fun getCapabilities(): Result<ApiCapabilitiesDto> = webSocketApiClient.get("$basePath/capabilities")
}
