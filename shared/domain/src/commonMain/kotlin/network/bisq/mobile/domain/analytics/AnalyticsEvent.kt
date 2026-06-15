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
        val all: List<AnalyticsEvent> by lazy { ScreenOpened.all + Settings.all }
    }

    /**
     * User toggled a Settings switch from the Settings screen. The event name
     * encodes both the toggle identity AND the new state, so there's no
     * separate payload — keeps the privacy contract obvious in event ingest.
     *
     * Carousel-driven analytics opt-in goes through [ScreenOpened.Dashboard]
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

        /**
         * UI language is now [code]. Emitted by `MainPresenter` whenever the
         * observed language flow changes — including the first non-blank value
         * after app launch (auto-detected baseline) AND any subsequent user
         * change via Settings → Language.
         *
         * The code is baked into the wire name via [sanitizeCode] so the event
         * name stays in the `[a-z0-9_]` alphabet (Bisq2 codes like `pcm-NG`
         * and `pt-BR` become `pcm_ng` and `pt_br`). The raw [code] property is
         * kept on the event so downstream tests can assert on the input.
         *
         * Cardinality is bounded by the project's translated languages — see
         * [TRACKED_LANGUAGE_CODES]. The `.all` companion below mirrors that
         * list (1 representative instance per code) so the contract test pins
         * coverage.
         */
        data class LanguageChanged(
            val code: String,
        ) : Settings("settings.language_changed_${sanitizeCode(code)}")

        companion object {
            /**
             * Codes considered "tracked" — pinned to the project's translatable
             * UI languages (`LanguageServiceFacade.DEFAULT_TRANSLATABLE_LANGUAGES`).
             * Adding a new Transifex translation should add its code here so the
             * contract test continues to pin coverage AND so a typo in the
             * `MainPresenter` observer would surface as the test asserting an
             * unknown name.
             *
             * Wire codes are derived via [sanitizeCode] — lowercase, `-` → `_`.
             */
            val TRACKED_LANGUAGE_CODES: List<String> =
                listOf(
                    "af-ZA",
                    "cs",
                    "de",
                    "en",
                    "es",
                    "fr",
                    "hi",
                    "id",
                    "it",
                    "pcm-NG",
                    "pt-BR",
                    "ru",
                    "tr",
                    "vi",
                )

            // See [ScreenViewed.all] kdoc for why `by lazy`.
            val all: List<Settings> by lazy {
                val toggles =
                    listOf(
                        AnalyticsEnabled,
                        AnalyticsDisabled,
                        PushNotificationsEnabled,
                        PushNotificationsDisabled,
                        KeepConnectedEnabled,
                        KeepConnectedDisabled,
                    )
                val languages = TRACKED_LANGUAGE_CODES.map { LanguageChanged(it) }
                toggles + languages
            }

            /**
             * Normalise a Bisq language code (`pcm-NG`, `pt-BR`, …) into the
             * `[a-z0-9_]` alphabet used by analytics event names. Idempotent on
             * already-normalised codes.
             *
             * Public so [LanguageChanged]'s name initialiser can use it AND so
             * other modules' observers can pre-sanitise before emitting if they
             * ever need to (current callers normalise via [normalizeLanguageCode]
             * which already gates against the tracked-codes allowlist).
             */
            fun sanitizeCode(code: String): String = code.lowercase().replace('-', '_')

            /**
             * Translate a raw language code as it might come from bisq2 (`en_US`,
             * `pt_BR`, `pcm`) or any other observable source into the canonical
             * Transifex form expected by [TRACKED_LANGUAGE_CODES]. Returns null
             * for blanks, unrecognised codes, or anything that doesn't survive
             * normalisation — callers MUST drop nulls silently (the privacy
             * contract is sealed events only, and we don't want to emit names
             * for codes we never reviewed).
             *
             * Mirrors `NodeSettingsServiceFacade.normalizeLanguageCode` so the
             * shared analytics observer can apply the same mapping without
             * pulling platform-specific (Android-Java) code into commonMain.
             * Kept terse — see that source for the full domain rationale.
             *
             * Public so callers in `:shared:presentation` (MainPresenter
             * observer) and `:shared:domain` (AnalyticsSettingsBaseline) can
             * share one implementation.
             */
            fun normalizeLanguageCode(code: String): String? {
                if (code.isBlank()) return null
                val withHyphens = code.replace('_', '-')
                val candidate =
                    when {
                        withHyphens == "pcm" -> "pcm-NG"
                        // Bisq2 stores e.g. `en_US`; collapse to `en`. Match ONLY
                        // `en` exactly OR `en-` prefixed locale variants — naive
                        // `startsWith("en")` would silently accept words like
                        // "engine" as English.
                        withHyphens == "en" || withHyphens.startsWith("en-") -> "en"
                        else -> withHyphens
                    }
                return candidate.takeIf { it in TRACKED_LANGUAGE_CODES }
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
    sealed class ScreenOpened(
        name: String,
    ) : AnalyticsEvent(name) {
        companion object Companion {
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
             * subclasses, each of which extends [ScreenOpened] — whose companion
             * is what's currently being initialised). Lazy defers the list build
             * until first read, by which time every subclass is fully loaded.
             */
            val all: List<ScreenOpened> by lazy {
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
        data object Splash : ScreenOpened("screen.splash_opened")

        data object Onboarding : ScreenOpened("screen.onboarding_opened")

        data object UserAgreement : ScreenOpened("screen.user_agreement_opened")

        data object CreateProfile : ScreenOpened("screen.create_profile_opened")

        data object Dashboard : ScreenOpened("screen.dashboard_opened")

        data object OfferbookMarket : ScreenOpened("screen.offerbook_market_opened")

        data object MyTrades : ScreenOpened("screen.my_trades_opened")

        data object Settings : ScreenOpened("screen.settings_opened")

        // -- Tier B: offer wizard funnel -------------------------------
        data object CreateOfferDirection : ScreenOpened("screen.create_offer_direction_opened")

        data object CreateOfferMarket : ScreenOpened("screen.create_offer_market_opened")

        data object CreateOfferAmount : ScreenOpened("screen.create_offer_amount_opened")

        data object CreateOfferPrice : ScreenOpened("screen.create_offer_price_opened")

        data object CreateOfferPaymentMethod : ScreenOpened("screen.create_offer_payment_method_opened")

        data object CreateOfferReview : ScreenOpened("screen.create_offer_review_opened")

        data object TakeOfferAmount : ScreenOpened("screen.take_offer_amount_opened")

        data object TakeOfferPaymentMethod : ScreenOpened("screen.take_offer_payment_method_opened")

        data object TakeOfferReview : ScreenOpened("screen.take_offer_review_opened")
    }
}
