package network.bisq.mobile.presentation.report_user

import network.bisq.mobile.data.replicated.chat.ChatMessage
import network.bisq.mobile.domain.utils.StringUtils.truncate
import kotlin.time.Instant

const val REPORTED_MESSAGE_MAX_LENGTH = 5000

/**
 * The chat message a report was opened from, appended to the report text for the moderator.
 * Fixed English labels: Desktop shows the raw string.
 */
data class ReportedMessage(
    val channel: String,
    val date: Long,
    val text: String,
) {
    val metadata: String
        get() =
            buildString {
                appendLine("--- Reported message ---")
                appendLine("Channel: $channel")
                appendLine("Sent: ${Instant.fromEpochSeconds(date / 1000)}")
                append("Message: ${text.truncate(REPORTED_MESSAGE_MAX_LENGTH, ellipsis = "…")}")
            }

    fun appendTo(reason: String): String = "$reason\n\n$metadata"

    companion object {
        const val PRIVATE_CHAT = "Private chat"

        fun tradeChat(tradeId: String): String = "Trade chat, trade $tradeId"

        fun of(
            message: ChatMessage<*>,
            channel: String,
        ): ReportedMessage = ReportedMessage(channel = channel, date = message.date, text = message.textString)
    }
}
