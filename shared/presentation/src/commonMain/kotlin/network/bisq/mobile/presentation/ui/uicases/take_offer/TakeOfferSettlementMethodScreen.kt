package network.bisq.mobile.presentation.ui.uicases.take_offer

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.organisms.PaymentMethodCard
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun TakeOfferSettlementMethodScreen() {
    val presenter: TakeOfferPaymentMethodPresenter = koinInject()

    val baseSidePaymentMethod: MutableStateFlow<Set<String>> = remember { MutableStateFlow(emptySet()) }
    val quoteSidePaymentMethod: MutableStateFlow<Set<String>> = remember { MutableStateFlow(emptySet()) }

    fun convertToSet(value: String?): Set<String> = value?.let { setOf(it) } ?: emptySet()

    LaunchedEffect(Unit) {
        presenter.baseSidePaymentMethod.collect { value ->
            baseSidePaymentMethod.value = convertToSet(value)
        }
    }

    LaunchedEffect(Unit) {
        presenter.quoteSidePaymentMethod.collect { value ->
            quoteSidePaymentMethod.value = convertToSet(value)
        }
    }

    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        "mobile.bisqEasy.takeOffer.progress.baseSidePaymentMethod".i18n(),
        stepIndex = 3,
        stepsLength = 4,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onBaseSideNext() },
        snackbarHostState = presenter.getSnackState()
    ) {

        BisqText.h3Regular("mobile.bisqEasy.takeOffer.paymentMethods.headline.btc".i18n())

        if (presenter.hasMultipleBaseSidePaymentMethods) {
            BisqGap.V2()
            BisqGap.V2()

            PaymentMethodCard(
                title = "bisqEasy.takeOffer.paymentMethods.subtitle.bitcoin.seller".i18n(presenter.quoteCurrencyCode),
                imagePaths = presenter.getBaseSidePaymentMethodsImagePaths(),
                availablePaymentMethods = presenter.baseSidePaymentMethods,
                selectedPaymentMethods = baseSidePaymentMethod,
                onToggle = { selected -> presenter.onBaseSidePaymentMethodSelected(selected) },
            )
        }
    }

}