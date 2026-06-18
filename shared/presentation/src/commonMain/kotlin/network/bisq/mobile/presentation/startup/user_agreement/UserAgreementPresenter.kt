package network.bisq.mobile.presentation.startup.user_agreement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.analytics.AnalyticsEvent
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter

open class UserAgreementPresenter(
    mainPresenter: MainPresenter,
    private val settingsServiceFacade: SettingsServiceFacade,
) : BasePresenter(mainPresenter),
    IAgreementPresenter {
    override fun analyticsScreenEvent(): AnalyticsEvent.ScreenOpened = AnalyticsEvent.ScreenOpened.UserAgreement

    private val _accepted = MutableStateFlow(false)
    override val isAccepted: StateFlow<Boolean> = _accepted.asStateFlow()

    private val _isAcceptTermsEnabled = MutableStateFlow(true)
    override val isAcceptTermsEnabled: StateFlow<Boolean> = _isAcceptTermsEnabled.asStateFlow()

    override fun onAccepted(accepted: Boolean) {
        _accepted.value = accepted
    }

    override fun onAcceptTerms() {
        if (!_isAcceptTermsEnabled.compareAndSet(expect = true, update = false)) {
            log.w { "onAcceptTerms called while accept is already in progress; ignoring" }
            return
        }

        presenterScope.launch {
            try {
                showLoading()
                settingsServiceFacade
                    .confirmTacAccepted(true)
                    .onSuccess {
                        navigateToOnboarding()
                        showSnackbar("mobile.startup.agreement.welcome".i18n())
                    }.onFailure { exception ->
                        handleError(exception)
                        _isAcceptTermsEnabled.value = true
                    }
            } finally {
                hideLoading()
            }
        }
    }

    private fun navigateToOnboarding() {
        navigateTo(NavRoute.Onboarding) {
            it.popUpTo(NavRoute.UserAgreement) { inclusive = true }
        }
    }
}
