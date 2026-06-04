package network.bisq.mobile.domain.analytics

/**
 * The complete, enumerated set of events the apps may emit to GlitchTip.
 *
 * **Why sealed.** Per the privacy agreement on bisq-network/bisq-mobile#525,
 * analytics must NEVER carry trade amounts, prices, payment-method content,
 * counterparty identities, user inputs, or any free-form payload. A `track(name,
 * props: Map<String, Any>)` overload makes accidental leakage one typo away;
 * a sealed hierarchy makes it structurally impossible.
 *
 * **Why per-event subclass instead of a single class with an enum field.**
 * Future events will carry small typed payloads (e.g. milestone names from a
 * fixed list, feature identifiers). Subclasses let each event declare exactly
 * the scope properties it needs, all reviewable in diff.
 *
 * **Adding an event.** One line: a `data object` (or `data class` if it
 * legitimately needs typed payload from a fixed enum). Code review then has a
 * single grep target — `AnalyticsEvent` — to audit the universe of what we
 * ever emit.
 *
 * Naming convention: `<category>.<thing>_<past_participle>` for screen-views
 * (e.g. `screen.dashboard_opened`), `<feature>.<action>` for custom events
 * (e.g. `trade.step_reached`). Lowercase, dot+underscore separators only.
 */
sealed class AnalyticsEvent(
    val name: String,
) {
    /**
     * A screen-level view event. Emitted from [BasePresenter] when an
     * individual presenter overrides `analyticsScreenEvent()`.
     *
     * Default in [BasePresenter] is `null` — auto-tracking everything would
     * make the audit surface unbounded; we opt-in per screen. First test of sentry-lib
     * registers exactly one ([Dashboard]) to verify ingestion end-to-end.
     * TODO: add the rest of the screen views events
     */
    sealed class ScreenViewed(
        name: String,
    ) : AnalyticsEvent(name) {
        data object Dashboard : ScreenViewed("screen.dashboard_opened")
    }
}
