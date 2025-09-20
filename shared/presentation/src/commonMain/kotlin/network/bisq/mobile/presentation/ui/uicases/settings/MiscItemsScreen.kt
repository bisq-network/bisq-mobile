package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.icon_web_link
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.ArrowRightIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.uicases.settings.MiscItemsPresenter.MenuItem
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
fun MiscItemsScreen() {
    val presenter: MiscItemsPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()
    val menuTree by presenter.menuItems.collectAsState()

    BisqStaticLayout(
        contentPadding = PaddingValues(all = BisqUIConstants.Zero),
        verticalArrangement = Arrangement.Top,
        isInteractive = isInteractive,
    ) {
        menuTree?.let { root ->
            Menu(menuItem = root) { selectedItem ->
                if (selectedItem is MenuItem.Leaf) {
                    presenter.onNavigateTo(selectedItem.route)
                }
            }
        }
    }
}


@Composable
private fun Menu(menuItem: MenuItem, onNavigate: (MenuItem) -> Unit) {
    when (menuItem) {
        is MenuItem.Parent -> menuItem.children.forEach { child ->
            ItemButton(label = child.label, icon = child.icon, onClick = { onNavigate(child) })
            BisqGap.VHalf()
        }

        else -> {
            ItemButton(label = menuItem.label, icon = menuItem.icon, onClick = { onNavigate(menuItem) })
            BisqGap.VHalf()
        }
    }
}

@Composable
private fun ItemButton(
    label: String,
    icon: DrawableResource? = null,
    onClick: () -> Unit
) {
    BisqButton(
        label,
        onClick = onClick,
        fullWidth = true,
        backgroundColor = BisqTheme.colors.dark_grey40,
        cornerRadius = BisqUIConstants.Zero,
        leftIcon = {
            if (icon != null) {
                Image(
                    painter = painterResource(icon),
                    contentDescription = label,
                    modifier = Modifier.size(20.dp)
                )
            }
        },

        rightIcon = { ArrowRightIcon() },
        textAlign = TextAlign.Start,
        padding = PaddingValues(all = BisqUIConstants.ScreenPadding)
    )
}

@Composable
fun WebLinkIcon(modifier: Modifier = Modifier.size(24.dp)) {
    Image(painterResource(Res.drawable.icon_web_link), "Web link icon", modifier = modifier)
}