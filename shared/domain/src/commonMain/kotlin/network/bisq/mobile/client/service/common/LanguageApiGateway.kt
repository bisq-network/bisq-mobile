package network.bisq.mobile.client.service.common


import network.bisq.mobile.client.websocket.WebSocketClient
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.client.websocket.api_proxy.WebSocketApiClient
import network.bisq.mobile.client.websocket.subscription.Topic
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.domain.utils.Logging

class LanguageApiGateway(
    private val webSocketApiClient: WebSocketApiClient,
    private val webSocketClientProvider: WebSocketClientProvider,
) : Logging {

    suspend fun subscribeI18NCodes(languageCode: String = "en"): WebSocketEventObserver {
        return webSocketClientProvider.get().subscribe(Topic.I18N_PAIRS, languageCode)
    }

    suspend fun subscribeAllLanguageCodes(languageCode: String = "en"): WebSocketEventObserver {
        return webSocketClientProvider.get().subscribe(Topic.LANGUAGE_PAIRS, languageCode)
    }

    suspend fun syncI18NCodes(subscriberId: String, languageCode: String = "en") {
        return webSocketClientProvider.get().publicSend(subscriberId, Topic.I18N_PAIRS, languageCode)
    }

    suspend fun syncAllLanguageCodes(subscriberId: String, languageCode: String = "en") {
        return webSocketClientProvider.get().publicSend(subscriberId, Topic.LANGUAGE_PAIRS, languageCode)
    }
}
