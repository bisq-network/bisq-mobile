package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.atoms.icons.rememberPlatformImagePainter
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

@Composable
fun CreateProfileScreen(
) {
    val presenter: CreateProfilePresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val generateKeyPairInProgress by presenter.generateKeyPairInProgress.collectAsState()
    val createAndPublishInProgress by presenter.createAndPublishInProgress.collectAsState()
    val nickName by presenter.nickName.collectAsState()
    val profileIcon by presenter.profileIcon.collectAsState()
    val nym by presenter.nym.collectAsState()
    val id by presenter.id.collectAsState()

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
            value = nickName,
            onValueChange = { name, _ -> presenter.setNickname(name) },
            placeholder = "onboarding.createProfile.nickName.prompt".i18n(),
            validation = {
                return@BisqTextField presenter.validateNickname(it)
            }
        )
        Spacer(modifier = Modifier.height(36.dp))

        profileIcon?.let { profileIcon ->
            val painter = rememberPlatformImagePainter(profileIcon)
            Button(contentPadding = PaddingValues(0.dp),
                enabled = !generateKeyPairInProgress,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.Black // Or any color you want for the text/icon
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp),
                onClick = { presenter.onGenerateKeyPair() }) {
                Image(
                    painter = painter,
                    contentDescription = "User profile icon generated from the hash of the public key",
                    modifier = Modifier.height(60.dp).width(60.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        BisqText.baseRegular(nym)
        Spacer(modifier = Modifier.height(12.dp))
        BisqText.smallRegularGrey(id)
        Spacer(modifier = Modifier.height(38.dp))
        BisqButton(
            text = "onboarding.createProfile.regenerate".i18n(),
            type = BisqButtonType.Grey,
            disabled = generateKeyPairInProgress,
            padding = PaddingValues(horizontal = 64.dp, vertical = 12.dp),
            onClick = { presenter.onGenerateKeyPair() }
        )
        Spacer(modifier = Modifier.height(40.dp))
        BisqButton(
            "action.next".i18n(),
            onClick = { presenter.onCreateAndPublishNewUserProfile() },
            disabled = nickName.isEmpty() || createAndPublishInProgress
        )
    }
}
