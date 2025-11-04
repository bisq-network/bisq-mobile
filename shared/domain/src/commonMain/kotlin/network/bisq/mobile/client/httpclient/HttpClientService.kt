package network.bisq.mobile.client.httpclient

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.httpclient.exception.PasswordIncorrectOrMissingException
import network.bisq.mobile.crypto.getSha256
import network.bisq.mobile.domain.createHttpClient
import network.bisq.mobile.domain.data.repository.SensitiveSettingsRepository
import network.bisq.mobile.domain.service.ServiceFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import kotlin.concurrent.Volatile

/**
 *  Listens to settings changes and creates a new httpclient accordingly
 *
 *  Calls made using this service will be suspended until a httpclient is instantiated
 */
class HttpClientService(
    private val kmpTorService: KmpTorService,
    private val sensitiveSettingsRepository: SensitiveSettingsRepository,
    private val jsonConfig: Json,
    private val defaultHost: String,
    private val defaultPort: Int,
) : ServiceFacade() {

    companion object {
        private const val MAX_BODY_SIZE_BYTES: Long = 5 * 1024 * 1024 // 5 MB limit
    }

    @Volatile
    private var lastConfig: HttpClientSettings? = null

    private var _httpClient: MutableStateFlow<HttpClient?> = MutableStateFlow(null)
    private val _httpClientChangedFlow = MutableSharedFlow<HttpClientSettings>(1)
    val httpClientChangedFlow get() = _httpClientChangedFlow.asSharedFlow()

    override fun activate() {
        super.activate()

        collectIO(sensitiveSettingsRepository.data) {
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
        val rawBase = if (!clientSettings.apiUrl.isNullOrBlank()) {
            clientSettings.apiUrl
        } else {
            "http://$defaultHost:$defaultPort"
        }
        val baseUrl = sanitizeBaseUrl(rawBase, defaultPort)
        if (baseUrl != rawBase) {
            log.w { "Sanitized baseUrl from '$rawBase' to '$baseUrl'" }
        }
        log.d { "HttpClient baseUrl set to $baseUrl" }
        val password = clientSettings.password
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
                url(baseUrl)
            }
            HttpResponseValidator {
                validateResponse { response ->
                    if (response.status == HttpStatusCode.Unauthorized) {
                        throw PasswordIncorrectOrMissingException()
                    }
                }
            }
            install(createClientPlugin("HttpApiAuthPlugin") {
                transformRequestBody { request, content, bodyType ->
                    var reconstructedBody: ByteArrayContent? = null
                    if (!password.isNullOrBlank()) {
                        val method = request.method.value
                        val timestamp = Clock.System.now().toEpochMilliseconds().toString()
                        val nonce = AuthUtils.generateNonce()
                        val normalizedPathAndQuery = AuthUtils.getNormalizedPathAndQuery(request.url.build())
                        val bodySha256Hex = when (content) {
                            is OutgoingContent.ByteArrayContent -> {
                                val bytes = content.bytes()
                                if (bytes.size > MAX_BODY_SIZE_BYTES) {
                                    throw IllegalArgumentException("Request body exceeds maximum size of $MAX_BODY_SIZE_BYTES bytes")
                                }
                                getSha256(bytes).toHexString()
                            }

                            is OutgoingContent.ReadChannelContent -> {
                                val bytes = content.readFrom()
                                    .readRemaining(MAX_BODY_SIZE_BYTES + 1) // + 1 to detect if max size has reached
                                    .readByteArray()
                                if (bytes.size > MAX_BODY_SIZE_BYTES) {
                                    throw IllegalArgumentException("Request body exceeds maximum size of $MAX_BODY_SIZE_BYTES bytes")
                                }
                                reconstructedBody =
                                    ByteArrayContent(bytes, content.contentType, content.status)
                                getSha256(bytes).toHexString()
                            }

                            is OutgoingContent.WriteChannelContent -> {
                                val channel = ByteChannel(autoFlush = true)
                                try {
                                    content.writeTo(channel)
                                    val bytes =
                                        channel.readRemaining(MAX_BODY_SIZE_BYTES + 1)
                                            .readByteArray()
                                    if (bytes.size > MAX_BODY_SIZE_BYTES) {
                                        throw IllegalArgumentException("Request body exceeds maximum size of $MAX_BODY_SIZE_BYTES bytes")
                                    }
                                    reconstructedBody =
                                        ByteArrayContent(bytes, content.contentType, content.status)
                                    getSha256(bytes).toHexString()
                                } finally {
                                    channel.close()
                                }
                            }

                            else -> null
                        }

                        val hash =
                            AuthUtils.generateAuthHash(
                                password,
                                nonce,
                                timestamp,
                                method,
                                normalizedPathAndQuery,
                                bodySha256Hex,
                            )

                        request.headers.append("AUTH-TOKEN", hash)
                        request.headers.append("AUTH-TS", timestamp)
                        request.headers.append("AUTH-NONCE", nonce)
                    }
                    reconstructedBody ?: content as OutgoingContent?
                }
            })
        }
    }
}