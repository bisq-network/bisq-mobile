package network.bisq.mobile.presentation.startup.user_agreement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.domain.usecase.startup.ResolveStartupDestinationUseCase
import network.bisq.mobile.domain.usecase.startup.StartupDestination
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter

open class UserAgreementPresenter(
    mainPresenter: MainPresenter,
    private val settingsServiceFacade: SettingsServiceFacade,
    private val resolveStartupDestination: ResolveStartupDestinationUseCase,
) : BasePresenter(mainPresenter),
    IAgreementPresenter {
    override fun analyticsScreenEvent(): AnalyticsEvent.ScreenOpened = AnalyticsEvent.ScreenOpened.UserAgreement

    private val _uiState = MutableStateFlow(UserAgreementUiState())
    override val uiState: StateFlow<UserAgreementUiState> = _uiState.asStateFlow()

    private val _isAcceptTermsEnabled = MutableStateFlow(true)
    override val isAcceptTermsEnabled: StateFlow<Boolean> = _isAcceptTermsEnabled.asStateFlow()

    override fun onAction(action: UserAgreementUiAction) {
        when (action) {
            is UserAgreementUiAction.OnAcceptedChange -> _uiState.value = UserAgreementUiState(isAccepted = action.accepted)
            UserAgreementUiAction.OnAcceptTerms -> onAcceptTerms()
        }
    }

    private fun onAcceptTerms() {
        guardedSuspendAction(
            _isAcceptTermsEnabled,
            "onAcceptTerms",
            reEnableGuardOnComplete = false,
        ) {
            settingsServiceFacade
                .confirmTacAccepted(true)
                .onSuccess {
                    // Core asks for the terms again when their version changes, so the user may already
                    // have profiles: route the same way the splash does rather than assuming a fresh install.
                    when (resolveStartupDestination()) {
                        StartupDestination.HOME -> navigateToHome()
                        StartupDestination.ONBOARDING -> {
                            navigateToOnboarding()
                            showSnackbar("mobile.startup.agreement.welcome".i18n())
                        }
                        StartupDestination.CREATE_PROFILE -> navigateToCreateProfile()
                    }
                }.onFailure { exception ->
                    handleError(exception)
                    _isAcceptTermsEnabled.value = true
                }
        }
    }

    private fun navigateToHome() {
        navigateTo(NavRoute.TabContainer) {
            it.popUpTo(NavRoute.UserAgreement) { inclusive = true }
        }
    }

    private fun navigateToOnboarding() {
        navigateTo(NavRoute.Onboarding) {
            it.popUpTo(NavRoute.UserAgreement) { inclusive = true }
        }
    }

    private fun navigateToCreateProfile() {
        navigateTo(NavRoute.CreateProfile(true)) {
            it.popUpTo(NavRoute.UserAgreement) { inclusive = true }
        }
    }
}
