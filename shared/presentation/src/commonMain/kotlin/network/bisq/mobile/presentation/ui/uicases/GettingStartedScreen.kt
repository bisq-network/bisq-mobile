package network.bisq.mobile.presentation.ui.uicases

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

interface IGettingStarted : ViewPresenter {
    val title: String
    val bulletPoints: List<String>
    val offersOnline: StateFlow<Number>
    val publishedProfiles: StateFlow<Number>

    fun onStartTrading()
    fun navigateLearnMore()
}

@Composable
fun GettingStartedScreen() {
    val presenter: GettingStartedPresenter = koinInject()
    val tabPresenter: ITabContainerPresenter = koinInject()
    val offersOnline: Number = presenter.offersOnline.collectAsState().value
    val publishedProfiles: Number = presenter.publishedProfiles.collectAsState().value

    RememberPresenterLifecycle(presenter)

    BisqScrollLayout(
        padding = PaddingValues(all = 0.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        isInteractive = presenter.isInteractive.collectAsState().value
    ) {

        Column {
            PriceProfileCard(
                price = presenter.formattedMarketPrice.collectAsState().value,
                priceText = "Market price"
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    PriceProfileCard(
                        price = offersOnline.toString(),
                        priceText = "Offers online"
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    PriceProfileCard(
                        price = publishedProfiles.toString(),
                        priceText = "Published profiles"
                    )
                }
            }
        }
        BisqButton("Chat", onClick = { presenter.navigateToChat() })
        WelcomeCard(
            presenter = presenter,
            title = presenter.title,
            bulletPoints = presenter.bulletPoints,
            primaryButtonText = "Start Trading",
            footerLink = "action.learnMore".i18n()
        )
    }
}

@Composable
fun WelcomeCard(
    presenter: GettingStartedPresenter,
    title: String,
    bulletPoints: List<String>,
    primaryButtonText: String,
    footerLink: String
) {
    NeumorphicCard {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(BisqTheme.colors.dark2)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title
            BisqText.h4Regular(
                text = title,
                color = BisqTheme.colors.light1
            )

            // Bullet Points
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                bulletPoints.forEach { point ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(6.dp)) {
                            drawCircle(color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        BisqText.baseMedium(
                            text = point,
                            color = BisqTheme.colors.light2
                        )
                    }
                }
            }

            // Primary Button
            Button(
                onClick={ presenter.onStartTrading() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = BisqTheme.colors.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                BisqText.baseBold(
                    text = primaryButtonText,
                    color = Color.White,
                )
            }

//            BisqButton("Create offer", onClick={ presenter.navigateToCreateOffer() })

            // Footer Link
            Text(
                text = footerLink,
//                style = BisqTheme.type.caption,
                color = BisqTheme.colors.primary,
                modifier = Modifier
                    .align(Alignment.Start)
                    .clickable { presenter.navigateLearnMore() }
            )
        }
    }
}

@Composable
fun PriceProfileCard(price: String, priceText: String) {
    Column(
        modifier = Modifier
            .clip(shape = RoundedCornerShape(4.dp))
            .background(color = BisqTheme.colors.dark3)
            .padding(vertical = 12.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BisqText.largeRegular(
            text = price,
            color = BisqTheme.colors.light1,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(12.dp))

        BisqText.smallRegular(
            text = priceText,
            color = BisqTheme.colors.grey1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun NeumorphicCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {

    Box(
        modifier = modifier
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(5.dp), spotColor = BisqTheme.colors.primary)
            .padding(2.dp)
    ) {
        content()
    }

}
