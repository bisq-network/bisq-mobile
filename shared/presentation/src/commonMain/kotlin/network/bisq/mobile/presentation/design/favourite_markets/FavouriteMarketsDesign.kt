/**
 * FavouriteMarketsDesign.kt
 *
 * Compose specification for favourite markets in the Bisq Easy market list (the Offers tab market
 * picker). Not wired to a presenter; state shown in the previews below is produced entirely by the
 * `simulatedXxx` helpers, which take only primitives.
 *
 * ------------------------------------------------------------------------------------
 * 1. PURPOSE
 * ------------------------------------------------------------------------------------
 * `shared/presentation/.../tabs/offers/OfferbookMarketScreen.kt` lists every market with a search
 * field, a sort/filter sheet, and one flat `LazyColumn`. A trader who regularly checks the same
 * few markets has no way to shortcut past the full list. This specification adds a star toggle
 * per market row, a pinned block of favourited markets above the main list, and an "Only
 * favourites" filter — mirroring `bisq2 apps/desktop/desktop/.../bisq_easy/offerbook/
 * BisqEasyOfferbookView.java`'s separate favourites table.
 *
 * ------------------------------------------------------------------------------------
 * 2. DATA AND PERSISTENCE PER APP
 * ------------------------------------------------------------------------------------
 * Favourites persist node-side, in bisq2 settings, for both apps — not in the mobile app's own
 * local `Settings` DataStore that already backs `marketFilter`/`marketSortBy`
 * (`shared/domain/.../domain/repository/SettingsRepository.kt`). `bisq2 settings/src/main/java/
 * bisq/settings/FavouriteMarketsService.java` already exists: `addFavourite`/`removeFavourite`/
 * `isFavourite` operate on `SettingsService.getFavouriteMarkets()` (an `ObservableSet<Market>`),
 * gated by `canAddNewFavourite()` at `MAX_ALLOWED_FAVOURITES = 5`. A profile's favourites are
 * therefore the same set whether read from Desktop or from the node app sharing that profile.
 *
 * Node: reads and writes `FavouriteMarketsService` directly in-process, exactly like Desktop does
 * — no bisq2 API work needed.
 *
 * Connect: blocking prerequisite. `bisq2 api/src/main/java/bisq/api/dto/settings/SettingsDto.java`
 * and `api/src/main/java/bisq/api/rest_api/endpoints/settings/SettingsChangeRequest.java` are flat
 * records with no favourites field, and `SettingsRestApi.updateSetting` is a single `@PATCH
 * /settings` endpoint keyed on which one nullable field in the request is non-null — there is no
 * per-field granular update path today. The rule: add a dedicated `PUT
 * /settings/favourite-markets/{market}` (add) and `DELETE /settings/favourite-markets/{market}`
 * (remove) pair on `SettingsRestApi`, calling `FavouriteMarketsService.addFavourite`/
 * `removeFavourite` directly. This maps 1:1 onto `FavouriteMarketsService`'s existing single-market
 * methods, so the client sends exactly the one market it is toggling rather than holding and
 * resending the whole set, and it keeps the max-reached case a first-class, unambiguous response
 * instead of something inferred from a diff against a bulk PATCH. `PUT` returns `204 No Content` on
 * success and also when the market is already a favourite (idempotent: `isFavourite` is checked
 * before the capacity check, so a repeated `PUT` never fails); `409 Conflict` (body: the existing
 * `bisqEasy.offerbook.marketListCell.favourites.maxReached.popup` text) is returned only when
 * adding a new market while `canAddNewFavourite()` is false; `DELETE` returns `204 No Content`
 * whether or not the market was already a favourite, matching `removeFavourite`'s own no-op-if-
 * absent behaviour. `SettingsDto`'s GET response also needs the favourites set added so a fresh
 * session knows the starting state, and a node-side facade mapping plus a client-side REST call are
 * both needed before Connect can ship this feature — see "10. Implementation notes."
 *
 * ------------------------------------------------------------------------------------
 * 3. LIST STRUCTURE
 * ------------------------------------------------------------------------------------
 * Favourites render as a pinned block above the main list — not merely sorted to the top of one
 * list — matching Desktop's separate `favouritesTableView`. [FavouriteMarketsUiState.favouriteItems]
 * and [FavouriteMarketsUiState.marketItems] are two distinct lists; the main list excludes
 * whatever is already in the favourites block (Desktop's own predicate does the same:
 * `BisqEasyOfferbookController`'s main-table predicate explicitly excludes
 * `item.getIsFavourite().get()`), so nothing renders twice. [FavouriteMarketsList] renders, in
 * order: the favourites block (if non-empty) under [FavouritesSectionHeader] — a small label plus
 * a thin `BisqHDivider` — then the main list. The selected sort ([MarketSortBy]) applies
 * independently inside each block, so a favourite market's position within the pinned block still
 * reflects "most offers" / name order the same way the main list does; sorting is not "favourites
 * always first, then the chosen order" collapsed into one comparator, it is two lists each sorted
 * by the same comparator. A favourite with zero offers under "most offers" uses the same secondary
 * tie-break the main list already applies (`compareByDescending { numOffers }.thenBy {
 * localeFiatCurrencyName }` in `ComputeOfferbookMarketListUseCase`) — no separate tie-break rule for
 * the favourites block.
 *
 * The active [MarketFilter] applies to the favourites block, not only to the main list — see
 * [applyOfferFilter]. With `WithOffers` active, a favourite with zero offers is excluded from
 * [FavouriteMarketsUiState.favouriteItems]: it stays a favourite (the underlying set is
 * unchanged) and reappears in the pinned block the moment the filter changes to `All` or
 * `Favourites`, it is only not rendered while `WithOffers` is active. With `Favourites` active,
 * every favourite appears in the pinned block regardless of offer count — that filter is itself
 * the "show my favourites" scope, so it does not additionally impose the with-offers restriction
 * on top. Both `favouriteItems` and `marketItems` are produced by running the identical offer-count
 * predicate against the favourites set and the non-favourites set respectively, not two different
 * rules — see [FavouriteMarketsList_WithOffersExcludesZeroOffers_Preview].
 *
 * ------------------------------------------------------------------------------------
 * 4. STAR TOGGLE
 * ------------------------------------------------------------------------------------
 * [FavouriteStarToggle] is a visible trailing icon on every row — [FavouriteMarketRow] — not
 * hidden behind a long-press or swipe, so a trader can see and change a market's favourite state
 * without leaving the list. It renders the same drawable assets `StarRating` already ships
 * (`icon_star_green` filled, `icon_star_grey_hollow` hollow) rather than a new icon, since this app
 * already treats a filled green star as "positive/selected" via the reputation stars. Content
 * description reuses the existing, already-translated strings —
 * `bisqEasy.offerbook.marketListCell.favourites.tooltip.addToFavourites` /
 * `.removeFromFavourites` — as real accessibility text on the icon, not a `semantics {}` block used
 * as a test-id substitute.
 *
 * The toggle exists only on this list's rows. It is not duplicated on the offer-detail or
 * take-offer screens — the market list is the single place favourite state is set or changed.
 *
 * ------------------------------------------------------------------------------------
 * 5. MAX-5 RULE AND FEEDBACK
 * ------------------------------------------------------------------------------------
 * The cap is `FavouriteMarketsService.MAX_ALLOWED_FAVOURITES = 5`, enforced node-side; the UI does
 * not duplicate the count check, it surfaces whatever the add call reports. Feedback for a
 * rejected add is a snackbar, reusing the existing, already-translated
 * `bisqEasy.offerbook.marketListCell.favourites.maxReached.popup` string verbatim — see
 * [SimulatedMaxFavouritesSnackbar]. A snackbar matches the weight of the action that triggered it:
 * toggling one star is a quick, reversible tap, not a decision that needs a blocking dialog and an
 * explicit acknowledgement. `OfferbookMarketPresenter.onSelectMarket` already uses exactly this
 * pattern for a comparable failure (`showSnackbar(..., type = SnackbarType.ERROR)` when market
 * selection fails) — reusing it here is consistent with how this screen already reports a
 * rejected action, rather than introducing a second feedback mechanism for the same class of
 * event. The rejection is not persisted UI state (no `maxFavouritesReached` field on
 * [FavouriteMarketsUiState]): it is a one-shot presenter side effect, the same way the existing
 * selection-failure snackbar is.
 *
 * ------------------------------------------------------------------------------------
 * 6. FILTER AND EMPTY STATE
 * ------------------------------------------------------------------------------------
 * [MarketFilter] gains a third value, `Favourites`, alongside the existing `WithOffers`/`All`
 * (production: `shared/domain/.../data/model/market/MarketFilters.kt`), selected the same way as
 * today through the existing filter sheet's segmented button
 * (`network.bisq.mobile.presentation.common.ui.components.organisms.market.MarketFilters`) — see
 * [FavouriteMarketFilters]. The label reuses the existing, already-translated
 * `bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.favourites` ("Only favourites").
 * `SearchWithFilterField`'s `isFilterActive` flag, currently `filter == MarketFilter.WithOffers`,
 * needs to become `filter != MarketFilter.All` so the search bar's filter icon also highlights
 * when `Favourites` is selected.
 *
 * With `Favourites` selected and zero favourites, [FavouritesEmptyState] replaces the main list:
 * one short hint line plus a link back to `All`, deliberately kept to a single short block rather
 * than a full-height empty-state illustration — the situation is "you haven't used this filter
 * yet," not an error or a dead end, and a trader who just wants to see markets again should not
 * have to open the filter sheet to undo a filter they can see they set. The link dispatches the
 * same `OnFilterChanged(MarketFilter.All)` action the filter sheet itself would, not a separate
 * action, since it is the identical operation reached through a shortcut.
 *
 * ------------------------------------------------------------------------------------
 * 7. ACCESSIBILITY
 * ------------------------------------------------------------------------------------
 * - The star toggle's content description states the action it performs (add/remove), not just
 *   "favourite," and changes with state, so a screen reader announces what tapping it will do next
 *   rather than only its current status.
 * - [FavouritesSectionHeader]'s label is real text, not decoration baked into an image, so it
 *   scales and reads with the rest of the screen's type.
 * - [FavouriteMarketRow]'s currency name uses `singleLine = true` (matching production
 *   `MarketCard`), so a long localized name truncates predictably instead of pushing the row's
 *   height around as it wraps — see [FavouriteMarketRow_LongLocaleName_Preview].
 * - The empty state's link text meets normal touch-target sizing via its own padding, not relying
 *   on the short hint line above it also being tappable.
 *
 * ------------------------------------------------------------------------------------
 * 8. PROPOSED I18N KEYS
 * ------------------------------------------------------------------------------------
 * English base values only, per repo convention — nothing is added to `mobile.properties` by this
 * file; production implementation adds these.
 *
 * New:
 *   mobile.components.marketFilter.favourites.sectionHeader = "Favourites"
 *   mobile.components.marketFilter.favourites.emptyState.hint = "You haven't added any
 *     favourites yet."
 *   mobile.components.marketFilter.favourites.emptyState.switchToAll = "Show all markets"
 *
 * Reused, already present (all 14 locales) in `GeneratedResourceBundles_*.kt`:
 *   bisqEasy.offerbook.marketListCell.favourites.maxReached.popup = "There's only space for 5
 *     favourites. Remove a favourite and try again." (snackbar text)
 *   bisqEasy.offerbook.marketListCell.favourites.tooltip.addToFavourites = "Add to favourites"
 *     (star toggle content description, not-yet-favourite state)
 *   bisqEasy.offerbook.marketListCell.favourites.tooltip.removeFromFavourites = "Remove from
 *     favourites" (star toggle content description, favourite state)
 *   bisqEasy.offerbook.dropdownMenu.sortAndFilterMarkets.favourites = "Only favourites" (filter
 *     option label)
 *   mobile.components.currencyCard.numberOfOffers (offers-count text, already used by
 *     production `MarketCard`)
 *
 * ------------------------------------------------------------------------------------
 * 9. TESTS TO ADD
 * ------------------------------------------------------------------------------------
 * - Presenter test: a market added to favourites moves from the main list into the favourites
 *   block on the next state emission and disappears from the main list; the sixth add attempt is
 *   rejected and triggers the max-reached snackbar without changing the favourites set; the
 *   selected sort produces the same order within the favourites block and within the main list
 *   independently.
 * - Presenter test for the filter: `Favourites` with a non-empty favourites set shows only the
 *   favourites block with an empty main list; `Favourites` with zero favourites drives the empty
 *   state; switching back to `All` restores the main list; a favourite with zero offers is
 *   excluded from the pinned block while `WithOffers` is active and reappears there (without
 *   having been re-added) once the filter changes to `All` or `Favourites`.
 * - UI test for the star toggle: tapping it dispatches the toggle action with the tapped market's
 *   code; the icon and its content description both flip between the add/remove pair on state
 *   change.
 *
 * ------------------------------------------------------------------------------------
 * 10. IMPLEMENTATION NOTES FOR THE DEVELOPER
 * ------------------------------------------------------------------------------------
 * - `SettingsServiceFacade` (`shared/domain/.../data/service/settings/SettingsServiceFacade.kt`)
 *   already has the exact shape this needs for a node-backed, per-app-mirrored setting — compare
 *   `useAnimations: StateFlow<Boolean>` + `setUseAnimations(value): Result<Unit>`. Add
 *   `favouriteMarkets: StateFlow<Set<String>>` (market codes) + `addFavouriteMarket(marketCode):
 *   Result<Unit>` + `removeFavouriteMarket(marketCode): Result<Unit>` following that same pattern.
 * - `NodeSettingsServiceFacade` (`apps/nodeApp/.../domain/service/settings/
 *   NodeSettingsServiceFacade.kt`): bind directly to `FavouriteMarketsService`/
 *   `settingsService.getFavouriteMarkets()`, the same way `useAnimations` binds to
 *   `settingsService.useAnimations`.
 * - `ClientSettingsServiceFacade` (`apps/clientApp/.../domain/service/settings/
 *   ClientSettingsServiceFacade.kt`): once the bisq2 prerequisite in "2. Data and persistence per
 *   app" lands, call the new endpoint(s) and update the local `StateFlow` from the response,
 *   mirroring how `useAnimations` round-trips through `SettingsDto`.
 * - `MarketListItem` (`shared/domain/.../data/model/offerbook/MarketListItem.kt`) needs an
 *   `isFavourite: Boolean` field; `ComputeOfferbookMarketListUseCase` needs to split its output
 *   into a favourites list and a main list (excluding favourites) instead of one combined list,
 *   running the same offer-count predicate against both and sorting each independently by the
 *   existing comparator, per "3. List structure."
 * - `MarketFilter` (`shared/domain/.../data/model/market/MarketFilters.kt`) needs the third
 *   `Favourites` entry; `MarketFilters` (`shared/presentation/.../organisms/market/
 *   MarketFilters.kt`)'s segmented button already iterates `MarketFilter.entries`, so it picks up
 *   the new value once the enum has it and `getDisplayName()` is extended.
 * - `OfferbookMarketUiAction` needs an `OnToggleFavourite(marketCode: String)` case; the presenter
 *   calls the new facade methods and shows the existing max-reached snackbar on a rejected add —
 *   see "5. Max-5 rule and feedback."
 * - `SearchWithFilterField`'s `isFilterActive` call site in `OfferbookMarketScreen.kt` needs to
 *   change from `== MarketFilter.WithOffers` to `!= MarketFilter.All` — see "6. Filter and empty
 *   state."
 */
package network.bisq.mobile.presentation.design.favourite_markets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.icon_star_green
import bisqapps.shared.presentation.generated.resources.icon_star_grey_hollow
import network.bisq.mobile.i18n.i18nPlural
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSegmentButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.jetbrains.compose.resources.painterResource

// -------------------------------------------------------------------------------------
// State and actions (production shape lives in OfferbookMarketPresenter, see
// "10. Implementation notes for the developer" above)
// -------------------------------------------------------------------------------------

/** Mirrors `shared/domain/.../data/model/market/MarketFilters.kt`'s `MarketFilter`, plus `Favourites`. */
internal enum class MarketFilter { WithOffers, All, Favourites }

/** Unchanged from production `MarketSortBy` — sketched locally to keep this file self-contained. */
internal enum class MarketSortBy { MostOffers, NameAZ, NameZA }

/** Primitive stand-in for `network.bisq.mobile.data.model.offerbook.MarketListItem`. */
internal data class FavouriteMarketListItem(
    val marketCode: String,
    val quoteCurrencyCode: String,
    val localeFiatCurrencyName: String,
    val numOffers: Int,
    val isFavourite: Boolean = false,
)

/**
 * Stands in for the slice of `OfferbookMarketPresenter` state this design touches.
 *
 * @param favouriteItems The pinned block: the favourites set with [filter]'s offer-count
 * predicate applied (see [applyOfferFilter]) and sorted by [sortBy]. NOT "all favourites,
 * unfiltered" — a favourite with zero offers is absent from this list while `WithOffers` is
 * active, even though it remains a favourite — see "3. List structure."
 * @param marketItems The main list: the non-favourites set with the same predicate applied and
 * sorted, EXCLUDING anything present in [favouriteItems] — see "3. List structure."
 */
internal data class FavouriteMarketsUiState(
    val filter: MarketFilter = MarketFilter.All,
    val sortBy: MarketSortBy = MarketSortBy.MostOffers,
    val favouriteItems: List<FavouriteMarketListItem> = emptyList(),
    val marketItems: List<FavouriteMarketListItem> = emptyList(),
)

internal sealed interface FavouriteMarketsUiAction {
    data class OnToggleFavourite(
        val marketCode: String,
    ) : FavouriteMarketsUiAction

    data class OnMarketSelected(
        val marketCode: String,
    ) : FavouriteMarketsUiAction

    data class OnFilterChanged(
        val filter: MarketFilter,
    ) : FavouriteMarketsUiAction

    data class OnSortByChanged(
        val sortBy: MarketSortBy,
    ) : FavouriteMarketsUiAction
}

/** Builds a [FavouriteMarketListItem] from primitives with realistic defaults. */
internal fun simulatedFavouriteMarketListItem(
    marketCode: String = "BTC/USD",
    quoteCurrencyCode: String = "USD",
    localeFiatCurrencyName: String = "US Dollar",
    numOffers: Int = 12,
    isFavourite: Boolean = false,
): FavouriteMarketListItem =
    FavouriteMarketListItem(
        marketCode = marketCode,
        quoteCurrencyCode = quoteCurrencyCode,
        localeFiatCurrencyName = localeFiatCurrencyName,
        numOffers = numOffers,
        isFavourite = isFavourite,
    )

private val DEFAULT_MAIN_ITEMS =
    listOf(
        simulatedFavouriteMarketListItem(marketCode = "BTC/BRL", quoteCurrencyCode = "BRL", localeFiatCurrencyName = "Brazilian Real", numOffers = 3),
        simulatedFavouriteMarketListItem(marketCode = "BTC/GBP", quoteCurrencyCode = "GBP", localeFiatCurrencyName = "British Pound", numOffers = 1),
        simulatedFavouriteMarketListItem(marketCode = "BTC/JPY", quoteCurrencyCode = "JPY", localeFiatCurrencyName = "Japanese Yen", numOffers = 0),
    )

/**
 * The same offer-count predicate `ComputeOfferbookMarketListUseCase` already applies to the main
 * list, run against a candidate set — used here against both the favourites set and the
 * non-favourites set, so [FavouriteMarketsUiState.favouriteItems] and
 * [FavouriteMarketsUiState.marketItems] are produced by one rule, not two — see
 * "3. List structure."
 */
internal fun applyOfferFilter(
    items: List<FavouriteMarketListItem>,
    filter: MarketFilter,
): List<FavouriteMarketListItem> =
    when (filter) {
        MarketFilter.WithOffers -> items.filter { it.numOffers > 0 }
        MarketFilter.All, MarketFilter.Favourites -> items
    }

/** Builds a [FavouriteMarketsUiState] from primitives with realistic defaults. */
internal fun simulatedFavouriteMarketsUiState(
    filter: MarketFilter = MarketFilter.All,
    sortBy: MarketSortBy = MarketSortBy.MostOffers,
    favouriteItems: List<FavouriteMarketListItem> = emptyList(),
    marketItems: List<FavouriteMarketListItem> = DEFAULT_MAIN_ITEMS,
): FavouriteMarketsUiState =
    FavouriteMarketsUiState(
        filter = filter,
        sortBy = sortBy,
        favouriteItems = favouriteItems,
        marketItems = marketItems,
    )

// -------------------------------------------------------------------------------------
// Star toggle — see "4. Star toggle"
// -------------------------------------------------------------------------------------

@Composable
internal fun FavouriteStarToggle(
    isFavourite: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = if (isFavourite) "Remove from favourites" else "Add to favourites"
    IconButton(
        onClick = onToggle,
        modifier = modifier.testTag("favourite_star_toggle"),
    ) {
        Image(
            painter = painterResource(if (isFavourite) Res.drawable.icon_star_green else Res.drawable.icon_star_grey_hollow),
            contentDescription = description,
            modifier = Modifier.size(20.dp),
        )
    }
}

// -------------------------------------------------------------------------------------
// Market row — reproduces production MarketCard's layout with the star toggle added
// -------------------------------------------------------------------------------------

@Composable
internal fun FavouriteMarketRow(
    item: FavouriteMarketListItem,
    onToggleFavourite: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = BisqUIConstants.ScreenPadding)
                .testTag("favourite_market_row_${item.marketCode}"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(3.0f)) {
            DynamicImage(
                "files/markets/fiat/market_${item.quoteCurrencyCode.lowercase()}.png",
                modifier = Modifier.size(36.dp),
            )
            BisqGap.HHalf()
            Column {
                BisqText.BaseLight(text = item.localeFiatCurrencyName, singleLine = true)
                BisqText.BaseLightGrey(item.quoteCurrencyCode)
            }
        }
        BisqText.BaseLight(
            text = "mobile.components.currencyCard.numberOfOffers".i18nPlural(item.numOffers),
            color = BisqTheme.colors.primary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.0f),
        )
        FavouriteStarToggle(isFavourite = item.isFavourite, onToggle = onToggleFavourite)
    }
}

// -------------------------------------------------------------------------------------
// Pinned block + main list — see "3. List structure" and "6. Filter and empty state"
// -------------------------------------------------------------------------------------

@Composable
private fun FavouritesSectionHeader() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = BisqUIConstants.ScreenPaddingHalf)
                .testTag("favourites_section_header"),
    ) {
        BisqText.SmallMedium(
            text = "Favourites",
            color = BisqTheme.colors.mid_grey20,
            modifier = Modifier.padding(horizontal = BisqUIConstants.ScreenPadding),
        )
        BisqGap.VQuarter()
        BisqHDivider()
    }
}

@Composable
internal fun FavouritesEmptyState(onSwitchToAll: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(BisqUIConstants.ScreenPadding)
                .testTag("favourites_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BisqText.SmallLight(
            text = "You haven't added any favourites yet.",
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
        )
        BisqGap.VQuarter()
        BisqText.SmallMedium(
            text = "Show all markets",
            color = BisqTheme.colors.primary,
            modifier =
                Modifier
                    .clickable(onClick = onSwitchToAll)
                    .padding(BisqUIConstants.ScreenPaddingHalf)
                    .testTag("favourites_empty_state_switch_to_all"),
        )
    }
}

@Composable
internal fun FavouriteMarketsList(
    uiState: FavouriteMarketsUiState,
    onAction: (FavouriteMarketsUiAction) -> Unit,
) {
    val showEmptyState = uiState.filter == MarketFilter.Favourites && uiState.favouriteItems.isEmpty()

    LazyColumn {
        if (uiState.favouriteItems.isNotEmpty()) {
            item(key = "favourites_header") { FavouritesSectionHeader() }
            items(uiState.favouriteItems, key = { "favourite_${it.marketCode}" }) { item ->
                FavouriteMarketRow(
                    item = item,
                    onToggleFavourite = { onAction(FavouriteMarketsUiAction.OnToggleFavourite(item.marketCode)) },
                    onClick = { onAction(FavouriteMarketsUiAction.OnMarketSelected(item.marketCode)) },
                )
            }
        }

        if (showEmptyState) {
            item(key = "favourites_empty_state") {
                FavouritesEmptyState(onSwitchToAll = { onAction(FavouriteMarketsUiAction.OnFilterChanged(MarketFilter.All)) })
            }
        } else if (uiState.filter != MarketFilter.Favourites) {
            // Under the Favourites filter the pinned block is the whole list; the main list never renders.
            items(uiState.marketItems, key = { it.marketCode }) { item ->
                FavouriteMarketRow(
                    item = item,
                    onToggleFavourite = { onAction(FavouriteMarketsUiAction.OnToggleFavourite(item.marketCode)) },
                    onClick = { onAction(FavouriteMarketsUiAction.OnMarketSelected(item.marketCode)) },
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Filter control — see "6. Filter and empty state"
// -------------------------------------------------------------------------------------

private fun MarketFilter.displayName(): String =
    when (this) {
        MarketFilter.WithOffers -> "With offers"
        MarketFilter.All -> "All"
        MarketFilter.Favourites -> "Only favourites"
    }

@Composable
internal fun FavouriteMarketFilters(
    filter: MarketFilter,
    onFilterChange: (MarketFilter) -> Unit,
) {
    Column(modifier = Modifier.padding(all = BisqUIConstants.ScreenPadding2X)) {
        BisqSegmentButton(
            label = "Show markets",
            items = MarketFilter.entries.map { it to it.displayName() },
            value = filter,
            onValueChange = { pair -> onFilterChange(pair.first) },
        )
    }
}

// -------------------------------------------------------------------------------------
// Max-reached feedback — see "5. Max-5 rule and feedback"
// -------------------------------------------------------------------------------------

@Composable
internal fun SimulatedMaxFavouritesSnackbar() {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .padding(BisqUIConstants.ScreenPadding)
                .testTag("favourites_max_reached_snackbar"),
    ) {
        BisqText.SmallLight(
            text = "There's only space for 5 favourites. Remove a favourite and try again.",
            color = BisqTheme.colors.white,
        )
    }
}

// -------------------------------------------------------------------------------------
// Previews
// -------------------------------------------------------------------------------------

/** List with zero favourites — no pinned block, plain main list. */
@ExcludeFromCoverage
@Preview(name = "1. List — 0 favourites")
@Composable
private fun FavouriteMarketsList_ZeroFavourites_Preview() {
    BisqTheme.Preview {
        FavouriteMarketsList(
            uiState = simulatedFavouriteMarketsUiState(),
            onAction = {},
        )
    }
}

/** List with 2 favourites — pinned block, section header, then the main list. */
@ExcludeFromCoverage
@Preview(name = "2. List — 2 favourites")
@Composable
private fun FavouriteMarketsList_TwoFavourites_Preview() {
    BisqTheme.Preview {
        FavouriteMarketsList(
            uiState =
                simulatedFavouriteMarketsUiState(
                    favouriteItems =
                        listOf(
                            simulatedFavouriteMarketListItem(isFavourite = true),
                            simulatedFavouriteMarketListItem(
                                marketCode = "BTC/EUR",
                                quoteCurrencyCode = "EUR",
                                localeFiatCurrencyName = "Euro",
                                numOffers = 8,
                                isFavourite = true,
                            ),
                        ),
                ),
            onAction = {},
        )
    }
}

/** List with 5 favourites — the pinned block at its maximum size. */
@ExcludeFromCoverage
@Preview(name = "3. List — 5 favourites (maximum)", heightDp = 700)
@Composable
private fun FavouriteMarketsList_FiveFavourites_Preview() {
    BisqTheme.Preview {
        FavouriteMarketsList(
            uiState =
                simulatedFavouriteMarketsUiState(
                    favouriteItems =
                        listOf("USD", "EUR", "GBP", "BRL", "JPY").mapIndexed { index, code ->
                            simulatedFavouriteMarketListItem(
                                marketCode = "BTC/$code",
                                quoteCurrencyCode = code,
                                localeFiatCurrencyName = code,
                                numOffers = 5 - index,
                                isFavourite = true,
                            )
                        },
                    // Every default main-list market is a favourite here, so the main list is empty.
                    marketItems = emptyList(),
                ),
            onAction = {},
        )
    }
}

/**
 * "With offers" active, 2 favourites, one with zero offers — only the one with offers renders in
 * the pinned block; the zero-offer favourite is excluded upstream by [applyOfferFilter], not
 * hidden by the row itself. It remains a favourite and returns once the filter changes.
 */
@ExcludeFromCoverage
@Preview(name = "4. List — With offers excludes a zero-offer favourite")
@Composable
private fun FavouriteMarketsList_WithOffersExcludesZeroOffers_Preview() {
    val candidateFavourites =
        listOf(
            simulatedFavouriteMarketListItem(isFavourite = true),
            simulatedFavouriteMarketListItem(
                marketCode = "BTC/JPY",
                quoteCurrencyCode = "JPY",
                localeFiatCurrencyName = "Japanese Yen",
                numOffers = 0,
                isFavourite = true,
            ),
        )
    BisqTheme.Preview {
        FavouriteMarketsList(
            uiState =
                simulatedFavouriteMarketsUiState(
                    filter = MarketFilter.WithOffers,
                    favouriteItems = applyOfferFilter(candidateFavourites, MarketFilter.WithOffers),
                    marketItems = applyOfferFilter(DEFAULT_MAIN_ITEMS, MarketFilter.WithOffers),
                ),
            onAction = {},
        )
    }
}

/** Star toggle, both states, side by side. */
@ExcludeFromCoverage
@Preview(name = "5. Star toggle — both states")
@Composable
private fun FavouriteStarToggle_BothStates_Preview() {
    BisqTheme.Preview {
        Row(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BisqText.SmallLight("Not favourite", color = BisqTheme.colors.mid_grey20)
                FavouriteStarToggle(isFavourite = false, onToggle = {})
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BisqText.SmallLight("Favourite", color = BisqTheme.colors.mid_grey20)
                FavouriteStarToggle(isFavourite = true, onToggle = {})
            }
        }
    }
}

/** Max-reached feedback — snackbar reusing the existing translated string. */
@ExcludeFromCoverage
@Preview(name = "6. Max-reached feedback")
@Composable
private fun SimulatedMaxFavouritesSnackbar_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            SimulatedMaxFavouritesSnackbar()
        }
    }
}

/** Filter control with "Only favourites" alongside the existing two options. */
@ExcludeFromCoverage
@Preview(name = "7. Filter menu — with Only favourites")
@Composable
private fun FavouriteMarketFilters_Preview() {
    BisqTheme.Preview {
        FavouriteMarketFilters(filter = MarketFilter.Favourites, onFilterChange = {})
    }
}

/** "Only favourites" filter active with zero favourites — the empty state. */
@ExcludeFromCoverage
@Preview(name = "8. List — Only favourites, empty state")
@Composable
private fun FavouriteMarketsList_FavouritesEmptyState_Preview() {
    BisqTheme.Preview {
        FavouriteMarketsList(
            uiState = simulatedFavouriteMarketsUiState(filter = MarketFilter.Favourites, marketItems = emptyList()),
            onAction = {},
        )
    }
}

/** Row with a long localized currency name — truncates on one line rather than wrapping. */
@ExcludeFromCoverage
@Preview(name = "9. Row — long locale name")
@Composable
private fun FavouriteMarketRow_LongLocaleName_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            FavouriteMarketRow(
                item =
                    simulatedFavouriteMarketListItem(
                        marketCode = "BTC/AED",
                        quoteCurrencyCode = "AED",
                        localeFiatCurrencyName = "United Arab Emirates Dirham (long form, simulated locale)",
                        numOffers = 4,
                        isFavourite = true,
                    ),
                onToggleFavourite = {},
                onClick = {},
            )
        }
    }
}
