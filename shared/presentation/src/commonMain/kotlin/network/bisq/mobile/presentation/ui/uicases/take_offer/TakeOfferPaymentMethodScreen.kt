package network.bisq.mobile.presentation.ui.uicases.take_offer

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.organisms.PaymentMethodCard
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.helpers.convertToSet
import org.koin.compose.koinInject

@Composable
fun TakeOfferPaymentMethodScreen() {
    val presenter: TakeOfferPaymentMethodPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val quoteSidePaymentMethod: MutableStateFlow<Set<String>> = remember { MutableStateFlow(emptySet()) }

    LaunchedEffect(Unit) {
        presenter.quoteSidePaymentMethod.collect { value ->
            quoteSidePaymentMethod.value = convertToSet(value)
        }
    }

    MultiScreenWizardScaffold(
        "mobile.bisqEasy.takeOffer.progress.quoteSidePaymentMethod".i18n(),
        stepIndex = 2,
        stepsLength = 4,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onQuoteSideNext() },
        snackbarHostState = presenter.getSnackState(),
        showUserAvatar = false,
        closeAction = true,
        onConfirmedClose = presenter::onClose,
    ) {

        BisqText.h3Light("mobile.bisqEasy.takeOffer.paymentMethods.headline.fiat".i18n())

        if (presenter.hasMultipleQuoteSidePaymentMethods) {
            BisqGap.V2()
            BisqGap.V2()

            PaymentMethodCard(
                title = (if (presenter.isTakerBtcBuyer)
                    "bisqEasy.takeOffer.paymentMethods.subtitle.fiat.seller"
                else
                    "bisqEasy.takeOffer.paymentMethods.subtitle.fiat.buyer").i18n(presenter.quoteCurrencyCode),
                imagePaths = presenter.getQuoteSidePaymentMethodsImagePaths(),
                availablePaymentMethods = presenter.quoteSidePaymentMethods.toMutableSet(),
                selectedPaymentMethods = quoteSidePaymentMethod,
                onToggle = { selected -> presenter.onQuoteSidePaymentMethodSelected(selected) },
            )
        }
    }

}