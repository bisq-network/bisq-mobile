package network.bisq.mobile.client.websocket.messages

import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("WebSocketRestApiResponse")
data class WebSocketRestApiResponse(
    override val requestId: String,
    val statusCode: Int, // Http Status code
    val body: String // In error case the error message
) : WebSocketResponse {
    val httpStatusCode: HttpStatusCode get() = HttpStatusCode.fromValue(statusCode)
    fun isSuccess(): Boolean {
        return httpStatusCode.isSuccess()
    }
}

