package network.bisq.mobile.client.service.common

import io.ktor.util.collections.ConcurrentMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.service.market.MarketPriceApiGateway
import network.bisq.mobile.client.websocket.subscription.WebSocketEventPayload
import network.bisq.mobile.domain.data.BackgroundDispatcher
import network.bisq.mobile.domain.data.model.MarketPriceItem
import network.bisq.mobile.domain.formatters.MarketPriceFormatter
import network.bisq.mobile.domain.service.common.LanguageServiceFacade
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.utils.Logging

class ClientLanguageServiceFacade(
    private val apiGateway: MarketPriceApiGateway, // TODO: Later change it to LanguageService specific API Gateway
    private val json: Json
) : LanguageServiceFacade, Logging {

    // Properties
    private val _i18nCodes: MutableStateFlow<List<String>> = MutableStateFlow(emptyList())
    override val i18nCodes: StateFlow<List<String>> get() = _i18nCodes

    // Misc
    private val coroutineScope = CoroutineScope(BackgroundDispatcher)
    private var job: Job? = null

    // Life cycle
    override fun activate() {
        job = coroutineScope.launch {
            // TODO:Mock
            _i18nCodes.value = listOf("en", "de", "es")

            // TODO: Fetch from API Gateway/WS
        }
    }

    override fun deactivate() {
        cancelJob()
    }

    private fun cancelJob() {
        job?.cancel()
        job = null
    }
}