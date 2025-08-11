package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.settings.AboutSettingsVO
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.BisqLogoCircle
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

interface IAboutPresenter : ViewPresenter {
    val appName: String
    fun versioning(): AboutSettingsVO

    fun navigateToMobileGitHub()
}

@Composable
fun AboutScreen() {
    val presenter: IAboutPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val versioning = remember { presenter.versioning() }
    val isInteractive by presenter.isInteractive.collectAsState()

    BisqScrollScaffold(
        topBar = { TopBar("mobile.settings.about".i18n(), showUserAvatar = false) },
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        snackbarHostState = presenter.getSnackState(),
        isInteractive = isInteractive,
    ) {

        BisqGap.V3()

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            BisqLogoCircle(Modifier.size(150.dp))
        }

        BisqGap.V2()

        BisqText.h3Regular(
            text = "${presenter.appName} v${versioning.appVersion}",
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        BisqGap.V3()

        if (versioning.apiVersion != null) {
            BisqText.baseRegular(
                text = "Node-API v${versioning.apiVersion}",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            BisqGap.V1()
        }

        if (versioning.torVersion != null) {
            BisqText.baseRegular(
                text = "Tor v${versioning.torVersion}",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            BisqGap.V1()
        }

        if (versioning.bisqCoreVersion != null) {
            BisqText.baseRegular(
                text = "Core v${versioning.bisqCoreVersion}",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            BisqGap.V1()
        }

        BisqGap.V3()

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            LinkButton(
                "support.resources.resources.sourceCode".i18n(),
                link = BisqLinks.BISQ_MOBILE_GH,
                onClick = { presenter.navigateToMobileGitHub() }
            )
        }
    }
}
