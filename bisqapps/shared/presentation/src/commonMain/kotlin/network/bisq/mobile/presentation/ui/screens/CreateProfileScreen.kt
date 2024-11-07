package network.bisq.mobile.presentation.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bot_image
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
fun CreateProfileScreen(
    rootNavController: NavController
) {
    textState = remember { mutableStateOf("") }
    BisqScrollLayout() {
        BisqLogo()
        Spacer(modifier = Modifier.height(24.dp))
        BisqText.h1Light(
            text = "Create your profile",
            color = BisqTheme.colors.grey1,
        )
        Spacer(modifier = Modifier.height(12.dp))
        BisqText.baseRegular(
            text = "Your public profile consists of a nickname (picked by you) and bot icon (generated cryptographically)",
            color = BisqTheme.colors.grey3,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(36.dp))
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            //TODO: Convert this into a Form field component, which is Label + TextField
            BisqText.baseRegular(
                text = "Profile nickname",
                color = BisqTheme.colors.light2,
            )
            MaterialTextField(textState.value, onValueChanged = { textState.value = it })
        }
        Spacer(modifier = Modifier.height(36.dp))
        Image(painterResource(Res.drawable.img_bot_image), "Crypto generated image (PoW)")
        Spacer(modifier = Modifier.height(32.dp))
        BisqText.baseRegular(
            text = "Sleepily-Distracted-Zyophyte-257",
            color = BisqTheme.colors.light1,
        )
        Spacer(modifier = Modifier.height(12.dp))
        BisqText.baseRegular(
            text = "BOT ID",
            color = BisqTheme.colors.grey2,
        )
        Spacer(modifier = Modifier.height(38.dp))
        BisqButton(
            text ="Generate new bot iconnn",
            backgroundColor = BisqTheme.colors.dark5,
            padding = PaddingValues(horizontal = 64.dp, vertical = 12.dp),
            onClick = {}
        )
        Spacer(modifier = Modifier.height(40.dp))
        BisqButton(
            "Next",
            onClick = {
                if (textState.value.isNotEmpty()) {
                    rootNavController.navigate(Routes.BisqUrl.name) {
                        popUpTo(Routes.CreateProfile.name) {
                            inclusive = true
                        }
                    }
                }
            },
            backgroundColor = if (textState.value.isEmpty()) BisqTheme.colors.primaryDisabled else BisqTheme.colors.primary
        )
    }
}