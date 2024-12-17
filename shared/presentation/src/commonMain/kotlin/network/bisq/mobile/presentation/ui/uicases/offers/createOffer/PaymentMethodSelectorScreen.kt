package network.bisq.mobile.presentation.ui.uicases.offers.createOffer

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.organisms.PaymentMethodCard
import network.bisq.mobile.presentation.ui.composeModels.PaymentTypeData
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

val paymentTransferList = listOf(
    PaymentTypeData(
        image = "drawable/payment/fiat/ach_transfer.png",
        title = "Venmo"
    ),
    PaymentTypeData(
        image = "drawable/payment/fiat/ach_transfer.png",
        title = "Strike"
    ),
    PaymentTypeData(
        image = "drawable/payment/fiat/ach_transfer.png",
        title = "ACH"
    ),
    PaymentTypeData(
        image = "drawable/payment/fiat/ach_transfer.png",
        title = "US Money Order"
    ),
)

val paymentReceiverList = listOf(
    PaymentTypeData(
        image = "drawable/payment/bitcoin/main_chain.png",
        title = "OnChain"
    ),
    PaymentTypeData(
        image = "drawable/payment/bitcoin/ln.png",
        title = "Lightning"
    ),
)

@Composable
fun CreateOfferPaymentMethodSelectorScreen() {
    val commonStrings = LocalStrings.current.common
    val stringsBisqEasy = LocalStrings.current.bisqEasy
    val presenter: ICreateOfferPresenter = koinInject()
    val isBuy = presenter.direction.collectAsState().value.isBuy

    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        stringsBisqEasy.bisqEasy_takeOffer_progress_method,
        stepIndex = 5,
        stepsLength = 6,
        prevOnClick = { presenter.goBack() },
        nextButtonText = commonStrings.buttons_next,
        nextOnClick = { presenter.navigateToReviewOffer() }
    ) {
        BisqText.h3Regular(
            text = stringsBisqEasy.bisqEasy_takeOffer_paymentMethods_headline_fiatAndBitcoin,
            color = BisqTheme.colors.light1
        )

        BisqGap.V1()

        val paymentMethodText = if (isBuy)
            stringsBisqEasy.bisqEasy_takeOffer_paymentMethods_subtitle_fiat_buyer("USD")
        else
            stringsBisqEasy.bisqEasy_takeOffer_paymentMethods_subtitle_fiat_seller("USD")
        PaymentMethodCard(
            paymentMethodTitle = paymentMethodText,
            paymentTypeList = paymentTransferList
        )

        BisqGap.V2()

        val settlementMethodText = if (isBuy)
            stringsBisqEasy.bisqEasy_takeOffer_paymentMethods_subtitle_bitcoin_buyer
        else
            stringsBisqEasy.bisqEasy_takeOffer_paymentMethods_subtitle_bitcoin_seller
        PaymentMethodCard(
            paymentMethodTitle = settlementMethodText,
            paymentTypeList = paymentReceiverList
        )
    }
}