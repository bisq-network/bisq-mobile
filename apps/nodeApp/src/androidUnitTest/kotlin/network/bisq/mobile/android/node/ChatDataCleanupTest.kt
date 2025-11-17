package network.bisq.mobile.android.node

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for the chat data cleanup logic that removes corrupted chat files
 * containing deprecated SubDomain enum values from bisq2 versions prior to 2.1.7.
 *
 * This test verifies:
 * 1. Files containing deprecated enum values are deleted
 * 2. Clean files are preserved
 * 3. Private trade chat data is never touched
 * 4. The cleanup handles missing directories gracefully
 */
class ChatDataCleanupTest {

    private lateinit var testRoot: File
    private lateinit var bisqDir: File
    private lateinit var dbDir: File
    private lateinit var cacheDir: File
    private lateinit var privateDir: File

    @Before
    fun setUp() {
        testRoot = Files.createTempDirectory("chatCleanupTest").toFile()
        bisqDir = File(testRoot, "Bisq2_mobile").apply { mkdirs() }
        dbDir = File(bisqDir, "db").apply { mkdirs() }
        cacheDir = File(dbDir, "cache").apply { mkdirs() }
        privateDir = File(dbDir, "private").apply { mkdirs() }
    }

    @After
    fun tearDown() {
        testRoot.deleteRecursively()
    }

    @Test
    fun `cleanup deletes files containing deprecated EVENTS_CONFERENCES enum`() {
        // Given: A file containing deprecated enum value
        val corruptedFile = File(cacheDir, "CommonPublicChatChannelStore")
        corruptedFile.writeText("some data EVENTS_CONFERENCES more data", Charsets.UTF_8)

        // When: Cleanup runs
        runCleanup(cacheDir)

        // Then: File should be deleted
        assertFalse(corruptedFile.exists(), "File with EVENTS_CONFERENCES should be deleted")
    }

    @Test
    fun `cleanup deletes files containing deprecated EVENTS_MEETUPS enum`() {
        // Given: A file containing deprecated enum value
        val corruptedFile = File(cacheDir, "EventsChannelStore")
        corruptedFile.writeText("EVENTS_MEETUPS", Charsets.UTF_8)

        // When: Cleanup runs
        runCleanup(cacheDir)

        // Then: File should be deleted
        assertFalse(corruptedFile.exists(), "File with EVENTS_MEETUPS should be deleted")
    }

    @Test
    fun `cleanup deletes files containing deprecated EVENTS_PODCASTS enum`() {
        // Given: A file containing deprecated enum value
        val corruptedFile = File(cacheDir, "DiscussionChannelStore")
        corruptedFile.writeText("prefix EVENTS_PODCASTS suffix", Charsets.UTF_8)

        // When: Cleanup runs
        runCleanup(cacheDir)

        // Then: File should be deleted
        assertFalse(corruptedFile.exists(), "File with EVENTS_PODCASTS should be deleted")
    }

    @Test
    fun `cleanup deletes files containing deprecated EVENTS_TRADE_EVENTS enum`() {
        // Given: A file containing deprecated enum value
        val corruptedFile = File(cacheDir, "SupportChannelStore")
        corruptedFile.writeText("EVENTS_TRADE_EVENTS", Charsets.UTF_8)

        // When: Cleanup runs
        runCleanup(cacheDir)

        // Then: File should be deleted
        assertFalse(corruptedFile.exists(), "File with EVENTS_TRADE_EVENTS should be deleted")
    }

    @Test
    fun `cleanup deletes files containing ChatChannelDomain_EVENTS`() {
        // Given: A file containing deprecated domain
        val corruptedFile = File(cacheDir, "CommonPublicChatChannelStore")
        corruptedFile.writeText("ChatChannelDomain.EVENTS", Charsets.UTF_8)

        // When: Cleanup runs
        runCleanup(cacheDir)

        // Then: File should be deleted
        assertFalse(corruptedFile.exists(), "File with ChatChannelDomain.EVENTS should be deleted")
    }

    @Test
    fun `cleanup preserves clean files`() {
        // Given: A clean file without deprecated values
        val cleanFile = File(cacheDir, "CommonPublicChatChannelStore")
        cleanFile.writeText("DISCUSSION SUPPORT BISQ_EASY valid data", Charsets.UTF_8)

        // When: Cleanup runs
        runCleanup(cacheDir)

        // Then: File should still exist
        assertTrue(cleanFile.exists(), "Clean file should be preserved")
        assertTrue(cleanFile.readText(Charsets.UTF_8).contains("valid data"), "Clean file content should be intact")
    }

    @Test
    fun `cleanup handles multiple files correctly`() {
        // Given: Mix of corrupted and clean files
        val corruptedFile1 = File(cacheDir, "CommonPublicChatChannelStore")
        corruptedFile1.writeText("EVENTS_CONFERENCES", Charsets.UTF_8)

        val cleanFile = File(cacheDir, "DiscussionChannelStore")
        cleanFile.writeText("DISCUSSION channel data", Charsets.UTF_8)

        val corruptedFile2 = File(cacheDir, "EventsChannelStore")
        corruptedFile2.writeText("EVENTS_MEETUPS", Charsets.UTF_8)

        // When: Cleanup runs
        runCleanup(cacheDir)

        // Then: Only corrupted files should be deleted
        assertFalse(corruptedFile1.exists(), "Corrupted file 1 should be deleted")
        assertFalse(corruptedFile2.exists(), "Corrupted file 2 should be deleted")
        assertTrue(cleanFile.exists(), "Clean file should be preserved")
    }

    @Test
    fun `cleanup does not touch private directory`() {
        // Given: Files in private directory (trade chats)
        val privateFile = File(privateDir, "BisqEasyOpenTradeChannelStore")
        privateFile.writeText("EVENTS_CONFERENCES trade chat data", Charsets.UTF_8)

        // When: Cleanup runs
        runCleanup(cacheDir)

        // Then: Private file should never be touched
        assertTrue(privateFile.exists(), "Private directory files must never be deleted")
        assertTrue(privateFile.readText(Charsets.UTF_8).contains("trade chat data"), "Private file content must be intact")
    }

    @Test
    fun `cleanup handles missing cache directory gracefully`() {
        // Given: Cache directory doesn't exist
        cacheDir.deleteRecursively()

        // When/Then: Cleanup should not throw exception
        runCleanup(cacheDir)
    }

    @Test
    fun `cleanup handles empty cache directory`() {
        // Given: Empty cache directory
        assertTrue(cacheDir.exists())
        assertTrue(cacheDir.listFiles()?.isEmpty() ?: true)

        // When/Then: Cleanup should not throw exception
        runCleanup(cacheDir)
    }

    @Test
    fun `cleanup only processes known public chat files`() {
        // Given: Unknown file in cache directory
        val unknownFile = File(cacheDir, "UnknownStore")
        unknownFile.writeText("EVENTS_CONFERENCES", Charsets.UTF_8)

        // When: Cleanup runs
        runCleanup(cacheDir)

        // Then: Unknown file should not be touched (not in the list)
        assertTrue(unknownFile.exists(), "Unknown files should not be processed")
    }

    /**
     * Simulates the cleanup logic from NodeMainApplication.maybeCleanupCorruptedChatData()
     *
     * Note: This intentionally duplicates the production logic to test behavior independently.
     * This is a one-time migration fix for deprecated SubDomain enums from bisq2 < 2.1.7.
     * Once users upgrade, this code becomes a no-op and is unlikely to change.
     *
     * If the production logic changes significantly, update this test helper accordingly.
     */
    private fun runCleanup(cacheDir: File) {
        try {
            if (!cacheDir.exists()) {
                return
            }

            val deprecatedEnumValues = listOf(
                "EVENTS_CONFERENCES",
                "EVENTS_MEETUPS",
                "EVENTS_PODCASTS",
                "EVENTS_TRADE_EVENTS",
                "ChatChannelDomain.EVENTS"
            )

            val publicChatFiles = listOf(
                "CommonPublicChatChannelStore",
                "DiscussionChannelStore",
                "EventsChannelStore",
                "SupportChannelStore"
            )

            publicChatFiles.forEach { fileName ->
                val file = File(cacheDir, fileName)
                if (file.exists() && file.isFile) {
                    try {
                        val containsDeprecatedValue = file.readText(Charsets.UTF_8).let { content ->
                            deprecatedEnumValues.any { deprecated -> content.contains(deprecated) }
                        }

                        if (containsDeprecatedValue) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        // If we can't read the file, delete it to be safe
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            // Cleanup errors should not crash the app
        }
    }
}

