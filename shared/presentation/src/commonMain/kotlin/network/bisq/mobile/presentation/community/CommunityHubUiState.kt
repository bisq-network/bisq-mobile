package network.bisq.mobile.presentation.community

import network.bisq.mobile.domain.service.community.CommunitySegment

/**
 * @param liveSegments the segments the hub may render, in tab order. The segmented tab row
 *   only renders when there is more than one — a single-segment row would be a control with
 *   nothing to control.
 * @param selectedSegment the segment whose content fills the hub body; null when nothing is
 *   live (the hub then shows its empty state).
 * @param segmentUnreadCounts per-segment unread counts for the tab pills — the "where" to the
 *   entry badge's "whether". A segment that is not live is absent; a live segment with nothing
 *   unread is present as zero (the pill hides itself at zero).
 */
data class CommunityHubUiState(
    val liveSegments: List<CommunitySegment> = emptyList(),
    val selectedSegment: CommunitySegment? = null,
    val segmentUnreadCounts: Map<CommunitySegment, Int> = emptyMap(),
)

sealed interface CommunityHubUiAction {
    data class OnSegmentSelect(
        val segment: CommunitySegment,
    ) : CommunityHubUiAction

    data object OnOpenSupportChannel : CommunityHubUiAction
}
