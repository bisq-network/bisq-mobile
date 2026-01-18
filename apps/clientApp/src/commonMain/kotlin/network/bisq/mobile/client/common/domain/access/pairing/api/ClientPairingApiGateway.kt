package network.bisq.mobile.client.common.domain.access.pairing.api

import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.path
import network.bisq.mobile.client.common.domain.access.pairing.api.dto.PairingRequestDto
import network.bisq.mobile.client.common.domain.access.pairing.api.dto.PairingResponseDto
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.websocket.api_proxy.WebSocketRestApiException
import network.bisq.mobile.domain.utils.Logging

class ClientPairingApiGateway(
    private val httpClientService: HttpClientService,
) : Logging {
    private val path = "/api/v1/pairing"

    suspend fun requestPairing(request: PairingRequestDto): Result<PairingResponseDto> {
        log.i { "HTTP POST to $path" }
        log.i { "Request body: $request" }
        try {
            val response: HttpResponse =
                httpClientService.post {
                    url {
                        path(path)
                    }
                    contentType(ContentType.Application.Json)
                    accept(ContentType.Application.Json)
                    setBody(request)
                }
            log.i { "HTTP POST done status=${response.status}" }
            return getResultFromHttpResponse<PairingResponseDto>(response)
        } catch (e: Exception) {
            log.e(e) { "HTTP POST failed for $path: ${e.message}" }
            return Result.failure(e)
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
                    errorText
                )
            )
        }
}
