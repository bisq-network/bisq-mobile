package network.bisq.mobile.presentation.ui.components.molecules.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun BreadcrumbNavigation(
    path: List<MenuItem>,
    onBreadcrumbClick: (Int) -> Unit
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical= BisqUIConstants.ScreenPaddingHalfQuarter),
    ) {
        path.forEachIndexed { index, menuItem ->
            BisqText.baseRegularGrey(
                menuItem.label,
                modifier = Modifier.clickable { onBreadcrumbClick(index) }
            )
            if (index != path.lastIndex) {
                BisqText.baseRegularGrey(" > ") // Separator
            }
        }
    }
}