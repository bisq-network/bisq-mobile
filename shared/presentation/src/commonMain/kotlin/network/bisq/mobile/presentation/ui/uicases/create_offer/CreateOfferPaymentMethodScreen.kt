package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.BisqIconButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.CloseIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.organisms.PaymentMethodCard
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun CreateOfferPaymentMethodSelectorScreen() {
    val presenter: CreateOfferPaymentMethodPresenter = koinInject()

    val selectedBaseSidePaymentMethods = remember { presenter.selectedBaseSidePaymentMethods }
    val selectedQuoteSidePaymentMethods: MutableStateFlow<Set<String>> = remember { presenter.selectedQuoteSidePaymentMethods }

    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        "bisqEasy.takeOffer.progress.method".i18n(),
        stepIndex = 5,
        stepsLength = 6,
        prevOnClick = { presenter.onBack() },
        nextButtonText = "action.next".i18n(),
        nextOnClick = { presenter.onNext() },
        snackbarHostState = presenter.getSnackState(),
        showUserAvatar = false,
        extraActions = {
            BisqIconButton(onClick = {
                presenter.onClose()
            }, size = BisqUIConstants.topBarAvatarSize){
                CloseIcon()
            }
        },
    ) {
        BisqText.h3Regular("bisqEasy.takeOffer.paymentMethods.headline.fiatAndBitcoin".i18n())

        BisqGap.V1()

        PaymentMethodCard(
            title = presenter.quoteSideHeadline,
            imagePaths = presenter.getQuoteSidePaymentMethodsImagePaths(),
            availablePaymentMethods = presenter.availableQuoteSidePaymentMethods,
            selectedPaymentMethods = selectedQuoteSidePaymentMethods,
            onToggle = { selected -> presenter.onToggleQuoteSidePaymentMethod(selected) },
        )

        BisqGap.V2()

        PaymentMethodCard(
            title = presenter.baseSideHeadline,
            imagePaths = presenter.getBaseSidePaymentMethodsImagePaths(),
            availablePaymentMethods = presenter.availableBaseSidePaymentMethods,
            selectedPaymentMethods = selectedBaseSidePaymentMethods,
            onToggle = { selected -> presenter.onToggleBaseSidePaymentMethod(selected) },
        )
    }
}
