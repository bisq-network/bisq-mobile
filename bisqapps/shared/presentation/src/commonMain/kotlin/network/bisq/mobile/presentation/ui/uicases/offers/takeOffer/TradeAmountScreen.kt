package network.bisq.mobile.presentation.ui.uicases.offers.takeOffer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.molecules.BisqAmountSelector
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun TakeOfferTradeAmountScreen() {
    val strings = LocalStrings.current

    val navController: NavHostController = koinInject(named("RootNavController"))

    MultiScreenWizardScaffold(
        strings.take_offer,
        stepIndex = 1,
        stepsLength = 3,
        nextOnClick = {
            navController.navigate(Routes.TakeOfferPaymentMethod.name)
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BisqText.h3Regular(
                text = "How much do you want to spend?",
                color = BisqTheme.colors.light1
            )
            BisqText.largeLight(
                text = "The offer allows you to choose a trade amount between 500.00 and 646.00 USD",
                color = BisqTheme.colors.grey2
            )
        }

        BisqAmountSelector(minAmount = 500.0f, maxAmount = 900.0f, exchangeRate = 95000.0, currency = "USD")
    }
}