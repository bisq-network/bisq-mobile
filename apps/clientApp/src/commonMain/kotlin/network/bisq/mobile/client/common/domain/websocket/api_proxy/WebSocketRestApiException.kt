package network.bisq.mobile.client.common.domain.websocket.api_proxy

import io.ktor.http.HttpStatusCode

class WebSocketRestApiException(val httpStatusCode: HttpStatusCode, message: String) : Exception(message) {
}