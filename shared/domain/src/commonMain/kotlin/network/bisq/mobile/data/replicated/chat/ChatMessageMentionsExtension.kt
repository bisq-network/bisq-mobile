package network.bisq.mobile.data.replicated.chat

import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id

/**
 * Inclusive start, exclusive end of a mention substring — the same interval
 * [androidx.compose.ui.text.AnnotatedString] uses for a span.
 */
data class ChatMentionRange(
    val start: Int,
    val endExclusive: Int,
)

/**
 * Mirrors bisq2 desktop's mention semantics (`ChatMessage.wasMentioned` / `wasCited`), which the
 * MENTION notification level combines — "we treat citations also like mentions". A mention is the
 * plain-text convention `@userName`: no markup and no protocol entity, so interop with desktop is
 * free in both directions. The substring imprecision (`@Bob` matches inside `@Bobby`) is
 * desktop's, inherited on purpose: bug-compatible beats subtly divergent across the apps.
 *
 * Checked against ALL of the user's profiles, like desktop checks all identities.
 *
 * Ranges and [mentionsOrCites] share this one case-sensitive substring rule. Citations produce no
 * range — they still count as a mention for notifications, but there is nothing in the body to
 * highlight. Overlapping ranges from a user who owns both `Bob` and `Bobby` collapse to one span
 * covering `@Bobby`.
 */
fun ChatMessage<*>.mentionsOrCites(myProfiles: Collection<UserProfileVO>): Boolean =
    mentionRanges(myProfiles).isNotEmpty() ||
        myProfiles.any { profile -> citation?.authorUserProfileId == profile.id }

fun ChatMessage<*>.mentionRanges(myProfiles: Collection<UserProfileVO>): List<ChatMentionRange> = mentionRanges(textString, myProfiles)

fun mentionRanges(
    text: String,
    myProfiles: Collection<UserProfileVO>,
): List<ChatMentionRange> {
    if (text.isEmpty() || myProfiles.isEmpty()) {
        return emptyList()
    }
    val ranges = ArrayList<ChatMentionRange>()
    for (profile in myProfiles) {
        val needle = "@${profile.userName}"
        if (needle.length <= 1) {
            continue
        }
        var start = 0
        while (start <= text.length - needle.length) {
            val index = text.indexOf(needle, start)
            if (index < 0) {
                break
            }
            ranges.add(ChatMentionRange(index, index + needle.length))
            start = index + 1
        }
    }
    return mergeOverlappingRanges(ranges)
}

private fun mergeOverlappingRanges(ranges: List<ChatMentionRange>): List<ChatMentionRange> {
    if (ranges.size <= 1) {
        return ranges
    }
    val sorted = ranges.sortedWith(compareBy({ it.start }, { it.endExclusive }))
    val merged = ArrayList<ChatMentionRange>(sorted.size)
    var current = sorted.first()
    for (index in 1 until sorted.size) {
        val next = sorted[index]
        current =
            if (next.start <= current.endExclusive) {
                ChatMentionRange(current.start, maxOf(current.endExclusive, next.endExclusive))
            } else {
                merged.add(current)
                next
            }
    }
    merged.add(current)
    return merged
}
