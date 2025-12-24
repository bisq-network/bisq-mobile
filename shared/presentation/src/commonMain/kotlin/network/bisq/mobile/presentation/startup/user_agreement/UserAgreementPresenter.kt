package network.bisq.mobile.presentation.startup.user_agreement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.BasePresenter
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter

open class UserAgreementPresenter(
    mainPresenter: MainPresenter,
    private val settingsServiceFacade: SettingsServiceFacade,
) : BasePresenter(mainPresenter),
    IAgreementPresenter {
    private val _accepted = MutableStateFlow(false)
    override val isAccepted: StateFlow<Boolean> get() = _accepted.asStateFlow()

    override fun onAccepted(accepted: Boolean) {
        _accepted.value = accepted
    }

    override fun onAcceptTerms() {
        showLoading()
        presenterScope.launch {
            try {
                settingsServiceFacade.confirmTacAccepted(true)
                navigateToOnboarding()
                showSnackbar("mobile.startup.agreement.welcome".i18n())
            } catch (e: Exception) {
                log.e(e) { "Failed to save user agreement acceptance" }
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
