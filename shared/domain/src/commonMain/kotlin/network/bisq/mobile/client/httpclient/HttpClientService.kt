package network.bisq.mobile.client.httpclient


import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import network.bisq.mobile.domain.createHttpClient
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.network.KmpTorService

/**
 *  Listens to settings changes and creates a new httpclient accordingly
 *
 *  Calls made using this service will be suspended until a httpclient is instantiated
 */
class HttpClientService(
    private val kmpTorService: KmpTorService,
    private val settingsRepository: SettingsRepository,
    private val jsonConfig: Json,
    private val defaultHost: String,
    private val defaultPort: Int,
) : ServiceFacade() {
    private var lastConfig: HttpClientSettings? = null

    private var _httpClient: MutableStateFlow<HttpClient?> = MutableStateFlow(null)
    private val _httpClientChangedFlow = MutableSharedFlow<HttpClientSettings>(1)
    val httpClientChangedFlow get() = _httpClientChangedFlow.asSharedFlow()

    override fun activate() {
        super.activate()

        collectIO(settingsRepository.data) {
            val newConfig = HttpClientSettings.from(it, kmpTorService)
            if (lastConfig != newConfig) {
                lastConfig = newConfig
                _httpClient.value?.close()
                _httpClient.value = createNewInstance(newConfig)
                _httpClientChangedFlow.emit(newConfig)
            }
        }
    }

    override fun deactivate() {
        super.deactivate()

        disposeClient()
    }

    /**
     * Do not use this to send requests. this is only intended to be used by web socket service
     * to instantiate it's websocket client.
     */
    suspend fun getClient(): HttpClient {
        return withContext(serviceScope.coroutineContext) {
            _httpClient.filterNotNull().first()
        }
    }

    fun disposeClient() {
        _httpClient.value?.close()
        _httpClient.value = null
        lastConfig = null
    }

    suspend fun get(block: HttpRequestBuilder.() -> Unit): HttpResponse {
        return getClient().get {
            block(this)
        }
    }

    suspend fun post(block: HttpRequestBuilder.() -> Unit): HttpResponse {
        return getClient().post {
            block(this)
        }
    }

    suspend fun patch(block: HttpRequestBuilder.() -> Unit): HttpResponse {
        return getClient().patch {
            block(this)
        }
    }

    suspend fun delete(block: HttpRequestBuilder.() -> Unit): HttpResponse {
        return getClient().delete {
            block(this)
        }
    }

    fun createNewInstance(clientSettings: HttpClientSettings): HttpClient {
        val proxy = clientSettings.bisqProxyConfig()
        if (proxy != null) {
            log.d { "Using proxy from settings: $proxy" }
        }
        val apiUrl = if (!clientSettings.apiUrl.isNullOrBlank()) {
            clientSettings.apiUrl
        } else {
            "http://$defaultHost:$defaultPort"
        }
        return createHttpClient(proxy) {
            install(WebSockets)
            install(ContentNegotiation) {
                json(jsonConfig)
            }
            if (proxy?.isTorProxy == true) {
                // we set these to very high values here but we use more limited timeouts
                // when trying connection in services
                install(HttpTimeout) {
                    requestTimeoutMillis = 180_000
                    connectTimeoutMillis = 120_000 // not supported by Darwin engine
                    socketTimeoutMillis = 120_000 // not supported by Curl engine
                }
            }
            defaultRequest {
                url(apiUrl)
            }
        }
    }
}