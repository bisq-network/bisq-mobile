package network.bisq.mobile.client.common.domain.service.settings

import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.data.replicated.settings.ApiVersionSettingsVO
import network.bisq.mobile.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * API Gateway for mobile app settings synchronized with the trusted node.
 * Settings are read and updated over the WebSocket API so the client stays aligned
 * with server-side defaults, UI preferences, and feature flags.
 *
 * - GET /settings - Fetch current settings ([SettingsVO])
 * - GET /settings/version - Fetch API version settings ([ApiVersionSettingsVO])
 * - PATCH /settings - Partial updates via [SettingsChangeRequest]
 *
 * Coverage exclusion rationale - [WebSocketApiClient] uses inline reified functions
 * (get<T>, patch<T, R>) which cannot be mocked in unit tests. Integration tests with
 * a real or fake HTTP server would be needed for proper coverage.
 */
@ExcludeFromCoverage
class SettingsApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
) : Logging {
    private val basePath = "settings"

    suspend fun getSettings(): Result<SettingsVO> = webSocketApiClient.get(basePath)

    suspend fun getApiVersion(): Result<ApiVersionSettingsVO> = webSocketApiClient.get("$basePath/version")

    suspend fun confirmTacAccepted(value: Boolean): Result<Unit> {
        val patch =
            webSocketApiClient.patch<Unit, SettingsChangeRequest>(
                basePath,
                SettingsChangeRequest(isTacAccepted = value),
            )
        return patch
    }

    suspend fun confirmTradeRules(value: Boolean): Result<Unit> =
        webSocketApiClient.patch(
            basePath,
            SettingsChangeRequest(tradeRulesConfirmed = value),
        )

    suspend fun setLanguageCode(value: String): Result<Unit> {
        log.i { "settings: setLanguageCode via API value=$value" }
        return webSocketApiClient.patch(
            basePath,
            SettingsChangeRequest(languageCode = value),
        )
    }

    suspend fun setSupportedLanguageCodes(value: Set<String>): Result<Unit> =
        webSocketApiClient.patch(
            basePath,
            SettingsChangeRequest(supportedLanguageCodes = value),
        )

    suspend fun setCloseMyOfferWhenTaken(value: Boolean): Result<Unit> =
        webSocketApiClient.patch(
            basePath,
            SettingsChangeRequest(closeMyOfferWhenTaken = value),
        )

    suspend fun setMaxTradePriceDeviation(value: Double): Result<Unit> =
        webSocketApiClient.patch(
            basePath,
            SettingsChangeRequest(maxTradePriceDeviation = value),
        )

    suspend fun setNumDaysAfterRedactingTradeData(value: Int): Result<Unit> =
        webSocketApiClient.patch(
            basePath,
            SettingsChangeRequest(numDaysAfterRedactingTradeData = value),
        )

    suspend fun setUseAnimations(value: Boolean): Result<Unit> =
        webSocketApiClient.patch(
            basePath,
            SettingsChangeRequest(useAnimations = value),
        )
}
