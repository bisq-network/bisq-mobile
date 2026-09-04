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
        val all: List<AnalyticsEvent> by lazy { ScreenOpened.all + Settings.all + Trade.all + Contact.all }
    }

    /**
     * Trade-protocol analytics — the trading funnel and the health of each user-driven step.
     *
     * Privacy: sealed slugs only, NO trade id / amount / peer / user. We record the STEP and its
     * OUTCOME, never which trade. Cardinality is bounded (see [all]).
     *
     * Four signals:
     *  - [PhaseOpened]: the user VIEWED a phase screen — the funnel of how far users navigate.
     *  - [PhaseReached]: a trade ENTERED a phase (state-based, every open trade). Comparing reached vs
     *    opened volumes surfaces gaps — e.g. a phase trades reach but users never open.
     *  - [Action]: outcome of a user-driven step — [Outcome.CONFIRMED] (the user's own next state
     *    advanced), [Outcome.FAILED] (the request errored), [Outcome.STALLED] (request accepted but the
     *    local state never advanced within the watch window — the "it didn't work" symptom). We only
     *    watch the user's OWN next state, a fast local dispatch — never the counterparty's, which can
     *    legitimately take hours/days.
     *  - lifecycle ([Taken]/[Completed]/[Cancelled]/[Rejected]/[Errored]) for trade volume and
     *    success-vs-fail ratios.
     */
    sealed class Trade(
        name: String,
    ) : AnalyticsEvent(name) {
        enum class Phase(
            val slug: String,
        ) {
            // The number keeps GlitchTip `ORDER BY title` in funnel order and mirrors bisq2's own
            // phase1/2/3 vocabulary; the trailing word makes the slug self-documenting in the dashboard.
            BUYER_1("buyer_1_setup"),
            BUYER_2("buyer_2_fiat"),
            BUYER_3("buyer_3_btc"),
            BUYER_4("buyer_4_complete"),
            SELLER_1("seller_1_setup"),
            SELLER_2("seller_2_fiat"),
            SELLER_3("seller_3_btc"),
            SELLER_4("seller_4_complete"),
        }

        enum class Step(
            val slug: String,
        ) {
            ACCOUNT_DATA("account_data"),
            BTC_ADDRESS("btc_address"),
            FIAT_SENT("fiat_sent"),
            FIAT_RECEIPT("fiat_receipt"),
            BTC_SENT("btc_sent"),
            BTC_RECEIVED("btc_received"),
        }

        enum class Outcome(
            val slug: String,
        ) {
            CONFIRMED("confirmed"),
            FAILED("failed"),
            STALLED("stalled"),
        }

        /**
         * Why the user interrupted (cancelled/rejected) a trade — from the optional single-tap
         * chips on the interrupt confirmation dialog. Fixed enum, never free text.
         * [UNSPECIFIED] means the chips were skipped.
         *
         * The point of the taxonomy: separate liquidity/human interrupts (price moved, changed
         * mind) from app-caused ones ([NO_PROGRESS] — the desync/stuck-trade symptom), so the
         * monthly funnel can attribute non-completion.
         */
        enum class InterruptReason(
            val slug: String,
        ) {
            PEER_UNRESPONSIVE("peer_unresponsive"),
            PRICE_MOVED("price_moved"),
            PAYMENT_METHOD_ISSUE("payment_method_issue"),

            /** The user-visible trade state stopped advancing — the stuck-trade symptom. */
            NO_PROGRESS("no_progress"),

            /** The user found the trade flow too complicated/confusing — the UX-friction signal (#1795). */
            TOO_COMPLEX("too_complex"),
            CHANGED_MIND("changed_mind"),
            OTHER("other"),

            /** The optional reason chips were skipped. */
            UNSPECIFIED("unspecified"),
        }

        /**
         * Time since the trade's last WITNESSED state transition when the user cancelled, bucketed
         * (never raw durations — bounded names only). A cancel after days without a transition is
         * a desync fingerprint; minutes is a human decision.
         *
         * [UNKNOWN] keeps the data honest: the trade DTO carries no per-state timestamps, so only
         * transitions the app itself witnessed can be timed. Witnessed transition times are
         * persisted (`TradeStallClockRepository`), so the clock survives app restarts — [UNKNOWN]
         * remains for trades whose state changed entirely outside any session, whose age is
         * unknowable and MUST NOT masquerade as a short stall.
         */
        enum class StallBucket(
            val slug: String,
        ) {
            /** No transition witnessed this session — age unknowable, not "recent". */
            UNKNOWN("unknown"),
            UNDER_1H("lt_1h"),
            H1_TO_24H("1h_24h"),
            D1_TO_3D("1d_3d"),
            OVER_3D("gt_3d"),
            ;

            companion object {
                private const val HOUR_MS = 60 * 60 * 1000L
                private const val DAY_MS = 24 * HOUR_MS

                fun fromMillis(millis: Long): StallBucket =
                    when {
                        millis < HOUR_MS -> UNDER_1H
                        millis < DAY_MS -> H1_TO_24H
                        millis < 3 * DAY_MS -> D1_TO_3D
                        else -> OVER_3D
                    }
            }
        }

        /** The user VIEWED this phase's screen (view-based; emitted from the trade-detail presenter). */
        data class PhaseOpened(
            val phase: Phase,
        ) : Trade("trade.phase_${phase.slug}_opened")

        /**
         * A trade ENTERED this phase (state-based; emitted from the facade for every open trade,
         * whether or not the user is viewing it). Comparing `reached` vs `opened` volumes surfaces
         * gaps — e.g. trades that reached a step but were never opened.
         */
        data class PhaseReached(
            val phase: Phase,
        ) : Trade("trade.phase_${phase.slug}_reached")

        data class Action(
            val step: Step,
            val outcome: Outcome,
        ) : Trade("trade.${step.slug}_${outcome.slug}")

        data object Taken : Trade("trade.taken")

        data object Completed : Trade("trade.completed")

        /**
         * Reason and stall bucket are baked into the wire name (the [Settings.LanguageChanged]
         * pattern): GlitchTip groups by title and events carry no identity to join on, so one name
         * holding both is the only way to correlate the user's claim against the objective stall —
         * e.g. `changed_mind` with `gt_3d` is a mislabelled stuck trade. `report.py` counts the
         * funnel by `trade.cancelled` PREFIX, so the variants keep aggregating into the existing
         * month-over-month numbers (old app versions' plain `trade.cancelled` included).
         */
        data class Cancelled(
            val reason: InterruptReason,
            val stall: StallBucket,
        ) : Trade("trade.cancelled_${reason.slug}_${stall.slug}")

        /**
         * Reason only — no stall bucket. Rejection happens right after take (phase 1), so
         * time-since-last-transition is nearly always minutes: a bucket would add 28 wire names
         * without signal.
         */
        data class Rejected(
            val reason: InterruptReason,
        ) : Trade("trade.rejected_${reason.slug}")

        /** A protocol/peer error surfaced on the trade (from the trade model's error flows). */
        data object Errored : Trade("trade.errored")

        /**
         * A trade was detected desynced from the peer — parked in INIT past
         * [network.bisq.mobile.domain.utils.TradeOutOfSyncDetector]'s threshold, the stuck-FSM
         * symptom (bisq-network/bisq2 out-of-order eventQueue bug). Once per trade per session;
         * measures how often users hit the situation the out-of-sync recovery pane exists for.
         */
        data object OutOfSyncDetected : Trade("trade.out_of_sync_detected")

        companion object {
            // `by lazy` for the same class-init-cycle reason as [ScreenOpened.all].
            val all: List<Trade> by lazy {
                Phase.entries.map { PhaseOpened(it) } +
                    Phase.entries.map { PhaseReached(it) } +
                    Step.entries.flatMap { step -> Outcome.entries.map { outcome -> Action(step, outcome) } } +
                    InterruptReason.entries.flatMap { reason -> StallBucket.entries.map { Cancelled(reason, it) } } +
                    InterruptReason.entries.map { Rejected(it) } +
                    listOf(Taken, Completed, Errored, OutOfSyncDetected)
            }
        }
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

        data object AutoAddToContactsEnabled : Settings("settings.auto_add_to_contacts_enabled")

        data object AutoAddToContactsDisabled : Settings("settings.auto_add_to_contacts_disabled")

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
                        AutoAddToContactsEnabled,
                        AutoAddToContactsDisabled,
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
                    CommunityHub,
                    CommunityContacts,
                    CommunityDiscussions,
                    CommunitySupport,
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

        // -- Tier C: community ------------------------------------------
        data object CommunityHub : ScreenOpened("screen.community_hub_opened")

        data object CommunityContacts : ScreenOpened("screen.community_contacts_opened")

        /** The Discussions tab of the hub. Both come from `PublicChatPresenter`, one per chat domain. */
        data object CommunityDiscussions : ScreenOpened("screen.community_discussions_opened")

        /** The Support channel, pushed from the hub's pinned quick access or More → Help, not a tab. */
        data object CommunitySupport : ScreenOpened("screen.community_support_opened")
    }

    /**
     * My Contacts. Privacy: the contact list is the user's social graph, so events carry
     * NO peer identity, tag/notes/trust values, or list sizes — action slugs only. Auto-adds
     * performed by bisq2 core (trade/chat) are deliberately NOT mirrored here: they are system
     * behavior and would double as a proxy for trade/chat activity timing. [Added]/[Removed]
     * fire from the user's own Peer Profile actions only.
     */
    sealed class Contact(
        name: String,
    ) : AnalyticsEvent(name) {
        enum class EditedField(
            val slug: String,
        ) {
            TAG("tag"),
            NOTES("notes"),
            TRUST_SCORE("trust_score"),
        }

        enum class FailedAction(
            val slug: String,
        ) {
            ADD("add"),
            REMOVE("remove"),
            EDIT("edit"),
        }

        /** Manual add from Peer Profile. */
        data object Added : Contact("contact.added")

        data object Removed : Contact("contact.removed")

        /**
         * Which annotation fields changed in one Save — never the values. Baked into the wire
         * name (the [Settings.LanguageChanged] pattern) in declaration order, e.g.
         * `contact.details_edited_tag_trust_score`. 7 bounded combinations.
         */
        data class DetailsEdited(
            val fields: Set<EditedField>,
        ) : Contact("contact.details_edited_${fieldsSlug(fields)}")

        /** The More-menu "My Contacts" deep link (tab opens are [ScreenOpened.CommunityContacts]). */
        data object OpenedViaMoreMenu : Contact("contact.opened_via_more_menu")

        /** A contact mutation failed — the in-the-wild bug signal for this feature. */
        data class ActionFailed(
            val action: FailedAction,
        ) : Contact("contact.action_failed_${action.slug}")

        companion object {
            private fun fieldsSlug(fields: Set<EditedField>): String =
                EditedField.entries
                    .filter { it in fields }
                    .joinToString("_") { it.slug }
                    .ifEmpty { "none" }

            // `by lazy` for the same class-init-cycle reason as [ScreenOpened.all].
            val all: List<Contact> by lazy {
                val fieldCombos =
                    (1..7).map { mask ->
                        EditedField.entries.filterIndexed { i, _ -> mask and (1 shl i) != 0 }.toSet()
                    }
                fieldCombos.map { DetailsEdited(it) } +
                    FailedAction.entries.map { ActionFailed(it) } +
                    listOf(Added, Removed, OpenedViaMoreMenu)
            }
        }
    }
}
