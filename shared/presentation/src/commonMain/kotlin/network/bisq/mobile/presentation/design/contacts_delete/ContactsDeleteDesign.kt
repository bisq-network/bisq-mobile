package network.bisq.mobile.presentation.design.contacts_delete

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.data.replicated.user.contact_list.ContactReasonEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.UserProfileIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.community.contacts.ContactCard
import network.bisq.mobile.presentation.community.contacts.ContactListItemUiState
import network.bisq.mobile.presentation.community.contacts.ContactsListContent
import network.bisq.mobile.presentation.community.contacts.ContactsListUiState

/**
 * ContactsDeleteDesign.kt
 *
 * Specification for removing a contact whose profile can no longer be found on the network.
 * Design POC only: nothing here is wired to a presenter; fake data comes from the `simulatedXxx`
 * helpers.
 *
 * ------------------------------------------------------------------------------------
 * 1. SOLUTION IN ONE PARAGRAPH
 * ------------------------------------------------------------------------------------
 * The dead end is the Peer Profile "not found" state. For a peer that IS a contact, that state
 * stops being a dead end: it shows the contact's identity from the local contact entry, explains
 * the situation, and offers "Remove from contacts" (confirmed by a dialog) next to "Try again".
 * The Contacts list additionally gets a long-press shortcut that opens the same confirmation
 * dialog, with an equivalent TalkBack/VoiceOver custom action, so a user can clean up without
 * opening each dead profile. Tapping a contact keeps opening the profile; there is no dialog on
 * tap and no "gone" badge in the list.
 *
 * ------------------------------------------------------------------------------------
 * 2. WHY NOT A DIALOG ON TAP
 * ------------------------------------------------------------------------------------
 * - A tap means "show me this person". An immediate yes/no dialog that asks to delete answers
 *   a question the user did not ask, and "yes/no" is ambiguous (yes, it is gone? yes, delete?).
 * - The user may want to keep the entry: it carries a private tag and notes that are the only
 *   record of who the peer was. Showing that identity on the not-found screen lets them decide.
 * - A not-found result may be transient (see section 4). A modal delete prompt makes a recoverable
 *   situation look final.
 *
 * ------------------------------------------------------------------------------------
 * 3. WHY CONFIRM, NOT UNDO
 * ------------------------------------------------------------------------------------
 * Removal discards the tag and notes. An undo snackbar would have to re-add the contact, and
 * re-adding resolves the profile through the same lookup that just failed, so undo cannot be
 * guaranteed. Removal therefore uses a confirmation dialog ([ConfirmationDialog], the same
 * pattern as the ignore confirmation on the same screen). After a confirmed removal a plain
 * success snackbar (no action) confirms what happened.
 *
 * ------------------------------------------------------------------------------------
 * 4. WHEN THE REMOVE ACTION APPEARS
 * ------------------------------------------------------------------------------------
 * [PeerProfileGoneUiState.lookup] mirrors the existing flags and must not be collapsed:
 *
 * - [GoneLookup.NOT_FOUND] (`isNotFound`): the lookup completed and returned nothing. Remove is
 *   offered. Copy states what was observed ("not found on the network") and does not claim the
 *   profile was deleted, because the same result can occur while the node is still syncing or
 *   before the peer republishes. "Try again" is therefore shown too.
 * - [GoneLookup.LOAD_FAILED] (`isLoadFailed`): the lookup could not complete (on Connect, a
 *   dropped connection to the paired node). NO remove action: the app learned nothing about the
 *   peer. Existing retry-only behaviour is kept; the only addition is that the contact's name is
 *   shown and the copy reassures that the contact is still saved.
 * - Not a contact: the generic not-found message, no remove action (nothing to remove), plus
 *   "Try again". Not-found can be transient, and a retry costs nothing; omitting it would leave
 *   the user with only the back button on a possibly recoverable state.
 *
 * The Contacts list does NOT mark gone profiles proactively. A contact entry stores a snapshot
 * of the profile and no last-seen signal; knowing which are gone would need one lookup per
 * contact on every list open (a node round trip each on Connect, which also cannot tell "gone"
 * from "offline"). Rejected for cost and unreliability.
 *
 * ------------------------------------------------------------------------------------
 * 5. BEHAVIOUR
 * ------------------------------------------------------------------------------------
 * Peer Profile, not found + contact:
 * - Layout, top to bottom, centred: avatar, nickname, optional tag, body text, full-width
 *   "Remove from contacts" ([BisqButtonType.Danger]), then "Try again" ([BisqButtonType.Clear]).
 *   The app-bar title is not repeated.
 * - "Remove from contacts" opens the confirmation dialog. Confirm: the button shows its loading
 *   state, the existing remove-contact call runs (`ContactsServiceFacade.removeContact`), on
 *   success the screen pops back to the Contacts list and the success snackbar is shown there.
 *   On failure the dialog closes, the screen stays, and the existing contact-action-failed error
 *   snackbar is shown.
 * - "Try again" re-runs the lookup (existing retry path) and returns to the loading state.
 *
 * Contacts list:
 * - Tap: opens Peer Profile (unchanged). Long-press on a card: opens the same confirmation
 *   dialog. Haptic feedback on long-press (platform default of `combinedClickable`).
 * - Each card exposes a custom accessibility action "Remove contact" ([removeContactAction]) so
 *   the shortcut is not gesture-only. Production change: `ContactCard` swaps `debouncedClickable`
 *   for a debounced `combinedClickable` taking `onLongClick`.
 * - Confirm: the entry is removed; the list updates from `ContactsServiceFacade.contacts` and the
 *   success snackbar is shown. Removing the last contact shows the existing empty state.
 * - Long-press is a shortcut, not the primary path; the visible path is the Peer Profile screen,
 *   where the user already lands. No per-card overflow button, to keep cards uncluttered.
 *
 * Failure handling (both entry points): a failed removal closes the dialog and shows an error
 * snackbar with a "Retry" action that re-runs the removal. On Peer Profile the Remove button also
 * stays enabled, so the user can retry without the snackbar. On Connect a failed call can still
 * have been applied by the node (e.g. a timeout after the node removed the entry), so the failure
 * text says the removal could not be confirmed and asks the user to check their contacts before
 * retrying; it does not claim the contact is unchanged. The list keeps rendering from
 * `ContactsServiceFacade.contacts`, so an entry the node did remove disappears on its own.
 *
 * Loaded Peer Profile: the existing contact toggle ("Remove contact" on a profile that resolved
 * normally) opens the same confirmation dialog before removing, for consistency. "Add contact" is
 * unchanged (non-destructive, no confirmation).
 *
 * Remove button colour: [BisqButtonType.Danger] (filled red). Red is the app's delete signal, and
 * on the not-found screen it is the only destructive control, so the weight is deliberate. On a
 * loaded profile, where Remove sits among other actions, use [BisqButtonType.WarningOutline] so it
 * does not dominate.
 *
 * Opt-in long-press: `ContactCard` is used only by `ContactsListContent` (plus its previews and
 * `ContactCardUiTest`). Add a nullable `onLongClick: (() -> Unit)? = null` parameter. When null the
 * card keeps its current `debouncedClickable` behaviour unchanged; only when non-null does it use
 * a debounced `combinedClickable` with `onLongClick`. `ContactsListContent` passes it; nothing else
 * changes.
 *
 * Confirmation dialog (both entry points, one component): headline "Remove contact?", body names
 * the peer and states that the tag and notes are deleted and the action cannot be undone, buttons
 * stacked vertically: "Remove" (confirm) and "Cancel". Dismiss on outside tap equals Cancel.
 *
 * Text expansion: all copy wraps; button labels are single words or short phrases, and buttons
 * are full-width so German and Russian expansion does not clip. Touch targets use the standard
 * [BisqButton] height.
 *
 * ------------------------------------------------------------------------------------
 * 6. COPY (hardcoded in this POC; production uses i18n keys, English added to mobile.properties)
 * ------------------------------------------------------------------------------------
 * - mobile.peerProfile.contactNotFound.body, hedged variant (shown in the previews): "This profile
 *   was not found on the network. The peer may have deleted it, or your node may still be
 *   syncing." Honest in every case and prompts a retry, but longer and less decisive.
 * - mobile.peerProfile.contactNotFound.body, plain variant: "This profile is no longer available on
 *   the network." Shorter and clearer, but asserts permanence; it is only accurate if the lookup
 *   can never return not-found during node bootstrap or before the peer republishes. Choose after
 *   tracing the node-side lookup: if it can, ship the hedged variant.
 * - mobile.peerProfile.contactNotFound.remove: "Remove from contacts"
 * - mobile.peerProfile.contactNotFound.retry: "Try again"
 * - mobile.peerProfile.contactLoadFailed.body: "Could not load this profile. Check your
 *   connection and try again. Your contact is still saved."
 * - mobile.community.contacts.remove.headline: "Remove contact?"
 * - mobile.community.contacts.remove.message: "{0} will be removed from your contacts, including
 *   your tag and notes. This cannot be undone."
 * - mobile.community.contacts.remove.confirm: "Remove"
 * - mobile.community.contacts.remove.a11yAction: "Remove contact"
 * - mobile.community.contacts.remove.success: "Contact removed"
 * - mobile.community.contacts.remove.failed: "Could not confirm the contact was removed. Check your
 *   contacts before trying again."
 * - mobile.community.contacts.remove.retry: "Retry"
 * Reused as is: `action.cancel`, `mobile.peerProfile.contacts.actionFailed`,
 * `mobile.peerProfile.loadFailed` (non-contact path keeps `mobile.peerProfile.notFound`).
 *
 * ------------------------------------------------------------------------------------
 * 7. STATE AND ACTIONS
 * ------------------------------------------------------------------------------------
 * - Peer Profile: the existing `PeerProfileUiState` already has `isNotFound`, `isLoadFailed`,
 *   `isContact` and the contact's profile data is available from the contact entry; add
 *   `showRemoveContactDialog` and reuse the contact-action in-flight flag. Actions:
 *   [PeerProfileGoneUiAction]. `OnRemoveContactClick` already exists and removes immediately
 *   today; on this state it must open the dialog first, and a new confirm action performs it.
 * - Contacts: add [ContactsRemoveUiState.pendingRemoval] and [ContactsRemoveUiAction]; the
 *   confirm action calls the same `removeContact` facade method as Peer Profile. No new facade,
 *   endpoint, or analytics beyond the existing `Contact.Removed` event.
 * - On Connect, removal is a node call, so it can fail; the failure snackbar covers it.
 */

enum class GoneLookup { NOT_FOUND, LOAD_FAILED }

data class PeerProfileGoneUiState(
    val lookup: GoneLookup,
    val contact: ContactListItemUiState?,
    val showRemoveDialog: Boolean = false,
    val isRemoving: Boolean = false,
    val useHedgedCopy: Boolean = true,
)

sealed interface PeerProfileGoneUiAction {
    data object OnRemoveContactClick : PeerProfileGoneUiAction

    data object OnConfirmRemove : PeerProfileGoneUiAction

    data object OnDismissRemoveDialog : PeerProfileGoneUiAction

    data object OnRetryLoadClick : PeerProfileGoneUiAction

    data object OnRetryRemoveClick : PeerProfileGoneUiAction
}

data class ContactsRemoveUiState(
    val list: ContactsListUiState,
    val pendingRemoval: ContactListItemUiState? = null,
    /** True while the confirmed removal of [pendingRemoval] is in flight (a node call on Connect). */
    val isRemoving: Boolean = false,
    /** Set after a failed removal; drives the error snackbar whose Retry re-runs the removal. */
    val failedRemoval: ContactListItemUiState? = null,
)

sealed interface ContactsRemoveUiAction {
    data class OnContactClick(
        val profileId: String,
    ) : ContactsRemoveUiAction

    data class OnContactLongClick(
        val profileId: String,
    ) : ContactsRemoveUiAction

    data object OnConfirmRemove : ContactsRemoveUiAction

    data object OnDismissRemoveDialog : ContactsRemoveUiAction

    data object OnRetryRemove : ContactsRemoveUiAction
}

// ============================================================================================
// Composables
// ============================================================================================

/** Peer Profile body for a peer that is not found, or whose lookup failed, in the contact case. */
@Composable
fun PeerProfileGoneContent(
    uiState: PeerProfileGoneUiState,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    onAction: (PeerProfileGoneUiAction) -> Unit,
) {
    val contact = uiState.contact
    Box(
        modifier = Modifier.fillMaxSize().padding(BisqUIConstants.ScreenPadding2X),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (contact != null) {
                UserProfileIcon(
                    userProfile = contact.peerProfile,
                    userProfileIconProvider = userProfileIconProvider,
                    size = BisqUIConstants.ScreenPadding6X,
                )
                BisqGap.V1()
                BisqText.H4Regular(text = contact.peerProfile.userName, textAlign = TextAlign.Center)
                if (!contact.tag.isNullOrBlank()) {
                    BisqGap.VQuarter()
                    BisqText.SmallRegularGrey(text = contact.tag, textAlign = TextAlign.Center)
                }
                BisqGap.V2()
            }
            val body =
                when (uiState.lookup) {
                    GoneLookup.NOT_FOUND ->
                        if (uiState.useHedgedCopy) {
                            "This profile was not found on the network. The peer may have deleted it, or your node may still be syncing."
                        } else {
                            "This profile is no longer available on the network."
                        }
                    GoneLookup.LOAD_FAILED ->
                        "Could not load this profile. Check your connection and try again. Your contact is still saved."
                }
            BisqText.BaseRegular(
                text = body,
                color = if (uiState.lookup == GoneLookup.NOT_FOUND) BisqTheme.colors.white else BisqTheme.colors.warning,
                textAlign = TextAlign.Center,
            )
            BisqGap.V3()
            // Remove is offered only for a definitive not-found, never for a failed lookup.
            if (uiState.lookup == GoneLookup.NOT_FOUND && contact != null) {
                BisqButton(
                    text = "Remove from contacts",
                    type = BisqButtonType.Danger,
                    fullWidth = true,
                    isLoading = uiState.isRemoving,
                    onClick = { onAction(PeerProfileGoneUiAction.OnRemoveContactClick) },
                )
                BisqGap.V1()
            }
            BisqButton(
                text = "Try again",
                type = if (uiState.lookup == GoneLookup.LOAD_FAILED) BisqButtonType.Default else BisqButtonType.Clear,
                fullWidth = true,
                disabled = uiState.isRemoving,
                onClick = { onAction(PeerProfileGoneUiAction.OnRetryLoadClick) },
            )
        }
    }
    if (uiState.showRemoveDialog && contact != null) {
        RemoveContactDialog(
            peerName = contact.peerProfile.userName,
            isRemoving = uiState.isRemoving,
            onConfirm = { onAction(PeerProfileGoneUiAction.OnConfirmRemove) },
            onDismiss = { onAction(PeerProfileGoneUiAction.OnDismissRemoveDialog) },
        )
    }
}

/**
 * Action area of a loaded Peer Profile for a peer that is already a contact. Only the contact
 * action row is shown; the rest of the profile body is unchanged and out of scope.
 */
@Composable
fun PeerProfileLoadedContactActions(
    contact: ContactListItemUiState,
    showRemoveDialog: Boolean,
    isRemoving: Boolean,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    onAction: (PeerProfileGoneUiAction) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(BisqUIConstants.ScreenPadding2X),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        UserProfileIcon(
            userProfile = contact.peerProfile,
            userProfileIconProvider = userProfileIconProvider,
            size = BisqUIConstants.ScreenPadding6X,
        )
        BisqGap.V1()
        BisqText.H4Regular(text = contact.peerProfile.userName, textAlign = TextAlign.Center)
        BisqGap.V3()
        BisqButton(
            text = "Remove contact",
            type = BisqButtonType.WarningOutline,
            fullWidth = true,
            isLoading = isRemoving,
            onClick = { onAction(PeerProfileGoneUiAction.OnRemoveContactClick) },
        )
    }
    if (showRemoveDialog) {
        RemoveContactDialog(
            peerName = contact.peerProfile.userName,
            isRemoving = isRemoving,
            onConfirm = { onAction(PeerProfileGoneUiAction.OnConfirmRemove) },
            onDismiss = { onAction(PeerProfileGoneUiAction.OnDismissRemoveDialog) },
        )
    }
}

/** Contacts tab body with the long-press / accessibility remove shortcut on every card. */
@Composable
fun ContactsRemovableList(
    uiState: ContactsRemoveUiState,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    onAction: (ContactsRemoveUiAction) -> Unit,
) {
    // Loading and empty states stay with ContactsListContent.
    if (uiState.list.isLoading || uiState.list.contacts.isEmpty()) {
        ContactsListContent(
            uiState = uiState.list,
            userProfileIconProvider = userProfileIconProvider,
            onContactClick = {},
        )
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            uiState.list.contacts.forEach { contact ->
                ContactCard(
                    contact = contact,
                    userProfileIconProvider = userProfileIconProvider,
                    onClick = { onAction(ContactsRemoveUiAction.OnContactClick(contact.id)) },
                    modifier =
                        Modifier.removeContactAction(
                            onRemove = { onAction(ContactsRemoveUiAction.OnContactLongClick(contact.id)) },
                        ),
                )
            }
        }
    }
    uiState.pendingRemoval?.let { pending ->
        RemoveContactDialog(
            peerName = pending.peerProfile.userName,
            isRemoving = uiState.isRemoving,
            onConfirm = { onAction(ContactsRemoveUiAction.OnConfirmRemove) },
            onDismiss = { onAction(ContactsRemoveUiAction.OnDismissRemoveDialog) },
        )
    }
}

/**
 * Screen-reader entry point for the long-press shortcut. In production the long-press gesture
 * itself lives in `ContactCard` (`combinedClickable`); this modifier adds the equivalent
 * TalkBack/VoiceOver custom action so the shortcut is reachable without the gesture.
 */
private fun Modifier.removeContactAction(onRemove: () -> Unit): Modifier =
    semantics {
        customActions =
            listOf(
                CustomAccessibilityAction("Remove contact") {
                    onRemove()
                    true
                },
            )
    }

/** One confirmation dialog for both entry points. */
@Composable
fun RemoveContactDialog(
    peerName: String,
    isRemoving: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ConfirmationDialog(
        headline = "Remove contact?",
        headlineColor = BisqTheme.colors.warning,
        headlineLeftIcon = { WarningIcon() },
        message = "$peerName will be removed from your contacts, including your tag and notes. This cannot be undone.",
        confirmButtonText = "Remove",
        dismissButtonText = "Cancel",
        confirmButtonLoading = isRemoving,
        verticalButtonPlacement = true,
        onConfirm = onConfirm,
        onDismiss = { onDismiss() },
    )
}

/**
 * Stand-in for the app's success snackbar (`BisqSnackbar`, `SnackbarType.SUCCESS`), which is
 * driven by the presenter's `showSnackbar`; rendered statically so it shows in previews.
 */
@Composable
private fun SimulatedSnackbar(
    message: String,
    isError: Boolean = false,
    actionLabel: String? = null,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(BisqUIConstants.ScreenPadding),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                    .background((if (isError) BisqTheme.colors.danger else BisqTheme.colors.primaryDim).copy(alpha = 0.95f))
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BisqText.BaseRegular(
                    text = message,
                    color = if (isError) BisqTheme.colors.dark_grey10 else BisqTheme.colors.white,
                    modifier = Modifier.weight(1f),
                )
                if (actionLabel != null) {
                    BisqText.BaseBold(
                        text = actionLabel,
                        color = if (isError) BisqTheme.colors.dark_grey10 else BisqTheme.colors.white,
                        modifier = Modifier.padding(start = BisqUIConstants.ScreenPadding),
                    )
                }
            }
        }
    }
}

// ============================================================================================
// Simulated data
// ============================================================================================

private val previewIconProvider: suspend (UserProfileVO) -> PlatformImage = { createEmptyImage() }

private fun simulatedContact(
    name: String,
    tag: String? = null,
    reason: ContactReasonEnum = ContactReasonEnum.BISQ_EASY_TRADE,
    date: String = "12 Jul 2026",
    trust: Double = 0.6,
): ContactListItemUiState =
    ContactListItemUiState(
        id = name,
        peerProfile = createMockUserProfile(name),
        trustScore = trust,
        contactReason = reason,
        dateAddedLabel = date,
        tag = tag,
    )

private fun simulatedContacts(): List<ContactListItemUiState> =
    listOf(
        simulatedContact("SatoshiFan#1234", tag = "Reliable SEPA trader", trust = 0.92),
        simulatedContact("BitcoinBee#5678", reason = ContactReasonEnum.PRIVATE_CHAT, date = "3 weeks ago", trust = 0.5),
        simulatedContact("OldTimer#4242", tag = "Met in Discussions", reason = ContactReasonEnum.MANUALLY_ADDED, date = "5 Jan 2026", trust = 0.15),
    )

// ============================================================================================
// Previews
// ============================================================================================

@ExcludeFromCoverage
@Preview(name = "1 Contacts list - long-press / a11y action available on every card")
@Composable
private fun ContactsList_Default_Preview() {
    BisqTheme.Preview {
        ContactsRemovableList(
            uiState = ContactsRemoveUiState(list = ContactsListUiState(contacts = simulatedContacts())),
            userProfileIconProvider = previewIconProvider,
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "2 Contacts list - confirm dialog after long-press")
@Composable
private fun ContactsList_ConfirmDialog_Preview() {
    val contacts = simulatedContacts()
    BisqTheme.Preview {
        ContactsRemovableList(
            uiState = ContactsRemoveUiState(list = ContactsListUiState(contacts = contacts), pendingRemoval = contacts[2]),
            userProfileIconProvider = previewIconProvider,
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "3 Contacts list - after removal, success snackbar")
@Composable
private fun ContactsList_AfterRemoval_Preview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.fillMaxSize()) {
            ContactsRemovableList(
                uiState = ContactsRemoveUiState(list = ContactsListUiState(contacts = simulatedContacts().take(2))),
                userProfileIconProvider = previewIconProvider,
                onAction = {},
            )
            SimulatedSnackbar(message = "Contact removed")
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "4 Contacts list - empty after last removal")
@Composable
private fun ContactsList_EmptyAfterLastRemoval_Preview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.fillMaxSize()) {
            ContactsRemovableList(
                uiState = ContactsRemoveUiState(list = ContactsListUiState()),
                userProfileIconProvider = previewIconProvider,
                onAction = {},
            )
            SimulatedSnackbar(message = "Contact removed")
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "5 Peer profile - not found, is a contact (remove + try again)")
@Composable
private fun PeerProfileGone_NotFoundContact_Preview() {
    BisqTheme.Preview {
        PeerProfileGoneContent(
            uiState = PeerProfileGoneUiState(GoneLookup.NOT_FOUND, simulatedContact("OldTimer#4242", tag = "Met in Discussions")),
            userProfileIconProvider = previewIconProvider,
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "6 Peer profile - not found, confirm dialog")
@Composable
private fun PeerProfileGone_ConfirmDialog_Preview() {
    BisqTheme.Preview {
        PeerProfileGoneContent(
            uiState =
                PeerProfileGoneUiState(
                    GoneLookup.NOT_FOUND,
                    simulatedContact("OldTimer#4242", tag = "Met in Discussions"),
                    showRemoveDialog = true,
                ),
            userProfileIconProvider = previewIconProvider,
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "7 Peer profile - not found, removing (loading)")
@Composable
private fun PeerProfileGone_Removing_Preview() {
    BisqTheme.Preview {
        PeerProfileGoneContent(
            uiState =
                PeerProfileGoneUiState(
                    GoneLookup.NOT_FOUND,
                    simulatedContact("OldTimer#4242"),
                    showRemoveDialog = true,
                    isRemoving = true,
                ),
            userProfileIconProvider = previewIconProvider,
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "8 Peer profile - remove failed (error snackbar with Retry, Remove stays enabled)")
@Composable
private fun PeerProfileGone_RemoveFailed_Preview() {
    BisqTheme.Preview {
        Box(modifier = Modifier.fillMaxSize()) {
            PeerProfileGoneContent(
                uiState = PeerProfileGoneUiState(GoneLookup.NOT_FOUND, simulatedContact("OldTimer#4242")),
                userProfileIconProvider = previewIconProvider,
                onAction = {},
            )
            SimulatedSnackbar(message = "Could not confirm the contact was removed. Check your contacts before trying again.", isError = true, actionLabel = "Retry")
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "9 Peer profile - lookup FAILED (network): NO remove, retry only")
@Composable
private fun PeerProfileGone_LoadFailed_Preview() {
    BisqTheme.Preview {
        PeerProfileGoneContent(
            uiState = PeerProfileGoneUiState(GoneLookup.LOAD_FAILED, simulatedContact("OldTimer#4242", tag = "Met in Discussions")),
            userProfileIconProvider = previewIconProvider,
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "10 Peer profile - not found, NOT a contact (no remove, Try again)")
@Composable
private fun PeerProfileGone_NotFoundNotContact_Preview() {
    BisqTheme.Preview {
        PeerProfileGoneContent(
            uiState = PeerProfileGoneUiState(GoneLookup.NOT_FOUND, contact = null),
            userProfileIconProvider = previewIconProvider,
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "11 Contacts list - remove failed (error snackbar with Retry, entry kept)")
@Composable
private fun ContactsList_RemoveFailed_Preview() {
    val contacts = simulatedContacts()
    BisqTheme.Preview {
        Box(modifier = Modifier.fillMaxSize()) {
            ContactsRemovableList(
                uiState = ContactsRemoveUiState(list = ContactsListUiState(contacts = contacts), failedRemoval = contacts[2]),
                userProfileIconProvider = previewIconProvider,
                onAction = {},
            )
            SimulatedSnackbar(message = "Could not confirm the contact was removed. Check your contacts before trying again.", isError = true, actionLabel = "Retry")
        }
    }
}

@ExcludeFromCoverage
@Preview(name = "12 Loaded peer profile (contact) - remove action")
@Composable
private fun PeerProfileLoaded_RemoveAction_Preview() {
    BisqTheme.Preview {
        PeerProfileLoadedContactActions(
            contact = simulatedContact("SatoshiFan#1234", tag = "Reliable SEPA trader"),
            showRemoveDialog = false,
            isRemoving = false,
            userProfileIconProvider = previewIconProvider,
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "13 Loaded peer profile (contact) - remove now confirms")
@Composable
private fun PeerProfileLoaded_RemoveConfirm_Preview() {
    BisqTheme.Preview {
        PeerProfileLoadedContactActions(
            contact = simulatedContact("SatoshiFan#1234", tag = "Reliable SEPA trader"),
            showRemoveDialog = true,
            isRemoving = false,
            userProfileIconProvider = previewIconProvider,
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "14 Peer profile - not found, plain copy alternative")
@Composable
private fun PeerProfileGone_PlainCopy_Preview() {
    BisqTheme.Preview {
        PeerProfileGoneContent(
            uiState = PeerProfileGoneUiState(GoneLookup.NOT_FOUND, simulatedContact("OldTimer#4242"), useHedgedCopy = false),
            userProfileIconProvider = previewIconProvider,
            onAction = {},
        )
    }
}
