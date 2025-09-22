package network.bisq.mobile.presentation.ui.uicases.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.WebLinkIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject


@Composable
fun SupportScreen() {
    val presenter: SupportPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()
    val reportUrl by presenter.reportUrl.collectAsState()

    BisqScrollScaffold(
        topBar = { TopBar("mobile.more.support".i18n(), showUserAvatar = false) },
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.Zero),
        snackbarHostState = presenter.getSnackState(),
        isInteractive = isInteractive,
    ) {
        BisqText.h2Light("mobile.support.headline".i18n())
        BisqGap.V2()

        BisqText.baseLight(
            text = "mobile.support.intro".i18n(),
            color = BisqTheme.colors.light_grey50,
        )
        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.Zero)) {
            SupportWeblink(
                text = "mobile.support.matrix".i18n(),
                link = BisqLinks.MATRIX,
                onClick = { presenter.onOpenWebUrl(BisqLinks.MATRIX) },
            )
            SupportWeblink(
                text = "mobile.support.forum".i18n(),
                link = BisqLinks.FORUM,
                onClick = { presenter.onOpenWebUrl(BisqLinks.FORUM) },
            )
            SupportWeblink(
                text = "mobile.support.telegram".i18n(),
                link = BisqLinks.TELEGRAM,
                onClick = { presenter.onOpenWebUrl(BisqLinks.TELEGRAM) },
            )
            SupportWeblink(
                text = "mobile.support.reddit".i18n(),
                link = BisqLinks.REDDIT,
                onClick = { presenter.onOpenWebUrl(BisqLinks.REDDIT) },
            )
        }

        BisqGap.V2()

        BisqText.baseLight(
            text = "mobile.support.learnMore".i18n(),
            color = BisqTheme.colors.light_grey50,
        )
        LinkButton(
            text = "mobile.support.wiki".i18n(),
            link = BisqLinks.BISQ_EASY_WIKI_URL,
            onClick = { presenter.onOpenWebUrl(BisqLinks.BISQ_EASY_WIKI_URL) },
            color = BisqTheme.colors.primary,
            padding = PaddingValues(all = BisqUIConstants.Zero),
        )

        BisqHDivider(modifier = Modifier.padding(top = BisqUIConstants.ScreenPadding2X, bottom = BisqUIConstants.ScreenPadding3X))

        // AI support
        BisqText.h3Light("mobile.support.ai.headline".i18n())
        BisqGap.V2()
        BisqText.baseLight(
            text = "mobile.support.ai.info".i18n() + " ",
            color = BisqTheme.colors.light_grey50,
        )
        LinkButton(
            text = "mobile.support.ai.open".i18n(),
            link = BisqLinks.BISQ_AI,
            onClick = { presenter.onOpenWebUrl(BisqLinks.BISQ_AI) },
            color = BisqTheme.colors.primary,
            padding = PaddingValues(all = BisqUIConstants.Zero),
        )

        BisqHDivider(modifier = Modifier.padding(top = BisqUIConstants.ScreenPadding2X, bottom = BisqUIConstants.ScreenPadding3X))

        // troubleShooting
        BisqText.h3Light("mobile.support.troubleShooting.headline".i18n())
        BisqGap.V2()
        BisqText.baseLight(
            text = "mobile.support.troubleShooting.report".i18n() + " ",
            color = BisqTheme.colors.light_grey50,
        )
        LinkButton(
            text = "mobile.support.troubleShooting.github".i18n(),
            link = reportUrl,
            onClick = { presenter.onOpenWebUrl(reportUrl) },
            color = BisqTheme.colors.primary,
            padding = PaddingValues(all = BisqUIConstants.Zero),
        )

        // Restart / Shutdown is only supported on Android
        if (!presenter.isIOS()) {
            BisqHDivider(modifier = Modifier.padding(top = BisqUIConstants.ScreenPadding2X, bottom = BisqUIConstants.ScreenPadding3X))

            // connectivity
            BisqText.h3Light("mobile.support.connectivity.headline".i18n())
            BisqGap.V2()
            BisqText.baseLight(
                text = "mobile.support.connectivity.info".i18n(),
                color = BisqTheme.colors.light_grey50,
            )
            BisqGap.V1()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    BisqUIConstants.ScreenPadding,
                    Alignment.CenterHorizontally
                )
            ) {
                BisqButton(
                    text = "mobile.support.connectivity.restart".i18n(),
                    onClick = { presenter.onRestartApp() },
                    type = BisqButtonType.Outline

                )
                BisqButton(
                    text = "mobile.support.connectivity.shutdown".i18n(),
                    onClick = { presenter.onTerminateApp() },
                    type = BisqButtonType.Outline
                )
            }
        }
    }
}

@Composable
fun SupportWeblink(
    text: String,
    link: String,
    onClick: (() -> Unit)? = null,
) {
    LinkButton(
        text,
        link = link,
        onClick = onClick,
        leftIcon = { WebLinkIcon(modifier = Modifier.size(16.dp).alpha(0.5f)) },
        color = BisqTheme.colors.mid_grey20,
        padding = PaddingValues(
            horizontal = BisqUIConstants.ScreenPaddingHalf,
            vertical = BisqUIConstants.Zero
        ),
    )
}