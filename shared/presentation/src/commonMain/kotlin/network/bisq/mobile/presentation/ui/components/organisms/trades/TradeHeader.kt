package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.animation.*
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bitcoin_payment_confirmation
import bisqapps.shared.presentation.generated.resources.img_bitcoin_payment_waiting
import kotlinx.coroutines.delay
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.ui.components.atoms.ProfileRating
import network.bisq.mobile.presentation.ui.components.atoms.icons.SwapHArrowIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.UpIcon
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBoxStyle
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRow
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun TradeHeader(
    offer: OfferListItem,
) {

    val enterTransition = remember {
        expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(300)
        ) + fadeIn(
            initialAlpha = 0.3f,
            animationSpec = tween(300)
        )
    }
    val exitTransition = remember {
        shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(300)
        ) + fadeOut(
            animationSpec = tween(300)
        )
    }
    var visible by remember { mutableStateOf(false) }

    val transitionState = remember {
        MutableTransitionState(visible).apply {
            targetState = !visible
        }
    }
    val transition = rememberTransition(transitionState)
    val arrowRotationDegree by transition.animateFloat({
        tween(durationMillis = 300)
    }) {
        if (visible) 0f else 180f
    }

    Row(modifier = Modifier.clip(shape = RoundedCornerShape(12.dp))) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(color = BisqTheme.colors.dark5)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileRating(offer)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BisqText.xsmallRegular(text = "10000.02 USD")
                    SwapHArrowIcon()
                    BisqText.xsmallRegular(text = "0.00173399 BTC")
                }
            }
            AnimatedVisibility(
                visible = visible,
                enter = enterTransition,
                exit = exitTransition
            ) {
                Column {

                    InfoRow(
                        style = InfoBoxStyle.Style2,
                        label1 = "Trade ID",
                        value1 = "07b9bab1",
                        label2 = "Date",
                        value2 = "29 Sep 2024",
                    )

                    Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))

                    InfoRow(
                        style = InfoBoxStyle.Style2,
                        label1 = "Floating percentage",
                        value1 = "1.71%",
                        label2 = "Price",
                        value2 = "9567056.04 USD/BTC",
                    )

                    Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))

                    InfoRow(
                        style = InfoBoxStyle.Style2,
                        label1 = "Payment method",
                        value1 = "CashApp",
                        label2 = "Settlement method",
                        value2 = "Lightning",
                    )

                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BisqButton(
                    text = "Cancel Trade",
                    color = BisqTheme.colors.primary,
                    onClick = {},
                    backgroundColor = Color.Transparent,
                    padding = PaddingValues(horizontal = 70.dp, vertical = 6.dp)
                )
                IconButton(
                    onClick = {
                        visible = !visible
                    }
                ) {
                    UpIcon(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(shape = RoundedCornerShape(12.dp))
                            .rotate(arrowRotationDegree)
                            .background(color = BisqTheme.colors.primary)
                    )
                }
            }
        }
    }
}
