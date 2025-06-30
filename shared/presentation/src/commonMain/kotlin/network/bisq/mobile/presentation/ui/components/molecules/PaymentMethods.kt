package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.helpers.i18NPaymentMethod
import network.bisq.mobile.presentation.ui.theme.BisqTheme

// TODO: Get params and render apt
@Composable
fun PaymentMethods(
    baseSidePaymentMethods: List<String>,
    quoteSidePaymentMethods: List<String>
) {
    var customMethodCounter = 1
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            quoteSidePaymentMethods.forEach { paymentMethod ->
                Box(contentAlignment = Alignment.Center) {
                    val (_, missing) = i18NPaymentMethod(paymentMethod)
                    val fallbackPath = "drawable/payment/fiat/custom_payment_${customMethodCounter}.png"
                    DynamicImage(
                        path = "drawable/payment/fiat/${
                            paymentMethod
                                .lowercase()
                                .replace("-", "_")
                        }.png",
                        contentDescription =  if (missing) "mobile.components.paymentMethods.customPaymentMethod".i18n(paymentMethod) else paymentMethod,
                        fallbackPath = fallbackPath,
                        onImageLoadError = { customMethodCounter++ },
                        modifier = Modifier.size(20.dp),
                    )
                    if (missing) {
                        val firstChar = if (paymentMethod.isNotEmpty()) paymentMethod[0].toString() else "?"
                        BisqText.baseRegular(
                            text = firstChar,
                            textAlign = TextAlign.Center,
                            color = BisqTheme.colors.dark_grey20,
                            modifier = Modifier.size(20.dp).wrapContentSize(Alignment.Center)
                        )
                    }
                }
            }
        }
        DynamicImage(
            "drawable/payment/interchangeable_grey.png",
            modifier = Modifier.size(16.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            baseSidePaymentMethods.forEach { paymentMethod ->
                DynamicImage(
                    "drawable/payment/bitcoin/${
                        paymentMethod
                            .lowercase()
                            .replace("-", "_")
                    }.png",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}