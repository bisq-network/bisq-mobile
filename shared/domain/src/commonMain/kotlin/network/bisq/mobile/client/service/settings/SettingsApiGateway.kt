package network.bisq.mobile.client.service.settings

import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.domain.data.replicated.settings.ApiVersionSettingsVO
import network.bisq.mobile.domain.data.replicated.settings.SettingsVO
import network.bisq.mobile.domain.utils.Logging

class SettingsApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
) : Logging {
    private val basePath = "settings"

    suspend fun getSettings(): Result<SettingsVO> {
        return webSocketApiClient.get(basePath)
    }

    suspend fun getApiVersion(): Result<ApiVersionSettingsVO> {
        return webSocketApiClient.get("$basePath/version")
    }

    suspend fun confirmTacAccepted(value: Boolean): Result<Unit> {
        val patch = webSocketApiClient.patch<Unit, SettingsChangeRequest>(
            basePath,
            SettingsChangeRequest(isTacAccepted = value)
        )
        return patch
    }

    suspend fun confirmTradeRules(value: Boolean): Result<Unit> {
        return webSocketApiClient.patch(
            basePath,
            SettingsChangeRequest(tradeRulesConfirmed = value)
        )
    }

    suspend fun setLanguageCode(value: String): Result<Unit> {
        return webSocketApiClient.patch(
            basePath,
            SettingsChangeRequest(languageCode = value)
        )
    }

    suspend fun setSupportedLanguageCodes(value: Set<String>): Result<Unit> {
        return webSocketApiClient.patch(
            basePath,
            SettingsChangeRequest(supportedLanguageCodes = value)
        )
    }

    suspend fun setCloseMyOfferWhenTaken(value: Boolean): Result<Unit> {
        return webSocketApiClient.patch(
            basePath,
            SettingsChangeRequest(closeMyOfferWhenTaken = value)
        )
    }

    suspend fun setMaxTradePriceDeviation(value: Double): Result<Unit> {
        return webSocketApiClient.patch(
            basePath,
            SettingsChangeRequest(maxTradePriceDeviation = value)
        )
    }

    suspend fun setNumDaysAfterRedactingTradeData(value: Int): Result<Unit> {
        return webSocketApiClient.patch(
            basePath,
            SettingsChangeRequest(numDaysAfterRedactingTradeData = value)
        )
    }

    suspend fun setUseAnimations(value: Boolean): Result<Unit> {
        return webSocketApiClient.patch(
            basePath,
            SettingsChangeRequest(useAnimations = value)
        )
    }

}

