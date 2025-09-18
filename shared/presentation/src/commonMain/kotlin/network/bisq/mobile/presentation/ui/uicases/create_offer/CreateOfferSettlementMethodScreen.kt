package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.organisms.PaymentMethodCard
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun CreateOfferSettlementMethodScreen() {
    val presenter: CreateOfferPaymentMethodPresenter = koinInject()
    val createPresenter: CreateOfferPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val selectedBaseSidePaymentMethods = remember { presenter.selectedBaseSidePaymentMethods }
    val availableBaseSidePaymentMethods by presenter.availableBaseSidePaymentMethods.collectAsState()

    MultiScreenWizardScaffold(
        "mobile.bisqEasy.createOffer.progress.baseSidePaymentMethod".i18n(),
        stepIndex = if (createPresenter.skipCurrency) 5 else 6,
        stepsLength = if (createPresenter.skipCurrency) 6 else 7,
        prevOnClick = { presenter.onBack() },
        nextButtonText = "action.next".i18n(),
        nextOnClick = { presenter.onBaseSideNext() },
        snackbarHostState = presenter.getSnackState(),
        showUserAvatar = false,
        closeAction = true,
        onConfirmedClose = presenter::onClose,
    ) {
        BisqText.h3Light("mobile.bisqEasy.createOffer.paymentMethods.headline.btc".i18n())

        BisqGap.V1()

        PaymentMethodCard(
            title = presenter.baseSideHeadline,
            imagePaths = presenter.getBaseSidePaymentMethodsImagePaths(),
            availablePaymentMethods = availableBaseSidePaymentMethods,
            selectedPaymentMethods = selectedBaseSidePaymentMethods,
            onToggle = { selected -> presenter.onToggleBaseSidePaymentMethod(selected) },
        )
    }
}