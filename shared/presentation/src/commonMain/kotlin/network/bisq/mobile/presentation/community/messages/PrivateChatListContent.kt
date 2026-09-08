package network.bisq.mobile.presentation.community.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.common.ui.components.atoms.debouncedClickable
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.common.ui.components.molecules.UserProfileIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * The Community hub's Messages tab body (#1825), realizing the private-chat-list design PoC
 * (removed once implemented, like the rest of the milestone-11 community set). Pure UI: takes
 * [PrivateChatListUiState] + callbacks, no presenter here — [MessagesTabContent] does the wiring.
 *
 * Per the PoC: rows read as visual siblings of the hub's other row families (48 dp avatar,
 * name + relative time, star rating, single-line preview, and the same manual unread pill as
 * `CommunityTopBarIcon` — a plain `Box`, not a `BadgedBox`, so nothing can clip it). The empty
 * state directs to the Offerbook rather than dead-ending, mirroring `NoTradesSection`.
 */
@Composable
fun PrivateChatListContent(
    uiState: PrivateChatListUiState,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    onAction: (PrivateChatListUiAction) -> Unit,
) {
    when {
        uiState.isLoading -> LoadingState()
        uiState.conversations.isEmpty() -> {
            PrivateChatListEmptyState(onBrowseOfferbook = { onAction(PrivateChatListUiAction.OnBrowseOfferbookClick) })
        }
        else -> {
            BisqScrollLayout(contentPadding = PaddingValues(vertical = BisqUIConstants.ScreenPaddingHalf)) {
                uiState.conversations.forEach { conversation ->
                    ConversationRow(
                        conversation = conversation,
                        userProfileIconProvider = userProfileIconProvider,
                        onClick = { onAction(PrivateChatListUiAction.OnConversationClick(conversation.channelId)) },
                    )
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = BisqTheme.colors.dark_grey50,
                        modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivateChatListEmptyState(onBrowseOfferbook: () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = BisqUIConstants.ScreenPadding2X),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding3X),
        ) {
            BisqText.H4LightGrey(
                text = "mobile.privateChats.list.empty".i18n(),
                textAlign = TextAlign.Center,
            )
            BisqText.BaseLight(
                text = "mobile.privateChats.list.empty.hint".i18n(),
                color = BisqTheme.colors.mid_grey20,
                textAlign = TextAlign.Center,
            )
            BisqGap.V1()
            BisqButton(
                text = "mobile.privateChats.list.empty.browseOfferbook".i18n(),
                onClick = onBrowseOfferbook,
            )
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: PrivateChatListItemUiState,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .debouncedClickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingHalfQuarter),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UserProfileIcon(
            userProfile = conversation.peerProfile,
            userProfileIconProvider = userProfileIconProvider,
            size = BisqUIConstants.ScreenPadding4X,
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BisqText.BaseRegular(
                    text = conversation.peerProfile.userName,
                    color = BisqTheme.colors.white,
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                BisqText.SmallRegular(text = conversation.lastMessageTimeLabel, color = BisqTheme.colors.mid_grey20)
            }
            StarRating(rating = conversation.starRating)
            BisqText.StyledText(
                text = conversation.lastMessagePreview,
                style = BisqTheme.typography.smallLight,
                color = BisqTheme.colors.mid_grey30,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Same manual pill shape as CommunityTopBarIcon — a plain Box, not a BadgedBox/IconButton
        // pairing, so it has no clipping risk (see the badge-clipping bug fixed in that file).
        if (conversation.unreadCount > 0) {
            Box(
                modifier =
                    Modifier
                        .background(BisqTheme.colors.primary, shape = CircleShape)
                        .padding(horizontal = BisqUIConstants.ScreenPaddingHalf, vertical = BisqUIConstants.ScreenPaddingQuarter),
            ) {
                BisqText.XSmallMedium(text = conversation.unreadCount.toString(), color = BisqTheme.colors.white)
            }
        }
    }
}

// ============================================================================================
// Preview fixtures
// ============================================================================================

private val previewUserProfileIconProvider: suspend (UserProfileVO) -> PlatformImage = { createEmptyImage() }

internal fun sampleConversation(
    channelId: String,
    peerName: String,
    preview: String,
    timeLabel: String,
    epochMillis: Long,
    starRating: Double,
    unreadCount: Long,
): PrivateChatListItemUiState =
    PrivateChatListItemUiState(
        channelId = channelId,
        peerProfile = createMockUserProfile(peerName),
        starRating = starRating,
        lastMessagePreview = preview,
        lastMessageTimeLabel = timeLabel,
        lastMessageEpochMillis = epochMillis,
        unreadCount = unreadCount,
    )

internal fun sampleConversations(): List<PrivateChatListItemUiState> =
    listOf(
        sampleConversation("discussion.a-b", "SatoshiFan#1234", "Sure, let me know when you're ready.", "2 min ago", 4_000L, 4.5, 3),
        sampleConversation("discussion.a-c", "BitcoinBee#5678", "You: Thanks for the quick response!", "3 hours ago", 3_000L, 3.8, 0),
        sampleConversation("discussion.a-d", "CryptoNomad#9012", "Hello! I saw your offer in the offerbook.", "1 day ago", 2_000L, 2.1, 1),
        sampleConversation("discussion.a-e", "PeerNode#3456", "What payment methods do you accept?", "4 days ago", 1_000L, 4.9, 0),
    )

// ============================================================================================
// Previews
// ============================================================================================

@ExcludeFromCoverage
@Preview(name = "Messages — populated")
@Composable
private fun PrivateChatListContent_PopulatedPreview() {
    BisqTheme.Preview {
        PrivateChatListContent(
            uiState = PrivateChatListUiState(conversations = sampleConversations()),
            userProfileIconProvider = previewUserProfileIconProvider,
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Messages — empty")
@Composable
private fun PrivateChatListContent_EmptyPreview() {
    BisqTheme.Preview {
        PrivateChatListContent(
            uiState = PrivateChatListUiState(),
            userProfileIconProvider = previewUserProfileIconProvider,
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Messages — loading")
@Composable
private fun PrivateChatListContent_LoadingPreview() {
    BisqTheme.Preview {
        PrivateChatListContent(
            uiState = PrivateChatListUiState(isLoading = true),
            userProfileIconProvider = previewUserProfileIconProvider,
            onAction = {},
        )
    }
}
