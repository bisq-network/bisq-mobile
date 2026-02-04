package network.bisq.mobile.client.common.presentation.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.client.shared.BuildConfig
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.settings.support.SupportPresenter
import network.bisq.mobile.presentation.settings.support.SupportWeblink
import org.koin.compose.koinInject

// TODO: Coverage exclusion rationale - Compose UI screen cannot be unit tested.
// Requires Compose UI testing framework for proper coverage.
@ExcludeFromCoverage
@Composable
fun ClientSupportScreen() {
    // Use the standard SupportPresenter for base functionality
    val supportPresenter: SupportPresenter = koinInject()
    val clientPresenter: ClientSupportPresenter = koinInject()

    RememberPresenterLifecycle(supportPresenter)
    RememberPresenterLifecycle(clientPresenter)

    val isInteractive by supportPresenter.isInteractive.collectAsState()
    val reportUrl by supportPresenter.reportUrl.collectAsState()

    // Client-specific push notification state
    val deviceToken by clientPresenter.deviceToken.collectAsState()
    val isDeviceRegistered by clientPresenter.isDeviceRegistered.collectAsState()
    val tokenRequestInProgress by clientPresenter.tokenRequestInProgress.collectAsState()

    BisqScrollScaffold(
        topBar = { TopBar("mobile.more.support".i18n(), showUserAvatar = false) },
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.Zero),
        snackbarHostState = supportPresenter.getSnackState(),
        isInteractive = isInteractive,
    ) {
        BisqText.H2Light("mobile.support.headline".i18n())
        BisqGap.V2()

        BisqText.BaseLight(
            text = "mobile.support.intro".i18n(),
            color = BisqTheme.colors.light_grey50,
        )
        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.Zero)) {
            SupportWeblink(
                text = "mobile.support.matrix".i18n(),
                link = BisqLinks.MATRIX,
                onClick = { supportPresenter.onOpenWebUrl(BisqLinks.MATRIX) },
            )
            SupportWeblink(
                text = "mobile.support.forum".i18n(),
                link = BisqLinks.FORUM,
                onClick = { supportPresenter.onOpenWebUrl(BisqLinks.FORUM) },
            )
            SupportWeblink(
                text = "mobile.support.telegram".i18n(),
                link = BisqLinks.TELEGRAM,
                onClick = { supportPresenter.onOpenWebUrl(BisqLinks.TELEGRAM) },
            )
            SupportWeblink(
                text = "mobile.support.reddit".i18n(),
                link = BisqLinks.REDDIT,
                onClick = { supportPresenter.onOpenWebUrl(BisqLinks.REDDIT) },
            )
        }

        BisqGap.V2()

        BisqText.BaseLight(
            text = "mobile.support.learnMore".i18n(),
            color = BisqTheme.colors.light_grey50,
        )
        LinkButton(
            text = "mobile.support.wiki".i18n(),
            link = BisqLinks.BISQ_EASY_WIKI_URL,
            onClick = { supportPresenter.onOpenWebUrl(BisqLinks.BISQ_EASY_WIKI_URL) },
            color = BisqTheme.colors.primary,
            padding = PaddingValues(all = BisqUIConstants.Zero),
        )

        BisqHDivider(modifier = Modifier.padding(top = BisqUIConstants.ScreenPadding2X, bottom = BisqUIConstants.ScreenPadding3X))

        // troubleShooting
        BisqText.H3Light("mobile.support.troubleShooting.headline".i18n())
        BisqGap.V2()
        BisqText.BaseLight(
            text = "mobile.support.troubleShooting.report".i18n() + " ",
            color = BisqTheme.colors.light_grey50,
        )
        LinkButton(
            text = "mobile.support.troubleShooting.github".i18n(),
            link = reportUrl,
            onClick = { supportPresenter.onOpenWebUrl(reportUrl) },
            color = BisqTheme.colors.primary,
            padding = PaddingValues(all = BisqUIConstants.Zero),
        )

        // Push Notifications Debug Section (only in debug builds)
        if (BuildConfig.IS_DEBUG) {
            BisqHDivider(modifier = Modifier.padding(top = BisqUIConstants.ScreenPadding2X, bottom = BisqUIConstants.ScreenPadding3X))

            BisqText.H3Light("Push Notifications (Debug)")
            BisqGap.V2()

            BisqText.BaseLight(
                text = "Debug feature to test APNs device token registration. Only works on real iOS devices.",
                color = BisqTheme.colors.light_grey50,
            )
            BisqGap.V1()

            // Registration status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BisqText.BaseRegular("Status:")
                BisqText.BaseRegular(
                    text = if (isDeviceRegistered) "✅ Registered" else "❌ Not Registered",
                    color = if (isDeviceRegistered) BisqTheme.colors.primary else BisqTheme.colors.danger,
                )
            }

            BisqGap.V1()

            // Device token display
            if (deviceToken != null) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
                ) {
                    BisqText.BaseRegular("Device Token:")
                    BisqText.SmallLight(
                        text = deviceToken ?: "",
                        color = BisqTheme.colors.light_grey50,
                    )
                }
                BisqGap.V1()
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                BisqButton(
                    text = if (tokenRequestInProgress) "Requesting..." else "Request Device Token",
                    onClick = { clientPresenter.onRequestDeviceToken() },
                    type = BisqButtonType.Outline,
                    disabled = tokenRequestInProgress,
                )

                if (deviceToken != null) {
                    BisqButton(
                        text = "Copy Token",
                        onClick = {
                            deviceToken?.let { token ->
                                clientPresenter.onCopyToken(token)
                            }
                        },
                        type = BisqButtonType.Outline,
                    )
                }
            }

            if (tokenRequestInProgress) {
                BisqGap.V1()
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        color = BisqTheme.colors.primary,
                        strokeWidth = 2.dp,
                    )
                }
            }
        }
    }
}
