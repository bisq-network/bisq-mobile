package network.bisq.mobile.presentation.community.contacts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.data.replicated.user.contact_list.ContactReasonEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.LoadingState
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * The Community hub's Contacts tab body (issue #1238). Pure UI: takes [ContactsListUiState]
 * + callbacks, no presenter here — the wiring PR mounts this behind
 * `CommunityHubScreenContent`'s Contacts placeholder.
 *
 * Deliberately a plain scrollable [BisqScrollLayout] (not `LazyColumn`): Contacts is a
 * directory with no pagination per the milestone-11 IA decision, matching the small-list
 * idiom used elsewhere (e.g. `IgnoredUsersContent`). No FAB — adding a contact happens from
 * the Peer Profile screen, not from this directory (explicit IA decision, see
 * `CommunityHubScreen.kt`'s muted-tab treatment for the related "directory, not inbox"
 * rationale).
 */
@Composable
fun ContactsListContent(
    uiState: ContactsListUiState,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    onContactClick: (String) -> Unit,
) {
    if (uiState.isLoading) {
        LoadingState()
    } else if (uiState.contacts.isEmpty()) {
        ContactsEmptyState()
    } else {
        BisqScrollLayout(
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            uiState.contacts.forEach { contact ->
                ContactCard(
                    contact = contact,
                    userProfileIconProvider = userProfileIconProvider,
                    onClick = { onContactClick(contact.id) },
                )
            }
        }
    }
}

@Composable
private fun ContactsEmptyState() {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(BisqUIConstants.ScreenPadding2X),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            BisqText.H4LightGrey(
                text = "mobile.community.contacts.empty.title".i18n(),
                textAlign = TextAlign.Center,
            )
            BisqGap.VHalf()
            BisqText.BaseRegularGrey(
                text = "mobile.community.contacts.empty.hint".i18n(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ============================================================================================
// Previews
// ============================================================================================

private val previewUserProfileIconProvider: suspend (UserProfileVO) -> PlatformImage = { createEmptyImage() }

@ExcludeFromCoverage
@Preview(name = "Contacts — populated (varied reasons/tags/trust)")
@Composable
private fun ContactsListContent_PopulatedPreview() {
    BisqTheme.Preview {
        ContactsListContent(
            uiState =
                ContactsListUiState(
                    contacts =
                        listOf(
                            sampleContact(
                                id = "contact-1",
                                peerName = "SatoshiFan#1234",
                                trustScore = 0.92,
                                contactReason = ContactReasonEnum.MANUALLY_ADDED,
                                dateAddedLabel = "12 Jul 2026",
                                tag = "Reliable SEPA trader",
                            ),
                            sampleContact(
                                id = "contact-2",
                                peerName = "BitcoinBee#5678",
                                trustScore = 0.5,
                                contactReason = ContactReasonEnum.BISQ_EASY_TRADE,
                                dateAddedLabel = "3 weeks ago",
                            ),
                            sampleContact(
                                id = "contact-3",
                                peerName = "CryptoNomad#9012",
                                trustScore = 0.15,
                                contactReason = ContactReasonEnum.PRIVATE_CHAT,
                                dateAddedLabel = "Yesterday",
                                tag = "Met in Discussions",
                            ),
                            sampleContact(
                                id = "contact-4",
                                peerName = "PeerNode#3456",
                                trustScore = 0.78,
                                contactReason = ContactReasonEnum.MUSIG_TRADE,
                                dateAddedLabel = "5 Jan 2026",
                            ),
                        ),
                ),
            userProfileIconProvider = previewUserProfileIconProvider,
            onContactClick = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Contacts — empty")
@Composable
private fun ContactsListContent_EmptyPreview() {
    BisqTheme.Preview {
        ContactsListContent(
            uiState = ContactsListUiState(),
            userProfileIconProvider = previewUserProfileIconProvider,
            onContactClick = {},
        )
    }
}
