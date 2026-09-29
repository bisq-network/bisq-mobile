/**
 * SellerQrCodeDesign.kt
 *
 * Compose specification for a seller-side QR code of the buyer's Bitcoin payout address or
 * Lightning invoice, shown at Bisq Easy trade phase 3a. Not wired to a presenter; state shown in
 * the previews below is produced entirely by the `simulatedXxx` helpers, which take only
 * primitives.
 *
 * ------------------------------------------------------------------------------------
 * 1. PURPOSE
 * ------------------------------------------------------------------------------------
 * At phase 3a the seller has the buyer's payout data (`bisqEasyTradeModel.bitcoinPaymentData`) in
 * `shared/presentation/.../trade/trade_detail/states/seller_state_3/state_a/SellerState3a.kt` as
 * read-only text. Manually transcribing a bech32 address or a Lightning invoice on a phone is
 * error-prone, and copying through the OS clipboard is a known address-swap vector on Android. A
 * QR the seller scans with a wallet app removes both paths. This specification adds a "Show QR"
 * action to that screen and a full-screen sheet that renders the code, the payload as text, a
 * copy action, and an "open in wallet" action.
 *
 * ------------------------------------------------------------------------------------
 * 2. DATA AND PAYLOAD
 * ------------------------------------------------------------------------------------
 * Two independent choices exist: which string gets encoded, and how large/how error-tolerant the
 * resulting code is. This section covers the first.
 *
 * On-chain: the code always encodes a BIP21 URI, `bitcoin:<address>?amount=<btc>` — see
 * [buildQrPayload]. Prefilling the amount removes a manual re-entry step at exactly the point a
 * seller is retyping a monetary value, and the amount is already displayed and confirmed
 * elsewhere on the same screen, so encoding it does not introduce a second, independent source of
 * truth for what to send. A wallet that ignores or mis-parses the BIP21 `amount` parameter still
 * has a correct fallback: the sheet always shows the raw address in copyable text below the code
 * (see "5. Full-screen sheet layout"), so manual entry or copy remains available regardless of
 * how a given wallet handles the URI.
 *
 * Lightning: the code always encodes the raw invoice, unmodified, matching
 * `bisq2/apps/desktop/desktop/.../common/qr/QrCodeDisplay.java`'s literal encode-as-is behaviour.
 * A BOLT11 invoice already commits to an amount (or is explicitly amountless, in which case the
 * wallet prompts) — there is no separate "prefill the amount" step to add for Lightning.
 *
 * Amount source: the BIP21 `amount` parameter must be built from the trade's numeric base amount
 * with a literal `.` decimal separator, never from `TradeItemPresentationModel.formattedBaseAmount`
 * as displayed on screen. `formattedBaseAmount` is produced by `AmountFormatter.format`, which
 * routes through `expect val decimalFormatter: DecimalFormatter`
 * (`shared/domain/.../data/utils/PlatformDomainAbstractions.kt`) — a locale-aware formatter with
 * no locale-invariant mode, so on a German or French device it renders with a `,` separator (e.g.
 * "0,00250000"), which is not valid BIP21. No existing formatter call in this codebase produces a
 * locale-invariant decimal string; encoding the BIP21 amount needs its own small, dedicated
 * conversion (see "11. Implementation notes"), not a reuse of the display string.
 *
 * ------------------------------------------------------------------------------------
 * 3. SCREEN INTEGRATION
 * ------------------------------------------------------------------------------------
 * No inline QR thumbnail is shown next to the address field. [SellerAddressRowWithQrAction]
 * places a "Show QR" icon next to the field's copy icon, opening [SellerQrCodeSheet] on tap. A
 * thumbnail sized to stay scannable (matching the module counts in "4. QR rendering") needs on
 * the order of 116-130dp on a side; a portrait phone's content width after screen padding is
 * roughly 336-386dp, which leaves too little room for the address text next to a thumbnail that
 * size — especially for the wider Lightning code next to a ~200+ character invoice. The code
 * therefore only ever renders full-width, in the sheet.
 *
 * Today's production `BitcoinLnAddressField(disabled = true)` call renders no trailing icon at
 * all (its trailing icon is built only for the non-disabled, editable case), so the address field
 * has no copy affordance today either; [SellerAddressRowWithQrAction] adds both a
 * `CopyIconButton` and the QR action together, closing that gap rather than adding a second icon
 * next to a first one that does not yet exist in production.
 *
 * ------------------------------------------------------------------------------------
 * 4. QR RENDERING
 * ------------------------------------------------------------------------------------
 * No QR encoder exists in this codebase. `shared/kscan` wraps a camera-based QR *scanner*
 * (`org.ncgroup.kscan`); nothing generates a QR bitmap from a string. Previews render a simulated
 * module grid instead — see [simulatedQrModules] and [QrCodeCanvas] — deterministic per seed, with
 * finder patterns stamped in three corners so it reads as a QR code without depending on a real
 * encoder library.
 *
 * Module counts ([QR_MODULE_COUNT_ON_CHAIN], [QR_MODULE_COUNT_LIGHTNING]) mirror desktop's own
 * sizing comments — "Typical bitcoin address require size of 29" and "Typical LN invoice require
 * size of 65" in `SellerState3a.java`, which are QR module counts, not pixel sizes. The BIP21
 * URI's `?amount=...` suffix adds roughly 15-20 characters over a raw address, which is why the
 * on-chain constant here is 33, one QR version above desktop's raw-address baseline of 29. A
 * denser code needs a larger physical box to stay scannable — [SellerQrCodeSheet_Lightning_Preview]
 * renders visibly denser modules than [SellerQrCodeSheet_OnChain_Preview] at the same box size,
 * which is the concrete reason the sheet uses one large, width-filling code (see
 * [SELLER_QR_MAX_SIZE_DP]) rather than trying to keep the on-chain and Lightning codes the same
 * visual density.
 *
 * Error correction level [SELLER_QR_ERROR_CORRECTION_LEVEL] (L, low, ~7% recovery) matches
 * `QrCodeDisplay`'s behaviour — its `QRCodeWriter.encode` call passes no
 * `EncodeHintType.ERROR_CORRECTION` hint, so it falls back to the encoder library's default,
 * which is L. Level L maximizes data capacity per module, which matters for the already-dense
 * Lightning invoice; a freshly rendered on-screen code (not a printed, handled, or aged physical
 * one) has little use for the damage tolerance higher levels trade capacity for. The code must
 * render with a quiet zone (blank margin) of at least [SELLER_QR_QUIET_ZONE_MODULES] modules on
 * every side — encoder libraries typically default to this value, and it must not be reduced,
 * since an undersized quiet zone is a common cause of scan failures independent of the code's own
 * contrast or size.
 *
 * The code renders black-on-white regardless of the app's dark theme — [QrCodeCanvas] hardcodes
 * this — because scanners and wallet cameras expect maximum contrast; matching the app's dark
 * background would reduce scan reliability for no benefit.
 *
 * ------------------------------------------------------------------------------------
 * 5. FULL-SCREEN SHEET LAYOUT
 * ------------------------------------------------------------------------------------
 * [SellerQrCodeSheet] is a [BisqBottomSheet] (not a dialog — this codebase already reserves
 * dialogs for decisions the user must resolve; showing a code to scan is not one). Content, top to
 * bottom: title reusing `bisqEasy.tradeState.info.seller.phase3a.qrCodeDisplay.window.title`
 * ("Scan QR Code for trade '{0}'"), an address-changed banner when applicable (see
 * "8. Error and edge states"), the code sized to the sheet's available width and capped at
 * [SELLER_QR_MAX_SIZE_DP], one hint line confirming the amount is included (see
 * [amountHintFor]), the raw payload as wrapped, non-truncated text with a copy action, and an
 * "Open in wallet" action (see "7. Open in wallet action"). The code and the text below it always
 * show the same payload the code encodes, not a different, more "readable" form — a seller
 * comparing what the wallet camera resolved against what the sheet shows as text must be
 * comparing the same string.
 *
 * ------------------------------------------------------------------------------------
 * 6. SECURE SCREEN PROTECTION
 * ------------------------------------------------------------------------------------
 * [SellerQrCodeSheet] calls `SecureScreenEffect()` itself. `SellerState3a` already calls it for
 * the whole screen, and `SecureScreenEffect`'s Android implementation targets
 * `context.findActivity()?.window` — the Activity's window — with reference-counted ownership, so
 * a second call while the first is still active is a harmless increment, not a duplicate
 * protection. Whether a `BisqBottomSheet` shares that exact window object depends on which of its
 * three render paths is taken (`ModalBottomSheet`, the non-dialog `Popup` fallback for affected
 * devices, or the inline preview fallback) and is not something this design asserts as certain.
 * Calling `SecureScreenEffect()` in the sheet is correct either way: if the sheet shares the
 * parent's window, the call is a no-op increment; if a future render path does not share it (or if
 * this sheet is ever opened from a screen that does not already protect itself), the sheet's own
 * call is what protects the address, the invoice, and the trade amount from being captured by a
 * screenshot, screen recording, or an unblanked Recents thumbnail.
 *
 * ------------------------------------------------------------------------------------
 * 7. OPEN IN WALLET ACTION
 * ------------------------------------------------------------------------------------
 * "Open in wallet" ships for both payment kinds from the start: `UrlLauncher.openUrl` (see below)
 * handles both `bitcoin:` and `lightning:` schemes identically, so there is no technical reason to
 * gate one kind behind a later release.
 *
 * The action dispatches [SellerQrUiAction.OnOpenInWallet], which the presenter should implement
 * with the existing `UrlLauncher.openUrl(uri)`
 * (`shared/domain/.../data/utils/PlatformDomainAbstractions.kt`) — no new platform code is needed
 * for this action. `AndroidUrlLauncher` already opens arbitrary URI schemes through
 * `Intent(Intent.ACTION_VIEW, ...)` and returns `false` on `ActivityNotFoundException`;
 * `IOSUrlLauncher` already checks `canOpenURL` before calling `openURL` and returns `false` when no
 * handler is registered. Both already handle the "no wallet installed" case as a boolean result
 * rather than a crash — the presenter needs only to show a message when that result is `false`
 * (see [SellerQrCodeSheet_NoWalletFound_Preview]), not new fallback logic.
 *
 * The URI passed to `openUrl` is a schemed URI even where the code itself is not: the on-chain
 * BIP21 payload is already `bitcoin:...`, but the Lightning payload is the raw invoice with no
 * scheme (see "2. Data and payload") and needs `lightning:` prepended only for this action, since
 * an `Intent`/`openURL` call needs a scheme to route to a handler while a QR camera scan does not.
 * See [buildWalletUri].
 *
 * Sharing the rendered QR as an image is out of scope and must not be added: handing the bitmap to
 * the OS share sheet exposes the address and the trade amount to whatever arbitrary app the user
 * picks from that sheet — a wider, unbounded leak surface than the clipboard risk this design
 * already reduces, since a share-sheet target is not restricted to apps that registered
 * themselves as payment-URI handlers the way "open in wallet" is.
 *
 * ------------------------------------------------------------------------------------
 * 8. ERROR AND EDGE STATES
 * ------------------------------------------------------------------------------------
 * Address not yet available: `bisqEasyTradeModel.bitcoinPaymentData` is a nullable
 * `StateFlow<String?>`; while null, [SellerQrUiState.paymentData] is null and the "Show QR"
 * affordance is disabled rather than opening a sheet with nothing to encode — see
 * [SellerAddressRow_AddressNotYetAvailable_Preview].
 *
 * Address changed mid-trade: `bitcoinPaymentData` is a live `StateFlow`, not a one-shot value,
 * which is why it can change after the seller has already opened the sheet. If it changes while
 * [SellerQrUiState.paymentData] backs an already-visible sheet,
 * [SellerQrUiState.addressChangedNotice] drives a banner telling the seller the code just changed
 * and to scan again — a seller could be mid-scan with a wallet camera pointed at a code that is
 * about to stop matching what the buyer actually provided. The banner requires an explicit
 * dismiss ([SellerQrUiAction.OnAcknowledgeAddressChanged]) rather than clearing itself on the next
 * recomposition, so the seller has to register that the code changed before continuing, not just
 * happen to glance at a banner that is already gone. See
 * [SellerQrCodeSheet_AddressChanged_Preview].
 *
 * Lightning invoice expiry: this codebase has no BOLT11 decoder — `LightningInvoiceValidation`
 * (`shared/presentation/.../common/ui/utils/Validations.kt`) only checks the bech32 shape and
 * length, it does not decode tagged fields, so the invoice's expiry cannot currently be read or
 * displayed. This design does not add an expiry indicator because there is no data to back one; an
 * expired invoice fails at payment time, downstream of this sheet, exactly as it does today.
 *
 * Long payload text wrapping: the sheet's payload text has no `maxLines` cap and no ellipsis (see
 * "5. Full-screen sheet layout") — it is the one surface meant to show the complete string, so a
 * long Lightning invoice wraps across as many lines as it needs. See
 * [SellerQrCodeSheet_LongPayloadWrap_Preview].
 *
 * ------------------------------------------------------------------------------------
 * 9. PROPOSED I18N KEYS
 * ------------------------------------------------------------------------------------
 * English base values only, per repo convention — nothing is added to `mobile.properties` by this
 * file; production implementation adds these.
 *
 * New:
 *   mobile.bisqEasy.tradeState.info.seller.phase3a.qrCode.showAction = "Show QR"
 *   mobile.bisqEasy.tradeState.info.seller.phase3a.qrCode.amountIncluded = "This code includes
 *     the trade amount ({0})."
 *   mobile.bisqEasy.tradeState.info.seller.phase3a.qrCode.invoiceIncludesAmount = "The invoice
 *     already specifies the amount to send."
 *   mobile.bisqEasy.tradeState.info.seller.phase3a.qrCode.openInWallet = "Open in wallet"
 *   mobile.bisqEasy.tradeState.info.seller.phase3a.qrCode.noWalletFound = "No wallet app found to
 *     open this code."
 *   mobile.bisqEasy.tradeState.info.seller.phase3a.qrCode.addressNotYetAvailable = "The buyer
 *     hasn't sent a payout address yet."
 *   mobile.bisqEasy.tradeState.info.seller.phase3a.qrCode.addressChanged = "The buyer's payment
 *     data changed. Scan the code again."
 *   mobile.bisqEasy.tradeState.info.seller.phase3a.qrCode.close = "Close"
 *
 * Reused, already present (all 14 locales) in `GeneratedResourceBundles_*.kt`:
 *   bisqEasy.tradeState.info.seller.phase3a.qrCodeDisplay.window.title = "Scan QR Code for trade
 *     ''{0}''" (sheet title)
 *   bisqEasy.tradeState.info.seller.phase3a.bitcoinPayment.description.MAIN_CHAIN / .LN (field
 *     label, already used by the production screen)
 *   mobile.components.copyIconButton.copied (already used by [CopyIconButton])
 *
 * ------------------------------------------------------------------------------------
 * 10. TESTS TO ADD
 * ------------------------------------------------------------------------------------
 * - Encoder unit test: BIP21 output matches `bitcoin:<address>?amount=<btc>` exactly; the amount
 *   always uses `.` as the decimal separator regardless of the device's default locale (see
 *   "2. Data and payload" — this is the case most likely to regress silently); amounts format
 *   with up to 8 decimal places with no scientific notation at the smallest tradable amount;
 *   Lightning output is always the raw invoice.
 * - UI test for the sheet: the hint line names the correct amount; the copy action copies exactly
 *   the string the code encodes; the address-changed banner appears when
 *   [SellerQrUiState.addressChangedNotice] flips to true while the sheet is open, and stays until
 *   explicitly dismissed.
 * - Presenter test for the address-missing state: "Show QR" stays disabled while
 *   `bitcoinPaymentData` is null; it becomes available the moment the flow emits a non-null value.
 *
 * ------------------------------------------------------------------------------------
 * 11. IMPLEMENTATION NOTES FOR THE DEVELOPER
 * ------------------------------------------------------------------------------------
 * - QR encoding needs a small multiplatform `expect fun encodeQrCode(data: String, moduleCount:
 *   Int? = null): QrMatrix` (or equivalent) in shared domain, with `actual` implementations behind
 *   it. The Android zxing-cpp artifact already wired for scanning ships only the reader class
 *   (`zxingcpp.BarcodeReader`); it has no writer, so Android needs the zxing Java core
 *   `QRCodeWriter` desktop already depends on. iOS needs CoreImage's `CIQRCodeGenerator`. Both
 *   should honor [SELLER_QR_ERROR_CORRECTION_LEVEL] and [SELLER_QR_QUIET_ZONE_MODULES]. This
 *   design's previews do not depend on either encoder — see [simulatedQrModules].
 * - The BIP21 amount needs a locale-invariant decimal string builder — see "2. Data and payload."
 *   Do not reuse `TradeItemPresentationModel.formattedBaseAmount` for the URI.
 * - `SellerState3aPresenter`: add `paymentData`-derived [SellerQrUiState], a sheet-visibility flag,
 *   and an `OnOpenInWallet` handler calling the existing `UrlLauncher.openUrl` (see "7. Open in
 *   wallet action") — no new platform abstraction required for that action.
 * - `BitcoinLnAddressField`'s disabled path currently renders no trailing icon; the production
 *   screen needs its own row (mirroring [SellerAddressRowWithQrAction], not a change to the shared
 *   component's disabled branch, since that component is also used, disabled, by other read-only
 *   displays that do not need a QR action).
 */
package network.bisq.mobile.presentation.design.seller_qr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CopyIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ExclamationRedIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.bottom_sheet.BisqBottomSheet
import network.bisq.mobile.presentation.common.ui.security.SecureScreenEffect
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// -------------------------------------------------------------------------------------
// Tunable constants — no magic numbers inside the composables below; adjust these by
// hand once a real encoder (see "11. Implementation notes for the developer") is wired in.
// -------------------------------------------------------------------------------------

/** Upper bound on the sheet's QR edge length; below this the code fills the available width. */
internal const val SELLER_QR_MAX_SIZE_DP = 260

/**
 * Quiet-zone width a real encoder must reserve around the code, in QR modules; used here as the
 * simulated canvas's visual margin in dp, since the simulated grid does not model a real
 * module-to-dp scale.
 */
internal const val SELLER_QR_QUIET_ZONE_MODULES = 4

/** Error-correction level every code in this design renders at — see "4. QR rendering." */
internal const val SELLER_QR_ERROR_CORRECTION_LEVEL = "L"

/** Module count for the on-chain BIP21 code — see "4. QR rendering." */
internal const val QR_MODULE_COUNT_ON_CHAIN = 33

/** Module count for a Lightning invoice code — see "4. QR rendering." */
internal const val QR_MODULE_COUNT_LIGHTNING = 65

// -------------------------------------------------------------------------------------
// State and actions (production shape lives in SellerState3aPresenter, see
// "11. Implementation notes for the developer" above)
// -------------------------------------------------------------------------------------

internal enum class BitcoinPaymentKind { ON_CHAIN, LIGHTNING }

/**
 * Stands in for the slice of `SellerState3aPresenter` state this design touches.
 *
 * @param baseAmountBtc Locale-invariant numeric string ("0.00250000", `.` decimal separator) —
 * the value the BIP21 `amount` parameter is built from. NOT the same string as
 * [baseAmountDisplay]; see "2. Data and payload."
 * @param baseAmountDisplay The already-localized on-screen amount text, e.g. "0.00250000 BTC".
 * @param addressChangedNotice True while the address-changed banner should be visible — see
 * "8. Error and edge states."
 */
internal data class SellerQrUiState(
    val tradeId: String,
    val paymentKind: BitcoinPaymentKind,
    val paymentData: String?,
    val baseAmountBtc: String,
    val baseAmountDisplay: String,
    val addressChangedNotice: Boolean = false,
)

internal sealed interface SellerQrUiAction {
    data object OnShowQr : SellerQrUiAction

    data object OnCloseQr : SellerQrUiAction

    data object OnOpenInWallet : SellerQrUiAction

    data object OnAcknowledgeAddressChanged : SellerQrUiAction
}

/** Builds a [SellerQrUiState] from primitives with realistic defaults. */
internal fun simulatedSellerQrUiState(
    tradeId: String = "8f3ac210",
    paymentKind: BitcoinPaymentKind = BitcoinPaymentKind.ON_CHAIN,
    paymentData: String? = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
    baseAmountBtc: String = "0.00250000",
    baseAmountDisplay: String = "0.00250000 BTC",
    addressChangedNotice: Boolean = false,
): SellerQrUiState =
    SellerQrUiState(
        tradeId = tradeId,
        paymentKind = paymentKind,
        paymentData = paymentData,
        baseAmountBtc = baseAmountBtc,
        baseAmountDisplay = baseAmountDisplay,
        addressChangedNotice = addressChangedNotice,
    )

private const val SIMULATED_LIGHTNING_INVOICE =
    "lnbc25m1pvjluezsp5zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zyg3zygspp5qqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqqqsyqcyq5rqwzqfqypqdq5xysxxatsyp3k7enxv4jsxqzpuaztrnwngzn3kdzw5hydlzf03qdgm2hdq27cqv3agm2awhz5se903vruatfhq77w3ls4evs3ch9zw97j25emudupq63nyw24cg27h2rspfj9srp"

// -------------------------------------------------------------------------------------
// Payload builders — the logic "10. Tests to add" targets
// -------------------------------------------------------------------------------------

/** The exact string the QR code encodes — see "2. Data and payload." */
internal fun buildQrPayload(uiState: SellerQrUiState): String {
    val data = uiState.paymentData ?: return ""
    return when (uiState.paymentKind) {
        BitcoinPaymentKind.LIGHTNING -> data
        BitcoinPaymentKind.ON_CHAIN -> "bitcoin:$data?amount=${uiState.baseAmountBtc}"
    }
}

/** The schemed URI "Open in wallet" launches — see "7. Open in wallet action." */
internal fun buildWalletUri(uiState: SellerQrUiState): String {
    val payload = buildQrPayload(uiState)
    if (payload.isEmpty()) return payload
    return when (uiState.paymentKind) {
        BitcoinPaymentKind.LIGHTNING -> "lightning:$payload"
        BitcoinPaymentKind.ON_CHAIN -> payload
    }
}

private fun amountHintFor(uiState: SellerQrUiState): String =
    when (uiState.paymentKind) {
        BitcoinPaymentKind.LIGHTNING -> "The invoice already specifies the amount to send."
        BitcoinPaymentKind.ON_CHAIN -> "This code includes the trade amount (${uiState.baseAmountDisplay})."
    }

/** Module count mirroring desktop's own sizing comments — see "4. QR rendering." */
private fun qrModuleCountFor(uiState: SellerQrUiState): Int =
    when (uiState.paymentKind) {
        BitcoinPaymentKind.LIGHTNING -> QR_MODULE_COUNT_LIGHTNING
        BitcoinPaymentKind.ON_CHAIN -> QR_MODULE_COUNT_ON_CHAIN
    }

// -------------------------------------------------------------------------------------
// Simulated QR rendering (no encoder exists in this codebase — see "4. QR rendering")
// -------------------------------------------------------------------------------------

/**
 * A deterministic, QR-like module grid for previews: pseudo-random data cells seeded from
 * [seed] and [moduleCount] so the same inputs always render the same pattern, with a finder
 * pattern (concentric squares) stamped in three corners. Not a real QR encoder — see
 * "4. QR rendering" and "11. Implementation notes for the developer."
 */
internal fun simulatedQrModules(
    seed: String,
    moduleCount: Int,
): List<BooleanArray> {
    val grid = List(moduleCount) { BooleanArray(moduleCount) }
    for (y in 0 until moduleCount) {
        for (x in 0 until moduleCount) {
            grid[y][x] = pseudoRandomModule(seed, x, y)
        }
    }
    stampFinderPattern(grid, left = 0, top = 0)
    stampFinderPattern(grid, left = moduleCount - 7, top = 0)
    stampFinderPattern(grid, left = 0, top = moduleCount - 7)
    return grid
}

private fun pseudoRandomModule(
    seed: String,
    x: Int,
    y: Int,
): Boolean {
    val hash = seed.hashCode() * 31 + x * 131 + y * 17
    return (hash and 1) == 1
}

/** Stamps a 7x7 QR finder pattern (concentric squares) with its top-left corner at [left]/[top]. */
private fun stampFinderPattern(
    grid: List<BooleanArray>,
    left: Int,
    top: Int,
) {
    for (dy in 0 until 7) {
        for (dx in 0 until 7) {
            val onBorder = dx == 0 || dx == 6 || dy == 0 || dy == 6
            val onCore = dx in 2..4 && dy in 2..4
            grid[top + dy][left + dx] = onBorder || onCore
        }
    }
}

/** Renders a module grid black-on-white, regardless of app theme — see "4. QR rendering." */
@Composable
private fun QrCodeCanvas(
    modules: List<BooleanArray>,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier =
            modifier
                .background(Color.White)
                .testTag("seller_qr_canvas"),
    ) {
        val moduleCount = modules.size
        val moduleSize = size.width / moduleCount
        for (y in 0 until moduleCount) {
            for (x in 0 until moduleCount) {
                if (modules[y][x]) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(x * moduleSize, y * moduleSize),
                        size = Size(moduleSize, moduleSize),
                    )
                }
            }
        }
    }
}

/** Small glyph reused as the "Show QR" button icon — three finder-pattern corners at icon scale. */
@Composable
private fun QrGlyphIcon(modifier: Modifier = Modifier) {
    QrCodeCanvas(
        modules = simulatedQrModules(seed = "glyph", moduleCount = 15),
        modifier = modifier.size(20.dp),
    )
}

// -------------------------------------------------------------------------------------
// Screen integration — see "3. Screen integration"
// -------------------------------------------------------------------------------------

/** Address field row with the "Show QR" action beside copy; no inline thumbnail. */
@Composable
internal fun SellerAddressRowWithQrAction(
    uiState: SellerQrUiState,
    label: String,
    onAction: (SellerQrUiAction) -> Unit,
) {
    BisqTextFieldV0(
        label = label,
        value = uiState.paymentData ?: "data.na",
        enabled = false,
        trailingIcon =
            if (uiState.paymentData != null) {
                {
                    Row(horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter)) {
                        CopyIconButton(value = uiState.paymentData, showToast = false)
                        IconButton(
                            onClick = { onAction(SellerQrUiAction.OnShowQr) },
                            modifier = Modifier.testTag("seller_qr_show_action"),
                        ) {
                            QrGlyphIcon()
                        }
                    }
                }
            } else {
                null
            },
        bottomMessage = if (uiState.paymentData == null) "The buyer hasn't sent a payout address yet." else null,
        isError = false,
    )
}

// -------------------------------------------------------------------------------------
// Full-screen sheet — see "5. Full-screen sheet layout"
// -------------------------------------------------------------------------------------

@Composable
internal fun SellerQrCodeSheet(
    uiState: SellerQrUiState,
    onAction: (SellerQrUiAction) -> Unit,
) {
    SecureScreenEffect()

    BisqBottomSheet(onDismissRequest = { onAction(SellerQrUiAction.OnCloseQr) }) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BisqUIConstants.ScreenPadding2X)
                    .padding(bottom = BisqUIConstants.ScreenPadding2X),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BisqText.H5Light(text = "Scan QR Code for trade '${uiState.tradeId}'")

            if (uiState.addressChangedNotice) {
                BisqGap.V1()
                AddressChangedBanner(onAcknowledge = { onAction(SellerQrUiAction.OnAcknowledgeAddressChanged) })
            }

            BisqGap.V1()
            if (uiState.paymentData != null) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .widthIn(max = SELLER_QR_MAX_SIZE_DP.dp)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                            .background(Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    QrCodeCanvas(
                        modules = simulatedQrModules(seed = buildQrPayload(uiState), moduleCount = qrModuleCountFor(uiState)),
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(SELLER_QR_QUIET_ZONE_MODULES.dp)
                                .testTag("seller_qr_sheet_canvas"),
                    )
                }
            }

            BisqGap.VHalf()
            BisqText.SmallLight(
                text = amountHintFor(uiState),
                color = BisqTheme.colors.mid_grey20,
            )

            BisqGap.V1()
            BisqText.StyledText(
                text = buildQrPayload(uiState),
                style = BisqTheme.typography.smallLight.copy(fontFamily = FontFamily.Monospace),
                color = BisqTheme.colors.light_grey10,
                modifier = Modifier.testTag("seller_qr_sheet_payload_text"),
            )

            BisqGap.VHalf()
            Row(horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)) {
                CopyIconButton(value = buildQrPayload(uiState), showToast = false)
                BisqText.SmallMedium(text = "Copy", color = BisqTheme.colors.primary)
            }

            BisqGap.V2()
            BisqButton(
                text = "Open in wallet",
                type = BisqButtonType.Outline,
                fullWidth = true,
                onClick = { onAction(SellerQrUiAction.OnOpenInWallet) },
                modifier = Modifier.testTag("seller_qr_open_in_wallet"),
            )

            BisqGap.VHalf()
            BisqButton(
                text = "Close",
                fullWidth = true,
                onClick = { onAction(SellerQrUiAction.OnCloseQr) },
                modifier = Modifier.testTag("seller_qr_close"),
            )
        }
    }
}

@Composable
private fun AddressChangedBanner(onAcknowledge: () -> Unit) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color = BisqTheme.colors.warning.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                ).padding(BisqUIConstants.ScreenPadding)
                .testTag("seller_qr_address_changed_banner"),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        ExclamationRedIcon()
        Column {
            BisqText.SmallMedium(
                text = "The buyer's payment data changed. Scan the code again.",
                color = BisqTheme.colors.warning,
            )
            BisqGap.VQuarter()
            BisqText.SmallLight(
                text = "Dismiss",
                color = BisqTheme.colors.primary,
                modifier =
                    Modifier
                        .clickable(onClick = onAcknowledge)
                        .testTag("seller_qr_address_changed_acknowledge"),
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// Previews
// -------------------------------------------------------------------------------------

/** Screen row with the "Show QR" action, on-chain address present. */
@ExcludeFromCoverage
@Preview(name = "1. Screen — address row, on-chain")
@Composable
private fun SellerAddressRow_OnChain_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            BisqText.H5Light("Send 0.00250000 BTC to the buyer")
            BisqGap.VHalf()
            BisqTextFieldV0(
                label = "Amount to send",
                value = "0.00250000",
                enabled = false,
                trailingIcon = { CopyIconButton(value = "0.00250000", showToast = false) },
            )
            BisqGap.VHalf()
            SellerAddressRowWithQrAction(
                uiState = simulatedSellerQrUiState(),
                label = "Bitcoin address",
                onAction = {},
            )
        }
    }
}

/** Screen row with the "Show QR" action, Lightning invoice present. */
@ExcludeFromCoverage
@Preview(name = "2. Screen — address row, Lightning")
@Composable
private fun SellerAddressRow_Lightning_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            SellerAddressRowWithQrAction(
                uiState =
                    simulatedSellerQrUiState(
                        paymentKind = BitcoinPaymentKind.LIGHTNING,
                        paymentData = SIMULATED_LIGHTNING_INVOICE,
                    ),
                label = "Lightning invoice",
                onAction = {},
            )
        }
    }
}

/** Screen row, address not yet available — "Show QR" disabled, no value to encode. */
@ExcludeFromCoverage
@Preview(name = "3. Screen — address row, address not yet available")
@Composable
private fun SellerAddressRow_AddressNotYetAvailable_Preview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            SellerAddressRowWithQrAction(
                uiState = simulatedSellerQrUiState(paymentData = null),
                label = "Bitcoin address",
                onAction = {},
            )
        }
    }
}

/** Sheet — on-chain: BIP21 code, hint names the trade amount. */
@ExcludeFromCoverage
@Preview(name = "4. Sheet — on-chain", heightDp = 800)
@Composable
private fun SellerQrCodeSheet_OnChain_Preview() {
    BisqTheme.Preview {
        SellerQrCodeSheet(
            uiState = simulatedSellerQrUiState(),
            onAction = {},
        )
    }
}

/** Sheet — Lightning: denser code than the on-chain preview at the same box size. */
@ExcludeFromCoverage
@Preview(name = "5. Sheet — Lightning", heightDp = 850)
@Composable
private fun SellerQrCodeSheet_Lightning_Preview() {
    BisqTheme.Preview {
        SellerQrCodeSheet(
            uiState =
                simulatedSellerQrUiState(
                    paymentKind = BitcoinPaymentKind.LIGHTNING,
                    paymentData = SIMULATED_LIGHTNING_INVOICE,
                ),
            onAction = {},
        )
    }
}

/** Sheet — address changed while open: banner above the code, requires explicit dismiss. */
@ExcludeFromCoverage
@Preview(name = "6. Sheet — address changed mid-trade", heightDp = 850)
@Composable
private fun SellerQrCodeSheet_AddressChanged_Preview() {
    BisqTheme.Preview {
        SellerQrCodeSheet(
            uiState = simulatedSellerQrUiState(addressChangedNotice = true),
            onAction = {},
        )
    }
}

/** Sheet — long payload text wraps across lines, no truncation. */
@ExcludeFromCoverage
@Preview(name = "7. Sheet — long payload text wraps", heightDp = 900)
@Composable
private fun SellerQrCodeSheet_LongPayloadWrap_Preview() {
    BisqTheme.Preview {
        SellerQrCodeSheet(
            uiState =
                simulatedSellerQrUiState(
                    paymentKind = BitcoinPaymentKind.LIGHTNING,
                    paymentData = SIMULATED_LIGHTNING_INVOICE + SIMULATED_LIGHTNING_INVOICE.take(80),
                ),
            onAction = {},
        )
    }
}

/**
 * Sheet — "Open in wallet" returned no handler: an inline notice replaces the usual silent
 * success, matching `UrlLauncher.openUrl`'s existing `Boolean` result.
 */
@ExcludeFromCoverage
@Preview(name = "8. Sheet — no wallet app found", heightDp = 850)
@Composable
private fun SellerQrCodeSheet_NoWalletFound_Preview() {
    BisqTheme.Preview {
        Column {
            SellerQrCodeSheet(
                uiState = simulatedSellerQrUiState(),
                onAction = {},
            )
            BisqText.SmallLight(
                text = "No wallet app found to open this code.",
                color = BisqTheme.colors.warning,
                modifier =
                    Modifier
                        .padding(BisqUIConstants.ScreenPadding)
                        .testTag("seller_qr_no_wallet_found_notice"),
            )
        }
    }
}

/** QR module grid: on-chain vs. Lightning density at the same box size, see "4. QR rendering." */
@ExcludeFromCoverage
@Preview(name = "9. QR module grid — on-chain vs. Lightning density")
@Composable
private fun SimulatedQrModules_DensityComparison_Preview() {
    BisqTheme.Preview {
        Row(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BisqText.SmallLight("On-chain, ${QR_MODULE_COUNT_ON_CHAIN}×$QR_MODULE_COUNT_ON_CHAIN", color = BisqTheme.colors.mid_grey20)
                QrCodeCanvas(
                    modules = simulatedQrModules(seed = "onchain", moduleCount = QR_MODULE_COUNT_ON_CHAIN),
                    modifier = Modifier.size(140.dp),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BisqText.SmallLight("Lightning, ${QR_MODULE_COUNT_LIGHTNING}×$QR_MODULE_COUNT_LIGHTNING", color = BisqTheme.colors.mid_grey20)
                QrCodeCanvas(
                    modules = simulatedQrModules(seed = "lightning", moduleCount = QR_MODULE_COUNT_LIGHTNING),
                    modifier = Modifier.size(140.dp),
                )
            }
        }
    }
}
