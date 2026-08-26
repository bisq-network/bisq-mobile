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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowRightIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.QuestionIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware
import network.bisq.mobile.presentation.community.contacts.ContactsTabContent

@ExcludeFromCoverage
@Composable
fun CommunityHubScreen(initialSegment: CommunitySegment? = null) {
    val presenter = RememberPresenterLifecycleBackStackAware<CommunityHubPresenter>()

    // remember (not LaunchedEffect) so the deep-linked segment is selected DURING the first
    // composition — with LaunchedEffect the default segment (and its Support banner) renders
    // for one frame before the switch. Idempotent: selectInitialSegment is honored once.
    remember(initialSegment) {
        initialSegment?.let { presenter.selectInitialSegment(it) }
    }

    val uiState by presenter.uiState.collectAsState()

    CommunityHubScreenContent(
        uiState = uiState,
        onAction = presenter::onAction,
        topBar = { TopBar("mobile.community.title".i18n()) },
        segmentContent = { segment ->
            when (segment) {
                CommunitySegment.CONTACTS -> {
                    { ContactsTabContent() }
                }
                else -> null
            }
        },
    )
}

@Composable
fun CommunityHubScreenContent(
    uiState: CommunityHubUiState,
    onAction: (CommunityHubUiAction) -> Unit,
    topBar: @Composable () -> Unit = {},
    // Returns the selected segment's body composable, or null for the coming-soon placeholder.
    segmentContent: ((CommunitySegment) -> (@Composable () -> Unit)?)? = null,
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

            // The pinned Support reference belongs to the Discussions context only (see
            // CommunityHubScreenDesign.kt "SUPPORT — HUB-SIDE REFERENCE": it moves INTO the
            // Discussions content when that ships). Directory/inbox segments don't carry it.
            if (uiState.selectedSegment == null || uiState.selectedSegment == CommunitySegment.DISCUSSIONS) {
                SupportQuickAccessRow(onClick = { onAction(CommunityHubUiAction.OnOpenSupportChannel) })
            }

            // Shipped segments render their real body via segmentContent; the rest keep the
            // coming-soon placeholder. Previews pass no segmentContent (default null) so the
            // shell keeps rendering without Koin.
            val selected = uiState.selectedSegment
            val body = selected?.let { segmentContent?.invoke(it) }
            if (body == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val label = selected?.label()
                    if (label == null) {
                        BisqText.BaseRegularGrey(text = "mobile.community.comingSoon".i18n())
                    } else {
                        BisqText.BaseRegularGrey(text = "$label — ${"mobile.community.comingSoon".i18n()}")
                    }
                }
            } else {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    body()
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

// ============================================================================================
// Previews (shell states; the segments' real content is specced in design/community/)
// ============================================================================================

@ExcludeFromCoverage
@Preview
@Composable
private fun CommunityHubScreen_SingleSegmentPreview() {
    BisqTheme.Preview {
        CommunityHubScreenContent(
            uiState =
                CommunityHubUiState(
                    liveSegments = listOf(CommunitySegment.DISCUSSIONS),
                    selectedSegment = CommunitySegment.DISCUSSIONS,
                ),
            onAction = {},
            // Stateless TopBarContent: the production TopBar koin-injects and cannot render
            // in a plain preview.
            topBar = { TopBarContent(title = "mobile.community.title".i18n(), showBackButton = true, showUserAvatar = true) },
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CommunityHubScreen_AllSegmentsPreview() {
    BisqTheme.Preview {
        CommunityHubScreenContent(
            uiState =
                CommunityHubUiState(
                    liveSegments = CommunitySegment.entries.toList(),
                    selectedSegment = CommunitySegment.MESSAGES,
                ),
            onAction = {},
            topBar = { TopBarContent(title = "mobile.community.title".i18n(), showBackButton = true, showUserAvatar = true) },
        )
    }
}

/**
 * Proves the Contacts muted-tab treatment: Discussions selected (full primary) above
 * Contacts selected (muted light_grey50), comparable in one glance.
 */
@ExcludeFromCoverage
@Preview
@Composable
private fun CommunityHubScreen_ContactsMutedVsPrimaryPreview() {
    BisqTheme.Preview {
        Column {
            CommunitySegmentTabRow(
                liveSegments = CommunitySegment.entries.toList(),
                selected = CommunitySegment.DISCUSSIONS,
                onSelect = {},
            )
            CommunitySegmentTabRow(
                liveSegments = CommunitySegment.entries.toList(),
                selected = CommunitySegment.CONTACTS,
                onSelect = {},
            )
        }
    }
}
