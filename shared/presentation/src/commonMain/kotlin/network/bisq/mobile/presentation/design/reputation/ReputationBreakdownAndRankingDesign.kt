/**
 * ReputationBreakdownAndRankingDesign.kt
 *
 * Compose specification for a per-source reputation score breakdown and a network-wide ranking
 * list. Not wired to a presenter; state shown in the previews below is produced entirely by the
 * `simulatedXxx` helpers, which take only primitives.
 *
 * ------------------------------------------------------------------------------------
 * 1. PURPOSE
 * ------------------------------------------------------------------------------------
 * `shared/presentation/.../settings/reputation/ReputationScreen.kt` shows only the aggregate score
 * and a burn/bond how-to; it does not show which of the five sources produced that score, or where
 * a profile stands against every other profile on the network. This specification adds: a "My
 * score" block (rank, total, stars, per-source contribution) on the existing reputation screen; a
 * ranking screen listing every known profile, searchable and filterable by source; and the same
 * breakdown block on the peer profile screen.
 *
 * ------------------------------------------------------------------------------------
 * 2. DATA AND PREREQUISITE
 * ------------------------------------------------------------------------------------
 * Scope is per-source TOTALS only — the five `ReputationSource` values
 * (`BURNED_BSQ`, `BSQ_BOND`, `BISQ1_ACCOUNT_AGE`, `BISQ1_SIGNED_ACCOUNT_AGE_WITNESS`,
 * `PROFILE_AGE`) plus the total score, five-star score, and rank — not the underlying per-event
 * ledger (individual burn transactions, individual bonds, their lock times). Rank, total, and
 * stars already exist on both apps: `ReputationServiceFacade.getReputation(userProfileId)` returns
 * `ReputationScoreVO(totalScore, fiveSystemScore, ranking)`, and
 * `ReputationServiceFacade.scoreByUserProfileId: StateFlow<Map<String, Long>>` already carries
 * every known profile's total. Per-source totals do not exist on either app today; the node can
 * read them by adding calls into bisq2's `ReputationService`'s five per-source services
 * (`getProofOfBurnService`, `getBondedReputationService`, `getAccountAgeService`,
 * `getSignedWitnessService`, `getProfileAgeService` — each already sums to one number per profile,
 * the same way the node's own ranking view does), but the bisq2 REST `ReputationScoreDto` carries
 * only `totalScore`/`fiveSystemScore`/`ranking`, so Connect needs that DTO extended before this
 * ships there. The prerequisite is: extend bisq2's `ReputationScoreDto`
 * (`bisq2/api/src/main/java/bisq/api/dto/user/reputation/ReputationScoreDto.java`) with
 * `scoreBySource: Map<String, Long>` keyed by `ReputationSource.name()`, populated from the five
 * per-source services above, and advertise it via a matching `ApiFeature` entry that
 * `Feature.REPUTATION_SOURCE_BREAKDOWN` maps to (see "3. Gating"). No new endpoint: the existing
 * `/reputation/score/{userProfileId}` REST call and the reputation WebSocket subscription both
 * carry the extended DTO. An old node simply omits `scoreBySource`; the client never reads that
 * field unless the capability is advertised, so its absence is never misread as a genuine
 * all-zero breakdown. The per-event ledger (date, amount, lock time per individual
 * burn/bond/witness/age record) would need bisq2 to expose the underlying dated records
 * themselves, not just five summed numbers — a separate, larger addition, out of scope here.
 *
 * ------------------------------------------------------------------------------------
 * 3. GATING
 * ------------------------------------------------------------------------------------
 * Everything that depends on per-source totals is hidden unless the connected backend advertises
 * support, using the existing probe-based, fail-closed mechanism
 * (`shared/domain/.../domain/service/capabilities/BackendCapabilitiesService.kt`,
 * `BackendCapabilities.isSupported(Feature.X)`) — no version checks. A new key,
 * `Feature.REPUTATION_SOURCE_BREAKDOWN`, is added matching bisq2's `ApiFeature` for the DTO
 * extension in "2. Data and prerequisite." [ReputationBreakdownUiState.isSourceBreakdownSupported]
 * and [ReputationRankingUiState.isSourceBreakdownSupported] stand in for
 * `backendCapabilitiesService.capabilities.value.isSupported(Feature.REPUTATION_SOURCE_BREAKDOWN)`,
 * mirroring `MyTradesPresenter.showHistoryTab`'s `.map { it.isSupported(Feature.X) }` pattern and
 * `PublicChatUiState.isSupported`'s plain boolean field.
 *
 * Rank, total, and stars render unconditionally — they use the existing API, not the new one.
 * Only the per-source rows in [ReputationBreakdown] and the source filter entry in the ranking
 * screen are conditioned on support; when unsupported, the ranking still works on totals and the
 * filter control is hidden entirely, not shown disabled — a control a user can tap and get nothing
 * from is worse than a control that is not there, since the latter needs no explanation.
 *
 * ------------------------------------------------------------------------------------
 * 4. LIST AND ROW RULES
 * ------------------------------------------------------------------------------------
 * [ReputationRankingScreenContent] uses a plain `LazyColumn` over the full in-memory profile list —
 * no cap, no paging — because the underlying data
 * (`UserProfileServiceFacade.userProfiles` + `ReputationServiceFacade.scoreByUserProfileId`) is
 * already a fully-synced in-memory collection on both apps; `LazyColumn` only composes the rows
 * currently on screen regardless of list length, so list size is a search-debounce concern, not a
 * memory or composition one. Search text is debounced the same way market search already is
 * elsewhere in this app, rather than re-filtering the whole list on every keystroke.
 *
 * Each row shows exactly ONE number: the total score by default, or the selected source's value
 * while a source filter is active — never both at once. A row is already tight on a phone width
 * (avatar, name, one number, stars); Desktop can show a score column and a value column side by
 * side because it has a whole window to spend, mobile does not. A small caption under the number
 * names which value is showing ("Total score" or the source's own display name reused from
 * `reputation.source.*`), so switching the filter never leaves the number ambiguous about what it
 * now means.
 *
 * Sort order: without a source filter, rows sort by total score descending, ties broken by
 * display name. With a source filter active, rows sort by the selected source's value
 * descending, ties broken by total score descending then by name; a profile with no value at all
 * for the selected source sorts last — the row always sorts by the same number it is currently
 * displaying, so the visible order and the visible number never contradict each other.
 *
 * ------------------------------------------------------------------------------------
 * 5. SOURCE FILTER
 * ------------------------------------------------------------------------------------
 * [ReputationSourceFilterSheet] reuses the existing bottom-sheet single-select pattern
 * (`BisqSelect`, the same molecule `MarketFilters`' own sheet already uses) rather than a
 * segmented row of six labels — several source names are two words
 * (`reputation.source.BISQ1_SIGNED_ACCOUNT_AGE_WITNESS` = "Signed witness"), and six segments do
 * not fit a phone width the way `MarketFilter`'s two or three already barely do. The six entries
 * are "All" (reusing the existing generic `mobile.components.marketFilter.showMarkets.all` string)
 * plus the five `reputation.source.*` labels, already translated.
 *
 * ------------------------------------------------------------------------------------
 * 6. OWN POSITION
 * ------------------------------------------------------------------------------------
 * The user's own rank is shown in the "My score" block via the already-available
 * `ReputationScoreVO.ranking` — no ranking-screen mechanism is needed just to answer "where do I
 * stand," that number is already on the reputation screen before the ranking screen is ever
 * opened. The ranking screen additionally offers a "jump to me" action that scrolls the list to
 * the row matching the local profile id, rather than pinning that row at the top out of its sorted
 * position — pinning duplicates the row (it would appear twice: once at the top, once in its
 * natural sorted place) for a question the My-score block already answers; a scroll action costs
 * nothing extra once the row's index in the current sort/filter is known, which it is, since the
 * row is already being rendered somewhere in the same list.
 *
 * ------------------------------------------------------------------------------------
 * 7. ZERO VS. UNKNOWN
 * ------------------------------------------------------------------------------------
 * [ReputationDisplayStatus] has three values: `UNKNOWN` (the profile's score has not synced yet —
 * this is `PeerProfileScreen`'s existing `isReputationUnknown` condition, kept as-is, with its
 * existing `mobile.peerProfile.reputationUnavailable` copy and no star row at all, since an empty
 * star row would misread as a confirmed zero), `ZERO` (the score IS known and it is confirmed
 * zero), and `SCORED` (a positive total). `ZERO` gets its own copy
 * (`mobile.reputation.zeroReputation`) distinct from the unknown-state copy, and DOES render an
 * empty star row plus the rank — an empty row is the accurate representation of a confirmed zero,
 * which is exactly the misreading the `UNKNOWN` branch avoids by showing no stars at all. In the
 * ranking list itself, a zero-score row needs no special copy: it is listed last, in sorted order,
 * with a plain "0" — a list of numbers does not need the same disambiguation a single profile's
 * summary block does, and every row in that list already has a known, synced score by
 * construction (a profile with no data at all would not appear in the ranking in the first place).
 *
 * ------------------------------------------------------------------------------------
 * 8. ACCESSIBILITY
 * ------------------------------------------------------------------------------------
 * - Row names use `singleLine = true` with ellipsis rather than wrapping, so a long localized
 *   display name does not push row height around in a list this long — see
 *   [ReputationRankingScreenContent_LongLocaleText_Preview].
 * - The source filter's `BisqSelect` already provides standard list-item touch targets and
 *   keyboard/search navigation; no custom control is built for it.
 * - The zero-state and unknown-state copy are real, distinct text, not an icon-only distinction —
 *   a screen reader announces which of the two it is rather than only "empty stars."
 * - `Modifier.testTag(...)` marks each ranking row, the search field, the filter entry, the
 *   "jump to me" action, and the breakdown block's per-source rows.
 *
 * ------------------------------------------------------------------------------------
 * 9. PROPOSED I18N KEYS
 * ------------------------------------------------------------------------------------
 * English base values only, per repo convention — nothing is added to `mobile.properties` by this
 * file; production implementation adds these.
 *
 * New:
 *   mobile.reputation.myScore.headline = "My score"
 *   mobile.reputation.myScore.rank = "Rank #{0}"
 *   mobile.reputation.viewFullRanking = "View full ranking"
 *   mobile.reputation.sourceBreakdown.unsupportedNotice = "Per-source details aren't available on
 *     this connection yet."
 *   mobile.reputation.zeroReputation = "This profile hasn't built any reputation yet."
 *   mobile.reputation.ranking.searchEmptyState = "No profiles match your search."
 *   mobile.reputation.ranking.jumpToMe = "Jump to me"
 *   mobile.reputation.ranking.valueCaption.total = "Total score"
 *
 * Reused, already present (all 14 locales) in `shared/domain/.../GeneratedResourceBundles_*.kt`:
 *   reputation.source.BURNED_BSQ / .BSQ_BOND / .BISQ1_ACCOUNT_AGE /
 *     .BISQ1_SIGNED_ACCOUNT_AGE_WITNESS / .PROFILE_AGE (source display names and value captions)
 *   reputation.table.headline = "Reputation ranking" (ranking screen title)
 *   reputation.table.entriesUnit = "User profiles"
 *   reputation.table.columns.userProfile / .reputationScore / .reputation
 *   reputation.reputationScore.headline / .intro / .sellerReputation / .explanation.intro /
 *     .explanation.score.title / .explanation.score.description / .explanation.ranking.title /
 *     .explanation.ranking.description / .explanation.stars.title / .explanation.stars
 *     .description / .closing — Desktop's explanatory Score-tab text, folded into the existing
 *     how-to content on the reputation screen as plain sections, not a tab.
 *   mobile.components.marketFilter.showMarkets.all = "All" (source filter's "All" option)
 *   mobile.peerProfile.reputationUnavailable / mobile.peerProfile.reputation (existing
 *     `PeerProfileScreen` unknown-state and scored-state copy, reused by [ReputationBreakdown])
 *
 * ------------------------------------------------------------------------------------
 * 10. TESTS TO ADD
 * ------------------------------------------------------------------------------------
 * - Presenter test: the per-source rows and the filter entry are absent when
 *   `isSupported(Feature.REPUTATION_SOURCE_BREAKDOWN)` is false, and rank/total/stars still render
 *   from the existing API; they appear the moment the capability flips to true, with no version
 *   comparison involved.
 * - Presenter test for the ranking list: selecting a source changes every row's displayed number
 *   and caption without changing the row order truth (still sorted by the field the row displays);
 *   clearing the filter restores totals; the list is unaffected by profile count beyond normal
 *   `LazyColumn` composition (no artificial truncation at any size).
 * - Presenter test for own position: "jump to me" resolves to the correct row index for the
 *   current sort/filter/search combination, including when the filter changes after the action was
 *   last used.
 * - UI test for the breakdown block: `UNKNOWN` renders no stars and the unavailable copy; `ZERO`
 *   renders empty stars, the rank, and the zero copy; `SCORED` renders stars, total, rank, and
 *   (when supported) the five source rows.
 * - Presenter test for sort order: unfiltered rows sort by total score descending (ties by name);
 *   selecting a source re-sorts by that source's value descending, ties broken by total score
 *   descending then name, with no-value-for-that-source rows sorting last; clearing the filter
 *   restores the unfiltered order.
 *
 * ------------------------------------------------------------------------------------
 * 11. IMPLEMENTATION NOTES FOR THE DEVELOPER
 * ------------------------------------------------------------------------------------
 * - Add `REPUTATION_SOURCE_BREAKDOWN("reputation-source-breakdown")` to
 *   `shared/domain/.../domain/service/capabilities/Feature.kt`, matching the bisq2 `ApiFeature`
 *   key added alongside the `ReputationScoreDto` extension in "2. Data and prerequisite."
 * - `ReputationServiceFacade` needs a `getReputationSourceBreakdown(userProfileId):
 *   Result<Map<ReputationSource, Long>>` (or equivalent) call, gated the same way other
 *   capability-dependent calls already are; the node implementation reads the five per-source
 *   bisq2 services directly, the client implementation calls the extended REST endpoint.
 * - `NavRoute` needs a new `ReputationRanking` entry (plain, no parameters) reached from the
 *   existing `Reputation` screen's new "View full ranking" action.
 * - `ReputationScreen.kt`: add the "My score" block ([ReputationBreakdown] fed from the local
 *   profile's id) above the existing intro/formula content, and the "View full ranking" entry
 *   below it; fold `reputation.reputationScore.*` into the existing how-to `BisqCard`s as
 *   additional plain sections.
 * - `PeerProfileScreen.kt`: render [ReputationBreakdown] in place of the existing inline
 *   stars/text block (see the file's own `isReputationUnknown` branch), passing through the same
 *   `ZERO`/`UNKNOWN`/`SCORED` distinction this design adds.
 */
package network.bisq.mobile.presentation.design.reputation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSelect
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.StarRating
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// -------------------------------------------------------------------------------------
// State and actions (production shape: see "11. Implementation notes for the developer")
// -------------------------------------------------------------------------------------

internal enum class ReputationSource {
    BURNED_BSQ,
    BSQ_BOND,
    BISQ1_ACCOUNT_AGE,
    BISQ1_SIGNED_ACCOUNT_AGE_WITNESS,
    PROFILE_AGE,
}

private fun ReputationSource.displayName(): String =
    when (this) {
        ReputationSource.BURNED_BSQ -> "Burned BSQ"
        ReputationSource.BSQ_BOND -> "Bonded BSQ"
        ReputationSource.BISQ1_ACCOUNT_AGE -> "Account age"
        ReputationSource.BISQ1_SIGNED_ACCOUNT_AGE_WITNESS -> "Signed witness"
        ReputationSource.PROFILE_AGE -> "Profile age"
    }

/** See "7. Zero vs. unknown." */
internal enum class ReputationDisplayStatus { UNKNOWN, ZERO, SCORED }

internal data class ReputationSourceValue(
    val source: ReputationSource,
    val score: Long,
)

/**
 * Stands in for the slice of presenter state the reusable breakdown block reads — shared by the
 * reputation screen's "My score" block and `PeerProfileScreen`.
 *
 * @param isSourceBreakdownSupported See "3. Gating." When false, [sourceValues] is ignored and no
 * per-source rows render, regardless of its contents.
 */
internal data class ReputationBreakdownUiState(
    val status: ReputationDisplayStatus,
    val totalScore: Long,
    val starRating: Double,
    val rank: Int,
    val isSourceBreakdownSupported: Boolean,
    val sourceValues: List<ReputationSourceValue> = emptyList(),
)

internal data class RankingRow(
    val userProfileId: String,
    val displayName: String,
    val isOwnProfile: Boolean,
    val value: Long,
    val valueCaption: String,
    val starRating: Double,
)

internal data class ReputationRankingUiState(
    val searchText: String = "",
    val selectedSource: ReputationSource? = null,
    val isSourceBreakdownSupported: Boolean = true,
    val rows: List<RankingRow> = emptyList(),
)

internal sealed interface ReputationRankingUiAction {
    data class OnSearchTextChanged(
        val text: String,
    ) : ReputationRankingUiAction

    data object OnOpenSourceFilter : ReputationRankingUiAction

    data class OnSourceFilterChanged(
        val source: ReputationSource?,
    ) : ReputationRankingUiAction

    data class OnRowClicked(
        val userProfileId: String,
    ) : ReputationRankingUiAction

    data object OnJumpToMe : ReputationRankingUiAction
}

/** Builds a [ReputationBreakdownUiState] from primitives with realistic defaults. */
internal fun simulatedReputationBreakdownUiState(
    status: ReputationDisplayStatus = ReputationDisplayStatus.SCORED,
    totalScore: Long = 124_00,
    starRating: Double = 3.5,
    rank: Int = 42,
    isSourceBreakdownSupported: Boolean = true,
    sourceValues: List<ReputationSourceValue> =
        listOf(
            ReputationSourceValue(ReputationSource.BURNED_BSQ, 8_000),
            ReputationSourceValue(ReputationSource.BSQ_BOND, 4_000),
            ReputationSourceValue(ReputationSource.BISQ1_ACCOUNT_AGE, 200),
            ReputationSourceValue(ReputationSource.BISQ1_SIGNED_ACCOUNT_AGE_WITNESS, 0),
            ReputationSourceValue(ReputationSource.PROFILE_AGE, 0),
        ),
): ReputationBreakdownUiState =
    ReputationBreakdownUiState(
        status = status,
        totalScore = totalScore,
        starRating = starRating,
        rank = rank,
        isSourceBreakdownSupported = isSourceBreakdownSupported,
        sourceValues = sourceValues,
    )

/** Builds a [RankingRow] from primitives with realistic defaults. */
internal fun simulatedRankingRow(
    userProfileId: String = "profile-1",
    displayName: String = "HonestTrader42",
    isOwnProfile: Boolean = false,
    value: Long = 12_400,
    valueCaption: String = "Total score",
    starRating: Double = 3.5,
): RankingRow =
    RankingRow(
        userProfileId = userProfileId,
        displayName = displayName,
        isOwnProfile = isOwnProfile,
        value = value,
        valueCaption = valueCaption,
        starRating = starRating,
    )

private val DEFAULT_RANKING_ROWS =
    listOf(
        simulatedRankingRow(userProfileId = "profile-1", displayName = "HonestTrader42", value = 24_800, starRating = 4.5),
        simulatedRankingRow(userProfileId = "profile-2", displayName = "SatoshiFan", isOwnProfile = true, value = 12_400, starRating = 3.5),
        simulatedRankingRow(userProfileId = "profile-3", displayName = "NewToBisq", value = 0, starRating = 0.0),
    )

/** Builds a [ReputationRankingUiState] from primitives with realistic defaults. */
internal fun simulatedReputationRankingUiState(
    searchText: String = "",
    selectedSource: ReputationSource? = null,
    isSourceBreakdownSupported: Boolean = true,
    rows: List<RankingRow> = DEFAULT_RANKING_ROWS,
): ReputationRankingUiState =
    ReputationRankingUiState(
        searchText = searchText,
        selectedSource = selectedSource,
        isSourceBreakdownSupported = isSourceBreakdownSupported,
        rows = rows,
    )

// -------------------------------------------------------------------------------------
// Breakdown block — reused by "My score" and the peer profile screen, see "7. Zero vs.
// unknown"
// -------------------------------------------------------------------------------------

@Composable
internal fun ReputationBreakdown(uiState: ReputationBreakdownUiState) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("reputation_breakdown"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (uiState.status) {
            ReputationDisplayStatus.UNKNOWN -> {
                BisqText.BaseLightGrey(
                    text = "Reputation not available yet",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("reputation_breakdown_unknown"),
                )
            }

            ReputationDisplayStatus.ZERO -> {
                StarRating(rating = 0.0)
                BisqGap.VHalf()
                BisqText.BaseLightGrey(
                    text = "This profile hasn't built any reputation yet.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("reputation_breakdown_zero"),
                )
                BisqGap.VQuarter()
                BisqText.SmallLight(
                    text = "Rank #${uiState.rank}",
                    color = BisqTheme.colors.mid_grey20,
                )
            }

            ReputationDisplayStatus.SCORED -> {
                StarRating(rating = uiState.starRating)
                BisqGap.VHalf()
                BisqText.BaseLightGrey(
                    text = "Reputation ${uiState.totalScore}",
                    textAlign = TextAlign.Center,
                )
                BisqGap.VQuarter()
                BisqText.SmallLight(
                    text = "Rank #${uiState.rank}",
                    color = BisqTheme.colors.mid_grey20,
                )

                if (uiState.isSourceBreakdownSupported) {
                    BisqGap.V1()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        uiState.sourceValues.forEach { sourceValue ->
                            ReputationSourceRow(sourceValue)
                        }
                    }
                } else {
                    BisqGap.VHalf()
                    BisqText.SmallLight(
                        text = "Per-source details aren't available on this connection yet.",
                        color = BisqTheme.colors.mid_grey20,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("reputation_breakdown_unsupported_notice"),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReputationSourceRow(sourceValue: ReputationSourceValue) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = BisqUIConstants.ScreenPaddingQuarter)
                .testTag("reputation_source_row_${sourceValue.source.name}"),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BisqText.SmallLight(sourceValue.source.displayName(), color = BisqTheme.colors.light_grey10)
        BisqText.SmallMedium(sourceValue.score.toString(), color = BisqTheme.colors.white)
    }
}

// -------------------------------------------------------------------------------------
// Ranking screen — see "4. List and row rules" and "5. Source filter"
// -------------------------------------------------------------------------------------

@Composable
private fun SimulatedAvatarCircle(
    seed: String,
    size: androidx.compose.ui.unit.Dp = 40.dp,
) {
    val hue = (seed.hashCode() and 0xFF) / 255f
    Box(
        modifier =
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color.hsv(hue * 360f, 0.4f, 0.6f)),
    )
}

@Composable
internal fun ReputationRankingRow(
    row: RankingRow,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(
                    if (row.isOwnProfile) {
                        Modifier.background(BisqTheme.colors.primary.copy(alpha = 0.08f))
                    } else {
                        Modifier
                    },
                ).clickable(onClick = onClick)
                .padding(BisqUIConstants.ScreenPadding)
                .testTag("reputation_ranking_row_${row.userProfileId}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            SimulatedAvatarCircle(seed = row.userProfileId)
            BisqGap.HHalf()
            BisqText.BaseLight(text = row.displayName, singleLine = true)
        }
        Column(horizontalAlignment = Alignment.End) {
            BisqText.BaseLight(text = row.value.toString(), color = BisqTheme.colors.primary)
            BisqText.SmallLight(text = row.valueCaption, color = BisqTheme.colors.mid_grey20)
        }
        BisqGap.H1()
        StarRating(rating = row.starRating)
    }
}

@Composable
internal fun ReputationSourceFilterSheet(
    selectedSource: ReputationSource?,
    onSourceChange: (ReputationSource?) -> Unit,
) {
    BisqBottomSheet(onDismissRequest = {}) {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding2X)) {
            BisqSelect(
                label = "Filter by source",
                options = listOf<ReputationSource?>(null) + ReputationSource.entries,
                optionKey = { it?.name ?: "ALL" },
                optionLabel = { it?.displayName() ?: "All" },
                selectedKey = selectedSource?.name ?: "ALL",
                onSelect = { onSourceChange(it) },
            )
        }
    }
}

@Composable
internal fun ReputationRankingScreenContent(
    uiState: ReputationRankingUiState,
    onAction: (ReputationRankingUiAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(BisqUIConstants.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            BisqTextFieldV0(
                label = "Search",
                value = uiState.searchText,
                onValueChange = { onAction(ReputationRankingUiAction.OnSearchTextChanged(it)) },
                modifier = Modifier.weight(1f).testTag("reputation_ranking_search"),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = BisqUIConstants.ScreenPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (uiState.isSourceBreakdownSupported) {
                BisqButton(
                    text = uiState.selectedSource?.displayName() ?: "All sources",
                    type = BisqButtonType.Outline,
                    onClick = { onAction(ReputationRankingUiAction.OnOpenSourceFilter) },
                    modifier = Modifier.testTag("reputation_ranking_filter_entry"),
                )
            }
            BisqButton(
                text = "Jump to me",
                type = BisqButtonType.Underline,
                onClick = { onAction(ReputationRankingUiAction.OnJumpToMe) },
                modifier = Modifier.testTag("reputation_ranking_jump_to_me"),
            )
        }
        BisqGap.V1()
        BisqHDivider()

        if (uiState.rows.isEmpty()) {
            BisqGap.V2()
            BisqText.BaseLightGrey(
                text = "No profiles match your search.",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(BisqUIConstants.ScreenPadding).testTag("reputation_ranking_search_empty"),
            )
        } else {
            LazyColumn {
                items(uiState.rows, key = { it.userProfileId }) { row ->
                    ReputationRankingRow(
                        row = row,
                        onClick = { onAction(ReputationRankingUiAction.OnRowClicked(row.userProfileId)) },
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Previews
// -------------------------------------------------------------------------------------

/** My-score block — per-source breakdown supported. */
@ExcludeFromCoverage
@Preview(name = "1. My score — supported")
@Composable
private fun ReputationBreakdown_Supported_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            ReputationBreakdown(uiState = simulatedReputationBreakdownUiState())
        }
    }
}

/** My-score block — per-source breakdown not supported; rank/total/stars still render. */
@ExcludeFromCoverage
@Preview(name = "2. My score — unsupported")
@Composable
private fun ReputationBreakdown_Unsupported_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            ReputationBreakdown(uiState = simulatedReputationBreakdownUiState(isSourceBreakdownSupported = false))
        }
    }
}

/** Ranking — default, total score shown for every row. */
@ExcludeFromCoverage
@Preview(name = "3. Ranking — default (total)", heightDp = 700)
@Composable
private fun ReputationRankingScreenContent_Default_Preview() {
    BisqTheme.Preview {
        ReputationRankingScreenContent(
            uiState = simulatedReputationRankingUiState(),
            onAction = {},
        )
    }
}

/** Ranking — filtered by a source, each row shows that source's value and caption. */
@ExcludeFromCoverage
@Preview(name = "4. Ranking — filtered by a source", heightDp = 700)
@Composable
private fun ReputationRankingScreenContent_FilteredBySource_Preview() {
    BisqTheme.Preview {
        ReputationRankingScreenContent(
            uiState =
                simulatedReputationRankingUiState(
                    selectedSource = ReputationSource.BURNED_BSQ,
                    rows =
                        listOf(
                            simulatedRankingRow(userProfileId = "profile-1", displayName = "HonestTrader42", value = 8_000, valueCaption = "Burned BSQ"),
                            simulatedRankingRow(
                                userProfileId = "profile-2",
                                displayName = "SatoshiFan",
                                isOwnProfile = true,
                                value = 3_200,
                                valueCaption = "Burned BSQ",
                            ),
                        ),
                ),
            onAction = {},
        )
    }
}

/** Ranking — per-source breakdown not supported: the filter entry is absent, not disabled. */
@ExcludeFromCoverage
@Preview(name = "5. Ranking — unsupported (filter hidden)", heightDp = 700)
@Composable
private fun ReputationRankingScreenContent_Unsupported_Preview() {
    BisqTheme.Preview {
        ReputationRankingScreenContent(
            uiState = simulatedReputationRankingUiState(isSourceBreakdownSupported = false),
            onAction = {},
        )
    }
}

/** Ranking — search with no matches. */
@ExcludeFromCoverage
@Preview(name = "6. Ranking — search empty state")
@Composable
private fun ReputationRankingScreenContent_SearchEmpty_Preview() {
    BisqTheme.Preview {
        ReputationRankingScreenContent(
            uiState = simulatedReputationRankingUiState(searchText = "zzz-no-match", rows = emptyList()),
            onAction = {},
        )
    }
}

/** Ranking — a zero-reputation row sorts last with a plain "0", no special copy. */
@ExcludeFromCoverage
@Preview(name = "7. Ranking — zero-reputation row")
@Composable
private fun ReputationRankingScreenContent_ZeroRow_Preview() {
    BisqTheme.Preview {
        ReputationRankingScreenContent(
            uiState = simulatedReputationRankingUiState(),
            onAction = {},
        )
    }
}

/** Ranking — long localized display name truncates on one line rather than wrapping. */
@ExcludeFromCoverage
@Preview(name = "8. Ranking — long-locale row text")
@Composable
private fun ReputationRankingScreenContent_LongLocaleText_Preview() {
    BisqTheme.Preview {
        ReputationRankingScreenContent(
            uiState =
                simulatedReputationRankingUiState(
                    rows =
                        listOf(
                            simulatedRankingRow(
                                userProfileId = "profile-1",
                                displayName = "AVeryLongSimulatedDisplayNameThatShouldTruncateGracefully",
                                value = 24_800,
                            ),
                        ),
                ),
            onAction = {},
        )
    }
}

/** Peer profile — breakdown block with a scored profile. */
@ExcludeFromCoverage
@Preview(name = "9. Peer profile — breakdown (scored)")
@Composable
private fun ReputationBreakdown_PeerProfileScored_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding), horizontalAlignment = Alignment.CenterHorizontally) {
            SimulatedAvatarCircle(seed = "peer", size = 72.dp)
            BisqGap.V1()
            BisqText.H5Regular("HonestTrader42")
            BisqGap.VHalf()
            ReputationBreakdown(uiState = simulatedReputationBreakdownUiState())
        }
    }
}

/** Peer profile — genuine zero vs. not-yet-synced, side by side for comparison. */
@ExcludeFromCoverage
@Preview(name = "10. Peer profile — zero vs. not-synced")
@Composable
private fun ReputationBreakdown_ZeroVsUnknown_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            BisqText.SmallLight("Not yet synced:", color = BisqTheme.colors.mid_grey20)
            BisqGap.VHalf()
            ReputationBreakdown(uiState = simulatedReputationBreakdownUiState(status = ReputationDisplayStatus.UNKNOWN))
            BisqGap.V2()
            BisqText.SmallLight("Confirmed zero:", color = BisqTheme.colors.mid_grey20)
            BisqGap.VHalf()
            ReputationBreakdown(uiState = simulatedReputationBreakdownUiState(status = ReputationDisplayStatus.ZERO, totalScore = 0))
        }
    }
}

/** Source filter sheet — six entries: all plus the five sources. */
@ExcludeFromCoverage
@Preview(name = "11. Source filter sheet")
@Composable
private fun ReputationSourceFilterSheet_Preview() {
    BisqTheme.Preview {
        ReputationSourceFilterSheet(selectedSource = ReputationSource.BSQ_BOND, onSourceChange = {})
    }
}
