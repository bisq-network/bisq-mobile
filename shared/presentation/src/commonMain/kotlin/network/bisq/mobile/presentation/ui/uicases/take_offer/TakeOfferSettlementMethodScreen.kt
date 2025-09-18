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
fun TakeOfferSettlementMethodScreen() {
    val presenter: TakeOfferPaymentMethodPresenter = koinInject()
    val takeOfferPresenter: TakeOfferPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val baseSidePaymentMethod: MutableStateFlow<Set<String>> = remember { MutableStateFlow(emptySet()) }

    LaunchedEffect(Unit) {
        presenter.baseSidePaymentMethod.collect { value ->
            baseSidePaymentMethod.value = convertToSet(value)
        }
    }

    val takeOffer = takeOfferPresenter.takeOfferModel
    var stepIndex = 1
    if (takeOffer.hasAmountRange)
        stepIndex++
    if (takeOffer.hasMultipleQuoteSidePaymentMethods)
        stepIndex++

    MultiScreenWizardScaffold(
        "mobile.bisqEasy.takeOffer.progress.baseSidePaymentMethod".i18n(),
        stepIndex = stepIndex,
        stepsLength = takeOfferPresenter.totalSteps,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onBaseSideNext() },
        snackbarHostState = presenter.getSnackState(),
        showUserAvatar = false,
        closeAction = true,
        onConfirmedClose = presenter::onClose,
    ) {

        BisqText.h3Light("mobile.bisqEasy.takeOffer.paymentMethods.headline.btc".i18n())

        if (presenter.hasMultipleBaseSidePaymentMethods) {
            BisqGap.V2()
            BisqGap.V2()

            PaymentMethodCard(
                title = (if (presenter.isTakerBtcBuyer) "bisqEasy.takeOffer.paymentMethods.subtitle.bitcoin.seller"
                else "bisqEasy.takeOffer.paymentMethods.subtitle.bitcoin.buyer").i18n(),
                imagePaths = presenter.getBaseSidePaymentMethodsImagePaths(),
                availablePaymentMethods = presenter.baseSidePaymentMethods.toMutableSet(),
                selectedPaymentMethods = baseSidePaymentMethod,
                onToggle = { selected -> presenter.onBaseSidePaymentMethodSelected(selected) },
            )
        }
    }

}