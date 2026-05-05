package network.bisq.mobile.data.replicated.common.monetary

/**
 * Bisq2 stores fiat monetary values as `Long` in scaled (minor-unit) form.
 *
 * The `bisq.common.monetary.Fiat` class uses a fixed `smallestUnitExponent = 4`,
 * so a fiat amount like 8,975.07 ARS is transmitted as `89_750_700`. Dividing the
 * raw long by [FIAT_SCALE_FACTOR] (= 10^4) recovers the human-readable decimal.
 *
 * **Caveat:** this constant is correct only for fiat. BTC base amounts use
 * `smallestUnitExponent = 8` (satoshis) — use [NumberFormatter.btcFormat] for those,
 * or the per-instance precision exposed by [MonetaryVO] / [PriceQuoteVO] when one
 * is available (preferred for new code).
 */
const val FIAT_SCALE_FACTOR: Double = 10_000.0

/** Convert a scaled fiat long (e.g. server-side `quoteAmount`) into a plain decimal value. */
fun Long.fiatToDecimal(): Double = this.toDouble() / FIAT_SCALE_FACTOR
