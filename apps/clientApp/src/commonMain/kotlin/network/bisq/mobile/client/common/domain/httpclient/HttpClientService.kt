package network.bisq.mobile.client.common.domain.httpclient

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.io.readByteArray
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.httpclient.exception.PasswordIncorrectOrMissingException
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.utils.createHttpClient
import network.bisq.mobile.crypto.getSha256
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
    private val jsonConfig: Json,
    private val versionProvider: VersionProvider,
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
                    _httpClient.value?.close()
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

    suspend fun get(block: HttpRequestBuilder.() -> Unit): HttpResponse =
        getClient().get {
            block(this)
        }

    suspend fun post(block: HttpRequestBuilder.() -> Unit): HttpResponse =
        getClient().post {
            block(this)
        }

    suspend fun patch(block: HttpRequestBuilder.() -> Unit): HttpResponse =
        getClient().patch {
            block(this)
        }

    suspend fun put(block: HttpRequestBuilder.() -> Unit): HttpResponse =
        getClient().put {
            block(this)
        }

    suspend fun delete(block: HttpRequestBuilder.() -> Unit): HttpResponse =
        getClient().delete {
            block(this)
        }

    fun createNewInstance(clientSettings: HttpClientSettings): HttpClient {
        val proxy = clientSettings.bisqProxyConfig()
        if (proxy != null) {
            log.d { "Using proxy from settings: $proxy" }
        }
        val rawBase =
            if (!clientSettings.apiUrl.isNullOrBlank()) {
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
            install(UserAgent) {
                agent =
                    versionProvider.getAppNameAndVersion(
                        false,
                        getPlatformInfo().type == PlatformType.IOS,
                    )
            }
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
            install(
                createClientPlugin("HttpApiAuthPlugin") {
                    transformRequestBody { request, content, bodyType ->
                        var reconstructedBody: ByteArrayContent? = null
                        if (!password.isNullOrBlank()) {
                            val method = request.method.value
                            val timestamp =
                                Clock.System
                                    .now()
                                    .toEpochMilliseconds()
                                    .toString()
                            val nonce = AuthUtils.generateNonce()
                            val normalizedPathAndQuery =
                                AuthUtils.getNormalizedPathAndQuery(request.url.build())
                            val bodySha256Hex =
                                when (content) {
                                    is OutgoingContent.ByteArrayContent -> {
                                        val bytes = content.bytes()
                                        if (bytes.size > MAX_BODY_SIZE_BYTES) {
                                            throw IllegalArgumentException("Request body exceeds maximum size of $MAX_BODY_SIZE_BYTES bytes")
                                        }
                                        getSha256(bytes).toHexString()
                                    }

                                    is OutgoingContent.ReadChannelContent -> {
                                        val bytes =
                                            content
                                                .readFrom()
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
                                                channel
                                                    .readRemaining(MAX_BODY_SIZE_BYTES + 1)
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

                                    is String -> {
                                        if (content.isEmpty()) {
                                            null
                                        } else {
                                            val bytes = content.encodeToByteArray()
                                            if (bytes.size > MAX_BODY_SIZE_BYTES) {
                                                throw IllegalArgumentException(
                                                    "Request body exceeds maximum size of $MAX_BODY_SIZE_BYTES bytes",
                                                )
                                            }
                                            getSha256(bytes).toHexString()
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
                        reconstructedBody ?: when (content) {
                            is OutgoingContent -> content
                            else -> null // transformation not applicable
                        }
                    }
                },
            )
        }
    }
}
