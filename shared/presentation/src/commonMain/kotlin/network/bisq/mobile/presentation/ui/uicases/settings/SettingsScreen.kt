
package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ViewPresenter
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import network.bisq.mobile.presentation.ui.components.molecules.settings.BreadcrumbNavigation
import network.bisq.mobile.presentation.ui.components.molecules.settings.MenuItem
import network.bisq.mobile.presentation.ui.components.molecules.settings.SettingsMenu
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

interface ISettingsPresenter: ViewPresenter {
    fun menuTree(): MenuItem
    fun versioning(): Triple<String, String, String>
}

@Composable
fun SettingsScreen(isTabSelected: Boolean) {

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

    // Column is used leaving the possibility to the leaf views to set the scrolling as they please
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BreadcrumbNavigation(path = menuPath) { index ->
                if (index == menuPath.size - 1) {
                //                TODO Default: Do nth, otherwise we can choose the below
                //                currentMenu.value = menuPath[index - 1]
                //                menuPath.removeRange(index, menuPath.size)
                } else {
                    currentMenu.value = menuPath[index]
                    menuPath.removeRange(index + 1, menuPath.size)
                    selectedLeaf.value = null
                }
            }

            if (selectedLeaf.value == null) {
                SettingsMenu(menuItem = currentMenu.value) { selectedItem ->
                    menuPath.add(selectedItem)
                    if (selectedItem is MenuItem.Parent) {
                        selectedLeaf.value = null
                        currentMenu.value = selectedItem
                    } else {
                        selectedLeaf.value = selectedItem as MenuItem.Leaf
                    }
                }
            } else {
                selectedLeaf.value!!.content.invoke()
            }
        }
        SettingsFooter(settingsPresenter.versioning())
    }
}

@Composable
fun SettingsFooter(versioning: Triple<String, String, String>) {
    val versionNumber = versioning.first
    val networkName = versioning.second
    val networkVersion = versioning.third
    Row(
        modifier = Modifier
//            .background(color = Color.White.copy(alpha = 0.5f))
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bisq Connect v$versionNumber ($networkName: v$networkVersion)",
            fontSize = 12.sp,
            color = Color.White,
            modifier = Modifier.padding(vertical = 1.dp)
        )
    }
}