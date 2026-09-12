package network.bisq.mobile.data.replicated.chat

import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id

/**
 * Channel-scoped mention candidates from the raw, pre-search / pre-ignore message set plus
 * known participants and owned profiles. Deduplicated by profile id, first write wins.
 *
 * Ignored authors and the user's own profiles stay in the list — desktop filters neither.
 * Do not derive from a search- or ignore-filtered message list: the picker would shrink
 * while the user typed a search, and ignored authors would vanish.
 *
 * Scanning [ChatChannel.chatMessages] is O(n) in channel size. Callers that observe a
 * channel should cache the result on the raw-message / participant / owned-profile inputs
 * rather than folding the scan into a search or read-count combine.
 */
fun deriveMentionCandidates(
    messages: Iterable<ChatMessage<*>>,
    participants: Collection<UserProfileVO> = emptyList(),
    ownedProfiles: Collection<UserProfileVO> = emptyList(),
): List<UserProfileVO> {
    val byId = LinkedHashMap<String, UserProfileVO>()
    for (message in messages) {
        val profile = message.senderUserProfile
        byId.putFirstWins(profile)
    }
    for (profile in participants) {
        byId.putFirstWins(profile)
    }
    for (profile in ownedProfiles) {
        byId.putFirstWins(profile)
    }
    return byId.values.toList()
}

// `MutableMap.putIfAbsent` is JVM-only; common (iOS) maps don't have it.
private fun MutableMap<String, UserProfileVO>.putFirstWins(profile: UserProfileVO) {
    if (!containsKey(profile.id)) {
        put(profile.id, profile)
    }
}
