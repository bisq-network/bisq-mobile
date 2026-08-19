package network.bisq.mobile.presentation.community

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowRightIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.QuestionIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware

@Composable
fun CommunityHubScreen() {
    val presenter = RememberPresenterLifecycleBackStackAware<CommunityHubPresenter>()

    val uiState by presenter.uiState.collectAsState()

    CommunityHubScreenContent(
        uiState = uiState,
        onAction = presenter::onAction,
        topBar = { TopBar("mobile.community.title".i18n()) },
    )
}

@Composable
fun CommunityHubScreenContent(
    uiState: CommunityHubUiState,
    onAction: (CommunityHubUiAction) -> Unit,
    topBar: @Composable () -> Unit = {},
) {
    BisqScaffold(
        topBar = topBar,
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            if (uiState.liveSegments.size > 1) {
                CommunitySegmentTabRow(
                    liveSegments = uiState.liveSegments,
                    selected = uiState.selectedSegment,
                    onSelect = { onAction(CommunityHubUiAction.OnSegmentSelect(it)) },
                )
            }

            BisqGap.V1()

            SupportQuickAccessRow(onClick = { onAction(CommunityHubUiAction.OnOpenSupportChannel) })

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                // TODO replace the placeholder bodies with the segments' real content as each ships
                val label = uiState.selectedSegment?.label()
                if (label == null) {
                    BisqText.BaseRegularGrey(text = "mobile.community.comingSoon".i18n())
                } else {
                    BisqText.BaseRegularGrey(text = "$label — ${"mobile.community.comingSoon".i18n()}")
                }
            }
        }
    }
}

// Contacts renders muted even when live: it is a directory, not an inbox, and must not
// compete visually with the conversation tabs.
private val mutedSegments = setOf(CommunitySegment.CONTACTS)

@Composable
private fun CommunitySegmentTabRow(
    liveSegments: List<CommunitySegment>,
    selected: CommunitySegment?,
    onSelect: (CommunitySegment) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = BisqUIConstants.ScreenPadding)) {
        liveSegments.forEach { segment ->
            val isSelected = segment == selected
            val selectedColor = if (segment in mutedSegments) BisqTheme.colors.light_grey50 else BisqTheme.colors.primary
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable { onSelect(segment) }
                        .padding(vertical = BisqUIConstants.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BisqText.BaseRegular(
                    text = segment.label(),
                    color = if (isSelected) selectedColor else BisqTheme.colors.mid_grey20,
                )
                Box(
                    modifier =
                        Modifier
                            .padding(top = BisqUIConstants.ScreenPaddingQuarter)
                            .width(BisqUIConstants.ScreenPadding4X)
                            .height(2.dp)
                            .background(if (isSelected) selectedColor else BisqTheme.colors.dark_grey50),
                )
            }
        }
    }
}

@Composable
private fun SupportQuickAccessRow(onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .background(BisqTheme.colors.dark_grey40)
                .padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPadding),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(BisqUIConstants.ScreenPadding3X).background(BisqTheme.colors.dark_grey50, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            QuestionIcon(modifier = Modifier.size(BisqUIConstants.ScreenPadding2X))
        }
        Column(modifier = Modifier.weight(1f)) {
            BisqText.BaseMedium(text = "mobile.community.support.needHelp".i18n(), color = BisqTheme.colors.white)
            BisqText.SmallLight(text = "mobile.community.support.openChannel".i18n(), color = BisqTheme.colors.mid_grey20)
        }
        ArrowRightIcon()
    }
}

@Composable
private fun CommunitySegment.label(): String =
    when (this) {
        CommunitySegment.DISCUSSIONS -> "mobile.community.tab.discussions".i18n()
        CommunitySegment.MESSAGES -> "mobile.community.tab.messages".i18n()
        CommunitySegment.CONTACTS -> "mobile.community.tab.contacts".i18n()
    }
