package network.bisq.mobile.presentation.ui.uicases.offers

import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun TakeOfferReviewTradeScreen() {
    // TODO presenter for this? And call RememberLifecycle...

   TakeOfferScaffold {
        Column(modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp)) {
            BisqText.h3Regular(
                text = "Review trade",
                color = BisqTheme.colors.light1
            )
            Spacer(modifier = Modifier.height(32.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BisqText.largeRegular(
                            text = "I WANT TO",
                            color = BisqTheme.colors.grey2
                        )
                        BisqText.h5Regular(
                            text = "BUY Bitcoin"
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        BisqText.largeRegular(
                            text = "FIAT PAYMENT METHOD",
                            color = BisqTheme.colors.grey2
                        )
                        BisqText.h5Regular(
                            text = "Strike"
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BisqText.largeRegular(
                            text = "AMOUNT TO PAY",
                            color = BisqTheme.colors.grey2
                        )
                        BisqText.h5Regular(
                            text = "900.00"
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        BisqText.largeRegular(
                            text = "AMOUNT TO RECEIVE",
                            color = BisqTheme.colors.grey2
                        )
                        BisqText.h5Regular(
                            text = "0.00918367 btc"
                        )
                    }
                }
            }
            HorizontalDivider(
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 28.dp),
                color = Color(0XFF2B2B2B)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BisqText.largeRegular(
                        text = "Trade price",
                        color = BisqTheme.colors.grey2
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            BisqText.h5Regular(
                                text = "98,000.68"
                            )
                            BisqText.baseRegular(
                                text = "BTC/USD",
                                color = BisqTheme.colors.grey2
                            )
                        }
                        BisqText.smallRegular(
                            text = "Float price 1.00% above market price of 60,000 BTC/USD",
                            color = BisqTheme.colors.grey4
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        BisqText.largeRegular(
                            text = "Bitcoin settlement method",
                            color = BisqTheme.colors.grey2
                        )
                        BisqText.h5Regular(
                            text = "Lightning"
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        BisqText.largeRegular(
                            text = "Fiat payment",
                            color = BisqTheme.colors.grey2
                        )
                        BisqText.h5Regular(
                            text = "Strike"
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BisqText.largeRegular(
                        text = "Fees",
                        color = BisqTheme.colors.grey2
                    )
                    BisqText.h5Regular(
                        text = "No trade fees in Bisq Easy  :-)"
                    )
                }
            }
        }
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
        ) {
            BisqButton(
                text = "Back",
                backgroundColor = BisqTheme.colors.dark5,
                onClick = { },
                padding = PaddingValues(horizontal = 64.dp, vertical = 4.dp)
            )
            BisqButton(
                text = "Next",
                onClick = {
                    navController.navigate(Routes.TradeFlow.name)
                },
                padding = PaddingValues(horizontal = 64.dp, vertical = 4.dp)
            )
        }
    }
}