package network.bisq.mobile.client.websocket.api_proxy

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.websocket.WebSocketClient
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.utils.Logging
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

    suspend inline fun <reified T> get(path: String): T {
        return request<T>("GET", path)
    }

    suspend inline fun <reified T, reified R> post(path: String, requestBody: R): T {
        if (useHttpClientForPost) {
            return httpClient.post(apiUrl + path) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }.body<T>()

        } else {
            val bodyAsJson = json.encodeToString(requestBody)
            val request = request<T>("POST", path, bodyAsJson)
            return request
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend inline fun <reified T> request(
        method: String,
        path: String,
        bodyAsJson: String = "",
    ): T {
        val requestId = Uuid.random().toString()
        val fullPath = apiPath + path
        val responseClassName = WebSocketRestApiResponse::class.qualifiedName!!
        val webSocketRestApiRequest = WebSocketRestApiRequest(
            responseClassName,
            requestId,
            method,
            fullPath,
            bodyAsJson
        )
        val response = webSocketClient.sendRequestAndAwaitResponse(webSocketRestApiRequest)
        require(response is WebSocketRestApiResponse) { "Response not of expected type. response=$response" }
        val body = response.body
        val decodeFromString = json.decodeFromString<T>(body)
        return decodeFromString
    }
}