package network.bisq.mobile.presentation.settings.resources

import androidx.compose.foundation.layout.PaddingValues
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
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.AppLinkIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WebLinkIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.BisqLinks
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import org.koin.compose.koinInject

@ExcludeFromCoverage
@Composable
fun ResourcesScreen() {
    val presenter: ResourcesPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()
    val uiState by presenter.uiState.collectAsState()
    val dividerModifier =
        Modifier.padding(
            top = BisqUIConstants.ScreenPaddingHalf,
            bottom = BisqUIConstants.ScreenPadding,
        )

    BisqScrollScaffold(
        topBar = { TopBar("mobile.more.resources".i18n(), showUserAvatar = false) },
        horizontalAlignment = Alignment.Start,
        isInteractive = isInteractive,
    ) {
        Guides(presenter::onAction)

        BisqHDivider(modifier = dividerModifier)
        BisqGap.V1()

        WebResources(
            onAction = presenter::onAction,
            onError = { _ -> presenter.showSnackbar("mobile.error.cannotOpenUrl".i18n(), SnackbarType.ERROR) },
        )

        BisqHDivider(modifier = dividerModifier)
        BisqGap.V1()

        Version(uiState.versionInfo)

        BisqHDivider(modifier = dividerModifier)
        BisqGap.V1()

        DeviceInfo(uiState.deviceInfo)

        BisqHDivider(modifier = dividerModifier)
        BisqGap.V1()

        Legal(
            onAction = presenter::onAction,
            onError = { _ -> presenter.showSnackbar("mobile.error.cannotOpenUrl".i18n(), SnackbarType.ERROR) },
        )
    }
}

@ExcludeFromCoverage
@Composable
private fun Guides(
    onAction: (ResourcesUiAction) -> Unit,
) {
    BisqText.H3Light(
        "support.resources.guides.headline".i18n(),
        color = BisqTheme.colors.light_grey50,
    )

    AppLinkButton(
        "support.resources.guides.tradeGuide".i18n(),
        onClick = { onAction(ResourcesUiAction.OnNavigateToScreen(NavRoute.TradeGuideOverview)) },
    )
    AppLinkButton(
        "support.resources.guides.chatRules".i18n(),
        onClick = { onAction(ResourcesUiAction.OnNavigateToScreen(NavRoute.ChatRules)) },
    )
    AppLinkButton(
        "support.resources.guides.walletGuide".i18n(),
        onClick = { onAction(ResourcesUiAction.OnNavigateToScreen(NavRoute.WalletGuideIntro)) },
    )
}

@ExcludeFromCoverage
@Composable
private fun WebResources(
    onAction: (ResourcesUiAction) -> Unit,
    onError: (Throwable) -> Unit,
) {
    BisqText.H3Light(
        "support.resources.resources.headline".i18n(),
        color = BisqTheme.colors.light_grey50,
    )
    ResourceWeblink(
        "support.resources.resources.webpage".i18n(),
        link = BisqLinks.WEBPAGE,
        onClick = { onAction(ResourcesUiAction.OnNavigateToUrl(BisqLinks.WEBPAGE)) },
        onError = onError,
    )
    ResourceWeblink(
        "support.resources.resources.dao".i18n(),
        link = BisqLinks.DAO,
        onClick = { onAction(ResourcesUiAction.OnNavigateToUrl(BisqLinks.DAO)) },
        onError = onError,
    )
    ResourceWeblink(
        "support.resources.resources.sourceCode".i18n(),
        link = BisqLinks.BISQ_MOBILE_GH,
        onClick = { onAction(ResourcesUiAction.OnNavigateToUrl(BisqLinks.BISQ_MOBILE_GH)) },
        onError = onError,
    )
    ResourceWeblink(
        "support.resources.resources.community".i18n(),
        link = BisqLinks.MATRIX,
        onClick = { onAction(ResourcesUiAction.OnNavigateToUrl(BisqLinks.MATRIX)) },
        onError = onError,
    )
}

@ExcludeFromCoverage
@Composable
private fun Version(versionInfo: String) {
    BisqText.H3Light(
        "mobile.resources.version.headline".i18n(),
        color = BisqTheme.colors.light_grey50,
    )
    BisqText.BaseLight(
        text = versionInfo,
        color = BisqTheme.colors.mid_grey20,
        modifier =
            Modifier
                .padding(
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                    horizontal = BisqUIConstants.ScreenPadding2X,
                ),
    )
}

@ExcludeFromCoverage
@Composable
private fun DeviceInfo(deviceInfo: String) {
    BisqText.H3Light(
        "mobile.resources.deviceInfo.headline".i18n(),
        color = BisqTheme.colors.light_grey50,
    )
    BisqText.BaseLight(
        text = deviceInfo,
        color = BisqTheme.colors.mid_grey20,
        modifier =
            Modifier
                .padding(
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                    horizontal = BisqUIConstants.ScreenPadding2X,
                ),
    )
}

@ExcludeFromCoverage
@Composable
private fun Legal(
    onAction: (ResourcesUiAction) -> Unit,
    onError: (Throwable) -> Unit,
) {
    BisqText.H3Light(
        "support.resources.legal.headline".i18n(),
        color = BisqTheme.colors.light_grey50,
    )
    AppLinkButton(
        "support.resources.legal.tac".i18n(),
        onClick = { onAction(ResourcesUiAction.OnNavigateToScreen(NavRoute.UserAgreementDisplay)) },
    )
    ResourceWeblink(
        "support.resources.legal.license".i18n(),
        link = BisqLinks.LICENSE,
        onClick = { onAction(ResourcesUiAction.OnNavigateToUrl(BisqLinks.LICENSE)) },
        onError = onError,
    )
}

@ExcludeFromCoverage
@Composable
private fun ResourceWeblink(
    text: String,
    link: String,
    onClick: (() -> Unit)? = null,
    onError: ((Throwable) -> Unit)? = null,
) {
    LinkButton(
        text,
        link = link,
        onClick = onClick,
        onError = onError,
        leftIcon = { WebLinkIcon(modifier = Modifier.size(16.dp).alpha(0.5f)) },
        color = BisqTheme.colors.mid_grey20,
        padding =
            PaddingValues(
                horizontal = BisqUIConstants.ScreenPadding2X,
                vertical = BisqUIConstants.ScreenPaddingHalf,
            ),
    )
}

@ExcludeFromCoverage
@Composable
private fun AppLinkButton(
    text: String,
    onClick: (() -> Unit)? = null,
) {
    BisqButton(
        text = text,
        leftIcon = { AppLinkIcon(modifier = Modifier.size(16.dp).alpha(0.4f)) },
        color = BisqTheme.colors.mid_grey20,
        type = BisqButtonType.Underline,
        onClick = { onClick?.invoke() },
    )
}
