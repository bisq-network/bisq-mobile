package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.icon_question_mark
import network.bisq.mobile.components.MaterialTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.theme.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import org.koin.core.qualifier.named
import org.koin.core.parameter.parametersOf
import cafe.adriel.lyricist.LocalStrings

import kotlinx.coroutines.flow.StateFlow

interface ITrustedNodeSetupPresenter {
    val bisqUrl: StateFlow<String>
    val isConnected: StateFlow<Boolean>

    fun updateBisqUrl(newUrl: String)

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

    val bisqUrl = presenter.bisqUrl.collectAsState().value
    val isConnected = presenter.isConnected.collectAsState().value

    BisqScrollLayout() {
        BisqLogo()
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(horizontal = 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BisqText.baseRegular(
                    text = "Bisq URL",
                    color = BisqTheme.colors.light1,
                )
                Image(painterResource(Res.drawable.icon_question_mark), "Question mark")
            }

            MaterialTextField(bisqUrl, onValueChanged = { presenter.updateBisqUrl(it) })

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BisqButton(
                    text = "Paste",
                    onClick = {},
                    backgroundColor = BisqTheme.colors.dark5,
                    color = BisqTheme.colors.light1,
                    //leftIcon=Image(painterResource(Res.drawable.icon_copy), "Copy button")
                )

                BisqButton(
                    text = "Scan",
                    onClick = {},
                    //leftIcon=Image(painterResource(Res.drawable.icon_qr), "Scan button")
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
                color = if (bisqUrl.isEmpty()) BisqTheme.colors.grey1 else BisqTheme.colors.light1,
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
                        color = if (bisqUrl.isEmpty()) BisqTheme.colors.grey1 else BisqTheme.colors.light1,
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
