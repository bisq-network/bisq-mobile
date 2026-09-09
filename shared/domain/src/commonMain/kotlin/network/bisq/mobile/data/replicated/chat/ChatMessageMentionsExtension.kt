package network.bisq.mobile.data.replicated.chat

import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id

/**
 * Mirrors bisq2 desktop's mention semantics (`ChatMessage.wasMentioned` / `wasCited`), which the
 * MENTION notification level combines — "we treat citations also like mentions". A mention is the
 * plain-text convention `@userName`: no markup and no protocol entity, so interop with desktop is
 * free in both directions. The substring imprecision (`@Bob` matches inside `@Bobby`) is
 * desktop's, inherited on purpose: bug-compatible beats subtly divergent across the apps.
 *
 * Checked against ALL of the user's profiles, like desktop checks all identities.
 */
fun ChatMessage<*>.mentionsOrCites(myProfiles: Collection<UserProfileVO>): Boolean =
    myProfiles.any { profile ->
        textString.contains("@${profile.userName}") || citation?.authorUserProfileId == profile.id
    }
