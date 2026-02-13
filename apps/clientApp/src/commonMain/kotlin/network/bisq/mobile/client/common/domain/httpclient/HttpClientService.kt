package network.bisq.mobile.client.common.domain.httpclient

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.access.utils.Headers
import network.bisq.mobile.client.common.domain.httpclient.exception.UnauthorizedApiAccessException
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.utils.createHttpClient
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.domain.PlatformType
import network.bisq.mobile.domain.getPlatformInfo
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.domain.utils.awaitOrCancel
import kotlin.concurrent.Volatile

/**
 *  Listens to settings changes and creates a new httpclient accordingly
 *
 *  Calls made using this service will be suspended until a httpclient is instantiated
 */
class HttpClientService(
    private val kmpTorService: KmpTorService,
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    val json: Json,
    private val versionProvider: VersionProvider,
    private val defaultHost: String,
    private val defaultPort: Int,
) : ServiceFacade() {
    val apiPath = "/api/v1/"

    @Volatile
    private var lastConfig: HttpClientSettings? = null

    private var _httpClient: MutableStateFlow<HttpClient?> =
        MutableStateFlow(null)
    private val _httpClientChangedFlow =
        MutableSharedFlow<HttpClientSettings>(1)
    val httpClientChangedFlow get() = _httpClientChangedFlow.asSharedFlow()
    private val stopFlow =
        MutableSharedFlow<Unit>(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        ) // signal to cancel waiters

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun activate() {
        super.activate()

        stopFlow.resetReplayCache()

        serviceScope.launch(Dispatchers.Default) {
            getHttpClientSettingsFlow().collect { newConfig ->
                if (lastConfig != newConfig) {
                    lastConfig = newConfig
                    val oldClient = _httpClient.value
                    // Clear before closing so getClient() waits for the new client
                    // instead of returning the stale closed client
                    _httpClient.value = null
                    oldClient?.close()
                    _httpClient.value = createNewInstance(newConfig)
                    _httpClientChangedFlow.emit(newConfig)
                }
            }
        }
    }

    override suspend fun deactivate() {
        stopFlow.tryEmit(Unit)
        super.deactivate()

        disposeClient()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getHttpClientSettingsFlow(): Flow<HttpClientSettings> {
        // this allows waiting for kmp tor service to be initialized properly and listened to
        // if BisqProxyOption.INTERNAL_TOR proxy option is used
        return combine(
            sensitiveSettingsRepository.data,
            kmpTorService.state,
        ) { settings, state -> settings to state }
            .mapLatest { (settings, state) ->
                if (settings.selectedProxyOption == BisqProxyOption.INTERNAL_TOR) {
                    if (state is KmpTorService.TorState.Stopped) {
                        null
                    } else {
                        kmpTorService
                            .awaitSocksPort()
                            ?.let { HttpClientSettings.from(settings, it) }
                    }
                } else {
                    HttpClientSettings.from(settings, null)
                }
            }.filterNotNull()
    }

    /**
     * Do not use this to send requests. this is only intended to be used by web socket service
     * to instantiate it's websocket client.
     */
    suspend fun getClient(): HttpClient =
        awaitOrCancel(
            _httpClient.filterNotNull(),
            stopFlow,
        )

    fun disposeClient() {
        _httpClient.value?.close()
        _httpClient.value = null
        lastConfig = null
    }

    /**
     * Suspends until the HTTP client has been created/updated and is ready for use.
     * This should be called before making requests that depend on updated settings.
     * @param timeoutMs Maximum time to wait for the client to be ready (default 5000ms)
     * @return true if client is ready, false if timeout occurred
     */
    suspend fun awaitClientReady(timeoutMs: Long = 5000): Boolean =
        kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            httpClientChangedFlow.first()
            true
        } ?: false

    /**
     * Add authentication headers to the request if available
     */
    private fun HttpRequestBuilder.addAuthHeaders() {
        lastConfig?.sessionId?.let {
            header(Headers.SESSION_ID, it)
        }
        lastConfig?.clientId?.let {
            header(Headers.CLIENT_ID, it)
        }
    }

    /**
     * Redact sensitive fields from request body for logging.
     * Returns a summary with type name and field count, without exposing actual values.
     * Note: Must be internal (not private) to be accessible from inline functions.
     */
    @PublishedApi
    internal fun redactForLogging(body: Any?): String {
        if (body == null) return "null"
        val className = body::class.simpleName ?: "Unknown"
        return "[$className object]"
    }

    suspend fun get(block: HttpRequestBuilder.() -> Unit): HttpResponse =
        getClient().get {
            addAuthHeaders()
            block(this)
        }

    suspend inline fun <reified T, reified R> post(
        path: String,
        requestBody: R? = null,
        headers: Map<String, String> = emptyMap(),
    ): Result<T> {
        log.d { "HTTP POST to ${apiPath + path} with body: ${redactForLogging(requestBody)}" }
        try {
            val response: HttpResponse =
                post {
                    url {
                        path(apiPath + path)
                    }
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    if (requestBody != null) {
                        setBody(requestBody)
                    }

                    if (headers.isNotEmpty()) {
                        headers {
                            headers.forEach { (key, value) ->
                                append(key, value)
                            }
                        }
                    }
                }
            log.d { "HTTP POST done status=${response.status}" }
            return getResultFromHttpResponse<T>(response)
        } catch (e: Exception) {
            log.e(e) { "HTTP POST failed for ${apiPath + path}: ${e.message}" }
            return Result.failure(e)
        }
    }

    suspend fun post(block: HttpRequestBuilder.() -> Unit): HttpResponse =
        getClient().post {
            addAuthHeaders()
            block(this)
        }

    suspend inline fun <reified T, reified R> patch(
        path: String,
        requestBody: R,
    ): Result<T> {
        log.d { "HTTP PATCH to ${apiPath + path} with body: ${redactForLogging(requestBody)}" }
        try {
            val response: HttpResponse =
                patch {
                    url {
                        path(apiPath + path)
                    }
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(requestBody)
                }
            log.d { "HTTP PATCH done status=${response.status}" }
            return getResultFromHttpResponse<T>(response)
        } catch (e: Exception) {
            log.e(e) { "HTTP PATCH failed for ${apiPath + path}: ${e.message}" }
            return Result.failure(e)
        }
    }

    suspend fun patch(block: HttpRequestBuilder.() -> Unit): HttpResponse =
        getClient().patch {
            addAuthHeaders()
            block(this)
        }

    suspend inline fun <reified T, reified R> put(
        path: String,
        requestBody: R,
    ): Result<T> {
        log.d { "HTTP PUT to ${apiPath + path} with body: ${redactForLogging(requestBody)}" }
        try {
            val response: HttpResponse =
                put {
                    url {
                        path(apiPath + path)
                    }
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(requestBody)
                }
            log.d { "HTTP PUT done status=${response.status}" }
            return getResultFromHttpResponse<T>(response)
        } catch (e: Exception) {
            log.e(e) { "HTTP PUT failed for " + (apiPath + path) + ": ${e.message}" }
            return Result.failure(e)
        }
    }

    suspend fun put(block: HttpRequestBuilder.() -> Unit): HttpResponse =
        getClient().put {
            addAuthHeaders()
            block(this)
        }

    suspend fun delete(block: HttpRequestBuilder.() -> Unit): HttpResponse =
        getClient().delete {
            addAuthHeaders()
            block(this)
        }

    fun createNewInstance(httpClientSettings: HttpClientSettings): HttpClient {
        val proxy = httpClientSettings.bisqProxyConfig()
        if (proxy != null) {
            log.d { "Using proxy from settings: $proxy" }
        }
        val rawBase =
            if (!httpClientSettings.bisqApiUrl.isNullOrBlank()) {
                httpClientSettings.bisqApiUrl
            } else {
                "http://$defaultHost:$defaultPort"
            }

        val baseUrl = sanitizeBaseUrl(rawBase, defaultPort)
        if (baseUrl != rawBase) {
            log.w { "Sanitized baseUrl from '$rawBase' to '$baseUrl'" }
        }
        log.d { "HttpClient baseUrl set to $baseUrl" }

        val host =
            if (httpClientSettings.bisqApiUrl == null) {
                defaultHost
            } else {
                Url(httpClientSettings.bisqApiUrl).host
            }
        val tlsFingerprint = httpClientSettings.tlsFingerprint
        val sessionId = httpClientSettings.sessionId
        val clientId = httpClientSettings.clientId
        return createHttpClient(
            host = host,
            tlsFingerprint = tlsFingerprint,
            proxyConfig = proxy,
        ) {
            install(UserAgent) {
                agent =
                    versionProvider.getAppNameAndVersion(
                        false,
                        getPlatformInfo().type == PlatformType.IOS,
                    )
            }
            install(ContentNegotiation) {
                json(json)
            }
            if (proxy?.isTorProxy == true) {
                // we set these to very high values here but we use more limited timeouts
                // when trying connection in services
                install(HttpTimeout) {
                    requestTimeoutMillis = 180_000
                    connectTimeoutMillis =
                        120_000 // not supported by Darwin engine
                    socketTimeoutMillis =
                        120_000 // not supported by Curl engine
                }
            }
            defaultRequest {
                url(baseUrl)
            }
            HttpResponseValidator {
                validateResponse { response ->
                    if (response.status == HttpStatusCode.Unauthorized) {
                        throw UnauthorizedApiAccessException()
                    }
                }
            }
        }
    }

    suspend inline fun <reified T> getResultFromHttpResponse(response: HttpResponse): Result<T> =
        if (response.status.isSuccess()) {
            if (response.status == HttpStatusCode.NoContent) {
                try {
                    check(T::class == Unit::class) { "If we get a HttpStatusCode.NoContent response we expect return type Unit" }
                    Result.success(Unit as T)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            } else {
                Result.success(response.body<T>())
            }
        } else {
            val errorText = response.bodyAsText()
            Result.failure(
                WebSocketRestApiException(
                    response.status,
                    errorText,
                ),
            )
        }
}
