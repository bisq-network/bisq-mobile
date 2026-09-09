package network.bisq.mobile.presentation.settings.support

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import network.bisq.mobile.domain.service.community.CommunityHubService
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.domain.utils.DeviceInfoProvider
import network.bisq.mobile.domain.utils.StringUtils.urlEncode
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.main.MainPresenter

class SupportPresenter(
    mainPresenter: MainPresenter,
    private val versionProvider: VersionProvider,
    private val deviceInfoProvider: DeviceInfoProvider,
    private val communityHubService: CommunityHubService,
) : BasePresenter(mainPresenter) {
    // Availability seeded synchronously from the service's current value (stateIn Eagerly), the
    // way CommunityHubPresenter seeds its own state: this presenter is a Koin factory behind
    // RememberPresenterLifecycle, so the Help screen builds a fresh one every time it enters
    // composition, and a flag that only fills in onViewAttached pops the entry in a frame late on
    // every return from the Support channel.
    //
    // The Support channel is offered when DISCUSSIONS is live — a proxy for public chat being
    // served through [CommunityHubService.REQUIRED_FEATURES]. Exact predicate:
    // `PublicChatServiceFacade.isSupported`.
    private val _uiState =
        MutableStateFlow(
            SupportUiState(isSupportChannelAvailable = CommunitySegment.DISCUSSIONS in communityHubService.liveSegments.value),
        )
    val uiState: StateFlow<SupportUiState> = _uiState.asStateFlow()

    override fun onViewAttached() {
        super.onViewAttached()

        communityHubService.liveSegments
            .onEach { live ->
                _uiState.update { it.copy(isSupportChannelAvailable = CommunitySegment.DISCUSSIONS in live) }
            }.launchIn(presenterScope)

        val versionInfo = versionProvider.getVersionInfo(isDemo(), isIOS())
        val deviceInfo = deviceInfoProvider.getDeviceInfo()

        val body = "mobile.support.troubleShooting.github.body".i18n(versionInfo, deviceInfo)
        _uiState.update { it.copy(reportUrl = BisqLinks.BISQ_MOBILE_GH_ISSUES + "/new?body=" + body.urlEncode()) }
    }

    fun onAction(action: SupportUiAction) {
        when (action) {
            SupportUiAction.OnOpenSupportChannel -> navigateTo(NavRoute.SupportChannel)
            SupportUiAction.OnRestartApp -> restartApp()
            SupportUiAction.OnTerminateApp -> terminateApp()
        }
    }
}
