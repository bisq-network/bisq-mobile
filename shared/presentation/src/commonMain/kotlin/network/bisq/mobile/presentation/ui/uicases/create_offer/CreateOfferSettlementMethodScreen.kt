package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
fun CreateOfferSettlementMethodScreen() {
    val presenter: CreateOfferPaymentMethodPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val selectedBaseSidePaymentMethods = remember { presenter.selectedBaseSidePaymentMethods }

    MultiScreenWizardScaffold(
        "mobile.bisqEasy.createOffer.progress.baseSidePaymentMethod".i18n(),
        stepIndex = 6,
        stepsLength = 7,
        prevOnClick = { presenter.onBack() },
        nextButtonText = "action.next".i18n(),
        nextOnClick = { presenter.onBaseSideNext() },
        snackbarHostState = presenter.getSnackState(),
        showUserAvatar = false,
        extraActions = {
            BisqIconButton(onClick = {
                presenter.onClose()
            }, size = BisqUIConstants.topBarAvatarSize) {
                CloseIcon()
            }
        }
    ) {
        BisqText.h3Regular("mobile.bisqEasy.createOffer.paymentMethods.headline.btc".i18n())

        BisqGap.V1()

        PaymentMethodCard(
            title = presenter.baseSideHeadline,
            imagePaths = presenter.getBaseSidePaymentMethodsImagePaths(),
            availablePaymentMethods = presenter.availableBaseSidePaymentMethods,
            selectedPaymentMethods = selectedBaseSidePaymentMethods,
            onToggle = { selected -> presenter.onToggleBaseSidePaymentMethod(selected) },
        )
    }
}