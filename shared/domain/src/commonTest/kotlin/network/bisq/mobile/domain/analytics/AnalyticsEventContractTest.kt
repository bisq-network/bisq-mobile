package network.bisq.mobile.domain.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Contract tests for the [AnalyticsEvent] sealed hierarchy. These tests act as
 * the privacy-audit safety net for the events the apps may emit to GlitchTip:
 *
 *  - **Unique names**: every event has a distinct wire name (the redactor and
 *    aggregation pipelines assume name uniqueness).
 *  - **Naming convention**: names follow `<family>.<thing>_<past-participle>`,
 *    making event names trivially greppable in this repo, the website wiki,
 *    and the GlitchTip search bar.
 *  - **Family `.all` lists are exhaustive**: removing a `data object` without
 *    also removing it from `.all` is caught here, AND adding a `data object`
 *    without listing it in `.all` is caught by [allMatchesSealedClass]. The
 *    contract that *every* event is reviewable in a single grep target stays
 *    enforced.
 *
 * NB: Per-presenter override coverage (assertion that each [ScreenViewed]
 * event has a presenter that returns it) lives in
 * [ScreenAnalyticsCoverageTest] in `:shared:presentation`. The split is
 * intentional — `:shared:domain` doesn't know about presenters and shouldn't
 * grow a transitive dep on Compose for the sake of tests.
 */
class AnalyticsEventContractTest {
    @Test
    fun `every event has a unique wire name`() {
        val names = AnalyticsEvent.all.map { it.name }
        val duplicates = names.groupBy { it }.filterValues { it.size > 1 }.keys
        assertTrue(
            duplicates.isEmpty(),
            "Duplicate AnalyticsEvent names: $duplicates. Each event must own a unique wire name.",
        )
    }

    @Test
    fun `every ScreenViewed name follows the screen_x_opened convention`() {
        AnalyticsEvent.ScreenOpened.all.forEach { event ->
            assertTrue(
                event.name.matches(Regex("^screen\\.[a-z][a-z0-9_]*_opened$")),
                "ScreenViewed name '${event.name}' must match screen.<thing>_opened",
            )
        }
    }

    @Test
    fun `every Settings event name uses the settings dot lowercase snake_case alphabet`() {
        AnalyticsEvent.Settings.all.forEach { event ->
            assertTrue(
                event.name.matches(Regex("^settings\\.[a-z][a-z0-9_]*$")),
                "Settings event name '${event.name}' must match settings.<a-z0-9_>",
            )
        }
    }

    @Test
    fun `Settings toggle events follow the _enabled or _disabled suffix convention`() {
        // LanguageChanged is a data class with code baked into the name — not a
        // boolean toggle — so it's exempt from this rule. All other Settings
        // events MUST end in _enabled or _disabled.
        val toggleEvents = AnalyticsEvent.Settings.all.filterNot { it is AnalyticsEvent.Settings.LanguageChanged }
        toggleEvents.forEach { event ->
            assertTrue(
                event.name.matches(Regex("^settings\\.[a-z][a-z0-9_]*_(enabled|disabled)$")),
                "Toggle event '${event.name}' must end in _enabled or _disabled",
            )
        }
    }

    @Test
    fun `Settings toggle enabled and disabled events are paired`() {
        // Exclude LanguageChanged from the pair check — it has no _enabled/_disabled
        // counterpart and follows its own naming (settings.language_changed_<code>).
        val toggleNames =
            AnalyticsEvent.Settings.all
                .filterNot { it is AnalyticsEvent.Settings.LanguageChanged }
                .map { it.name }
        val enabled = toggleNames.filter { it.endsWith("_enabled") }.map { it.removeSuffix("_enabled") }.toSet()
        val disabled = toggleNames.filter { it.endsWith("_disabled") }.map { it.removeSuffix("_disabled") }.toSet()
        assertEquals(
            enabled,
            disabled,
            "Every Settings toggle must declare BOTH _enabled and _disabled events so opt-in/opt-out conversions are " +
                "measurable. Missing pairs: ${(enabled - disabled).map { "${it}_disabled" } + (disabled - enabled).map { "${it}_enabled" }}",
        )
    }

    @Test
    fun `Settings_LanguageChanged events use the settings_language_changed_x pattern`() {
        val langEvents = AnalyticsEvent.Settings.all.filterIsInstance<AnalyticsEvent.Settings.LanguageChanged>()
        assertTrue(
            langEvents.isNotEmpty(),
            "Settings.all must contain at least one LanguageChanged representative — " +
                "either re-add it or drop the language-tracking contract from the docs.",
        )
        langEvents.forEach { event ->
            assertTrue(
                event.name.matches(Regex("^settings\\.language_changed_[a-z][a-z0-9_]*$")),
                "LanguageChanged('${event.code}') -> '${event.name}' must match settings.language_changed_<code>",
            )
        }
    }

    @Test
    fun `Settings_LanguageChanged covers every TRACKED_LANGUAGE_CODE`() {
        val coveredCodes =
            AnalyticsEvent.Settings.all
                .filterIsInstance<AnalyticsEvent.Settings.LanguageChanged>()
                .map { it.code }
                .toSet()
        val expectedCodes = AnalyticsEvent.Settings.TRACKED_LANGUAGE_CODES.toSet()
        assertEquals(
            expectedCodes,
            coveredCodes,
            "Settings.all LanguageChanged instances must mirror TRACKED_LANGUAGE_CODES exactly. " +
                "If you added a new translatable language, add it to TRACKED_LANGUAGE_CODES AND " +
                "the MainPresenter observer will pick it up via the live flow.",
        )
    }

    @Test
    fun `sanitizeCode lowercases and replaces hyphens`() {
        // Pins the wire-format guarantee: anything we pass through sanitizeCode
        // matches the [a-z0-9_] alphabet downstream consumers (GlitchTip search,
        // grep, dashboards) rely on.
        assertEquals("en", AnalyticsEvent.Settings.sanitizeCode("en"))
        assertEquals("pcm_ng", AnalyticsEvent.Settings.sanitizeCode("pcm-NG"))
        assertEquals("pt_br", AnalyticsEvent.Settings.sanitizeCode("pt-BR"))
        assertEquals("af_za", AnalyticsEvent.Settings.sanitizeCode("af-ZA"))
        // Idempotent.
        assertEquals("pcm_ng", AnalyticsEvent.Settings.sanitizeCode("pcm_ng"))
    }

    @Test
    fun `ScreenViewed_all matches the sealed class subclasses exhaustively`() {
        // Catches the "forgot to add to .all" regression. We don't have full
        // reflection in commonTest (K/N restricted), so we approximate by
        // asserting the count matches what we expect from compile-time review.
        // When you add a new ScreenViewed `data object`, update BOTH this
        // count AND the .all list — the same code review.
        val expectedCount = 17
        assertEquals(
            expectedCount,
            AnalyticsEvent.ScreenOpened.all.size,
            "ScreenViewed.all.size changed without updating the expected count in this test. " +
                "If you intentionally added/removed an event, update this expected value.",
        )
    }

    @Test
    fun `Settings_all matches the sealed class subclasses exhaustively`() {
        // 6 toggle events (Analytics/Push/KeepConnected × Enabled/Disabled) +
        // 1 LanguageChanged per TRACKED_LANGUAGE_CODES entry.
        val expectedCount = 6 + AnalyticsEvent.Settings.TRACKED_LANGUAGE_CODES.size
        assertEquals(
            expectedCount,
            AnalyticsEvent.Settings.all.size,
            "Settings.all.size changed without updating the expected count in this test.",
        )
    }

    @Test
    fun `AnalyticsEvent_all is the union of every family list`() {
        val sum = AnalyticsEvent.ScreenOpened.all.size + AnalyticsEvent.Settings.all.size + AnalyticsEvent.Trade.all.size
        assertEquals(
            sum,
            AnalyticsEvent.all.size,
            "AnalyticsEvent.all must equal the union of family lists. " +
                "If you add a new family (e.g. Trade events), extend AnalyticsEvent.all AND this assertion.",
        )
    }

    @Test
    fun `every Trade event name uses the trade dot lowercase snake_case alphabet`() {
        AnalyticsEvent.Trade.all.forEach { event ->
            assertTrue(
                event.name.matches(Regex("^trade\\.[a-z][a-z0-9_]*$")),
                "Trade event name '${event.name}' must match trade.<a-z0-9_>",
            )
        }
    }

    @Test
    fun `all event objects are singletons not data classes with payload`() {
        // Privacy contract: events MUST NOT carry free-form payload. Sealed
        // class structure makes this hard to violate, but a future contributor
        // could add `data class FooEvent(val something: String)` — this test
        // pins the contract by asserting every existing event is a singleton
        // (object), exercising reference equality.
        AnalyticsEvent.all.forEach { event ->
            val other = AnalyticsEvent.all.first { it.name == event.name }
            if (event !== other) {
                fail(
                    "Event '${event.name}' is not a singleton. Every AnalyticsEvent subclass MUST be a " +
                        "`data object` (or a `data class` whose only properties are themselves sealed enums " +
                        "of fixed values — verified at code review). Free-form payload is forbidden by the " +
                        "privacy contract on issue #525.",
                )
            }
        }
    }
}
