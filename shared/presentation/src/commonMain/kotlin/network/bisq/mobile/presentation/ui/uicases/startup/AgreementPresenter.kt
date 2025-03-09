package network.bisq.mobile.presentation.ui.uicases.startup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.domain.service.settings.SettingsServiceFacade
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.navigation.Routes

open class AgreementPresenter(
    mainPresenter: MainPresenter,
    private val settingsServiceFacade: SettingsServiceFacade,
) : BasePresenter(mainPresenter), IAgreementPresenter {

    private val _accepted = MutableStateFlow(false)
    override val isAccepted: StateFlow<Boolean> = _accepted


    override fun onAccept(accepted: Boolean) {
        _accepted.value = accepted
    }

    override fun onAcceptClick() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                settingsServiceFacade.confirmTacAccepted(true)
                navigateToOnboarding()
            } catch (e: Exception) {
                log.e(e) { "Failed to save user agreement acceptance" }
            }

            showSnackbar("Welcome to Bisq!")
        }
    }

    private fun navigateToOnboarding() {
        navigateTo(Routes.Onboarding) {
            it.popUpTo(Routes.Agreement.name) { inclusive = true }
        }
    }

    override val terms =
        "1. The user is responsible for using the software in compliance with local laws. Don't use the software if using it is not legal in your jurisdiction.\n\n" +

                "2. Any market prices, network fee estimates, or other data obtained from servers operated by the Bisq DAO is provided on an 'as is, as available' basis without representation or warranty of any kind. It is your responsibility to verify any data provided in regards to inaccuracies or omissions.\n\n" +

                "3. Any Fiat payment method carries a potential risk for bank chargeback. By accepting the \"User Agreement\" the user confirms " +
                "to be aware of those risks and in no case will claim legal responsibility to the authors or copyright holders of the software.\n\n" +

                "4. Any dispute, controversy or claim arising out of or relating to the use of the software shall be settled by arbitration in " +
                "accordance with the Bisq arbitration rules as at present in force. The arbitration is conducted online. " +
                "The language to be used in the arbitration proceedings shall be English if not otherwise stated.\n\n" +

                "5. In no event, unless for damages caused by acts of intent and gross negligence, damages resulting from personal injury, " +
                "or damages ensuing from other instances where liability is required by applicable law or agreed to in writing, will any " +
                "developer, copyright holder and/or any other party who modifies and/or conveys the software as permitted above or " +
                "facilitates its operation, be liable for damages, including any general, special, incidental or consequential damages " +
                "arising out of the use or inability to use the software (including but not limited to loss of data or data being " +
                "rendered inaccurate or losses sustained by you or third parties or a failure of the software to operate with any " +
                "other software), even if such developer, copyright holder and/or other party has been advised of the possibility of such damages.\n\n" +

                "6. The user confirms that they have read and agreed to the rules regarding the dispute process:"

    override val rules =
        "  - Leave the \"reason for payment\" field empty. NEVER put the trade ID or any other text like 'bitcoin', 'BTC', or 'Bisq'.\n" +
                "  - If the bank of the fiat sender charges fees, the sender (BTC buyer) has to cover the fees.\n" +
                "  - In case of mediation, you must cooperate with the mediator and respond to each message within 48 hours.\n" +
                "  - The mediator has no enforcement power over the trade. They can only help the traders to come to a cooperative resolution.\n" +
                "  - In case of clear evidence for a scam or severe violation of the trade rules the mediator can ban the misbehaving trader and in " +
                "case that the trader was the Bitcoin seller and used 'account age' or 'signed account age witness' as reputation source, they will also " +
                "get banned on Bisq 1. If the seller has used 'bonded BSQ' as reputation source the mediator will report the incident to the DAO and " +
                "make a proposal for confiscating their bonded BSQ."

}
