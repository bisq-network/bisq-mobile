package network.bisq.mobile.client.common.domain.websocket.api_proxy

import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.httpclient.exception.UnauthorizedApiAccessException
import network.bisq.mobile.client.common.domain.service.network.ClientConnectivityService
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiRequest
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiResponse
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.domain.utils.Logging
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class WebSocketApiClient(
    val webSocketClientService: WebSocketClientService,
    val json: Json,
) : Logging {
    val apiPath = "/api/v1/"

    suspend inline fun <reified T> get(path: String): Result<T> = request<T>("GET", path)

    suspend inline fun <reified T> getNullable(path: String): Result<T?> = requestNullable<T>("GET", path)

    suspend inline fun <reified T> delete(path: String): Result<T> = request<T>("DELETE", path)

    suspend inline fun <reified T, reified R> delete(
        path: String,
        requestBody: R,
    ): Result<T> {
        val bodyAsJson = json.encodeToString(requestBody)
        return request<T>("DELETE", path, bodyAsJson)
    }

    suspend inline fun <reified T> put(path: String): Result<T> = request<T>("PUT", path)

    suspend inline fun <reified T, reified R> put(
        path: String,
        requestBody: R,
    ): Result<T> {
        val bodyAsJson = json.encodeToString(requestBody)
        return request<T>("PUT", path, bodyAsJson)
    }

    suspend inline fun <reified T> patch(path: String): Result<T> = request<T>("PATCH", path)

    suspend inline fun <reified T, reified R> patch(
        path: String,
        requestBody: R,
    ): Result<T> {
        val bodyAsJson = json.encodeToString(requestBody)
        log.d { "WS PATCH to ${apiPath + path}" }
        return request<T>("PATCH", path, bodyAsJson)
    }

    suspend inline fun <reified T, reified R> post(
        path: String,
        requestBody: R,
    ): Result<T> {
        val bodyAsJson = json.encodeToString(requestBody)
        return request<T>("POST", path, bodyAsJson)
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend inline fun <reified T> request(
        method: String,
        path: String,
        bodyAsJson: String = "",
    ): Result<T> =
        executeRequest<T, T>(method, path, bodyAsJson) {
            check(T::class == Unit::class) { "If we get a HttpStatusCode.NoContent response we expect return type Unit" }
            Result.success(Unit as T)
        }

    @OptIn(ExperimentalUuidApi::class)
    suspend inline fun <reified T> requestNullable(
        method: String,
        path: String,
        bodyAsJson: String = "",
    ): Result<T?> =
        executeRequest<T, T?>(method, path, bodyAsJson) {
            // For nullable requests, 204 No Content means null result
            Result.success(null)
        }

    @PublishedApi
    @OptIn(ExperimentalUuidApi::class)
    internal suspend inline fun <reified T, R> executeRequest(
        method: String,
        path: String,
        bodyAsJson: String = "",
        handleNoContent: () -> Result<R>,
    ): Result<R> {
        val requestId = Uuid.random().toString()
        val fullPath = apiPath + path
        val webSocketRestApiRequest =
            WebSocketRestApiRequest(
                requestId,
                method,
                fullPath,
                bodyAsJson,
            )
        try {
            val startTime = DateUtils.now()
            val response =
                webSocketClientService.sendRequestAndAwaitResponse(
                    webSocketRestApiRequest,
                )
            ClientConnectivityService.newRequestRoundTripTime(DateUtils.now() - startTime)
            require(response is WebSocketRestApiResponse) { "Response not of expected type. response=$response" }
            val body = response.body
            if (response.isSuccess()) {
                if (response.httpStatusCode == HttpStatusCode.NoContent) {
                    return handleNoContent()
                } else {
                    val decodeFromString = json.decodeFromString<T>(body)
                    @Suppress("UNCHECKED_CAST")
                    return Result.success(decodeFromString as R)
                }
            } else if (response.httpStatusCode == HttpStatusCode.Unauthorized) {
                return Result.failure(UnauthorizedApiAccessException())
            } else {
                val trimmed = body.trimStart()
                if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                    // TODO Not sure if we expect any json error messages.
                    return try {
                        val asMap =
                            json.decodeFromString<Map<String, String>>(trimmed)
                        val errorMessage =
                            asMap["error"]
                                ?: body
                        Result.failure(
                            WebSocketRestApiException(
                                response.httpStatusCode,
                                errorMessage,
                            ),
                        )
                    } catch (e: Exception) {
                        log.e(e) { "Failed to decode error message as json. body=$body" }
                        Result.failure(
                            WebSocketRestApiException(
                                response.httpStatusCode,
                                body,
                            ),
                        )
                    }
                } else {
                    return Result.failure(
                        WebSocketRestApiException(
                            response.httpStatusCode,
                            body,
                        ),
                    )
                }
            }
        } catch (e: CancellationException) {
            // no log on cancellation
            return Result.failure(e)
        } catch (e: Exception) {
            log.e(e) { "Failed to get WS request result: ${e.message}" }
            return Result.failure(e)
        }
    }
}
