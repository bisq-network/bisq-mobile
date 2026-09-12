package network.bisq.mobile.data.replicated.chat

import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ports desktop's `ChatMentionParserTest` at b03bcab3 and adds the filter, sort, and
 * conditional-space insertion cases that live on the desktop controller / popup.
 */
class ChatMentionParserTest {
    @Test
    fun `mention at the end of the text`() {
        assertEquals(
            ChatMentionMatch("jo", 0, 3),
            ChatMentionParser.findMentionAtCaret("@jo", 3),
        )
    }

    @Test
    fun `mention before existing text is found`() {
        assertEquals(
            ChatMentionMatch("jo", 0, 3),
            ChatMentionParser.findMentionAtCaret("@jo hello", 3),
        )
    }

    @Test
    fun `mention in the middle of a sentence`() {
        assertEquals(
            ChatMentionMatch("jo", 6, 9),
            ChatMentionParser.findMentionAtCaret("hello @jo world", 9),
        )
    }

    @Test
    fun `match never extends past the caret`() {
        assertEquals(
            ChatMentionMatch("al", 0, 3),
            ChatMentionParser.findMentionAtCaret("@alworld", 3),
        )
        assertEquals(
            ChatMentionMatch("j", 0, 2),
            ChatMentionParser.findMentionAtCaret("@joe", 2),
        )
    }

    @Test
    fun `indicator after new line`() {
        assertEquals(
            ChatMentionMatch("jo", 3, 6),
            ChatMentionParser.findMentionAtCaret("hi\n@jo", 6),
        )
    }

    @Test
    fun `bare indicator matches with empty query`() {
        assertEquals(
            ChatMentionMatch("", 0, 1),
            ChatMentionParser.findMentionAtCaret("@", 1),
        )
    }

    @Test
    fun `indicator not preceded by whitespace does not match`() {
        assertNull(ChatMentionParser.findMentionAtCaret("x@jo", 4))
        assertNull(ChatMentionParser.findMentionAtCaret("a@jo", 4))
        assertNull(ChatMentionParser.findMentionAtCaret("@a@b", 4))
    }

    @Test
    fun `caret before the indicator does not match`() {
        assertNull(ChatMentionParser.findMentionAtCaret("@jo", 0))
    }

    @Test
    fun `non word character between indicator and caret does not match`() {
        assertNull(ChatMentionParser.findMentionAtCaret("@j-o", 4))
        assertNull(ChatMentionParser.findMentionAtCaret("@j_o", 4))
        assertNull(ChatMentionParser.findMentionAtCaret("@j.o", 4))
    }

    @Test
    fun `unicode letters stay in the mention token`() {
        assertEquals(
            ChatMentionMatch("José", 0, "@José".length),
            ChatMentionParser.findMentionAtCaret("@José", "@José".length),
        )
        assertEquals(
            ChatMentionMatch("Élise", 0, "@Élise".length),
            ChatMentionParser.findMentionAtCaret("@Élise", "@Élise".length),
        )
    }

    @Test
    fun `a name that starts with a unicode letter is a mention token`() {
        assertEquals(
            ChatMentionMatch("É", 0, "@É".length),
            ChatMentionParser.findMentionAtCaret("@É", "@É".length),
        )
    }

    @Test
    fun `emoji and combining marks stay in the mention token`() {
        val withEmoji = "@José🎉"
        assertEquals(
            ChatMentionMatch("José🎉", 0, withEmoji.length),
            ChatMentionParser.findMentionAtCaret(withEmoji, withEmoji.length),
        )
        val decomposed = "@Jose\u0301"
        assertEquals(
            ChatMentionMatch("Jose\u0301", 0, decomposed.length),
            ChatMentionParser.findMentionAtCaret(decomposed, decomposed.length),
        )
    }

    @Test
    fun `the same query at the same indicator stays dismissed`() {
        val match = ChatMentionMatch("al", 0, 3)
        val dismissed = DismissedMentionToken(indicatorIndex = 0, query = "al")

        assertTrue(match.isDismissedBy(dismissed))
    }

    @Test
    fun `a new query at the same indicator is not dismissed`() {
        val match = ChatMentionMatch("bo", 0, 3)
        val dismissed = DismissedMentionToken(indicatorIndex = 0, query = "al")

        assertFalse(match.isDismissedBy(dismissed))
    }

    @Test
    fun `dismissing with the inserted name keeps a caret-in-token insert closed`() {
        val afterInsert = ChatMentionMatch("alice", 3, 9)
        val dismissed = DismissedMentionToken(indicatorIndex = 3, query = "alice")

        assertTrue(afterInsert.isDismissedBy(dismissed))
    }

    @Test
    fun `transient caret positions are tolerated`() {
        assertNull(ChatMentionParser.findMentionAtCaret("@jo", 5))
        assertNull(ChatMentionParser.findMentionAtCaret("@jo", -1))
        assertNull(ChatMentionParser.findMentionAtCaret("", 0))
    }

    @Test
    fun `empty query leaves the list unfiltered and sorts alphabetically`() {
        val alice = createMockUserProfile("Alice")
        val charlie = createMockUserProfile("Charlie")
        val bob = createMockUserProfile("Bob")

        assertEquals(
            listOf(alice, bob, charlie).map { it.userName },
            ChatMentionParser.filterAndSort(listOf(charlie, alice, bob), "").map { it.userName },
        )
    }

    @Test
    fun `filter is case-insensitive contains and prefix matches sort first`() {
        val alice = createMockUserProfile("Alice")
        val malice = createMockUserProfile("Malice")
        val bob = createMockUserProfile("Bob")
        val alan = createMockUserProfile("Alan")

        assertEquals(
            listOf(alan, alice, malice).map { it.userName },
            ChatMentionParser.filterAndSort(listOf(alice, malice, bob, alan), "al").map { it.userName },
        )
    }

    @Test
    fun `inserting at the end of the text adds a trailing space`() {
        val match = ChatMentionParser.findMentionAtCaret("Hi @al", 6)!!
        val insertion = ChatMentionParser.insertMention("Hi @al", match, "alice")

        assertEquals("Hi @alice ", insertion.text)
        assertEquals("Hi @alice ".length, insertion.caretPosition)
    }

    @Test
    fun `inserting before an existing space reuses it`() {
        val text = "Hi @jo there"
        val match = ChatMentionParser.findMentionAtCaret(text, 6)!!
        val insertion = ChatMentionParser.insertMention(text, match, "john")

        assertEquals("Hi @john there", insertion.text)
        assertEquals("Hi @john ".length, insertion.caretPosition)
    }

    @Test
    fun `inserting in front of a following word adds a space`() {
        val text = "@alworld"
        val match = ChatMentionParser.findMentionAtCaret(text, 3)!!
        val insertion = ChatMentionParser.insertMention(text, match, "alice")

        assertEquals("@alice world", insertion.text)
        assertEquals("@alice ".length, insertion.caretPosition)
    }

    @Test
    fun `inserting before punctuation adds no space`() {
        val text = "Hi @al, there"
        val match = ChatMentionParser.findMentionAtCaret(text, 6)!!
        val insertion = ChatMentionParser.insertMention(text, match, "alice")

        assertEquals("Hi @alice, there", insertion.text)
        assertEquals("Hi @alice".length, insertion.caretPosition)
    }

    @Test
    fun `insertion writes the full userName including an ambiguity suffix`() {
        val text = "@Al"
        val match = ChatMentionParser.findMentionAtCaret(text, 3)!!
        val insertion = ChatMentionParser.insertMention(text, match, "Alice [a1b2c3]")

        assertEquals("@Alice [a1b2c3] ", insertion.text)
        assertEquals("@Alice [a1b2c3] ".length, insertion.caretPosition)
    }
}
