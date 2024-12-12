
package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.ui.theme.BisqTheme

// UI model/s
sealed class MenuItem(val label: String, val onClick: (() -> Unit)? = null) {
    class Leaf(label: String, onClick: () -> Unit) : MenuItem(label, onClick)
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

@Composable
fun SettingsButton(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BisqTheme.colors.backgroundColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(color = BisqTheme.colors.primary, fontSize = 16.sp),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = ">",
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge.copy(color = BisqTheme.colors.secondary, fontSize = 16.sp),
            modifier = Modifier.weight(1f)
        )
    }
}

// TODO refactor to get the menu structure from presenter and let presenter decide what to do
// on click (in this way each presenter can customize the settings , useful for node vs xclients
@Composable
fun SettingsScreen() {
//    val currentMenu = remember { mutableStateOf(menuTree) }
    // TODO get menu tree from presenter
    val exampleMenuTree = MenuItem.Parent(
        label = "Settings",
        children = listOf(
            MenuItem.Parent(
                label = "Account",
                children = listOf(
                    MenuItem.Leaf(label = "User Profile", onClick = { println("Userprofile") }),
                    MenuItem.Leaf(label = "Payment Methods", onClick = { println("Payment methods") })
                )
            )
        )
    )
    val currentMenu = remember { mutableStateOf(exampleMenuTree) }

    SettingsMenu(menuItem = currentMenu.value) { selectedItem ->
        if (selectedItem is MenuItem.Parent) {
            currentMenu.value = selectedItem
        } else {
            selectedItem.onClick?.invoke()
        }
    }
}