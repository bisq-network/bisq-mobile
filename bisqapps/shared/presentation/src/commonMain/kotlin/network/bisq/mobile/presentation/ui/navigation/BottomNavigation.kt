package network.bisq.mobile.presentation.ui.navigation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bisqapps.shared.presentation.generated.resources.Res
import coil3.compose.AsyncImage
import network.bisq.mobile.presentation.ui.model.BottomNavigationItem
import network.bisq.mobile.presentation.ui.theme.backgroundColor
import network.bisq.mobile.presentation.ui.theme.primaryGreenColor
import network.bisq.mobile.presentation.ui.theme.primaryStandard
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
@Composable
fun BottomNavigation(
    items: List<BottomNavigationItem>,
    currentRoute: String,
    onItemClick: (BottomNavigationItem) -> Unit
) {

    NavigationBar(
        containerColor = backgroundColor
    ) {
        items.forEach { navigationItem ->
            NavigationBarItem(
                colors = NavigationBarItemColors(
                    selectedIndicatorColor = backgroundColor,
                    selectedIconColor = primaryStandard,
                    selectedTextColor = primaryStandard,
                    unselectedIconColor = Color.White,
                    unselectedTextColor = Color.White,
                    disabledIconColor = Color.Red,
                    disabledTextColor = Color.Red
                ),
                interactionSource = remember { MutableInteractionSource() },
                selected = currentRoute == navigationItem.route,
                onClick = { onItemClick(navigationItem) },
                icon = {
                    AsyncImage(
                        model = Res.getUri(navigationItem.icon),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        colorFilter = ColorFilter.tint(color = if (navigationItem.route == currentRoute) primaryGreenColor else Color.White )
                    )
                },
                label = {
                    Text(
                        text = navigationItem.title,
                        fontSize = 14.sp,
                        color = if (navigationItem.route == currentRoute) primaryGreenColor else Color.White
                    )
                }
            )
        }
    }
}