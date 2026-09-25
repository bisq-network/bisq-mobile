package network.bisq.mobile.presentation.report_user

import network.bisq.mobile.data.replicated.chat.common.createMockCommonPublicChatMessage
import network.bisq.mobile.i18n.PARAM_SEPARATOR
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportedMessageTest {
    @Test
    fun `metadata lists the channel the UTC send time and the message`() {
        val reported = ReportedMessage(channel = "discussion.bisq", date = 1_234_567_890_123L, text = "buy my coin")

        assertEquals(
            "--- Reported message ---\n" +
                "Channel: discussion.bisq\n" +
                "Sent: 2009-02-13T23:31:30Z\n" +
                "Message: buy my coin",
            reported.metadata,
        )
    }

    @Test
    fun `the metadata goes after the reason separated by a blank line`() {
        val reported = ReportedMessage(channel = "c", date = 0, text = "t")

        assertEquals("spam\n\n${reported.metadata}", reported.appendTo("spam"))
    }

    @Test
    fun `the quote is the text as the reporter saw it`() {
        val sent = "legit offer${PARAM_SEPARATOR}scam link"

        val reported = ReportedMessage.of(createMockCommonPublicChatMessage(text = sent), channel = "c")

        assertEquals(sent, reported.text)
    }

    @Test
    fun `a message over the quote limit is cut with an ellipsis`() {
        val reported = ReportedMessage(channel = "c", date = 0, text = "x".repeat(REPORTED_MESSAGE_MAX_LENGTH + 1))

        assertEquals(
            "x".repeat(REPORTED_MESSAGE_MAX_LENGTH - 1) + "…",
            reported.metadata.substringAfter("Message: "),
        )
    }

    @Test
    fun `a message at the quote limit is kept whole`() {
        val text = "x".repeat(REPORTED_MESSAGE_MAX_LENGTH)

        assertEquals(text, ReportedMessage(channel = "c", date = 0, text = text).metadata.substringAfter("Message: "))
    }

    @Test
    fun `the longest possible report fits bisq2's report limit`() {
        val reported =
            ReportedMessage(
                channel = ReportedMessage.tradeChat("t".repeat(36)),
                date = 1_234_567_890_123L,
                text = "x".repeat(CHAT_MESSAGE_MAX_LENGTH),
            )

        val report = reported.appendTo("r".repeat(REPORT_USER_MAX_MESSAGE_LENGTH))

        assertTrue(report.length <= REPORT_TO_MODERATOR_MAX_LENGTH, "report is ${report.length} chars")
    }

    private companion object {
        /** bisq2 `ChatMessage.MAX_TEXT_LENGTH`. */
        const val CHAT_MESSAGE_MAX_LENGTH = 10_000

        /** bisq2 `ReportToModeratorMessage.MAX_MESSAGE_LENGTH`; longer reports are rejected. */
        const val REPORT_TO_MODERATOR_MAX_LENGTH = 10_000
    }
}
