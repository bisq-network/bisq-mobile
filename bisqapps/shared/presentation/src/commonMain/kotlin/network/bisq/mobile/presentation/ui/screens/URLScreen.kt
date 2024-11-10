package network.bisq.mobile.presentation.ui.screens

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.icon_question_mark
import network.bisq.mobile.components.MaterialTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.foundation.BisqButton
import network.bisq.mobile.presentation.ui.components.foundation.BisqText
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.navigation.Routes
import network.bisq.mobile.presentation.ui.theme.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

private lateinit var textState: MutableState<String>

@OptIn(ExperimentalResourceApi::class)
@Composable
fun URLScreen(
    rootNavController: NavController
) {
    textState = remember { mutableStateOf("") }
    val isConnected by remember { mutableStateOf(false) }

    BisqScrollLayout() {
        BisqLogo()
        Spacer(modifier = Modifier.height(24.dp))
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
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

            MaterialTextField(textState.value, onValueChanged = { textState.value = it })

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

        var visible by remember {
            mutableStateOf(false)
        }

        if (!visible) {
            BisqButton(
                text = "Test Connection",
                color = if (textState.value.isEmpty()) BisqTheme.colors.grey1 else BisqTheme.colors.light1,
                onClick = { visible = !visible },
                padding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
            )
        } else {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(700)),
                ) {
                    BisqButton(
                        text = "Test Connection",
                        color = if (textState.value.isEmpty()) BisqTheme.colors.grey1 else BisqTheme.colors.light1,
                        onClick = { },
                        padding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(300)),

                    ) {
                    BisqButton(
                        text = "Next",
                        color = BisqTheme.colors.light1,
                        onClick = {
                            rootNavController.navigate(Routes.TabContainer.name) {
                                popUpTo(Routes.BisqUrl.name) {
                                    inclusive = true
                                }
                            }
                        },
                        padding = PaddingValues(horizontal = 32.dp, vertical = 12.dp),
                    )
                }
            }
        }
    }
}
