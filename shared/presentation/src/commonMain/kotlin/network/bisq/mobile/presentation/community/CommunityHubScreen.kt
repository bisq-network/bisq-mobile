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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import network.bisq.mobile.data.replicated.chat.ChatChannelDomainEnum
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.animations.AnimatedBadge
import network.bisq.mobile.presentation.common.ui.components.atoms.debouncedClickable
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowRightIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.QuestionIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.components.molecules.formatUnreadBadgeCount
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware
import network.bisq.mobile.presentation.community.contacts.ContactsTabContent
import network.bisq.mobile.presentation.community.messages.MessagesTabContent
import network.bisq.mobile.presentation.community.public_chat.PublicChatThread

@ExcludeFromCoverage
@Composable
fun CommunityHubScreen(initialSegment: CommunitySegment? = null) {
    val presenter = RememberPresenterLifecycleBackStackAware<CommunityHubPresenter>()

    // remember (not LaunchedEffect) so the deep-linked segment is selected DURING the first
    // composition — with LaunchedEffect the default segment (and its Support banner) renders
    // for one frame before the switch. This slot is gone and rebuilt every time the screen leaves
    // and re-enters composition, so it asks again on the way back from Support and on rotation;
    // the presenter is what makes the second ask a no-op.
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
                CommunitySegment.DISCUSSIONS -> {
                    { PublicChatThread(ChatChannelDomainEnum.DISCUSSION) }
                }
                CommunitySegment.MESSAGES -> {
                    { MessagesTabContent() }
                }
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
                    segmentUnreadCounts = uiState.segmentUnreadCounts,
                    onSelect = { onAction(CommunityHubUiAction.OnSegmentSelect(it)) },
                )
            }

            BisqGap.V1()

            // The pinned Support reference belongs to the Discussions context only. It stays
            // hub-side above the thread rather than inside it (a deliberate deviation from the
            // original design spec): this gate puts it in the same place on screen, and it keeps
            // the segment body a plain thread that the Support screen can reuse unchanged. Directory/inbox segments
            // don't carry it, and neither does the no-segment state: the row pushes a public chat
            // thread, and Discussions being live is what says this build serves one. That last arm is
            // hard to reach — TabContainerPresenter hides the hub icon while liveSegments is empty —
            // but the state is representable, so the gate stays.
            if (uiState.selectedSegment == CommunitySegment.DISCUSSIONS) {
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

/**
 * The tab's unread pill: the entry badge's own construction ([AnimatedBadge] hung over the
 * top-end corner, width-capped with shrinking text — the #1809-class fixes) recolored to the
 * neutral primary pill, since yellow is this app's warning color. Capped at 99+ for pixels
 * while the semantics carry the EXACT count for screen readers — precision the sighted UI
 * trades away for space. Renders nothing at zero, so the tab row shows no badge UI at all
 * when there is nothing unread.
 */
@Composable
private fun SegmentUnreadPill(
    segmentLabel: String,
    count: Int,
    modifier: Modifier = Modifier,
) {
    val text = formatUnreadBadgeCount(count) ?: return
    val exactCountDescription = "mobile.community.tab.unreadCountDescription".i18n(segmentLabel, count)
    AnimatedBadge(
        text = text,
        badgeColor = BisqTheme.colors.primary,
        modifier =
            modifier
                .widthIn(max = 26.dp)
                .semantics { contentDescription = exactCountDescription },
    ) {
        AutoResizeText(
            text = text,
            textStyle = BisqTheme.typography.xsmallMedium,
            textAlign = TextAlign.Center,
            minimumFontSize = 9.sp,
        )
    }
}

@Composable
private fun CommunitySegmentTabRow(
    liveSegments: List<CommunitySegment>,
    selected: CommunitySegment?,
    onSelect: (CommunitySegment) -> Unit,
    segmentUnreadCounts: Map<CommunitySegment, Int> = emptyMap(),
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
                // The Box hugs the label's own bounds (narrower than the weighted column), so the
                // pill overlays a corner of the TEXT and costs the row zero width — the same
                // optical-overlay technique as CommunityTopBarIcon, and deliberately not a
                // BadgedBox (see the #1809 clipping bug). Muted segments (Contacts, a directory)
                // are structurally excluded: even a buggy count cannot badge them.
                Box {
                    BisqText.BaseRegular(
                        text = segment.label(),
                        color = if (isSelected) selectedColor else BisqTheme.colors.mid_grey20,
                    )
                    if (segment !in mutedSegments) {
                        SegmentUnreadPill(
                            segmentLabel = segment.label(),
                            count = segmentUnreadCounts[segment] ?: 0,
                            modifier = Modifier.align(Alignment.TopEnd),
                        )
                    }
                }
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

/**
 * Debounced because it navigates and [network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManagerImpl]
 * pushes without `launchSingleTop`: a double tap would otherwise leave two Support screens on the
 * stack, and two backs to get out of them.
 */
@Composable
private fun SupportQuickAccessRow(onClick: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .debouncedClickable(role = Role.Button, onClick = onClick)
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
// Previews (shell states; each segment's real content lives with its own screen)
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
 * The pill's whole show/hide contract in one preview, top to bottom:
 * 1. nothing unread — NO pill anywhere (the new UI stays invisible until it is needed);
 * 2. unread on Messages only — one pill, on the tab that holds it;
 * 3. unread on both conversation tabs, Messages past the cap — two pills, "99+" on Messages;
 * 4. a (buggy) count for Contacts — still no pill: the directory tab is structurally excluded.
 */
@ExcludeFromCoverage
@Preview
@Composable
private fun CommunityHubScreen_TabUnreadPillStatesPreview() {
    BisqTheme.Preview {
        Column {
            CommunitySegmentTabRow(
                liveSegments = CommunitySegment.entries.toList(),
                selected = CommunitySegment.DISCUSSIONS,
                onSelect = {},
                segmentUnreadCounts = emptyMap(),
            )
            CommunitySegmentTabRow(
                liveSegments = CommunitySegment.entries.toList(),
                selected = CommunitySegment.DISCUSSIONS,
                onSelect = {},
                segmentUnreadCounts = mapOf(CommunitySegment.MESSAGES to 3),
            )
            CommunitySegmentTabRow(
                liveSegments = CommunitySegment.entries.toList(),
                selected = CommunitySegment.MESSAGES,
                onSelect = {},
                segmentUnreadCounts = mapOf(CommunitySegment.DISCUSSIONS to 12, CommunitySegment.MESSAGES to 150),
            )
            CommunitySegmentTabRow(
                liveSegments = CommunitySegment.entries.toList(),
                selected = CommunitySegment.DISCUSSIONS,
                onSelect = {},
                segmentUnreadCounts = mapOf(CommunitySegment.CONTACTS to 4),
            )
        }
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
