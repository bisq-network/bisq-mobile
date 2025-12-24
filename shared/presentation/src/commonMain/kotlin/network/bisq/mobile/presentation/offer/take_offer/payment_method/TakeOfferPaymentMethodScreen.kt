package network.bisq.mobile.presentation.offer.take_offer.payment_method

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.components.organisms.PaymentMethodCard
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.common.ui.utils.convertToSet
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferPresenter
import org.koin.compose.koinInject

@Composable
fun TakeOfferPaymentMethodScreen() {
    val presenter: TakeOfferPaymentMethodPresenter = koinInject()
    val takeOfferPresenter: TakeOfferPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val quoteSidePaymentMethod: MutableStateFlow<Set<String>> = remember { MutableStateFlow(emptySet()) }

    LaunchedEffect(Unit) {
        presenter.quoteSidePaymentMethod.collect { value ->
            quoteSidePaymentMethod.value = convertToSet(value)
        }
    }

    val takeOffer = takeOfferPresenter.takeOfferModel
    var stepIndex = 1
    if (takeOffer.hasAmountRange) {
        stepIndex++
    }

    MultiScreenWizardScaffold(
        "mobile.bisqEasy.takeOffer.progress.quoteSidePaymentMethod".i18n(),
        stepIndex = stepIndex,
        stepsLength = takeOfferPresenter.totalSteps,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onQuoteSideNext() },
        snackbarHostState = presenter.getSnackState(),
        showUserAvatar = false,
        closeAction = true,
        onConfirmedClose = presenter::onClose,
    ) {
        BisqText.H3Light("mobile.bisqEasy.takeOffer.paymentMethods.headline.fiat".i18n())

        if (presenter.hasMultipleQuoteSidePaymentMethods) {
            BisqGap.V2()
            BisqGap.V2()

            PaymentMethodCard(
                title =
                    (
                        if (presenter.isTakerBtcBuyer) {
                            "bisqEasy.takeOffer.paymentMethods.subtitle.fiat.seller"
                        } else {
                            "bisqEasy.takeOffer.paymentMethods.subtitle.fiat.buyer"
                        }
                    ).i18n(presenter.quoteCurrencyCode),
                imagePaths = presenter.getQuoteSidePaymentMethodsImagePaths(),
                availablePaymentMethods = presenter.quoteSidePaymentMethods.toMutableSet(),
                selectedPaymentMethods = quoteSidePaymentMethod,
                onToggle = { selected -> presenter.onQuoteSidePaymentMethodSelected(selected) },
            )
        }
    }
}
