package network.bisq.mobile.presentation.community

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import network.bisq.mobile.domain.service.community.CommunityHubService
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class CommunityHubPresenter(
    mainPresenter: MainPresenter,
    private val communityHubService: CommunityHubService,
) : BasePresenter(mainPresenter) {
    private val _uiState = MutableStateFlow(CommunityHubUiState())
    val uiState: StateFlow<CommunityHubUiState> = _uiState.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()
        communityHubService.liveSegments
            .onEach { live ->
                _uiState.update { state ->
                    val ordered = live.sortedBy { it.ordinal }
                    state.copy(
                        liveSegments = ordered,
                        // Keep the selection while it stays live; fall back to the first live
                        // segment (or the empty state) when it goes away.
                        selectedSegment = state.selectedSegment?.takeIf { it in live } ?: ordered.firstOrNull(),
                    )
                }
            }.launchIn(presenterScope)
    }

    fun onAction(action: CommunityHubUiAction) {
        when (action) {
            is CommunityHubUiAction.OnSegmentSelect ->
                _uiState.update { state ->
                    if (action.segment in state.liveSegments) state.copy(selectedSegment = action.segment) else state
                }
            CommunityHubUiAction.OnOpenSupportChannel -> {
                // TODO push the in-app Support chat screen once it exists
                log.i { "Support channel requested from the Community hub; screen not available yet" }
            }
        }
    }
}
