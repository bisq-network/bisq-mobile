package network.bisq.mobile.presentation.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.animations.AnimatedBadge
import network.bisq.mobile.presentation.ui.composeModels.BottomNavigationItem
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun BottomNavigation(
    items: List<BottomNavigationItem>,
    currentRoute: String,
    unreadTradeCount: Int,
    showAnimation: Boolean,
    onItemClick: (BottomNavigationItem) -> Unit
) {

    val MY_TRADES_TAB_INDEX = 2

    //  MaterialTheme v3 use a background for selected item and by that has a larger spacing between icon an text.
    // As we do not use a bag for selection the large space looks weird. As it does not allow customization we
    // add use a Column for icon, packing in the text there and leave the text empty
    // Default spacing between icon and label: ~4.dp.
    NavigationBar(
        containerColor = BisqTheme.colors.backgroundColor
    ) {
        items.forEachIndexed { index, navigationItem ->
            NavigationBarItem(
                colors = NavigationBarItemColors(
                    selectedIndicatorColor = BisqTheme.colors.backgroundColor,
                    selectedIconColor = BisqTheme.colors.primary,
                    selectedTextColor = BisqTheme.colors.primary,
                    unselectedIconColor = BisqTheme.colors.white,
                    unselectedTextColor = BisqTheme.colors.white,
                    disabledIconColor = BisqTheme.colors.danger,
                    disabledTextColor = BisqTheme.colors.danger
                ),
                interactionSource = remember { MutableInteractionSource() },
                selected = currentRoute == navigationItem.route,
                onClick = { onItemClick(navigationItem) },
                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val icon = @Composable {
                            Image(
                                painter = painterResource(navigationItem.icon),
                                contentDescription = navigationItem.title,
                                modifier = Modifier.size(24.dp),  // 24.dp is standard on Android
                                colorFilter = ColorFilter.tint(
                                    color = if (navigationItem.route == currentRoute) BisqTheme.colors.primary else Color.White
                                )
                            )
                        }

                        if (index == MY_TRADES_TAB_INDEX && unreadTradeCount > 0) {
                            BadgedBox(
                                badge = {
                                    AnimatedBadge(showAnimation = showAnimation) {
                                        BisqText.xsmallMedium(
                                            unreadTradeCount.toString(),
                                            textAlign = TextAlign.Center,
                                            color = BisqTheme.colors.dark_grey20
                                        )
                                    }
                                }
                            ) {
                                icon()
                            }
                        } else {
                            icon()
                        }

                        AutoResizeText(
                            text = navigationItem.title,
                            color = if (navigationItem.route == currentRoute) BisqTheme.colors.primary else BisqTheme.colors.white,
                            textStyle = BisqTheme.typography.xsmallRegular, // 12.dp is standard on Android
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            minimumFontSize = 8.sp
                        )
                    }
                },
                label = {}
            )
        }
    }
}