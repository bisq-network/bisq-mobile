package network.bisq.mobile.presentation.community.contacts

import network.bisq.mobile.data.replicated.user.contact_list.ContactReasonEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

/**
 * Presentation-layer model for one row of the Contacts directory (Community hub, Contacts
 * tab — issue #1238). This is deliberately NOT a mapped domain VO: no `Contact`/
 * `ContactListEntry` domain type exists on mobile yet. Field shapes mirror bisq2 core's
 * `ContactListService` so the eventual mapper (written in the wiring PR) is a direct
 * translation rather than a redesign:
 *
 * - [tag]: user-authored short label, core limit 30 chars. Not enforced here — this is a
 *   render-time model, and truncation/validation belongs to whoever writes the value, not
 *   whoever displays it.
 * - [trustScore]: normalized `0.0..1.0`. Distinct from the 5-star [network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO]
 *   used for offer/trade reputation elsewhere in the app — a contact's trust score is a
 *   local, relationship-specific signal, not the peer's network-wide reputation.
 * - [contactReason]: mirrors bisq2 core, see [ContactReasonEnum].
 * - [dateAddedLabel]: pre-formatted display string (mirrors `formattedDate`-style fields
 *   used across the app, e.g. `TradeItemPresentationModel.formattedDate`) — locale-aware
 *   formatting happens once, upstream of the UI.
 *
 * @param id the contact list entry's own identifier (not necessarily the same as
 *   [peerProfile]'s profile id) — passed back verbatim via `onContactClick`.
 */
data class ContactListItemUiState(
    val id: String,
    val peerProfile: UserProfileVO,
    val trustScore: Double,
    val contactReason: ContactReasonEnum,
    val dateAddedLabel: String,
    val tag: String? = null,
)

/**
 * The tab body's state. Contacts directories are small (no pagination per the IA decision
 * for this tab) — a plain list is enough, no cursor/paging fields.
 */
data class ContactsListUiState(
    val contacts: List<ContactListItemUiState> = emptyList(),
    /** True until the first facade emission — renders [LoadingState], not the empty state. */
    val isLoading: Boolean = false,
)
