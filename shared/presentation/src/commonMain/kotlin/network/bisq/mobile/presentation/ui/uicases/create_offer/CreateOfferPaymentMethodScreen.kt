package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import cafe.adriel.lyricist.LocalStrings
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.organisms.PaymentMethodCard
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

@Composable
fun CreateOfferPaymentMethodSelectorScreen() {
    val commonStrings = LocalStrings.current.common
    val stringsBisqEasy = LocalStrings.current.bisqEasy
    val presenter: CreateOfferPaymentMethodPresenter = koinInject()
    presenter.appStrings = LocalStrings.current // TODO find a more elegant solution

    val selectedBaseSidePaymentMethods = remember { presenter.selectedBaseSidePaymentMethods }
    val selectedQuoteSidePaymentMethods: MutableStateFlow<Set<String>> = remember { presenter.selectedQuoteSidePaymentMethods }

    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        stringsBisqEasy.bisqEasy_takeOffer_progress_method,
        stepIndex = 5,
        stepsLength = 6,
        prevOnClick = { presenter.onBack() },
        nextButtonText = commonStrings.buttons_next,
        nextOnClick = { presenter.onNext() },
        snackbarHostState = presenter.getSnackState()
    ) {
        BisqText.h3Regular(text = stringsBisqEasy.bisqEasy_takeOffer_paymentMethods_headline_fiatAndBitcoin)

        BisqGap.V1()

        PaymentMethodCard(
            title = presenter.quoteSideHeadline,
            imagePaths = presenter.getQuoteSidePaymentMethodsImagePaths(),
            availablePaymentMethods = presenter.availableQuoteSidePaymentMethods,
            i18n = LocalStrings.current.paymentMethod,
            selectedPaymentMethods = selectedQuoteSidePaymentMethods,
            onToggle = { selected -> presenter.onToggleQuoteSidePaymentMethod(selected) },
        )

        BisqGap.V2()

        PaymentMethodCard(
            title = presenter.baseSideHeadline,
            imagePaths = presenter.getBaseSidePaymentMethodsImagePaths(),
            availablePaymentMethods = presenter.availableBaseSidePaymentMethods,
            i18n = LocalStrings.current.paymentMethod,
            selectedPaymentMethods = selectedBaseSidePaymentMethods,
            onToggle = { selected -> presenter.onToggleBaseSidePaymentMethod(selected) },
        )
    }
}