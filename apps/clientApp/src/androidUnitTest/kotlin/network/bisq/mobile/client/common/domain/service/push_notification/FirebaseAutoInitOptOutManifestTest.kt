package network.bisq.mobile.client.common.domain.service.push_notification

import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import network.bisq.mobile.client.common.test_utils.TestApplication
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Regression guard: keeps the privacy opt-out meta-data in the production
 * manifest. If anyone removes one of these flags, Firebase will silently
 * auto-initialize, opening a connection to Google's servers for users who
 * never opted in. This test fails the build before that happens.
 *
 * The list mirrors `AndroidManifest.xml`. Most of the flags target Firebase
 * libraries we don't currently depend on (Analytics, Crashlytics, Performance);
 * they're declared defensively so accidental future inclusion can't leak data.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class)
class FirebaseAutoInitOptOutManifestTest {
    @Test
    fun `production manifest disables Firebase messaging auto-init`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val ai =
            context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA,
            )
        val metaData = ai.metaData ?: error("Application has no meta-data bundle")

        // The critical one — controls whether FCM contacts Google on app launch.
        assertTrue(
            metaData.containsKey("firebase_messaging_auto_init_enabled"),
            "firebase_messaging_auto_init_enabled is missing — FCM will auto-init " +
                "and open a Google connection for users who never opted in",
        )
        assertFalse(
            metaData.getBoolean("firebase_messaging_auto_init_enabled", true),
            "firebase_messaging_auto_init_enabled must be false until the user opts in",
        )
    }

    @Test
    fun `production manifest disables every other Firebase telemetry surface`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val metaData =
            context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                .metaData ?: error("Application has no meta-data bundle")

        // Belt-and-suspenders: even though we don't depend on these libs today,
        // declaring the flags prevents accidental future telemetry leakage.
        listOf(
            "firebase_analytics_collection_enabled",
            "google_analytics_adid_collection_enabled",
            "google_analytics_default_allow_ad_storage",
            "google_analytics_default_allow_analytics_storage",
            "firebase_crashlytics_collection_enabled",
            "firebase_performance_collection_enabled",
        ).forEach { key ->
            assertTrue(metaData.containsKey(key), "$key is missing from the manifest")
            assertFalse(
                metaData.getBoolean(key, true),
                "$key must be false (privacy-first default)",
            )
        }
    }
}
