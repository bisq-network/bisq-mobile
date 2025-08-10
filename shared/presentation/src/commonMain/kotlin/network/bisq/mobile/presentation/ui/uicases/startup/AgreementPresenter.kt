package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class AgreementPresenter(
    mainPresenter: MainPresenter,
    private val settingsServiceFacade: SettingsServiceFacade,
) : BasePresenter(mainPresenter), IAgreementPresenter {

    private val _accepted = MutableStateFlow(false)
    override val isAccepted: StateFlow<Boolean> get() = _accepted.asStateFlow()


    override fun onAccept(accepted: Boolean) {
        _accepted.value = accepted
    }

    override fun onAcceptClick() {
        launchUI {
            try {
                settingsServiceFacade.confirmTacAccepted(true)
                navigateToOnboarding()
            } catch (e: Exception) {
                log.e(e) { "Failed to save user agreement acceptance" }
            }

            showSnackbar("mobile.startup.agreement.welcome".i18n())
        }
    }

    private fun navigateToOnboarding() {
        navigateTo(Routes.Onboarding) {
            it.popUpTo(Routes.Agreement.name) { inclusive = true }
        }
    }

    override val terms =
        "mobile.terms.point1".i18n() +

        "mobile.terms.point2".i18n() +

        "mobile.terms.point3".i18n() +

        "mobile.terms.point4".i18n() +

        "mobile.terms.point5".i18n() +

        "mobile.terms.point6".i18n()

    override val rules = "mobile.rules".i18n()

}
