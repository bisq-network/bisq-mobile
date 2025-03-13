package network.bisq.mobile.presentation.ui.uicases.take_offer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.helpers.i18NPaymentMethod
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun TakeOfferPaymentMethodScreen() {
    val presenter: TakeOfferPaymentMethodPresenter = koinInject()

    val baseSidePaymentMethod = remember { mutableStateOf(presenter.baseSidePaymentMethod) }
    val quoteSidePaymentMethod = remember { mutableStateOf(presenter.quoteSidePaymentMethod) }

    RememberPresenterLifecycle(presenter, {
        baseSidePaymentMethod.value = presenter.baseSidePaymentMethod
        quoteSidePaymentMethod.value = presenter.quoteSidePaymentMethod
    })

    var customMethodCounter = 1

    MultiScreenWizardScaffold(
        "bisqEasy.takeOffer.progress.method".i18n(),
        stepIndex = 2,
        stepsLength = 3,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onNext() },
        snackbarHostState = presenter.getSnackState()
    ) {

        BisqText.h3Regular("bisqEasy.takeOffer.paymentMethods.headline.fiatAndBitcoin".i18n())

        if (presenter.hasMultipleQuoteSidePaymentMethods) {
            BisqGap.V2()
            BisqGap.V2()
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BisqText.largeLightGrey(
                    "bisqEasy.takeOffer.paymentMethods.subtitle.fiat.buyer".i18n(presenter.quoteCurrencyCode)
                )
                BisqGap.V2()
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 38.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
                ) {

                    presenter.quoteSidePaymentMethods.forEach { paymentMethod ->
                        // TODO: Make this to Toggle buttons. Can get paymentMethod as some Enum?

                        val isSelected = paymentMethod == quoteSidePaymentMethod.value
                        val backgroundColor by animateColorAsState(
                            targetValue = if (isSelected) BisqTheme.colors.primary else BisqTheme.colors.dark5
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape = RoundedCornerShape(6.dp))
                                .background(color = backgroundColor)
                                .clickable {
                                    quoteSidePaymentMethod.value = paymentMethod
                                    presenter.onQuoteSidePaymentMethodSelected(paymentMethod)
                                }
                                .padding(start = 18.dp)
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        )
                        {
                            DynamicImage(
                                path = "drawable/payment/fiat/${
                                    paymentMethod
                                        .lowercase()
                                        .replace("-", "_")
                                }.png",
                                fallbackPath = "drawable/payment/fiat/custom_payment_${customMethodCounter++}.png",
                                modifier = Modifier.size(15.dp),
                            )
                            BisqText.baseRegular(i18NPaymentMethod(paymentMethod))
                        }
                    }
                }
            }
        }

        if (presenter.hasMultipleBaseSidePaymentMethods) {
            BisqGap.V2()
            BisqGap.V2()
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BisqText.largeLightGrey("bisqEasy.takeOffer.paymentMethods.subtitle.bitcoin.seller".i18n())
                BisqGap.V1()
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 38.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
                ) {
                    presenter.baseSidePaymentMethods.forEach { paymentMethod ->
                        // TODO: Make this to Toggle buttons. Can get paymentMethod as some Enum?
                        val isSelected = paymentMethod == baseSidePaymentMethod.value
                        val backgroundColor by animateColorAsState(
                            targetValue = if (isSelected) BisqTheme.colors.primary else BisqTheme.colors.dark5
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape = RoundedCornerShape(6.dp))
                                .background(color = backgroundColor)
                                .clickable {
                                    baseSidePaymentMethod.value = paymentMethod
                                    presenter.onBaseSidePaymentMethodSelected(paymentMethod)
                                }
                                .padding(horizontal = 10.dp)
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DynamicImage(
                                "drawable/payment/bitcoin/${
                                    paymentMethod
                                        .lowercase()
                                        .replace("-", "_")
                                }.png",
                                modifier = Modifier.size(15.dp)
                            )

                            BisqText.baseRegular(i18NPaymentMethod(paymentMethod))
                        }
                    }
                }
            }
        }
    }
}