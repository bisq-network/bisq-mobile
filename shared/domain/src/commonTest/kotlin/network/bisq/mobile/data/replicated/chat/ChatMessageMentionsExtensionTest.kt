package network.bisq.mobile.data.replicated.chat

import network.bisq.mobile.data.replicated.chat.two_party.createMockTwoPartyPrivateChatMessage
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Mirrors bisq2 desktop's mention semantics exactly (ChatMessage.wasMentioned / wasCited): a
 * mention is the plain-text convention `@userName` and a citation of my message counts like a
 * mention. Includes desktop's documented imprecision on purpose — substring matching — because
 * bug-compatible beats subtly divergent across apps.
 */
class ChatMessageMentionsExtensionTest {
    private val me = createMockUserProfile("Alice")
    private val peer = createMockUserProfile("Bob")

    private fun message(
        text: String,
        citationAuthorId: String? = null,
    ) = createMockTwoPartyPrivateChatMessage(
        text = text,
        citation = citationAuthorId?.let { Citation(authorUserProfileId = it, text = "quoted", chatMessageId = null) },
        senderUserProfile = peer,
        myUserProfile = me,
    )

    @Test
    fun `a message containing at-userName mentions the profile`() {
        assertTrue(message("hey @${me.userName} look at this").mentionsOrCites(listOf(me)))
    }

    @Test
    fun `a plain message neither mentions nor cites`() {
        assertFalse(message("hello world").mentionsOrCites(listOf(me)))
    }

    @Test
    fun `the userName without the at sign is not a mention`() {
        assertFalse(message("${me.userName} said so").mentionsOrCites(listOf(me)))
    }

    /** Desktop's documented imprecision is inherited on purpose - substring match. */
    @Test
    fun `a longer name containing mine still matches like on desktop`() {
        assertTrue(message("ping @${me.userName}Smith").mentionsOrCites(listOf(me)))
    }

    @Test
    fun `a citation of my message counts like a mention`() {
        assertTrue(message("disagree", citationAuthorId = me.id).mentionsOrCites(listOf(me)))
    }

    @Test
    fun `a citation of someone else does not`() {
        assertFalse(message("disagree", citationAuthorId = peer.id).mentionsOrCites(listOf(me)))
    }

    @Test
    fun `any of my profiles matching is enough`() {
        val secondProfile = createMockUserProfile("AliceWork")
        assertTrue(message("cc @${secondProfile.userName}").mentionsOrCites(listOf(me, secondProfile)))
    }

    @Test
    fun `no profiles means no match`() {
        assertFalse(message("hey @${me.userName}").mentionsOrCites(emptyList()))
    }
}
