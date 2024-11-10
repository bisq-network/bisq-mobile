package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.icons.BellIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogoSmall
import network.bisq.mobile.presentation.ui.components.atoms.icons.UserIcon
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun TopBar(title: String = "",isHome:Boolean = false) {
    TopAppBar(
        modifier = Modifier.padding(horizontal = 16.dp).padding(end = 16.dp),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = BisqTheme.colors.backgroundColor,
        ),
        title = {
            if (isHome) {
                BisqLogoSmall(modifier = Modifier.height(34.dp).width(100.dp),)
            } else {
                BisqText.h4Medium(
                    text = title,
                    color = BisqTheme.colors.light1,
                )
            }
        },
        actions = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BellIcon(modifier = Modifier.size(30.dp))
                Spacer(modifier = Modifier.width(12.dp))
                UserIcon(modifier = Modifier.size(30.dp))
            }
        },
    )
}