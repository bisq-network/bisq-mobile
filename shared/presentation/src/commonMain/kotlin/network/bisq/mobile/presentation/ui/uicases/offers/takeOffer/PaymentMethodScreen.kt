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
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

interface ITakeOfferPaymentMethodPresenter : ViewPresenter {
    // TODO: Update later to refer to a single OfferListItem
    val offerListItems: StateFlow<List<OfferListItem>>

    fun paymentMethodConfirmed()
}

@Composable
fun TakeOfferPaymentMethodScreen() {
    val strings = LocalStrings.current.bisqEasy
    val presenter: ITakeOfferPaymentMethodPresenter = koinInject()

    val offer = presenter.offerListItems.collectAsState().value.first()
    var customMethodCounter = 1

    MultiScreenWizardScaffold(
        strings.bisqEasy_takeOffer_progress_method,
        stepIndex = 2,
        stepsLength = 3,
        prevOnClick = { presenter.goBack() },
        nextOnClick = { presenter.paymentMethodConfirmed() }
    ) {

        BisqText.h3Regular(
            text = strings.bisqEasy_takeOffer_paymentMethods_headline_fiatAndBitcoin,
            color = BisqTheme.colors.light1
        )
        BisqGap.V2()
        BisqGap.V2()
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BisqText.largeLight(
                text = strings.bisqEasy_takeOffer_paymentMethods_subtitle_fiat_buyer("USD"),
                color = BisqTheme.colors.grey2
            )
            BisqGap.V2()
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 38.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
            ) {
                offer.quoteSidePaymentMethods.forEach { paymentMethod ->
                    // TODO: Make this to Toggle buttons. Can get paymentMethod as some Enum?
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
        BisqGap.V2()
        BisqGap.V2()
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BisqText.largeLight(
                text = strings.bisqEasy_takeOffer_paymentMethods_subtitle_bitcoin_seller,
                color = BisqTheme.colors.grey2
            )
            BisqGap.V1()
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 38.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
            ) {
                offer.baseSidePaymentMethods.forEach { settlementMethod ->
                    // TODO: Make this to Toggle buttons. Can get paymentMethod as some Enum?
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

                        BisqText.baseRegular(text = settlementMethod)
                    }
                }
            }
        }
    }
}