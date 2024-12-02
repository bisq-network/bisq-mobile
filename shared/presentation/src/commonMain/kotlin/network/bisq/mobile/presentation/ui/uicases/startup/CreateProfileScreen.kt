package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import kotlin.math.log

@Composable
fun CreateProfileScreen(
) {
    val strings = LocalStrings.current
    val navController: NavHostController = koinInject(named("RootNavController"))
    val presenter: CreateProfilePresenter = koinInject { parametersOf(navController) }

    RememberPresenterLifecycle(presenter)

    BisqScrollScaffold {
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
        BisqTextField(
            label = strings.onboarding_createProfile_nickName,
            value = presenter.nickName.collectAsState().value,
            onValueChanged = { presenter.setNickname(it) },
            placeholder = strings.onboarding_createProfile_nickName_prompt
        )
        Spacer(modifier = Modifier.height(36.dp))

        presenter.profileIcon.collectAsState().value?.let { profileIcon ->
            // how can i convert a coil3.Bitmap to androidx.compose.ui.graphics.ImageBitmap in KMP
            val imageBitmap: ImageBitmap? = profileIcon as ImageBitmap
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "User profile icon generated from the hash of the public key",
                    modifier = Modifier.height(60.dp).width(60.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        BisqText.baseRegular(
            text = presenter.nym.collectAsState().value,
            color = BisqTheme.colors.light1,
        )
        Spacer(modifier = Modifier.height(12.dp))
        BisqText.baseRegular(
            text = presenter.id.collectAsState().value,
            color = BisqTheme.colors.grey2,
        )
        Spacer(modifier = Modifier.height(38.dp))
        BisqButton(
            text = strings.onboarding_createProfile_regenerate,
            backgroundColor = BisqTheme.colors.dark5,
            padding = PaddingValues(horizontal = 64.dp, vertical = 12.dp),
            onClick = { presenter.onGenerateKeyPair() }
        )
        Spacer(modifier = Modifier.height(40.dp))
        BisqButton(
            strings.buttons_next,
            onClick = { presenter.onCreateAndPublishNewUserProfile() },
            backgroundColor = if (presenter.nickName.value.isEmpty()) BisqTheme.colors.primaryDisabled else BisqTheme.colors.primary
        )
    }
}
