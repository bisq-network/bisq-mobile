package network.bisq.mobile.client.common.domain.access.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketMessage
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiRequest

/**
 * Redacts sensitive auth headers for log output only. Wire payloads must stay unchanged.
 */
object HeaderRedaction {
    const val REDACTED = "***"

    private val sensitiveHeaderNames = setOf(Headers.CLIENT_ID, Headers.SESSION_ID)

    private val lenientJson = Json { ignoreUnknownKeys = true }

    fun redactSensitiveHeaders(headers: Map<String, String>): Map<String, String> =
        headers.mapValues { (name, value) ->
            if (name in sensitiveHeaderNames) REDACTED else value
        }

    fun redactForLogging(message: WebSocketMessage): String =
        when (message) {
            is WebSocketRestApiRequest ->
                message.copy(headers = redactSensitiveHeaders(message.headers)).toString()
            else -> message.toString()
        }

    fun redactRawJsonForLogging(jsonString: String): String {
        return try {
            val element = lenientJson.parseToJsonElement(jsonString)
            if (element !is JsonObject) return jsonString
            val headersElement = element["headers"] ?: return jsonString
            if (headersElement !is JsonObject) return jsonString
            if (headersElement.keys.none { it in sensitiveHeaderNames }) return jsonString

            val redactedHeaders =
                JsonObject(
                    headersElement.mapValues { (key, value) ->
                        if (key in sensitiveHeaderNames) JsonPrimitive(REDACTED) else value
                    },
                )
            JsonObject(element.toMutableMap().apply { put("headers", redactedHeaders) }).toString()
        } catch (_: Exception) {
            jsonString
        }
    }
}
