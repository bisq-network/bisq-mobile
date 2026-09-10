package network.bisq.mobile.data.replicated.chat

import network.bisq.mobile.data.replicated.chat.two_party.createMockTwoPartyPrivateChatMessage
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import kotlin.test.Test
import kotlin.test.assertEquals

class ChatMentionCandidatesTest {
    private val me = createMockUserProfile("Alice")
    private val peer = createMockUserProfile("Bob")
    private val lurker = createMockUserProfile("Carol")

    @Test
    fun `authors of raw messages are candidates`() {
        val candidates =
            deriveMentionCandidates(
                messages = listOf(message(peer), message(me)),
            )

        assertEquals(listOf(peer.id, me.id), candidates.map { it.id })
    }

    @Test
    fun `participants and owned profiles that never posted are still included`() {
        val candidates =
            deriveMentionCandidates(
                messages = listOf(message(peer)),
                participants = listOf(lurker),
                ownedProfiles = listOf(me),
            )

        assertEquals(listOf(peer.id, lurker.id, me.id), candidates.map { it.id })
    }

    @Test
    fun `duplicate profile ids collapse to the first occurrence`() {
        val laterPeer = peer.copy(statement = "updated")
        val candidates =
            deriveMentionCandidates(
                messages = listOf(message(peer), message(laterPeer)),
                participants = listOf(peer),
                ownedProfiles = listOf(peer),
            )

        assertEquals(listOf(peer.id), candidates.map { it.id })
        assertEquals(peer.statement, candidates.single().statement)
    }

    @Test
    fun `owned profiles are not excluded`() {
        val candidates =
            deriveMentionCandidates(
                messages = emptyList(),
                ownedProfiles = listOf(me),
            )

        assertEquals(listOf(me.id), candidates.map { it.id })
    }

    @Test
    fun `empty inputs yield no candidates`() {
        assertEquals(emptyList(), deriveMentionCandidates(emptyList()))
    }

    private fun message(sender: UserProfileVO) =
        createMockTwoPartyPrivateChatMessage(
            text = "hello",
            senderUserProfile = sender,
            myUserProfile = me,
        )
}
