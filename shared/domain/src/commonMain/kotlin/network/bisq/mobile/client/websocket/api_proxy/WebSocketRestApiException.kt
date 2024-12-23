package network.bisq.mobile.client.websocket.api_proxy

import io.ktor.http.HttpStatusCode

class WebSocketRestApiException(val httpStatusCode: HttpStatusCode, val errorMessage: String) : Exception() {
}