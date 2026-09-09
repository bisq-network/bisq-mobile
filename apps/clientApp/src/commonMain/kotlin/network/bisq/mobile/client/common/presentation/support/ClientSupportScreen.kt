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
import network.bisq.mobile.presentation.settings.support.SupportChannelLink
import network.bisq.mobile.presentation.settings.support.SupportPresenter
import network.bisq.mobile.presentation.settings.support.SupportUiAction
import network.bisq.mobile.presentation.settings.support.SupportWeblink
import org.koin.compose.koinInject

// Coverage exclusion: a screen of static links and a debug panel. ClientSupportScreenUiTest
// covers the one conditional part, the in-app Support entry.
@ExcludeFromCoverage
@Composable
fun ClientSupportScreen(
    // Test seam: IS_DEBUG is a compile-time const whose value follows the gradle invocation, so a
    // bare read leaves one branch untestable per run. Same seam as ClientReputationServiceFacade.
    isDebug: Boolean = BuildConfig.IS_DEBUG,
) {
    // Use the standard SupportPresenter for base functionality
    val supportPresenter: SupportPresenter = koinInject()
    val clientPresenter: ClientSupportPresenter = koinInject()

    RememberPresenterLifecycle(supportPresenter)
    RememberPresenterLifecycle(clientPresenter)

    val supportUiState by supportPresenter.uiState.collectAsState()
    val reportUrl = supportUiState.reportUrl
    val isSupportChannelAvailable = supportUiState.isSupportChannelAvailable

    // Client-specific push notification state
    val clientUiState by clientPresenter.uiState.collectAsState()
    val deviceToken = clientUiState.deviceToken
    val isDeviceRegistered = clientUiState.isDeviceRegistered
    val tokenRequestInProgress = clientUiState.tokenRequestInProgress

    BisqScrollScaffold(
        topBar = { TopBar("mobile.more.support".i18n(), showUserAvatar = false) },
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.Zero),
    ) {
        BisqText.H2Light("mobile.support.headline".i18n())
        BisqGap.V2()

        BisqText.BaseLight(
            text = "mobile.support.intro".i18n(),
            color = BisqTheme.colors.light_grey50,
        )
        if (isSupportChannelAvailable) {
            BisqGap.V2()
            SupportChannelLink(onClick = { supportPresenter.onAction(SupportUiAction.OnOpenSupportChannel) })
        }
        BisqGap.V2()
        // Caption so the external links read as one named, secondary group — with or without the
        // in-app button above them.
        BisqText.SmallRegularGrey("mobile.support.communityChannels".i18n())
        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.Zero)) {
            SupportWeblink(
                text = "mobile.support.matrix".i18n(),
                link = BisqLinks.MATRIX,
            )
            SupportWeblink(
                text = "mobile.support.forum".i18n(),
                link = BisqLinks.FORUM,
            )
            SupportWeblink(
                text = "mobile.support.telegram".i18n(),
                link = BisqLinks.TELEGRAM,
            )
            SupportWeblink(
                text = "mobile.support.reddit".i18n(),
                link = BisqLinks.REDDIT,
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
            color = BisqTheme.colors.primary,
            padding = PaddingValues(all = BisqUIConstants.Zero),
        )

        // Push Notifications Debug Section (only in debug builds)
        if (isDebug) {
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
                    onClick = { clientPresenter.onAction(ClientSupportUiAction.OnRequestDeviceToken) },
                    type = BisqButtonType.Outline,
                    disabled = tokenRequestInProgress,
                )

                if (deviceToken != null) {
                    BisqButton(
                        text = "Copy Token",
                        onClick = {
                            deviceToken?.let { token ->
                                clientPresenter.onAction(ClientSupportUiAction.OnCopyToken(token))
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
