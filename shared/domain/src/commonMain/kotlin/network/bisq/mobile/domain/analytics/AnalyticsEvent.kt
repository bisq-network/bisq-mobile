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
 * ever emit. Add the new event to the family's `.all` list so the regression
 * test in `AnalyticsEventTest` (`shared/domain` commonTest) keeps the privacy
 * audit surface honest.
 *
 * Naming convention: `<category>.<thing>_<past_participle>` (e.g.
 * `screen.dashboard_opened`, `settings.analytics_enabled`). Lowercase,
 * dot+underscore separators only.
 */
sealed class AnalyticsEvent(
    val name: String,
) {
    companion object {
        /**
         * Every declared event across all families. Used by the contract test
         * to assert names are unique and follow the convention.
         */
        val all: List<AnalyticsEvent> by lazy { ScreenViewed.all + Settings.all }
    }

    /**
     * User toggled a Settings switch from the Settings screen. The event name
     * encodes both the toggle identity AND the new state, so there's no
     * separate payload — keeps the privacy contract obvious in event ingest.
     *
     * Carousel-driven analytics opt-in goes through [ScreenViewed.Dashboard]
     * follow-up signals (a user who opts in via carousel will have events
     * starting to appear); we intentionally don't add a carousel-specific
     * event here to avoid two ways to measure the same conversion.
     */
    sealed class Settings(
        name: String,
    ) : AnalyticsEvent(name) {
        data object AnalyticsEnabled : Settings("settings.analytics_enabled")

        data object AnalyticsDisabled : Settings("settings.analytics_disabled")

        data object PushNotificationsEnabled : Settings("settings.push_notifications_enabled")

        data object PushNotificationsDisabled : Settings("settings.push_notifications_disabled")

        data object KeepConnectedEnabled : Settings("settings.keep_connected_enabled")

        data object KeepConnectedDisabled : Settings("settings.keep_connected_disabled")

        companion object {
            // See [ScreenViewed.all] kdoc for why `by lazy`.
            val all: List<Settings> by lazy {
                listOf(
                    AnalyticsEnabled,
                    AnalyticsDisabled,
                    PushNotificationsEnabled,
                    PushNotificationsDisabled,
                    KeepConnectedEnabled,
                    KeepConnectedDisabled,
                )
            }
        }
    }

    /**
     * A screen-level view event. Emitted from [BasePresenter] when an
     * individual presenter overrides `analyticsScreenEvent()`.
     *
     * The override is opt-in per screen — auto-tracking everything would make
     * the audit surface unbounded. Adding a new screen view: declare a new
     * `data object` here, add it to [all], add the override on the presenter.
     * The contract test guarantees the three stay in sync.
     */
    sealed class ScreenViewed(
        name: String,
    ) : AnalyticsEvent(name) {
        companion object {
            /**
             * Exhaustive list of declared ScreenViewed events. Source of truth for
             * the contract test, which verifies every declared event has a presenter
             * override that returns it (and vice versa).
             *
             * If you add a `data object` below, add it here too — the test will
             * tell you to.
             *
             * `by lazy` is load-bearing: a strict `val = listOf(...)` triggers a
             * JVM class-init cycle (the companion's init references the sealed
             * subclasses, each of which extends [ScreenViewed] — whose companion
             * is what's currently being initialised). Lazy defers the list build
             * until first read, by which time every subclass is fully loaded.
             */
            val all: List<ScreenViewed> by lazy {
                listOf(
                    Splash,
                    Onboarding,
                    UserAgreement,
                    CreateProfile,
                    Dashboard,
                    OfferbookMarket,
                    MyTrades,
                    Settings,
                    CreateOfferDirection,
                    CreateOfferMarket,
                    CreateOfferAmount,
                    CreateOfferPrice,
                    CreateOfferPaymentMethod,
                    CreateOfferReview,
                    TakeOfferAmount,
                    TakeOfferPaymentMethod,
                    TakeOfferReview,
                )
            }
        }

        // -- Tier A: core funnel spine ---------------------------------
        data object Splash : ScreenViewed("screen.splash_opened")

        data object Onboarding : ScreenViewed("screen.onboarding_opened")

        data object UserAgreement : ScreenViewed("screen.user_agreement_opened")

        data object CreateProfile : ScreenViewed("screen.create_profile_opened")

        data object Dashboard : ScreenViewed("screen.dashboard_opened")

        data object OfferbookMarket : ScreenViewed("screen.offerbook_market_opened")

        data object MyTrades : ScreenViewed("screen.my_trades_opened")

        data object Settings : ScreenViewed("screen.settings_opened")

        // -- Tier B: offer wizard funnel -------------------------------
        data object CreateOfferDirection : ScreenViewed("screen.create_offer_direction_opened")

        data object CreateOfferMarket : ScreenViewed("screen.create_offer_market_opened")

        data object CreateOfferAmount : ScreenViewed("screen.create_offer_amount_opened")

        data object CreateOfferPrice : ScreenViewed("screen.create_offer_price_opened")

        data object CreateOfferPaymentMethod : ScreenViewed("screen.create_offer_payment_method_opened")

        data object CreateOfferReview : ScreenViewed("screen.create_offer_review_opened")

        data object TakeOfferAmount : ScreenViewed("screen.take_offer_amount_opened")

        data object TakeOfferPaymentMethod : ScreenViewed("screen.take_offer_payment_method_opened")

        data object TakeOfferReview : ScreenViewed("screen.take_offer_review_opened")
    }
}
