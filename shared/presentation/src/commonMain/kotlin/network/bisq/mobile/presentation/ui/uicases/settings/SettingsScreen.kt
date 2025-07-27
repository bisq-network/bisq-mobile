package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.ui.components.molecules.settings.BreadcrumbNavigation
import network.bisq.mobile.presentation.ui.components.molecules.settings.MenuItem
import network.bisq.mobile.presentation.ui.components.molecules.settings.SettingsMenu
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

interface ISettingsPresenter : ViewPresenter {
    fun menuTree(): MenuItem

    fun navigate(route: Routes)
    fun settingsNavigateBack()
}

@Composable
fun SettingsScreen(isTabSelected: Boolean) {

    val presenter: ISettingsPresenter = koinInject()
    val menuTree: MenuItem = presenter.menuTree()
    val currentMenu = remember { mutableStateOf(menuTree) }
    val menuPath = remember { mutableStateListOf(menuTree) }
    val selectedLeaf = remember { mutableStateOf<MenuItem.Leaf?>(null) }

    RememberPresenterLifecycle(presenter)
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
        isInteractive = presenter.isInteractive.collectAsState().value,
    ) {
        Column{
            BreadcrumbNavigation(path = menuPath) { index ->
                // if (index == menuPath.size - 1) {
                //                TODO Default: Do nth, otherwise we can choose the below
                //                currentMenu.value = menuPath[index - 1]
                //                menuPath.removeRange(index, menuPath.size)
                // } else {
                //     currentMenu.value = menuPath[index]
                //     menuPath.removeRange(index + 1, menuPath.size)
                //     selectedLeaf.value = null
                // }
            }

            SettingsMenu(menuItem = currentMenu.value) { selectedItem ->
                if (selectedItem is MenuItem.Leaf) {
                    presenter.navigate(selectedItem.route)
                }
            }
        }
    }
}