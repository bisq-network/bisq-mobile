package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.ui.components.molecules.settings.MenuItem
import network.bisq.mobile.presentation.ui.components.molecules.settings.SettingsMenu
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

interface ISettingsPresenter : ViewPresenter {
    val menuItems: StateFlow<MenuItem?>

    fun navigate(route: Routes)
}

@Composable
fun SettingsScreen(isTabSelected: Boolean) {
    val presenter: ISettingsPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()
    val menuTree by presenter.menuItems.collectAsState()

    BisqStaticLayout(
        padding = PaddingValues(all = BisqUIConstants.Zero),
        verticalArrangement = Arrangement.Top,
        isInteractive = isInteractive,
    ) {
        menuTree?.let { root ->
            SettingsMenu(menuItem = root) { selectedItem ->
                if (selectedItem is MenuItem.Leaf) {
                    presenter.navigate(selectedItem.route)
                }
            }
        }
    }
}