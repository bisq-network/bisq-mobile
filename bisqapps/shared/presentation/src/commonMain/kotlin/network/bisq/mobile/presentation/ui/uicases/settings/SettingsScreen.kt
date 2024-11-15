
package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@OptIn(ExperimentalResourceApi::class)
@Composable
fun SettingsScreen(
) {
    val navController: NavHostController = koinInject(named("RootNavController"))
    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center // Centers the content within the Box
    ) {
        BisqText.h2Regular(
            text = "Settings",
            color = BisqTheme.colors.light1,
        )
    }
}