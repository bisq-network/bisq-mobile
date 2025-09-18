package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.organisms.PaymentMethodCard
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun CreateOfferPaymentMethodScreen() {
    val presenter: CreateOfferPaymentMethodPresenter = koinInject()
    val createPresenter: CreateOfferPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val selectedQuoteSidePaymentMethods: MutableStateFlow<Set<String>> = remember { presenter.selectedQuoteSidePaymentMethods }
    val availableQuoteSidePaymentMethods by presenter.availableQuoteSidePaymentMethods.collectAsState()

    MultiScreenWizardScaffold(
        "mobile.bisqEasy.createOffer.progress.quoteSidePaymentMethod".i18n(),
        stepIndex = if (createPresenter.skipCurrency) 4 else 5,
        stepsLength = if (createPresenter.skipCurrency) 6 else 7,
        prevOnClick = { presenter.onBack() },
        nextButtonText = "action.next".i18n(),
        nextOnClick = { presenter.onQuoteSideNext() },
        snackbarHostState = presenter.getSnackState(),
        showUserAvatar = false,
        closeAction = true,
        onConfirmedClose = presenter::onClose,
    ) {
        BisqText.h3Light("mobile.bisqEasy.createOffer.paymentMethods.headline.fiat".i18n())

        BisqGap.V1()

        PaymentMethodCard(
            title = presenter.quoteSideHeadline,
            imagePaths = presenter.getQuoteSidePaymentMethodsImagePaths(),
            availablePaymentMethods = availableQuoteSidePaymentMethods,
            selectedPaymentMethods = selectedQuoteSidePaymentMethods,
            onToggle = presenter::onToggleQuoteSidePaymentMethod,
            showCustomPayment = true,
            onAddCustomPayment = { customPayment ->
                presenter.addCustomPayment(customPayment)
                presenter.onToggleQuoteSidePaymentMethod(customPayment)
            },
            onRemoveCustomPayment = { customPayment ->
                presenter.removeCustomPayment(customPayment)
            }
        )
    }
}
