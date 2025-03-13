package network.bisq.mobile.presentation.ui.components.molecules.settings

import androidx.compose.runtime.Composable
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.navigation.Routes

// UI model
sealed class MenuItem(val label: String) {
    class Leaf(label: String, val route: Routes) : MenuItem(label)
    class Parent(label: String, val children: List<MenuItem>) : MenuItem(label)
}

@Composable
fun SettingsMenu(menuItem: MenuItem, onNavigate: (MenuItem) -> Unit) {
    when (menuItem) {
        is MenuItem.Parent -> menuItem.children.forEach { child ->
            SettingsButton(label = child.label, onClick = { onNavigate(child) })
            BisqGap.VHalf()
        }

        else -> {
            SettingsButton(label = menuItem.label, onClick = { onNavigate(menuItem) })
            BisqGap.VHalf()
        }
    }
}