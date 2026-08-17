package network.bisq.mobile.data.replicated.chat.two_party

import network.bisq.mobile.data.replicated.chat.ChatMessageTypeEnum
import network.bisq.mobile.data.replicated.chat.Citation
import network.bisq.mobile.data.replicated.chat.priv.PrivateChatMessage
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.domain.utils.createUuid

/**
 * A message in a two-party private chat (DM), replicating Bisq 2's `TwoPartyPrivateChatMessage`.
 *
 * Adds nothing to [PrivateChatMessage] — exactly as upstream, where `TwoPartyPrivateChatMessage`
 * extends `PrivateChatMessage` without introducing a single field of its own. The trade sibling
 * [network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.BisqEasyOpenTradeMessage] is
 * where the extra trade fields live.
 */
class TwoPartyPrivateChatMessage(
    id: String,
    chatMessageType: ChatMessageTypeEnum,
    text: String?,
    citation: Citation?,
    citationAuthorUserProfile: UserProfileVO?,
    date: Long,
    senderUserProfile: UserProfileVO,
    myUserProfile: UserProfileVO,
    chatReactions: List<TwoPartyPrivateChatMessageReaction>,
) : PrivateChatMessage<TwoPartyPrivateChatMessageReaction>(
        id = id,
        chatMessageType = chatMessageType,
        text = text,
        citation = citation,
        citationAuthorUserProfile = citationAuthorUserProfile,
        date = date,
        senderUserProfile = senderUserProfile,
        myUserProfile = myUserProfile,
        chatReactions = chatReactions,
    )

/**
 * A fast way to create a mock DM, for tests and Compose previews. Mirrors
 * [network.bisq.mobile.data.replicated.chat.bisq_easy.open_trades.createMockBisqEasyOpenTradeMessage]
 * and lives in `commonMain` for the same reason it does: the chat previews are production composables
 * and cannot see a test source set.
 *
 * Fixture only. The two production mappings that build this type — `TwoPartyPrivateChatMessageMapping`
 * (node) and `TwoPartyPrivateChatDtoMapping` (client) — keep calling the constructor directly; every
 * field they set comes off the wire, so there is nothing for a default to supply.
 *
 * [date] is a constant rather than the current time so previews render deterministically and
 * `dateString` assertions stay stable. The [senderUserProfile] and [myUserProfile] defaults are
 * *different* profiles, so an unqualified mock is a message from the peer; pass the same instance to
 * both for one of mine.
 */
fun createMockTwoPartyPrivateChatMessage(
    id: String = createUuid(),
    chatMessageType: ChatMessageTypeEnum = ChatMessageTypeEnum.TEXT,
    text: String? = "Hello",
    citation: Citation? = null,
    citationAuthorUserProfile: UserProfileVO? = null,
    date: Long = 1234567890000L,
    senderUserProfile: UserProfileVO = createMockUserProfile("Alice"),
    myUserProfile: UserProfileVO = createMockUserProfile("Bob"),
    chatReactions: List<TwoPartyPrivateChatMessageReaction> = emptyList(),
) = TwoPartyPrivateChatMessage(
    id = id,
    chatMessageType = chatMessageType,
    text = text,
    citation = citation,
    citationAuthorUserProfile = citationAuthorUserProfile,
    date = date,
    senderUserProfile = senderUserProfile,
    myUserProfile = myUserProfile,
    chatReactions = chatReactions,
)
