package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.atoms.icons.rememberPlatformImagePainter
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject

@Composable
fun CreateProfileScreen(
) {
    val presenter: CreateProfilePresenter = koinInject()
    val navController: NavHostController = presenter.getRootNavController()

    RememberPresenterLifecycle(presenter)

    BisqScrollScaffold {
        BisqLogo()
        Spacer(modifier = Modifier.height(24.dp))
        BisqText.h1LightGrey("onboarding.createProfile.headline".i18n())
        Spacer(modifier = Modifier.height(12.dp))
        BisqText.baseRegularGrey(
            text = "onboarding.createProfile.subTitle".i18n(),
            modifier = Modifier.padding(horizontal = 24.dp),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(36.dp))
        BisqTextField(
            label = "onboarding.createProfile.nickName".i18n(),
            value = presenter.nickName.collectAsState().value,
            onValueChange = { name, isValid -> presenter.setNickname(name) },
            placeholder = "onboarding.createProfile.nickName.prompt".i18n(),
            validation = {

                if (it.length < 3) {
                    return@BisqTextField "Min length: 3 characters"
                }
                if (it.length > 256) {
                    return@BisqTextField "Max length: 256 characters"
                }

                return@BisqTextField null
            }
        )
        Spacer(modifier = Modifier.height(36.dp))

        presenter.profileIcon.collectAsState().value?.let { profileIcon ->
            val painter = rememberPlatformImagePainter(profileIcon)
            Image(painter = painter, contentDescription = "User profile icon generated from the hash of the public key", modifier = Modifier.height(60.dp).width(60.dp))
        }

        Spacer(modifier = Modifier.height(32.dp))
        BisqText.baseRegular(presenter.nym.collectAsState().value)
        Spacer(modifier = Modifier.height(12.dp))
        BisqText.baseRegularGrey(presenter.id.collectAsState().value)
        Spacer(modifier = Modifier.height(38.dp))
        BisqButton(
            text = "onboarding.createProfile.regenerate".i18n(),
            type = BisqButtonType.Grey,
            padding = PaddingValues(horizontal = 64.dp, vertical = 12.dp),
            onClick = { presenter.onGenerateKeyPair() }
        )
        Spacer(modifier = Modifier.height(40.dp))
        BisqButton(
            "action.next".i18n(),
            onClick = { presenter.onCreateAndPublishNewUserProfile() },
            backgroundColor = if (presenter.nickName.value.isEmpty()) BisqTheme.colors.primaryDisabled else BisqTheme.colors.primary
        )
    }
}
