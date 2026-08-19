package network.bisq.mobile.presentation.community

import network.bisq.mobile.domain.service.community.CommunitySegment

/**
 * @param liveSegments the segments the hub may render, in tab order. The segmented tab row
 *   only renders when there is more than one — a single-segment row would be a control with
 *   nothing to control.
 * @param selectedSegment the segment whose content fills the hub body; null when nothing is
 *   live (the hub then shows its empty state).
 */
data class CommunityHubUiState(
    val liveSegments: List<CommunitySegment> = emptyList(),
    val selectedSegment: CommunitySegment? = null,
)

sealed interface CommunityHubUiAction {
    data class OnSegmentSelect(
        val segment: CommunitySegment,
    ) : CommunityHubUiAction

    data object OnOpenSupportChannel : CommunityHubUiAction
}
