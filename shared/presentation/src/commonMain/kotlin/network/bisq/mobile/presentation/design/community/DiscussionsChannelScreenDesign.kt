/**
 * DiscussionsChannelScreenDesign.kt — Design PoC (Milestone 11 "Bisq community", issue #589)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * WHAT SHIPS THIS MILESTONE (lede — read this first)
 * ======================================================================================
 * bisq2 wires exactly ONE Discussion channel and ONE Support channel today —
 * `chat/src/main/java/bisq/chat/ChatService.java` passes a singleton `List.of(...)` to
 * `addToCommonPublicChatChannelServices(...)` for each of `ChatChannelDomain.DISCUSSION`
 * and `ChatChannelDomain.SUPPORT`. There is no channel directory to browse. This screen
 * IS the entire Discussions experience: `CommunityHubScreenDesign.kt`'s Discussions tab
 * renders this composable directly (`showTopBar = false`) — tapping the Community icon
 * takes you straight into the Discussion channel's message thread, no list or picker in
 * front of it.
 *
 * This same composable is reused, unchanged, as the Support channel's thread — see
 * "REUSED FOR THE SUPPORT CHANNEL" below — because bisq2 models both as the same kind of
 * object (`CommonPublicChatChannel`), differing only by `ChatChannelDomain`.
 *
 * ======================================================================================
 * WHY THERE'S NO CHANNEL LIST (evidence, not a stylistic choice)
 * ======================================================================================
 * An earlier pass of this PoC modeled Discussions as a searchable, section-grouped list
 * of several channels ("General Discussion," "Market Chat," "New Trader Help," "App
 * Support"). That was invented fixture data with no backend counterpart. bisq2's own
 * `ChatChannelDomain`/`SubDomain` enums (`chat` module) show it already ran that
 * experiment and reversed it: `SubDomain.java` carries `@Deprecated DISCUSSION_BITCOIN`,
 * `DISCUSSION_MARKETS`, `DISCUSSION_OFF_TOPIC`, and a whole `EVENTS` domain
 * (`CONFERENCES`/`MEETUPS`/`PODCASTS`/`TRADE_EVENTS`) — all migrated back onto the single
 * surviving `DISCUSSION_BISQ` channel. `SUPPORT_QUESTIONS`/`SUPPORT_REPORTS` were folded
 * the same way into `SUPPORT_SUPPORT`. A list UI with one row per section is ceremony,
 * not browsing, when there's nothing to browse — this screen drops it accordingly.
 *
 * ======================================================================================
 * REINTRODUCING A CHANNEL PICKER — the explicit trigger (not built preemptively)
 * ======================================================================================
 * If bisq2 ever ships a second live Discussion channel (a new non-deprecated
 * `SubDomain` under `ChatChannelDomain.DISCUSSION`, wired into `ChatService.java`'s
 * channel list), THAT is the moment to reintroduce a channel picker — not before. The
 * deprecation history above already shows this exact expansion was tried once and
 * reversed, so don't pre-build multi-channel UI speculatively. `DiscussionsTabContent`
 * in `CommunityHubScreenDesign.kt` is the reinsertion point: it currently renders this
 * screen directly; it would instead render a list that pushes into this same
 * channel-thread screen per row.
 *
 * ======================================================================================
 * REUSED FOR THE SUPPORT CHANNEL
 * ======================================================================================
 * `CommunityHubScreenDesign.kt`'s pinned "Need help?" reference in the Discussions tab
 * (see that file's "SUPPORT — HUB-SIDE REFERENCE" section) pushes to THIS SAME
 * `DiscussionsChannelScreenContent`, parameterized with the Support channel's data
 * (`channelName = "Support"`) and `showTopBar = true` (pushed as its own screen, not
 * embedded in a tab, so it needs its own back button). No new screen type — bisq2 itself
 * renders Discussion and Support through the same view shape on desktop
 * (`CommonChatTabController`/`CommonChatTabView`, `chatChannelDomain` is the only thing
 * that varies), and this mirrors that. The Support channel is also linked from the
 * existing More → Help → Support screen (`SupportScreen.kt`) — the two entry points
 * coexist and land on the same channel; see that reconciliation documented in
 * `CommunityHubScreenDesign.kt`.
 *
 * ======================================================================================
 * REUSE MAP — what production reuses as-is vs what needs new work
 * ======================================================================================
 * This PoC reuses REAL production components wherever they only need primitives, and
 * builds Simulated stand-ins only where a component's signature forces a domain type
 * (per this project's PoC convention — see feedback_design_poc_workflow.md).
 *
 * Reused directly, unchanged, in this file:
 *   - `TopBarContent`        (molecules/TopBar.kt)
 *   - `BisqSearchField`      (molecules/inputfield/SearchField.kt) — for search-in-channel
 *   - `ChatInputField`       (molecules/chat/ChatInputField.kt) — its `quotedMessage` param
 *      defaults to null, so it needs no BisqEasyOpenTradeMessageModel instance to preview.
 *
 * NOT reusable as-is in a domain-type-free PoC, but reused unchanged in PRODUCTION:
 *   - `ChatMessageList` / `ChatTextMessageBox` (organisms/chat, molecules/chat) — both
 *     require `List<BisqEasyOpenTradeMessageModel>`, a TRADE-scoped domain type. Public
 *     channel messages need their own adapter, analogous to the one the private-chat PoC
 *     already calls for (`TwoPartyPrivateChatMessage.toBisqEasyOpenTradeMessageModel()`
 *     in design/community/private_chat/PrivateChatScreenDesign.kt) — but NOT the same adapter, because a
 *     public channel message has no "trade" and no single "receiver". This is NEW
 *     mapping work, not a reuse of the DM adapter.
 *   - `ChatMessageContextMenu` — same domain-type constraint; reused unchanged in
 *     production once the adapter above exists. Its ignore/report menu items should now
 *     route to the Peer Profile screen's canonical handlers (see peer_profile/
 *     PeerProfileScreenDesign.kt) rather than trade-chat-local ones.
 *   - `ReactionInput` / `QuoteMessageBubble` (used inside ChatTextMessageBox) — unchanged.
 *
 * Because ChatMessageList/ChatTextMessageBox can't be previewed here without domain
 * types, this file provides `SimulatedChannelMessage` + `SimulatedChannelMessageBubble`
 * as a visual stand-in — deliberately styled to match ChatTextMessageBox's bubble shape,
 * colours (dark_grey40 inbound / primaryDisabled outbound) and corner radius, so the
 * production swap is a drop-in visual match, not a redesign.
 *
 * ======================================================================================
 * GAP: AVATAR / SENDER NAME IS NOT TAP-TARGETABLE TODAY
 * ======================================================================================
 * This is the one genuinely NEW requirement this screen introduces to the shared chat
 * components. Today, `ChatTextMessageBox` only wires `combinedClickable(onLongClick = ...)`
 * on the bubble to open `ChatMessageContextMenu` — there is no tap target on the sender's
 * name/avatar for navigating to their Peer Profile (#545). Production must add an
 * `onAvatarClick: (senderId: String) -> Unit` callback threaded through
 * `ChatMessageList` → `ChatTextMessageBox` → `UsernameMessageDeliveryAndDate`. This PoC's
 * `DiscussionsChannelUiAction.OnAvatarClick` models exactly that callback shape so the
 * production signature can be copied directly.
 *
 * Note this need already exists implicitly for TRADE chat too (tapping a trade partner's
 * name to see their Peer Profile is equally desirable there) — implementing it here
 * should extend `ChatMessageList`'s signature, not fork a public-channel-only variant.
 *
 * ======================================================================================
 * WHY MULTI-SENDER "JUST WORKS" (mostly)
 * ======================================================================================
 * Private DM's 1:1 assumption doesn't hold for a many-to-many public channel — messages
 * need each sender's name visible, not just an inbound/outbound binary. This is largely
 * already covered: `ChatTextMessageBox`'s existing `UsernameMessageDeliveryAndDate` row
 * already renders the sender's name above every bubble (see ChatTextMessageBox.kt L72-77),
 * because trade chat already supports a mediator as a third party. The gap is specifically
 * the tap-targetability called out above, not the name display itself.
 *
 * ======================================================================================
 * NO "LEAVE CHANNEL" ACTION (deliberate contrast with PrivateChatScreen)
 * ======================================================================================
 * `design/community/private_chat/PrivateChatScreenDesign.kt` has a `LeaveChatIconButton` in its TopBar,
 * because a DM is a persistent 1:1 relationship the user can deliberately exit. A public
 * channel is not "left" in the same destructive sense — membership is implicit, like
 * reading a public forum. Do NOT copy the leave-chat icon button into this screen during
 * implementation; it would be a copy-paste error, not a deliberate omission.
 *
 * ======================================================================================
 * SEARCH-IN-CHANNEL (#589 "reuses ... a search component")
 * ======================================================================================
 * Filters/highlights matching messages in the current channel (`isHighlighted` flag on
 * `SimulatedChannelMessage`, shown with a subtle primary-tinted background + border on
 * the matching bubble) via a `BisqSearchField` row. There is no other, directory-level
 * search anymore — with exactly one Discussion channel and one Support channel (see
 * "WHY THERE'S NO CHANNEL LIST" above), this is the ONLY search in the Discussions
 * surface. The toggle affordance's PLACEMENT depends on [DiscussionsChannelScreenContent]'s
 * `showTopBar` param, since it needs a home whether or not this screen owns its own
 * TopBar:
 *   - `showTopBar = true` (opened as its own pushed screen, e.g. via the Support
 *     reference or a future deep link): the search icon sits in the TopBar's
 *     `extraActions` slot, as before.
 *   - `showTopBar = false` (embedded as the Discussions tab body inside
 *     `CommunityHubScreenDesign.kt` — the shell owns the TopBar): the search icon moves
 *     inline into the member-count row, since there is no TopBar slot available in this
 *     context. Same `OnToggleSearch` action either way.
 *
 * ======================================================================================
 * i18n KEYS NEEDED
 * ======================================================================================
 * mobile.community.channel.search.toggle       → "Search messages"
 * mobile.community.channel.search.hint         → "Search in this channel..."
 * mobile.community.channel.search.matchCount   → "{0} matches"
 * mobile.community.channel.empty               → "No messages yet — be the first to say hello"
 * mobile.community.channel.memberCount         → "{0} members"
 *
 * ======================================================================================
 * TEXT EXPANSION
 * ======================================================================================
 * "{0} members" / "{0} matches" counters are short numeric strings — low risk even with
 * German/Russian expansion. Channel display names are operator-authored; no control here.
 */
package network.bisq.mobile.presentation.design.community

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.SearchIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.components.molecules.chat.ChatInputField
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// ============================================================================================
// Simulated data — no domain type dependencies
// ============================================================================================

internal data class SimulatedChannelMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timeLabel: String,
    val isMine: Boolean,
    val isHighlighted: Boolean = false,
)

// ============================================================================================
// UiState / UiAction
// ============================================================================================

internal data class DiscussionsChannelUiState(
    val channelName: String,
    val memberCount: Int,
    val messages: List<SimulatedChannelMessage> = emptyList(),
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val searchMatchCount: Int = 0,
    val isLoading: Boolean = false,
)

internal sealed interface DiscussionsChannelUiAction {
    data object OnToggleSearch : DiscussionsChannelUiAction

    data class OnSearchQueryChange(
        val query: String,
    ) : DiscussionsChannelUiAction

    data class OnAvatarClick(
        val senderId: String,
    ) : DiscussionsChannelUiAction

    data class OnSendMessage(
        val text: String,
    ) : DiscussionsChannelUiAction
}

// ============================================================================================
// Content
// ============================================================================================

/**
 * @param showTopBar `true` when this screen owns its own back-button TopBar (opened as a
 *   standalone pushed screen — e.g. the Support channel reference navigates here). `false`
 *   when embedded as the Discussions tab body inside `CommunityHubScreenDesign.kt`, which
 *   owns ONE shared TopBar for all tabs — see that file's "LAYOUT" section. The
 *   search-toggle icon relocates accordingly; see file KDoc "SEARCH-IN-CHANNEL".
 * @param modifier applied to the root `Column`, in addition to `fillMaxSize()`. Lets a
 *   caller such as `DiscussionsTabContent` constrain this composable to a `weight(1f)`
 *   slice of a taller `Column` (e.g. below the pinned Support reference row) instead of
 *   claiming the full available height unconditionally.
 */
@Composable
internal fun DiscussionsChannelScreenContent(
    uiState: DiscussionsChannelUiState,
    onAction: (DiscussionsChannelUiAction) -> Unit,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true,
) {
    Column(modifier = modifier.fillMaxSize().background(BisqTheme.colors.backgroundColor)) {
        if (showTopBar) {
            TopBarContent(
                title = uiState.channelName,
                showBackButton = true,
                showUserAvatar = false,
                extraActions = {
                    IconButton(onClick = { onAction(DiscussionsChannelUiAction.OnToggleSearch) }) {
                        SearchIcon()
                    }
                },
            )

            BisqText.SmallLight(
                text = "${uiState.memberCount} members",
                color = BisqTheme.colors.mid_grey20,
                modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingQuarter),
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingQuarter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BisqText.SmallLight(text = "${uiState.memberCount} members", color = BisqTheme.colors.mid_grey20)
                IconButton(onClick = { onAction(DiscussionsChannelUiAction.OnToggleSearch) }) {
                    SearchIcon()
                }
            }
        }

        if (uiState.isSearchActive) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = BisqUIConstants.ScreenPadding)) {
                BisqSearchField(
                    value = uiState.searchQuery,
                    onValueChange = { onAction(DiscussionsChannelUiAction.OnSearchQueryChange(it)) },
                    placeholder = "Search in this channel...",
                    modifier = Modifier.fillMaxWidth(),
                )
                if (uiState.searchQuery.isNotEmpty()) {
                    BisqText.SmallRegular(
                        text = "${uiState.searchMatchCount} matches",
                        color = BisqTheme.colors.mid_grey20,
                        modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPaddingQuarter),
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(BisqUIConstants.ScreenPadding)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        color = BisqTheme.colors.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                uiState.messages.isEmpty() -> {
                    BisqText.BaseLight(
                        text = "No messages yet — be the first to say hello",
                        color = BisqTheme.colors.mid_grey20,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                else -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                        modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                    ) {
                        uiState.messages.forEach { message ->
                            SimulatedChannelMessageBubble(
                                message = message,
                                onAvatarClick = { onAction(DiscussionsChannelUiAction.OnAvatarClick(message.senderId)) },
                            )
                        }
                    }
                }
            }
        }

        ChatInputField(
            onMessageSend = { text -> onAction(DiscussionsChannelUiAction.OnSendMessage(text)) },
            placeholder = "Write a message...",
        )
    }
}

// ---------------------------------------------------------------------------
// Simulated message bubble — visually matches ChatTextMessageBox for a drop-in swap
// ---------------------------------------------------------------------------

@Composable
private fun SimulatedChannelMessageBubble(
    message: SimulatedChannelMessage,
    onAvatarClick: () -> Unit,
) {
    val chatAlign = if (message.isMine) Alignment.End else Alignment.Start
    val bubbleColor = if (message.isMine) BisqTheme.colors.primaryDisabled else BisqTheme.colors.dark_grey40
    val highlightBorder =
        if (message.isHighlighted) {
            Modifier.border(1.dp, BisqTheme.colors.primary, RoundedCornerShape(BisqUIConstants.BorderRadius))
        } else {
            Modifier
        }

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = chatAlign) {
        if (!message.isMine) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter),
                modifier = Modifier.clickable(onClick = onAvatarClick),
            ) {
                Box(
                    modifier = Modifier.size(20.dp).background(BisqTheme.colors.dark_grey50, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    BisqText.XSmallMedium(text = message.senderName.first().toString(), color = BisqTheme.colors.mid_grey30)
                }
                BisqText.SmallRegular(text = message.senderName, color = BisqTheme.colors.light_grey10)
                BisqText.SmallRegular(text = message.timeLabel, color = BisqTheme.colors.mid_grey20)
            }
        } else {
            BisqText.SmallRegular(
                text = message.timeLabel,
                color = BisqTheme.colors.mid_grey20,
                modifier = Modifier.padding(end = BisqUIConstants.ScreenPaddingQuarter),
            )
        }

        Box(
            modifier =
                highlightBorder
                    .background(bubbleColor, shape = RoundedCornerShape(BisqUIConstants.BorderRadius))
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.BaseLight(text = message.text, color = BisqTheme.colors.white)
        }
    }
}

// ============================================================================================
// Preview fixtures
// ============================================================================================

/**
 * `internal` (not `private`) so `CommunityHubScreenDesign.kt`'s Discussions-tab previews
 * can reuse the exact same fixture instead of duplicating it — mirrors
 * `simulatedConversations()` in `private_chat/PrivateChatListScreenDesign.kt`.
 */
internal fun simulatedMessages() =
    listOf(
        SimulatedChannelMessage(
            id = "1",
            senderId = "peer-1",
            senderName = "SatoshiFan#1234",
            text = "Anyone had luck with SEPA transfers over 500 EUR lately?",
            timeLabel = "10:02",
            isMine = false,
        ),
        SimulatedChannelMessage(
            id = "2",
            senderId = "peer-2",
            senderName = "BitcoinBee#5678",
            text = "Yep, took about 20 minutes on my end, no issues.",
            timeLabel = "10:04",
            isMine = false,
        ),
        SimulatedChannelMessage(
            id = "3",
            senderId = "me",
            senderName = "You",
            text = "Good to know, thanks both!",
            timeLabel = "10:05",
            isMine = true,
        ),
    )

private fun simulatedSupportMessages() =
    listOf(
        SimulatedChannelMessage(
            id = "s1",
            senderId = "peer-3",
            senderName = "NewTrader#4321",
            text = "My trade has been stuck on \"waiting for confirmation\" for an hour, is that normal?",
            timeLabel = "09:41",
            isMine = false,
        ),
        SimulatedChannelMessage(
            id = "s2",
            senderId = "me",
            senderName = "You",
            text = "It can take a while depending on network fees — check the tx in a block explorer to be sure.",
            timeLabel = "09:44",
            isMine = true,
        ),
    )

// ============================================================================================
// Previews
// ============================================================================================

@ExcludeFromCoverage
@Preview(name = "Discussions channel — Populated, multi-sender, standalone (showTopBar = true)")
@Composable
private fun DiscussionsChannelScreen_PopulatedPreview() {
    BisqTheme.Preview {
        DiscussionsChannelScreenContent(
            uiState = DiscussionsChannelUiState(channelName = "Discussion", memberCount = 1284, messages = simulatedMessages()),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Discussions channel — Empty (no messages yet)")
@Composable
private fun DiscussionsChannelScreen_EmptyPreview() {
    BisqTheme.Preview {
        DiscussionsChannelScreenContent(
            uiState = DiscussionsChannelUiState(channelName = "Discussion", memberCount = 1284, messages = emptyList()),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Discussions channel — Loading")
@Composable
private fun DiscussionsChannelScreen_LoadingPreview() {
    BisqTheme.Preview {
        DiscussionsChannelScreenContent(
            uiState = DiscussionsChannelUiState(channelName = "Discussion", memberCount = 1284, isLoading = true),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Discussions channel — Search active, with match highlighted")
@Composable
private fun DiscussionsChannelScreen_SearchActiveWithMatchPreview() {
    BisqTheme.Preview {
        DiscussionsChannelScreenContent(
            uiState =
                DiscussionsChannelUiState(
                    channelName = "Discussion",
                    memberCount = 1284,
                    messages = simulatedMessages().mapIndexed { i, m -> if (i == 0) m.copy(isHighlighted = true) else m },
                    isSearchActive = true,
                    searchQuery = "SEPA",
                    searchMatchCount = 1,
                ),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Discussions channel — Search active, no matches")
@Composable
private fun DiscussionsChannelScreen_SearchActiveNoMatchPreview() {
    BisqTheme.Preview {
        DiscussionsChannelScreenContent(
            uiState =
                DiscussionsChannelUiState(
                    channelName = "Discussion",
                    memberCount = 1284,
                    messages = simulatedMessages(),
                    isSearchActive = true,
                    searchQuery = "xyz",
                    searchMatchCount = 0,
                ),
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Discussions channel — Long message wraps correctly")
@Composable
private fun DiscussionsChannelScreen_LongMessagePreview() {
    BisqTheme.Preview {
        DiscussionsChannelScreenContent(
            uiState =
                DiscussionsChannelUiState(
                    channelName = "Discussion",
                    memberCount = 1284,
                    messages =
                        listOf(
                            SimulatedChannelMessage(
                                id = "1",
                                senderId = "peer-1",
                                senderName = "SatoshiFan#1234",
                                text =
                                    "This is a much longer message to check that the bubble wraps properly across " +
                                        "multiple lines while keeping the sender name and timestamp readable above it.",
                                timeLabel = "10:02",
                                isMine = false,
                            ),
                        ),
                ),
            onAction = {},
        )
    }
}

/** Preview: isolated message bubble, demonstrating the avatar/name tap target for Peer Profile navigation. */
@ExcludeFromCoverage
@Preview(name = "Message bubble — avatar tap target isolated")
@Composable
private fun SimulatedChannelMessageBubble_TapTargetPreview() {
    BisqTheme.Preview {
        SimulatedChannelMessageBubble(message = simulatedMessages()[0], onAvatarClick = {})
    }
}

/**
 * Interactive preview: tapping the search icon reveals the in-channel search field,
 * and typing filters the visible match count. Demonstrates the search-in-channel flow
 * end to end — the only search in the Discussions surface now (see file KDoc "WHY
 * THERE'S NO CHANNEL LIST").
 */
@ExcludeFromCoverage
@Preview(name = "Discussions channel — Interactive search toggle, standalone")
@Composable
private fun DiscussionsChannelScreen_InteractiveSearchPreview() {
    var searchActive by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    BisqTheme.Preview {
        DiscussionsChannelScreenContent(
            uiState =
                DiscussionsChannelUiState(
                    channelName = "Discussion",
                    memberCount = 1284,
                    messages = simulatedMessages(),
                    isSearchActive = searchActive,
                    searchQuery = query,
                    searchMatchCount = simulatedMessages().count { it.text.contains(query, ignoreCase = true) && query.isNotEmpty() },
                ),
            onAction = { action ->
                when (action) {
                    DiscussionsChannelUiAction.OnToggleSearch -> searchActive = !searchActive
                    is DiscussionsChannelUiAction.OnSearchQueryChange -> query = action.query
                    else -> {}
                }
            },
        )
    }
}

/**
 * Preview: `showTopBar = false` — how this screen actually renders embedded as the
 * Discussions tab body inside `CommunityHubScreenDesign.kt` (the shell owns the TopBar,
 * so there's no back button here, and the search toggle moves inline into the
 * member-count row). This is the REALISTIC milestone-11 shape; see that file's
 * `CommunityHubScreen_Milestone11_*` previews for it in full hub context including the
 * pinned Support reference row above it.
 */
@ExcludeFromCoverage
@Preview(name = "Discussions channel — Embedded in hub (showTopBar = false)")
@Composable
private fun DiscussionsChannelScreen_EmbeddedPreview() {
    BisqTheme.Preview {
        DiscussionsChannelScreenContent(
            uiState = DiscussionsChannelUiState(channelName = "Discussion", memberCount = 1284, messages = simulatedMessages()),
            onAction = {},
            showTopBar = false,
        )
    }
}

/**
 * Preview: this same composable reused, unchanged, for the SUPPORT channel — proves the
 * "REUSED FOR THE SUPPORT CHANNEL" claim in the file KDoc. `showTopBar = true` because
 * this is how it's reached: pushed as its own screen from the hub's pinned "Need help?"
 * reference, not embedded in a tab.
 */
@ExcludeFromCoverage
@Preview(name = "Discussions channel — reused for Support channel (standalone)")
@Composable
private fun DiscussionsChannelScreen_SupportChannelReusePreview() {
    BisqTheme.Preview {
        DiscussionsChannelScreenContent(
            uiState = DiscussionsChannelUiState(channelName = "Support", memberCount = 301, messages = simulatedSupportMessages()),
            onAction = {},
            showTopBar = true,
        )
    }
}
