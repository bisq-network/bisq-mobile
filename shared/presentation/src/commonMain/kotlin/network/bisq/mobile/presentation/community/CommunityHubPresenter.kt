package network.bisq.mobile.presentation.community

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.service.community.CommunityHubService
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.main.MainPresenter

class CommunityHubPresenter(
    mainPresenter: MainPresenter,
    private val communityHubService: CommunityHubService,
) : BasePresenter(mainPresenter) {
    override fun analyticsScreenEvent(): AnalyticsEvent.ScreenOpened = AnalyticsEvent.ScreenOpened.CommunityHub

    // Seeded synchronously from the service's current value (stateIn Eagerly), so the shell
    // renders the right tabs/body on its very first frame instead of a no-segment flash.
    private val _uiState =
        MutableStateFlow(
            communityHubService.liveSegments.value.sortedBy { it.ordinal }.let { ordered ->
                CommunityHubUiState(liveSegments = ordered, selectedSegment = ordered.firstOrNull())
            },
        )
    val uiState: StateFlow<CommunityHubUiState> = _uiState.asStateFlow()

    // Deep-link target (e.g. More -> My Contacts): honored as soon as the segment is live,
    // then cleared — later liveSegments updates must not yank the user's own selection back.
    private var pendingInitialSegment: CommunitySegment? = null

    fun selectInitialSegment(segment: CommunitySegment) {
        pendingInitialSegment = segment
        _uiState.update { state ->
            if (segment in state.liveSegments) {
                pendingInitialSegment = null
                state.copy(selectedSegment = segment)
            } else {
                state
            }
        }
    }

    override fun onViewAttached() {
        super.onViewAttached()
        communityHubService.liveSegments
            .onEach { live ->
                _uiState.update { state ->
                    val ordered = live.sortedBy { it.ordinal }
                    val deepLinked = pendingInitialSegment?.takeIf { it in live }?.also { pendingInitialSegment = null }
                    state.copy(
                        liveSegments = ordered,
                        // Deep-link wins once; otherwise keep the selection while it stays live and
                        // fall back to the first live segment (or the empty state) when it goes away.
                        selectedSegment = deepLinked ?: state.selectedSegment?.takeIf { it in live } ?: ordered.firstOrNull(),
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
