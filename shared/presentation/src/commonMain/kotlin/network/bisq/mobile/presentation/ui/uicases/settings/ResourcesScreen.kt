package network.bisq.mobile.presentation.ui.uicases.settings

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
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.button.LinkButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.AppLinkIcon
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
fun ResourcesScreen() {
    val presenter: ResourcesPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isInteractive by presenter.isInteractive.collectAsState()
    val versionInfo by presenter.versionInfo.collectAsState()
    val deviceInfo by presenter.deviceInfo.collectAsState()

    BisqScrollScaffold(
        topBar = { TopBar("mobile.more.resources".i18n(), showUserAvatar = false) },
        horizontalAlignment = Alignment.Start,
        isInteractive = isInteractive,
    ) {
        BisqText.h3Light("support.resources.guides.headline".i18n(), color = BisqTheme.colors.light_grey50)
        AppLinkButton(
            "support.resources.guides.tradeGuide".i18n(),
            onClick = { presenter.onOpenTradeGuide() }
        )
        AppLinkButton(
            "support.resources.guides.chatRules".i18n(),
            onClick = { presenter.onOpenChatRules() }
        )
        AppLinkButton(
            "support.resources.guides.walletGuide".i18n(),
            onClick = { presenter.onOpenWalletGuide() }
        )

        BisqHDivider(modifier = Modifier.padding(top = BisqUIConstants.ScreenPadding, bottom = BisqUIConstants.ScreenPadding2X))
        BisqGap.V1()
        BisqText.h3Light("support.resources.resources.headline".i18n(), color = BisqTheme.colors.light_grey50)
        ResourceWeblink(
            "support.resources.resources.webpage".i18n(),
            link = BisqLinks.WEBPAGE,
            onClick = { presenter.onOpenWebUrl(BisqLinks.WEBPAGE) }
        )
        ResourceWeblink(
            "support.resources.resources.dao".i18n(),
            link = BisqLinks.DAO,
            onClick = { presenter.onOpenWebUrl(BisqLinks.DAO) }
        )
        ResourceWeblink(
            "support.resources.resources.sourceCode".i18n(),
            link = BisqLinks.BISQ_MOBILE_GH,
            onClick = { presenter.onOpenWebUrl(BisqLinks.BISQ_MOBILE_GH) }
        )
        ResourceWeblink(
            "support.resources.resources.community".i18n(),
            link = BisqLinks.MATRIX,
            onClick = { presenter.onOpenWebUrl(BisqLinks.MATRIX) }
        )

        BisqHDivider(modifier = Modifier.padding(top = BisqUIConstants.ScreenPadding, bottom = BisqUIConstants.ScreenPadding2X))
        BisqGap.V1()
        BisqText.h3Light("mobile.resources.version.headline".i18n(), color = BisqTheme.colors.light_grey50)
        BisqText.baseLight(
            text = versionInfo,
            color = BisqTheme.colors.mid_grey20,
            modifier = Modifier
                .padding(vertical = BisqUIConstants.ScreenPaddingHalf, horizontal = BisqUIConstants.ScreenPadding2X)
        )

        BisqHDivider(modifier = Modifier.padding(top = BisqUIConstants.ScreenPadding, bottom = BisqUIConstants.ScreenPadding2X))
        BisqGap.V1()
        BisqText.h3Light("mobile.resources.deviceInfo.headline".i18n(), color = BisqTheme.colors.light_grey50)
        BisqText.baseLight(
            text = deviceInfo,
            color = BisqTheme.colors.mid_grey20,
            modifier = Modifier
                .padding(vertical = BisqUIConstants.ScreenPaddingHalf, horizontal = BisqUIConstants.ScreenPadding2X)
        )

        BisqHDivider(modifier = Modifier.padding(top = BisqUIConstants.ScreenPadding, bottom = BisqUIConstants.ScreenPadding2X))
        BisqGap.V1()
        BisqText.h3Light("support.resources.legal.headline".i18n(), color = BisqTheme.colors.light_grey50)
        AppLinkButton(
            "support.resources.legal.tac".i18n(),
            onClick = { presenter.onOpenTac() }
        )
        ResourceWeblink(
            "support.resources.legal.license".i18n(),
            link = BisqLinks.LICENSE,
            onClick = { presenter.onOpenWebUrl(BisqLinks.LICENSE) }
        )
    }
}

@Composable
fun ResourceWeblink(
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
            horizontal = BisqUIConstants.ScreenPadding2X,
            vertical = BisqUIConstants.ScreenPaddingHalf
        ),
    )
}

@Composable
fun AppLinkButton(
    text: String,
    onClick: (() -> Unit)? = null,
) {
    BisqButton(
        text,
        leftIcon = { AppLinkIcon(modifier = Modifier.size(16.dp).alpha(0.4f)) },
        color = BisqTheme.colors.mid_grey20,
        type = BisqButtonType.Underline,
        onClick = { onClick?.invoke() },
    )
}