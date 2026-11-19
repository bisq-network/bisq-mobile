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
) {
    companion object {
        /**
         * The terms are the counterparty's own words: only trimmed, never translated or parsed.
         * Blank text means the maker set no terms. Never shortened: the taker accepts the whole text.
         */
        fun fromText(
            text: String?,
            displayMode: TradeTermsDisplayMode,
        ): TradeTermsUiState? = text?.trim()?.takeIf { it.isNotEmpty() }?.let { TradeTermsUiState(it, displayMode) }
    }
}
