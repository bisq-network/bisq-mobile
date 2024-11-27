package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.atoms.icons.QuestionIcon
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.theme.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import org.koin.core.parameter.parametersOf
import cafe.adriel.lyricist.LocalStrings

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.CopyIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.ScanIcon
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle

interface ITrustedNodeSetupPresenter: ViewPresenter {
    val bisqApiUrl: StateFlow<String>
    val isConnected: StateFlow<Boolean>

    fun updateBisqApiUrl(newUrl: String)

    fun testConnection(isTested: Boolean)

    fun navigateToNextScreen()
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun TrustedNodeSetupScreen(
) {
    val strings = LocalStrings.current
    val navController: NavHostController = koinInject(named("RootNavController"))
    val presenter: ITrustedNodeSetupPresenter = koinInject { parametersOf(navController) }

    val bisqApiUrl = presenter.bisqApiUrl.collectAsState().value
    val isConnected = presenter.isConnected.collectAsState().value

    RememberPresenterLifecycle(presenter)

    BisqScrollScaffold {
        BisqLogo()
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)
        ) {
            BisqTextField(
                label = "Bisq URL",
                onValueChanged = { presenter.updateBisqApiUrl(it) },
                value = bisqApiUrl,
                placeholder = "",
                labelRightSuffix = {
                    BisqButton(
                        iconOnly = { QuestionIcon() },
                        backgroundColor = BisqTheme.colors.backgroundColor,
                        onClick = { presenter.navigateToNextScreen() }
                    )
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BisqButton(
                    text = "Paste",
                    onClick = {},
                    backgroundColor = BisqTheme.colors.dark5,
                    color = BisqTheme.colors.light1,
                    leftIcon= { CopyIcon() }
                )

                BisqButton(
                    text = "Scan",
                    onClick = {},
                    leftIcon= { ScanIcon() }
                )
            }
            Spacer(modifier = Modifier.height(36.dp))
            BisqText.baseRegular(
                text = "STATUS",
                color = BisqTheme.colors.grey2,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BisqText.largeRegular(
                    text = if (isConnected) "Connected" else "Not Connected",
                    color = BisqTheme.colors.light1,
                )
                Spacer(modifier = Modifier.width(12.dp))
                BisqText.baseRegular(
                    text = "",
                    modifier = Modifier.clip(
                        RoundedCornerShape(5.dp)
                    ).background(color = if (isConnected) BisqTheme.colors.primary else BisqTheme.colors.danger)
                        .size(10.dp),
                )
            }
        }

        Spacer(modifier = Modifier.height(56.dp))

        if (!isConnected) {
            BisqButton(
                text = "Test Connection",
                color = if (bisqApiUrl.isEmpty()) BisqTheme.colors.grey1 else BisqTheme.colors.light1,
                onClick = {
                    presenter.testConnection(true)
                          },
                padding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                )
        } else {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 0.dp)
            ) {
                AnimatedVisibility(
                    visible = isConnected,
                    enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(700)),
                ) {
                    BisqButton(
                        text = "Test Connection",
                        color = if (bisqApiUrl.isEmpty()) BisqTheme.colors.grey1 else BisqTheme.colors.light1,
                        onClick = { presenter.testConnection(true) },
                        padding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                    )
                }
                //Spacer(modifier = Modifier.width(20.dp))
                AnimatedVisibility(
                    visible = isConnected,
                    enter = fadeIn(animationSpec = tween(300)),

                    ) {
                    BisqButton(
                        text = "Next",
                        color = BisqTheme.colors.light1,
                        onClick = { presenter.navigateToNextScreen() },
                        padding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}
