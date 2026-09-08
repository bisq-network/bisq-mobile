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
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
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
                CommunityHubUiState(
                    liveSegments = ordered,
                    selectedSegment = ordered.firstOrNull(),
                    segmentUnreadCounts = communityHubService.segmentUnreadCounts.value,
                )
            },
        )
    val uiState: StateFlow<CommunityHubUiState> = _uiState.asStateFlow()

    // Deep-link target (e.g. More -> My Contacts): honored as soon as the segment is live,
    // then cleared — later liveSegments updates must not yank the user's own selection back.
    private var pendingInitialSegment: CommunitySegment? = null

    // The route argument outlives every composition of its back-stack entry, so the screen asks
    // again on each one: after a trip to the Support screen, after a rotation. Only the first ask
    // is a deep link; the rest would overwrite whatever the user picked in between. Never reset —
    // a genuinely new deep link arrives on a new back-stack entry, and so on a new presenter.
    private var initialSegmentAsked = false

    fun selectInitialSegment(segment: CommunitySegment) {
        if (initialSegmentAsked) return
        initialSegmentAsked = true
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

        communityHubService.segmentUnreadCounts
            .onEach { counts -> _uiState.update { it.copy(segmentUnreadCounts = counts) } }
            .launchIn(presenterScope)
    }

    fun onAction(action: CommunityHubUiAction) {
        when (action) {
            is CommunityHubUiAction.OnSegmentSelect ->
                _uiState.update { state ->
                    if (action.segment in state.liveSegments) state.copy(selectedSegment = action.segment) else state
                }
            // Pushed rather than selected: Support is not a segment, so the hub keeps the segment it
            // was on and backing out lands where the user left.
            CommunityHubUiAction.OnOpenSupportChannel -> navigateTo(NavRoute.SupportChannel)
        }
    }
}
