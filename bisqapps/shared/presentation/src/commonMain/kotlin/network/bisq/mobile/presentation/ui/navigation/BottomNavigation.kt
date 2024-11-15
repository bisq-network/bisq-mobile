package network.bisq.mobile.presentation.ui.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.composeModels.BottomNavigationItem
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalResourceApi::class)
@Composable
fun BottomNavigation(
    items: List<BottomNavigationItem>,
    currentRoute: String,
    onItemClick: (BottomNavigationItem) -> Unit
) {

    NavigationBar(
        containerColor = BisqTheme.colors.backgroundColor
    ) {
        items.forEach { navigationItem ->
            NavigationBarItem(
                colors = NavigationBarItemColors(
                    selectedIndicatorColor = BisqTheme.colors.backgroundColor,
                    selectedIconColor = BisqTheme.colors.primary,
                    selectedTextColor = BisqTheme.colors.primary,
                    unselectedIconColor = Color.White,
                    unselectedTextColor = Color.White,
                    disabledIconColor = Color.Red,
                    disabledTextColor = Color.Red
                ),
                interactionSource = remember { MutableInteractionSource() },
                selected = currentRoute == navigationItem.route,
                onClick = { onItemClick(navigationItem) },
                icon = {
                    Image(
                        painterResource(navigationItem.icon), "",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(color = if (navigationItem.route == currentRoute) BisqTheme.colors.primary else Color.White)
                    )
                },
                label = {
                    BisqText.baseRegular(
                        text = navigationItem.title,
                        color = if (navigationItem.route == currentRoute) BisqTheme.colors.primary else BisqTheme.colors.light1,
                    )
                }
            )
        }
    }
}