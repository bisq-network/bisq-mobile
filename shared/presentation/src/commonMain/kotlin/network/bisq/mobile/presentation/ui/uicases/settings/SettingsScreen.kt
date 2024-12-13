
package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject


interface ISettingsPresenter: ViewPresenter {
    fun menuTree(): MenuItem
}

// UI model/s
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

@Composable
fun SettingsButton(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BisqTheme.colors.grey5)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(color = BisqTheme.colors.light1 , fontSize = 16.sp),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = ">",
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodyLarge.copy(color = BisqTheme.colors.light1, fontSize = 16.sp),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun BreadcrumbNavigation(
    path: List<MenuItem>,
    onBreadcrumbClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        path.forEachIndexed { index, menuItem ->
            Text(
                text = menuItem.label,
                style = MaterialTheme.typography.bodyLarge.copy(color = BisqTheme.colors.grey1),
                modifier = Modifier.clickable { onBreadcrumbClick(index) }
            )
            if (index != path.lastIndex) {
                Text(" > ", color = BisqTheme.colors.grey1) // Separator
            }
        }
    }
}

@Composable
fun SettingsScreen(isTabSelected: Boolean) {

//    val currentMenu = remember { mutableStateOf(menuTree) }
    val settingsPresenter: ISettingsPresenter = koinInject()
    val menuTree: MenuItem = settingsPresenter.menuTree()
    val currentMenu = remember { mutableStateOf(menuTree) }
    val menuPath = remember { mutableStateListOf(menuTree) }
    val selectedLeaf = remember { mutableStateOf<MenuItem.Leaf?>(null) }

    RememberPresenterLifecycle(settingsPresenter)
    // Reset to root menu when the tab is selected
    LaunchedEffect(isTabSelected) {
        if (isTabSelected) {
            currentMenu.value = menuTree
            selectedLeaf.value = null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BreadcrumbNavigation(path = menuPath) { index ->
            currentMenu.value = menuPath[index]
            // TODO might need complex index logic?
            selectedLeaf.value = null
            menuPath.removeRange(index + 1, menuPath.size)
        }

        if (selectedLeaf.value == null) {
            SettingsMenu(menuItem = currentMenu.value) { selectedItem ->
                if (selectedItem is MenuItem.Parent) {
                    selectedLeaf.value = null
                    currentMenu.value = selectedItem
                    menuPath.add(selectedItem)
                } else {
                    selectedLeaf.value = selectedItem as MenuItem.Leaf
                }
            }
        } else {
            selectedLeaf.value!!.content.invoke()
        }
    }
}