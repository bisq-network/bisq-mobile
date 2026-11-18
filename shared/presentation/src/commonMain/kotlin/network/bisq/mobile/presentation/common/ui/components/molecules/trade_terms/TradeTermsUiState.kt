package network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms

/** Which treatment [TradeTermsBlock] renders. */
enum class TradeTermsDisplayMode {
    /** Decision screens (take-offer review): a few lines plus an expander when the text overflows. */
    COLLAPSIBLE,

    /** Reading screens (peer profile): always the full text, no expander. */
    FULL,
}

/** A screen holds `null` instead of this when there are no terms to show. */
data class TradeTermsUiState(
    val text: String,
    val displayMode: TradeTermsDisplayMode,
    val isExpanded: Boolean = false,
    /** True when [text] was cut at the screen's cap; the block then says the end is missing. */
    val isTruncated: Boolean = false,
) {
    companion object {
        /**
         * The terms are the counterparty's own words: only trimmed, never translated or parsed.
         * Blank text means the maker set no terms. Text over [maxLength] is cut there and flagged,
         * never in the middle of a surrogate pair.
         */
        fun fromText(
            text: String?,
            displayMode: TradeTermsDisplayMode,
            maxLength: Int? = null,
        ): TradeTermsUiState? {
            val trimmed = text?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            if (maxLength == null || trimmed.length <= maxLength) {
                return TradeTermsUiState(trimmed, displayMode)
            }
            val cut = if (trimmed[maxLength - 1].isHighSurrogate()) maxLength - 1 else maxLength
            return TradeTermsUiState(trimmed.take(cut).trimEnd(), displayMode, isTruncated = true)
        }
    }
}
