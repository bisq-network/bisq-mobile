package network.bisq.mobile.domain.analytics

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.Settings
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import kotlin.test.Test

/**
 * Pins the contract that `AnalyticsSettingsBaseline.emit()` snapshots the
 * user-controlled settings AT the moment the SDK becomes ready and ships them
 * via the existing sealed event types. Without these tests the baseline could
 * silently regress: e.g. someone forgets to wire push, language drops out of
 * the baseline, or the snapshot reads from the wrong service.
 *
 * Each test sets a specific state via relaxed mocks, calls `emit()` once, and
 * verifies the expected event sequence on the analytics service.
 */
class AnalyticsSettingsBaselineTest {
    private data class Fixture(
        val baseline: AnalyticsSettingsBaseline,
        val analytics: AnalyticsService,
        val settingsRepository: SettingsRepository,
        val settingsFlow: MutableStateFlow<Settings>,
    )

    private fun build(
        keepConnectedInBackground: Boolean = false,
        languageCode: String = "en",
        pushEnabled: Boolean = false,
        analyticsBaselineSent: Boolean = false,
    ): Fixture {
        val analytics = mockk<AnalyticsService>(relaxed = true)

        // Push baseline reads from `Settings.pushNotificationsEnabled`, not
        // from the PushNotificationServiceFacade StateFlow (which lags on
        // Client and is permanently false on Node). See the kdoc in
        // AnalyticsSettingsBaseline.emit().
        val settingsFlow =
            MutableStateFlow(
                Settings(
                    analyticsEnabled = true,
                    analyticsPromptSeen = true,
                    keepConnectedInBackground = keepConnectedInBackground,
                    pushNotificationsEnabled = pushEnabled,
                    analyticsBaselineSent = analyticsBaselineSent,
                ),
            )
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.data } returns settingsFlow

        val settingsServiceFacade = mockk<SettingsServiceFacade>(relaxed = true)
        every { settingsServiceFacade.languageCode } returns MutableStateFlow(languageCode)

        val baseline =
            AnalyticsSettingsBaseline(
                analyticsService = analytics,
                settingsRepository = settingsRepository,
                settingsServiceFacade = settingsServiceFacade,
            )
        return Fixture(baseline, analytics, settingsRepository, settingsFlow)
    }

    @Test
    fun `emit fires the four expected baseline events on a fresh opt-in`() =
        runTest {
            val fx = build(languageCode = "en", pushEnabled = false)

            fx.baseline.emit()

            verifySequence {
                fx.analytics.track(AnalyticsEvent.Settings.AnalyticsEnabled)
                fx.analytics.track(AnalyticsEvent.Settings.LanguageChanged("en"))
                fx.analytics.track(AnalyticsEvent.Settings.PushNotificationsDisabled)
                fx.analytics.track(AnalyticsEvent.Settings.KeepConnectedDisabled)
            }
        }

    @Test
    fun `push baseline reads from Settings_pushNotificationsEnabled not the lagging facade StateFlow`() =
        runTest {
            // Regression: previously read `pushNotificationServiceFacade.isPushNotificationsEnabled.value`,
            // which is seeded false and updated asynchronously inside the
            // facade's activate() collector on Client (race) AND permanently
            // false on Node (NoOpPushNotificationServiceFacade never updates).
            // Reading from the already-collected Settings snapshot is the
            // ground truth in both apps.
            val fx = build(pushEnabled = true)

            fx.baseline.emit()

            verify(exactly = 1) { fx.analytics.track(AnalyticsEvent.Settings.PushNotificationsEnabled) }
            verify(exactly = 0) { fx.analytics.track(AnalyticsEvent.Settings.PushNotificationsDisabled) }
        }

    @Test
    fun `emit reflects each setting's actual current value`() =
        runTest {
            val fx =
                build(
                    keepConnectedInBackground = true,
                    languageCode = "es",
                    pushEnabled = true,
                )

            fx.baseline.emit()

            verifySequence {
                fx.analytics.track(AnalyticsEvent.Settings.AnalyticsEnabled)
                fx.analytics.track(AnalyticsEvent.Settings.LanguageChanged("es"))
                fx.analytics.track(AnalyticsEvent.Settings.PushNotificationsEnabled)
                fx.analytics.track(AnalyticsEvent.Settings.KeepConnectedEnabled)
            }
        }

    @Test
    fun `emit normalises bisq2-style raw language codes to canonical form`() =
        runTest {
            // Node's bisq2 may emit `en_US`, `pt_BR`, `pcm` — the baseline must
            // normalise so the event wire name matches the Transifex canonical
            // form (en, pt-BR, pcm-NG).
            val cases =
                mapOf(
                    "en_US" to "en",
                    "pt_BR" to "pt-BR",
                    "pcm" to "pcm-NG",
                    "af_ZA" to "af-ZA",
                )
            for ((raw, expected) in cases) {
                val fx = build(languageCode = raw)
                fx.baseline.emit()
                verify(exactly = 1) {
                    fx.analytics.track(AnalyticsEvent.Settings.LanguageChanged(expected))
                }
            }
        }

    @Test
    fun `emit skips the language event when the code is unrecognised`() =
        runTest {
            // Defensive: if the backend hands us a code we haven't whitelisted
            // (or a typo), we must NOT emit a LanguageChanged event with an
            // unreviewed wire name. The other three baseline events still fire.
            val fx = build(languageCode = "xx-ZZ")

            fx.baseline.emit()

            verifySequence {
                fx.analytics.track(AnalyticsEvent.Settings.AnalyticsEnabled)
                // No LanguageChanged here.
                fx.analytics.track(AnalyticsEvent.Settings.PushNotificationsDisabled)
                fx.analytics.track(AnalyticsEvent.Settings.KeepConnectedDisabled)
            }
        }

    // ============== Opt-in-cycle dedup ==============

    @Test
    fun `emit flips analyticsBaselineSent to true after a successful snapshot`() =
        runTest {
            // Pins the contract: after emit() completes, the flag is persisted
            // via the repository so subsequent cold starts within the same
            // opt-in cycle skip the snapshot. Critical for the cost-reduction
            // intent of Option B (once-per-opt-in, not once-per-cold-start).
            val fx = build(languageCode = "en")

            fx.baseline.emit()

            coVerify(exactly = 1) { fx.settingsRepository.setAnalyticsBaselineSent(true) }
        }

    @Test
    fun `emit is a no-op when analyticsBaselineSent is already true`() =
        runTest {
            // Subsequent cold starts within the same opt-in cycle must NOT
            // re-emit. The opt-out path is what resets the flag — verified
            // separately in SettingsPresenterTest.
            val fx = build(languageCode = "en", analyticsBaselineSent = true)

            fx.baseline.emit()

            verify(exactly = 0) { fx.analytics.track(any<AnalyticsEvent>()) }
            // And we don't pointlessly re-pin the flag either.
            coVerify(exactly = 0) { fx.settingsRepository.setAnalyticsBaselineSent(any()) }
        }

    @Test
    fun `emit twice in the same cycle emits only the first time`() =
        runTest {
            // Defence-in-depth: even if someone calls emit() twice in the same
            // process (which shouldn't happen — onSentryReady is CAS-guarded —
            // but we want the class itself to be safe), the second call is a
            // no-op because the first call wrote the flag.
            val fx = build(languageCode = "fr", pushEnabled = true)

            fx.baseline.emit()
            // Reflect the flag-write back into the flow so the second read
            // sees the persisted value (the relaxed mock doesn't do this
            // automatically — production DataStore would).
            fx.settingsFlow.value = fx.settingsFlow.value.copy(analyticsBaselineSent = true)
            fx.baseline.emit()

            verify(exactly = 1) { fx.analytics.track(AnalyticsEvent.Settings.AnalyticsEnabled) }
            verify(exactly = 1) { fx.analytics.track(AnalyticsEvent.Settings.LanguageChanged("fr")) }
            verify(exactly = 1) { fx.analytics.track(AnalyticsEvent.Settings.PushNotificationsEnabled) }
            verify(exactly = 1) { fx.analytics.track(AnalyticsEvent.Settings.KeepConnectedDisabled) }
        }
}
