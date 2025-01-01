package network.bisq.mobile.presentation.ui.uicases.trade.take_offer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.organisms.PaymentMethodCard
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

@Composable
fun TakeOfferPaymentMethodScreen() {
    val strings = LocalStrings.current.bisqEasy
    val presenter: TakeOfferPaymentMethodPresenter = koinInject()

    val baseSidePaymentMethod = remember { mutableStateOf(presenter.baseSidePaymentMethod) }
    val quoteSidePaymentMethod = remember { mutableStateOf(presenter.quoteSidePaymentMethod) }
    val quoteCurrencyCode = remember { mutableStateOf(presenter.quoteCurrencyCode) }

    RememberPresenterLifecycle(presenter, {
        baseSidePaymentMethod.value = presenter.baseSidePaymentMethod
        quoteSidePaymentMethod.value = presenter.quoteSidePaymentMethod
        quoteCurrencyCode.value = presenter.quoteCurrencyCode
    })

    MultiScreenWizardScaffold(
        strings.bisqEasy_takeOffer_progress_method,
        stepIndex = 2,
        stepsLength = 3,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onNext() },
        snackbarHostState = presenter.getSnackState(),
    ) {

        BisqText.h3Regular(
            text = strings.bisqEasy_takeOffer_paymentMethods_headline_fiatAndBitcoin,
            color = BisqTheme.colors.light1
        )

        if (presenter.hasMultipleQuoteSidePaymentMethods) {

            BisqGap.V2()

            PaymentMethodCard(
                title = strings.bisqEasy_takeOffer_paymentMethods_subtitle_fiat_buyer(quoteCurrencyCode.value),
                imagePaths = presenter.getQuoteSidePaymentMethodsImagePaths(),
                availablePaymentMethods = presenter.quoteSidePaymentMethods,
                i18n = LocalStrings.current.paymentMethod,
                selectedPaymentMethods = presenter.getPaymentMethodAsSet(quoteSidePaymentMethod.value),
                onToggle = { paymentMethod ->
                    quoteSidePaymentMethod.value = paymentMethod
                    presenter.onQuoteSidePaymentMethodSelected(paymentMethod)
                },
            )

        }

        if (presenter.hasMultipleBaseSidePaymentMethods) {

            BisqGap.V2()

            PaymentMethodCard(
                title = strings.bisqEasy_takeOffer_paymentMethods_subtitle_bitcoin_seller,
                imagePaths = presenter.getBaseSidePaymentMethodsImagePaths(),
                availablePaymentMethods = presenter.baseSidePaymentMethods,
                i18n = LocalStrings.current.paymentMethod,
                selectedPaymentMethods = presenter.getPaymentMethodAsSet(baseSidePaymentMethod.value),
                onToggle = { paymentMethod ->
                    baseSidePaymentMethod.value = paymentMethod
                    presenter.onBaseSidePaymentMethodSelected(paymentMethod)
                },
            )

        }
    }
}