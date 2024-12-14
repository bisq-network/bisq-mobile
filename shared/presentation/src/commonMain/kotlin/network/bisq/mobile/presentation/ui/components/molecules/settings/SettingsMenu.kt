package network.bisq.mobile.presentation.ui.components.molecules.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.theme.BisqTheme

// UI model
sealed class MenuItem(val label: String) {
    class Leaf(label: String, val content: @Composable () -> Unit) : MenuItem(label)
    class Parent(label: String, val children: List<MenuItem>) : MenuItem(label)
}

@Composable
fun SettingsMenu(menuItem: MenuItem, onNavigate: (MenuItem) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BisqTheme.colors.backgroundColor)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            when (menuItem) {
                is MenuItem.Parent -> menuItem.children.forEach { child ->
                    SettingsButton(label = child.label, onClick = { onNavigate(child) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {
                    SettingsButton(label = menuItem.label, onClick = { onNavigate(menuItem) })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}