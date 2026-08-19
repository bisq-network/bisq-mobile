package network.bisq.mobile.presentation.common.ui.components.molecules.chat.private_messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.common.ui.components.molecules.UserProfileIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * Identifies who a DM is with, since — unlike trade chat — there is no trade header above it.
 * Tapping opens the peer's profile.
 */
@Composable
fun PrivateChatPeerHeader(
    peerUserProfile: UserProfileVO,
    peerName: String,
    peerStarRating: Double,
    isPeerReputationUnknown: Boolean,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(BisqTheme.colors.dark_grey40)
                .clickable(onClick = onClick)
                .padding(BisqUIConstants.ScreenPadding)
                .testTag("private_chat_peer_header"),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserProfileIcon(peerUserProfile, userProfileIconProvider, 40.dp)
        // weight(1f) so a long nickname ellipsizes inside the row instead of pushing the star rating
        // out of it — a user name has no length bound this component can rely on.
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter),
        ) {
            BisqText.StyledText(
                text = peerName,
                style = BisqTheme.typography.baseMedium,
                color = BisqTheme.colors.white,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // No stars when the score is unknown: an empty five-star row reads as a real rating of
            // zero. Same call as PeerProfileScreen, which this header is one tap away from.
            if (!isPeerReputationUnknown) {
                StarRating(rating = peerStarRating)
            }
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun PrivateChatPeerHeaderPreview() {
    BisqTheme.Preview {
        PrivateChatPeerHeader(
            peerUserProfile = createMockUserProfile("SatoshiFan"),
            peerName = "SatoshiFan",
            peerStarRating = 4.5,
            isPeerReputationUnknown = false,
            userProfileIconProvider = { createEmptyImage() },
            onClick = {},
        )
    }
}

/** Guards the ellipsis: the name is bounded by `weight(1f)`, so a long one must not push the stars out. */
@ExcludeFromCoverage
@Preview
@Composable
private fun PrivateChatPeerHeaderLongNamePreview() {
    BisqTheme.Preview {
        PrivateChatPeerHeader(
            peerUserProfile = createMockUserProfile("AVeryLongNicknameThatSomeoneActuallyPicked"),
            peerName = "AVeryLongNicknameThatSomeoneActuallyPicked",
            peerStarRating = 3.0,
            isPeerReputationUnknown = false,
            userProfileIconProvider = { createEmptyImage() },
            onClick = {},
        )
    }
}
