package network.bisq.mobile.client.common.domain.access.utils

import kotlinx.coroutines.CancellationException
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
     * Redacts the values of sensitive fields inside a JSON request body.
     *
     * Parses BEFORE deciding anything: the parser decodes JSON escapes, so a field name written
     * as `deviceToken` still matches structurally — a substring pre-check on the raw string
     * would not see it and would echo the secret. A blank body (GET requests) has nothing to
     * parse or leak; any other body that cannot be parsed fails closed to [UNPARSEABLE_PAYLOAD].
     */
    fun redactSensitiveBodyFields(body: String): String {
        if (body.isBlank()) return body
        return try {
            val element = lenientJson.parseToJsonElement(body)
            if (element !is JsonObject) return UNPARSEABLE_PAYLOAD
            if (element.keys.none { isSensitiveBodyField(it) }) return body
            JsonObject(
                element.mapValues { (key, value) ->
                    if (isSensitiveBodyField(key)) JsonPrimitive(REDACTED) else value
                },
            ).toString()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Fail closed: never echo an unparseable payload that may contain credentials.
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
        return try {
            // Parse first, decide after: escape sequences in key names are only visible to the
            // parser, so any pre-parse substring check can be sidestepped. When nothing sensitive
            // is found STRUCTURALLY, the original string is returned byte-identical.
            val element = lenientJson.parseToJsonElement(jsonString)
            if (element !is JsonObject) return UNPARSEABLE_PAYLOAD

            val headersElement = element["headers"] as? JsonObject
            val hasSensitiveHeaders = headersElement?.keys?.any { isSensitiveHeader(it) } == true
            val bodyElement = element["body"] as? JsonPrimitive
            val redactedBody = if (bodyElement?.isString == true) redactSensitiveBodyFields(bodyElement.content) else null
            val bodyChanged = redactedBody != null && redactedBody != bodyElement?.content
            if (!hasSensitiveHeaders && !bodyChanged) return jsonString

            val redacted = element.toMutableMap()
            if (hasSensitiveHeaders && headersElement != null) {
                redacted["headers"] =
                    JsonObject(
                        headersElement.mapValues { (key, value) ->
                            if (isSensitiveHeader(key)) JsonPrimitive(REDACTED) else value
                        },
                    )
            }
            if (bodyChanged && redactedBody != null) {
                redacted["body"] = JsonPrimitive(redactedBody)
            }
            JsonObject(redacted).toString()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Fail closed: never echo an unparseable payload that may contain credentials.
            UNPARSEABLE_PAYLOAD
        }
    }
}
