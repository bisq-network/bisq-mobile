package network.bisq.mobile.presentation.ui.uicases.offers.takeOffer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import org.koin.compose.koinInject

@Composable
fun TakeOfferPaymentMethodScreen() {
    val strings = LocalStrings.current.common
    val presenter: PaymentMethodPresenter = koinInject()

    val offer = presenter.offerListItems.collectAsState().value.first()

    var customMethodCounter = 1
    MultiScreenWizardScaffold(
        strings.take_offer,
        stepIndex = 2,
        stepsLength = 3,
        prevOnClick = { presenter.goBack() },
        nextOnClick = { presenter.paymentMethodConfirmed() }
    ) {

        BisqText.h3Regular(
            text = "Which payment and settlement method do you want to use?",
            color = BisqTheme.colors.light1
        )
        Spacer(modifier = Modifier.height(22.dp))
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BisqText.largeLight(
                text = "Choose a payment method to transfer USD",
                color = BisqTheme.colors.grey2
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 38.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                offer.quoteSidePaymentMethods.forEach { paymentMethod ->
                    // TODO: Make this to Toggle buttons
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(shape = RoundedCornerShape(6.dp))
                            .background(color = BisqTheme.colors.dark5).padding(start = 18.dp)
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DynamicImage(
                            path = "drawable/payment/fiat/${
                                paymentMethod
                                    .lowercase()
                                    .replace("-", "_")
                            }.png",
                            fallbackPath = "drawable/payment/fiat/custom_payment_${customMethodCounter++}.png",
                            modifier = Modifier.size(15.dp),
                        )
                        BisqText.baseRegular(
                            text = paymentMethod
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(56.dp))
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BisqText.largeLight(
                text = "Choose a settlement method to send Bitcoin",
                color = BisqTheme.colors.grey2
            )
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 38.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {

                offer.baseSidePaymentMethods.forEach { settlementMethod ->
                    // TODO: Make this to Toggle buttons
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(shape = RoundedCornerShape(6.dp))
                            .background(color = BisqTheme.colors.dark5).padding(start = 18.dp)
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DynamicImage(
                            "drawable/payment/bitcoin/${
                                settlementMethod
                                    .lowercase()
                                    .replace("-", "_")
                            }.png",
                            modifier = Modifier.size(15.dp)
                        )

                        BisqText.baseRegular(
                            text = settlementMethod
                        )
                    }
                }
            }
        }
    }
}