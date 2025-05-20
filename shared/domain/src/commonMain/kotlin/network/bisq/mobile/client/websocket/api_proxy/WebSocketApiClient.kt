package network.bisq.mobile.client.websocket.api_proxy

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.websocket.WebSocketClientProvider
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.domain.service.network.ConnectivityService
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.domain.utils.Logging
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class WebSocketApiClient(
    val httpClient: HttpClient,
    val webSocketClientProvider: WebSocketClientProvider,
    val json: Json,
    private val defaultHost: String,
    private val defaultPort: Int
) : Logging {
    val apiPath = "/api/v1/"

    // POST and PATCH request are not working yes on the backend.
    // So we use httpClient instead.
    val useHttpClient = true

    suspend inline fun <reified T> get(path: String): Result<T> {
        return request<T>("GET", path)
    }

    suspend inline fun <reified T> delete(path: String): Result<T> {
        return request<T>("DELETE", path)
    }

    suspend inline fun <reified T> put(path: String): Result<T> {
        return request<T>("PUT", path)
    }

    suspend inline fun <reified T, reified R> put(path: String, requestBody: R): Result<T> {
        val bodyAsJson = json.encodeToString(requestBody)
        return request<T>("PUT", path, bodyAsJson)
    }

    suspend inline fun <reified T> patch(path: String): Result<T> {
        return request<T>("PATCH", path)
    }

    suspend inline fun <reified T, reified R> patch(path: String, requestBody: R): Result<T> {
        if (useHttpClient) {
            try {
                val apiUrl = currentApiUrl()
                val response: HttpResponse = httpClient.patch(apiUrl + path) {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(requestBody)
                }
                return getResultFromHttpResponse<T>(response)
            } catch (e: Exception) {
                return Result.failure(e)
            }
        } else {
            val bodyAsJson = json.encodeToString(requestBody)
            return request<T>("PATCH", path, bodyAsJson)
        }
    }

    suspend inline fun <reified T, reified R> post(path: String, requestBody: R): Result<T> {
        if (useHttpClient) {
            try {
                val apiUrl = currentApiUrl()
                val response: HttpResponse = httpClient.post(apiUrl + path) {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(requestBody)
                }
                return getResultFromHttpResponse<T>(response)
            } catch (e: Exception) {
                return Result.failure(e)
            }
        } else {
            val bodyAsJson = json.encodeToString(requestBody)
            return request<T>("POST", path, bodyAsJson)
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend inline fun <reified T> request(
        method: String,
        path: String,
        bodyAsJson: String = "",
    ): Result<T> {
        val requestId = Uuid.random().toString()
        val fullPath = apiPath + path
        val webSocketRestApiRequest = WebSocketRestApiRequest(
            requestId,
            method,
            fullPath,
            bodyAsJson
        )
        try {
            val startTime = DateUtils.now()
            val response = webSocketClientProvider.get().sendRequestAndAwaitResponse(webSocketRestApiRequest)
            ConnectivityService.newRequestRoundTripTime(DateUtils.now() - startTime)
            require(response is WebSocketRestApiResponse) { "Response not of expected type. response=$response" }
            val body = response.body
            if (response.isSuccess()) {
                if (response.httpStatusCode == HttpStatusCode.NoContent) {
                    check(T::class == Unit::class) { "If we get a HttpStatusCode.NoContent response we expect return type Unit" }
                    return Result.success(Unit as T)
                } else {
                    val decodeFromString = json.decodeFromString<T>(body)
                    return Result.success(decodeFromString)
                }
            } else {
                val decodeFromString = json.decodeFromString<Map<String, String>>(body)
                try {
                    val errorMessage = decodeFromString["error"]!!
                    return Result.failure(WebSocketRestApiException(response.httpStatusCode, errorMessage))
                } catch (e: Exception) {
                    return Result.failure(WebSocketRestApiException(response.httpStatusCode, body))
                }
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to get WS request result" }
            return Result.failure(e)
        }
    }


    suspend inline fun <reified T> getResultFromHttpResponse(response: HttpResponse): Result<T> {
        return if (response.status.isSuccess()) {
            Result.success(response.body<T>())
        } else {
            val errorText = response.bodyAsText()
            Result.failure(WebSocketRestApiException(response.status, errorText))
        }
    }

    fun currentApiUrl(): String {
        val wsClient = webSocketClientProvider.get()
//        var defaultApiUrl = "http://$defaultHost:$defaultPort$apiPath"
        val apiURL = "http://${wsClient.host}:${wsClient.port}$apiPath"
        return apiURL
    }
}