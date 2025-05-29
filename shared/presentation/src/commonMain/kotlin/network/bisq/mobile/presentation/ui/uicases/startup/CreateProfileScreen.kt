package network.bisq.mobile.presentation.ui.uicases.startup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogo
import network.bisq.mobile.presentation.ui.components.atoms.icons.rememberPlatformImagePainter
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun CreateProfileScreen(
) {
    val presenter: CreateProfilePresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val generateKeyPairInProgress by presenter.generateKeyPairInProgress.collectAsState()
    val createAndPublishInProgress by presenter.createAndPublishInProgress.collectAsState()
    val nickName by presenter.nickName.collectAsState()
    val nickNameValid by presenter.nickNameValid.collectAsState()
    val profileIcon by presenter.profileIcon.collectAsState()
    val nym by presenter.nym.collectAsState()
    val id by presenter.id.collectAsState()
    val botSize = BisqUIConstants.ScreenPadding5X

    BisqScrollScaffold {
        BisqLogo()
        BisqGap.V2()
        BisqText.h1LightGrey("onboarding.createProfile.headline".i18n())
        BisqGap.V1()
        BisqText.baseRegularGrey(
            text = "onboarding.createProfile.subTitle".i18n(),
            modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPadding2X),
            textAlign = TextAlign.Center,
        )
        BisqGap.V3()
        BisqTextField(
            label = "onboarding.createProfile.nickName".i18n(),
            value = nickName,
            onValueChange = { name, _ -> presenter.setNickname(name) },
            placeholder = "onboarding.createProfile.nickName.prompt".i18n(),
            validation = {
                return@BisqTextField presenter.validateNickname(it)
            }
        )
        BisqGap.V3()

        if (generateKeyPairInProgress) {
            CircularProgressIndicator(modifier = Modifier.size(botSize))
        } else {
            profileIcon?.let { profileIcon ->
                val painter = rememberPlatformImagePainter(profileIcon)
                Button(
                    contentPadding = PaddingValues(BisqUIConstants.Zero),
                    enabled = !generateKeyPairInProgress,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.Black // Or any color you want for the text/icon
                    ),
                    elevation = ButtonDefaults.buttonElevation(BisqUIConstants.Zero),
                    onClick = { presenter.onGenerateKeyPair() }) {
                    Image(
                        painter = painter,
                        contentDescription = "User profile icon generated from the hash of the public key",
                        modifier = Modifier.size(botSize)
                    )
                }
            }
        }

        val nymText = if (generateKeyPairInProgress) {
            "onboarding.createProfile.nym.generating".i18n()
        } else {
            nym
        }

        BisqGap.V3()
        BisqText.baseRegular(nymText)
        BisqGap.V3()
        BisqButton(
            text = "onboarding.createProfile.regenerate".i18n(),
            type = BisqButtonType.Grey,
            disabled = generateKeyPairInProgress,
            padding = PaddingValues(horizontal = BisqUIConstants.ScreenPadding5X, vertical = BisqUIConstants.ScreenPadding),
            onClick = { presenter.onGenerateKeyPair() }
        )
        BisqGap.V3()
        BisqButton(
            "action.next".i18n(),
            onClick = { presenter.onCreateAndPublishNewUserProfile() },
            disabled = nickName.isEmpty() || createAndPublishInProgress || generateKeyPairInProgress || !nickNameValid
        )
    }
}
