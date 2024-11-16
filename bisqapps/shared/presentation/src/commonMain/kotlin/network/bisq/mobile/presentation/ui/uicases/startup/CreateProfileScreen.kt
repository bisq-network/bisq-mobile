package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bot_image
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
import kotlinx.coroutines.flow.StateFlow
import cafe.adriel.lyricist.LocalStrings
import org.koin.core.parameter.parametersOf

interface ICreateProfilePresenter {
    val profileName: StateFlow<String>

    fun onProfileNameChanged(newName: String)
    fun navigateToNextScreen()
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun CreateProfileScreen(
) {
    val strings = LocalStrings.current
    val navController: NavHostController = koinInject(named("RootNavController"))
    val presenter: ICreateProfilePresenter = koinInject { parametersOf(navController) }

    val profileName = presenter.profileName.collectAsState().value

    BisqScrollLayout() {
        BisqLogo()
        Spacer(modifier = Modifier.height(24.dp))
        BisqText.h1Light(
            text = strings.onboarding_createProfile_headline,
            color = BisqTheme.colors.grey1,
        )
        Spacer(modifier = Modifier.height(12.dp))
        BisqText.baseRegular(
            text = strings.onboarding_createProfile_subTitle,
            color = BisqTheme.colors.grey3,
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(36.dp))
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            //TODO: Convert this into a Form field component, which is Label + TextField
            BisqText.baseRegular(
                text = strings.onboarding_createProfile_nickName,
                color = BisqTheme.colors.light2,
            )
            MaterialTextField(
                text = profileName,
                placeholder = strings.onboarding_createProfile_nickName_prompt,
                onValueChanged = { presenter.onProfileNameChanged(it) })
        }
        Spacer(modifier = Modifier.height(36.dp))
        Image(painterResource(Res.drawable.img_bot_image), "Crypto generated image (PoW)") // TODO: Translation
        Spacer(modifier = Modifier.height(32.dp))
        BisqText.baseRegular(
            text = "Sleepily-Distracted-Zyophyte-257",
            color = BisqTheme.colors.light1,
        )
        Spacer(modifier = Modifier.height(12.dp))
        BisqText.baseRegular(
            text = strings.onboarding_createProfile_nym,
            color = BisqTheme.colors.grey2,
        )
        Spacer(modifier = Modifier.height(38.dp))
        BisqButton(
            text = strings.onboarding_createProfile_regenerate,
            backgroundColor = BisqTheme.colors.dark5,
            padding = PaddingValues(horizontal = 64.dp, vertical = 12.dp),
            onClick = {}
        )
        Spacer(modifier = Modifier.height(40.dp))
        BisqButton(
            strings.buttons_next,
            onClick = { presenter.navigateToNextScreen() },
            backgroundColor = if (profileName.isEmpty()) BisqTheme.colors.primaryDisabled else BisqTheme.colors.primary
        )
    }
}