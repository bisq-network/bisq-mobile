package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ViewPresenter
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.ui.components.molecules.settings.BreadcrumbNavigation
import network.bisq.mobile.presentation.ui.components.molecules.settings.MenuItem
import network.bisq.mobile.presentation.ui.components.molecules.settings.SettingsMenu
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

interface ISettingsPresenter : ViewPresenter {
    val appName: String
    fun menuTree(): MenuItem
    fun versioning(): Triple<String, String, String>

    fun navigate(route: Routes)
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

    BisqStaticLayout(
        padding = PaddingValues(all = BisqUIConstants.Zero),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column{
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

            SettingsMenu(menuItem = currentMenu.value) { selectedItem ->
                if (selectedItem is MenuItem.Leaf) {
                    settingsPresenter.navigate(selectedItem.route)
                }
            }
        }
        SettingsFooter(settingsPresenter.appName, settingsPresenter.versioning())
    }
}

@Composable
fun SettingsFooter(appName: String, versioning: Triple<String, String, String>) {
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
            text = "$appName v$versionNumber ($networkName: v$networkVersion)",
            fontSize = 12.sp,
            color = Color.White,
            modifier = Modifier.padding(vertical = 1.dp)
        )
    }
}