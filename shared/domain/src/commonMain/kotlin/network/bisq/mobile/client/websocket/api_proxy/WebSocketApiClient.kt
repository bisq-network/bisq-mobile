package network.bisq.mobile.client.websocket.api_proxy

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.service.offer.CreateOfferRequest
import network.bisq.mobile.client.websocket.WebSocketClient
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.domain.utils.Logging
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class WebSocketApiClient(
    val httpClient: HttpClient,
    val webSocketClient: WebSocketClient,
    val json: Json,
    host: String,
    port: Int
) : Logging {
    val apiPath = "/api/v1/"
    var apiUrl = "http://$host:$port$apiPath"

    // POST request still not working, but issue is likely on the bisq2 side.
    // So we use httpClient instead.
    val useHttpClientForPost = true

    suspend inline fun <reified T> get(path: String): Result<T> {
        return request<T>("GET", path)
    }

    suspend inline fun <reified T, reified R> post(path: String, requestBody: R): Result<T> {
        if (requestBody is CreateOfferRequest) {
            val jsonString = json.encodeToString(CreateOfferRequest.serializer(), requestBody)
            println(jsonString)
        }
        if (useHttpClientForPost) {
            try {
                val response: HttpResponse = httpClient.post(apiUrl + path) {
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(requestBody)
                }
                return Result.success(response.body<T>())
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
            val response = webSocketClient.sendRequestAndAwaitResponse(webSocketRestApiRequest)
            require(response is WebSocketRestApiResponse) { "Response not of expected type. response=$response" }
            val body = response.body
            if (response.isSuccess()) {
                val decodeFromString = json.decodeFromString<T>(body)
                return Result.success(decodeFromString)
            } else {
                return Result.failure(WebSocketRestApiException(response.httpStatusCode, response.body))
            }
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}