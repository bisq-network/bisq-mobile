package network.bisq.mobile.client.common.domain.access.utils

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketMessage
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketRestApiRequest

/**
 * Redacts sensitive auth headers AND sensitive request-body fields for log output only.
 * Wire payloads must stay unchanged.
 *
 * The body fields matter as much as the headers: the mobile-device registration body carries
 * the FCM/APNs device token (a stable per-install identifier) and the push-notification
 * symmetric key — logging it verbatim hands anyone with logcat access the key that decrypts
 * every relayed push. `onNewToken` refuses to log even a token prefix; this keeps the WS
 * request log to the same standard.
 */
object HeaderRedaction {
    const val REDACTED = "***"
    const val UNPARSEABLE_PAYLOAD = "[unparseable payload]"

    private val sensitiveHeaderNames = setOf(Headers.CLIENT_ID, Headers.SESSION_ID)

    private val sensitiveBodyFieldNames = setOf("deviceToken", "symmetricKeyBase64")

    private val lenientJson = Json { ignoreUnknownKeys = true }

    private fun isSensitiveHeader(name: String): Boolean = sensitiveHeaderNames.any { it.equals(name, ignoreCase = true) }

    private fun isSensitiveBodyField(name: String): Boolean = sensitiveBodyFieldNames.any { it.equals(name, ignoreCase = true) }

    fun redactSensitiveHeaders(headers: Map<String, String>): Map<String, String> =
        headers.mapValues { (name, value) ->
            if (isSensitiveHeader(name)) REDACTED else value
        }

    /**
     * Redacts the values of sensitive fields inside a JSON request body. Untouched when the body
     * mentions none of them; fails closed to [UNPARSEABLE_PAYLOAD] when it mentions one but cannot
     * be parsed well enough to redact it precisely.
     */
    fun redactSensitiveBodyFields(body: String): String {
        if (sensitiveBodyFieldNames.none { body.contains(it, ignoreCase = true) }) return body
        return try {
            val element = lenientJson.parseToJsonElement(body)
            if (element !is JsonObject) return UNPARSEABLE_PAYLOAD
            JsonObject(
                element.mapValues { (key, value) ->
                    if (isSensitiveBodyField(key)) JsonPrimitive(REDACTED) else value
                },
            ).toString()
        } catch (_: Exception) {
            UNPARSEABLE_PAYLOAD
        }
    }

    fun redactForLogging(message: WebSocketMessage): String =
        when (message) {
            is WebSocketRestApiRequest ->
                message
                    .copy(
                        headers = redactSensitiveHeaders(message.headers),
                        body = redactSensitiveBodyFields(message.body),
                    ).toString()
            else -> message.toString()
        }

    fun redactRawJsonForLogging(jsonString: String): String {
        // Fast path: nothing sensitive is even mentioned, echo unchanged.
        val mentionsSensitive =
            sensitiveHeaderNames.any { jsonString.contains(it, ignoreCase = true) } ||
                sensitiveBodyFieldNames.any { jsonString.contains(it, ignoreCase = true) }
        if (!mentionsSensitive) return jsonString

        return try {
            val element = lenientJson.parseToJsonElement(jsonString)
            if (element !is JsonObject) return UNPARSEABLE_PAYLOAD
            val redacted = element.toMutableMap()
            (element["headers"] as? JsonObject)?.let { headersElement ->
                redacted["headers"] =
                    JsonObject(
                        headersElement.mapValues { (key, value) ->
                            if (isSensitiveHeader(key)) JsonPrimitive(REDACTED) else value
                        },
                    )
            }
            (element["body"] as? JsonPrimitive)?.let { bodyElement ->
                if (bodyElement.isString) {
                    redacted["body"] = JsonPrimitive(redactSensitiveBodyFields(bodyElement.content))
                }
            }
            JsonObject(redacted).toString()
        } catch (_: Exception) {
            // Fail closed: never echo an unparseable payload that may contain credentials.
            UNPARSEABLE_PAYLOAD
        }
    }
}
