package network.bisq.mobile.presentation.ui.uicases

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import bisqapps.shared.presentation.generated.resources.*
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.icon_tag_outlined
import bisqapps.shared.presentation.generated.resources.img_fiat_btc
import bisqapps.shared.presentation.generated.resources.img_learn_and_discover
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

interface IGettingStarted {
    val btcPrice: StateFlow<String>
    val offersOnline: StateFlow<Number>
    val publishedProfiles: StateFlow<Number>
}

@Composable
fun GettingStartedScreen() {
    val presenter: IGettingStarted = koinInject()
    val btcPrice:String = presenter.btcPrice.collectAsState().value
    val offersOnline:Number = presenter.offersOnline.collectAsState().value
    val publishedProfiles:Number = presenter.publishedProfiles.collectAsState().value

    // TODO attach view should happen here to let the presenter know?

    Column(
        modifier = Modifier.fillMaxSize(),
    ) {
        // TODO: Should be a child of Scaffold, in TabContainerScreen
        TopBar(isHome = true)

        // TODO: Should use BisqScrollLayout. But it has Scaffold inside it already!
        Column(
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 15.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            Column {
                PriceProfileCard(
                    price = btcPrice,
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
            WelcomeCard(
                title = "Get your first BTC",
                buttonText = "Enter Bisq Easy"
            )
            Column {
                InstructionCard(
                    image = Res.drawable.img_fiat_btc,
                    title = "Multiple trade protocols",
                    description = "Checkout the roadmap for upcoming trade protocols. Get an overview about the features of the different protocols.",
                    buttonText = "Explore trade protocols"
                )
                Spacer(modifier = Modifier.height(24.dp))
                InstructionCard(
                    image = Res.drawable.img_learn_and_discover,
                    title = "Learn & discover",
                    description = "Learn about Bitcoin and checkout upcoming events. Meet other Bisq users in the discussion chat.",
                    buttonText = "Learn more"
                )
            }
        }
    }
}

@Composable
fun WelcomeCard(title: String, buttonText: String) {
    NeumorphicCard{
        Column(
            modifier = Modifier.shadow(
                ambientColor = Color.Blue, spotColor = BisqTheme.colors.primary,
                elevation = 2.dp,
                shape = RoundedCornerShape(5.dp),

                ).clip(shape = RoundedCornerShape(5.dp)).background(color = BisqTheme.colors.dark2)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            BisqText.h4Regular(
                text = title,
                color = BisqTheme.colors.light1,
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureCard(
                    image = Res.drawable.icon_tag_outlined,
                    title = "Start trading or browser open offers in the offerbook"
                )
                FeatureCard(
                    image = Res.drawable.icon_chat_outlined,
                    title = "Chat based and guided user interface for trading"
                )
                FeatureCard(
                    image = Res.drawable.icon_star_outlined,
                    title = "Security is based on seller’s reputation"
                )
            }
            BisqText.baseMedium(
                text = buttonText,
                color = BisqTheme.colors.light1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(4.dp))
                    .background(color = BisqTheme.colors.primary)
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
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

@OptIn(ExperimentalResourceApi::class)
@Composable
fun FeatureCard(image: DrawableResource, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
//        AsyncImage(
//            model = Res.getUri(imagePath),
//            contentDescription = null,
//            modifier = Modifier.size(20.dp)
//        )
        Image(painterResource(image), null, Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(9.dp))
        BisqText.smallRegular(
            text = title,
            color = BisqTheme.colors.light1,
        )

    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun InstructionCard(image: DrawableResource, title: String, description: String, buttonText: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(shape = RoundedCornerShape(8.dp)).background(color = BisqTheme.colors.dark3)
            .padding(vertical = 18.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
//        AsyncImage(
//            model = Res.getUri(imagePath),
//            contentDescription = null,
//            modifier = Modifier.size(50.dp)
//        )
        Image(painterResource(image), "")
        BisqText.baseRegular(
            text = title,
            color = BisqTheme.colors.light1,
        )
        BisqText.baseRegular(
            text = description,
            color = BisqTheme.colors.grey3,
            textAlign = TextAlign.Center,
        )
        BisqText.smallRegular(
            text = buttonText,
            color = BisqTheme.colors.light1,
            modifier = Modifier
                .clip(shape = RoundedCornerShape(4.dp))
                .background(color = BisqTheme.colors.dark5)
                .padding(horizontal = 18.dp, vertical = 6.dp),
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
