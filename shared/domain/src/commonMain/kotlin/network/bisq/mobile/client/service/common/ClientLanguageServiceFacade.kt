package network.bisq.mobile.client.service.common

import io.ktor.util.collections.ConcurrentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.service.market.MarketPriceApiGateway
import network.bisq.mobile.client.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.formatters.MarketPriceFormatter
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientLanguageServiceFacade(
    private val apiGateway: LanguageApiGateway,
    private val json: Json
) : LanguageServiceFacade, Logging {

    // Properties
    private val _i18nPairs: MutableStateFlow<List<Pair<String, String>>> = MutableStateFlow(emptyList())
    override val i18nPairs: StateFlow<List<Pair<String, String>>> = _i18nPairs

    private val _allPairs: MutableStateFlow<List<Pair<String, String>>> = MutableStateFlow(emptyList())
    override val allPairs: StateFlow<List<Pair<String, String>>> = _allPairs

    private val _defaultLanguage: MutableStateFlow<String> = MutableStateFlow("en")
    private val _i18nObserver: MutableStateFlow<WebSocketEventObserver?> = MutableStateFlow(null)
    private val _allPairsObserver: MutableStateFlow<WebSocketEventObserver?> = MutableStateFlow(null)

    override fun setDefaultLanguage(languageCode: String) {
        _defaultLanguage.value = languageCode
    }

    // Misc
    private val coroutineScope = CoroutineScope(BackgroundDispatcher)
    private var job: Job? = null

    // Life cycle
    override fun activate() {
        job = coroutineScope.launch {
            launch {
                if (_i18nObserver.value == null) {
                    _i18nObserver.value = apiGateway.subscribeI18NCodes(_defaultLanguage.value)
                }
                _i18nObserver.value!!.webSocketEvent.collect{ webSocketEvent ->
                    try {
                        if (webSocketEvent?.deferredPayload == null) {
                            return@collect
                        }
                        val webSocketEventPayload: WebSocketEventPayload<Map<String, String>> =
                            WebSocketEventPayload.from(json, webSocketEvent)
                        val response = webSocketEventPayload.payload
                        println(response)
                        _i18nPairs.value = response.toList()
                    } catch (e: Exception) {
                        log.e(e.toString(), e)
                    }
                }
            }

            launch {
                if (_allPairsObserver.value == null) {
                    _allPairsObserver.value = apiGateway.subscribeAllLanguageCodes(_defaultLanguage.value)
                }
                _allPairsObserver.value!!.webSocketEvent.collect{ webSocketEvent ->
                    try {
                        if (webSocketEvent?.deferredPayload == null) {
                            return@collect
                        }
                        val webSocketEventPayload: WebSocketEventPayload<Map<String, String>> =
                            WebSocketEventPayload.from(json, webSocketEvent)
                        val response = webSocketEventPayload.payload
                        _allPairs.value = response.toList()
                    } catch (e: Exception) {
                        log.e(e.toString(), e)
                    }
                }
            }

        }
    }

    override suspend fun sync() {
        val subscriberId = _i18nObserver.value?.webSocketEvent?.value?.subscriberId ?: ""
        apiGateway.syncI18NCodes(subscriberId, _defaultLanguage.value)
        apiGateway.syncAllLanguageCodes(subscriberId, _defaultLanguage.value)
    }

    override fun deactivate() {
        cancelJob()
    }

    private fun cancelJob() {
        job?.cancel()
        job = null
    }
}