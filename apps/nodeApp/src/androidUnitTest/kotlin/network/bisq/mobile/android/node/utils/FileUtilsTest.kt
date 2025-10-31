package network.bisq.mobile.android.node.utils

import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.nio.file.Files

import java.io.FileInputStream

class FileUtilsTest {

    @Test
    fun moveDirReplace_replacesTargetAndRemovesSource_onSuccess() {
        val root = Files.createTempDirectory("moveDirReplaceTest").toFile().apply { deleteOnExit() }

        val sourceDir = File(root, "source").apply { assertTrue(mkdirs()) }
        val targetDir = File(root, "target").apply { assertTrue(mkdirs()) }

        // Prepare source contents
        val srcFile = File(sourceDir, "a.txt").apply { writeText("hello"); deleteOnExit() }
        // Prepare existing target contents
        val oldFile = File(targetDir, "old.txt").apply { writeText("old"); deleteOnExit() }

        moveDirReplace(sourceDir, targetDir)

        assertTrue(targetDir.exists())
        assertTrue(targetDir.isDirectory)
        assertFalse("Old file must be gone after replace", File(targetDir, "old.txt").exists())
        assertTrue("New file must be present after replace", File(targetDir, "a.txt").exists())
        assertEquals("hello", File(targetDir, "a.txt").readText())
        assertFalse("Source directory must be removed after move", sourceDir.exists())
        assertFalse("Temp backup must be cleaned up", File(root, "${targetDir.name}.old").exists())
    }

    @Test
    fun moveDirReplace_throwsWhenSourceMissing() {
        val root = Files.createTempDirectory("moveDirReplaceTestMissing").toFile().apply { deleteOnExit() }
        val sourceDir = File(root, "nonexistent-source")
        val targetDir = File(root, "target").apply { assertTrue(mkdirs()) }

        assertThrows(IllegalArgumentException::class.java) {
            moveDirReplace(sourceDir, targetDir)
        }
    }

    @Test
    fun zipAndUnzip_preservesTopLevelPrivateAndSettings() {
        val root = Files.createTempDirectory("zipUnzipTest").toFile().apply { deleteOnExit() }
        val sourceDb = File(root, "sourceDb").apply { assertTrue(mkdirs()) }
        val privateDir = File(sourceDb, "private").apply { assertTrue(mkdirs()) }
        val settingsDir = File(sourceDb, "settings").apply { assertTrue(mkdirs()) }
        // Populate with a couple of files
        File(privateDir, "wallet.dat").apply { writeText("wallet"); deleteOnExit() }
        File(settingsDir, "config.json").apply { writeText("{}\n"); deleteOnExit() }

        val zipFile = File(root, "backup.zip")
        zipDirectory(sourceDb, zipFile)
        assertTrue(zipFile.exists())

        val unzipTarget = File(root, "unzipped").apply { assertTrue(mkdirs()) }
        FileInputStream(zipFile).use { fis ->
            unzipToDirectory(fis, unzipTarget)
        }

        val outPrivate = File(unzipTarget, "private")
        val outSettings = File(unzipTarget, "settings")
        assertTrue("'private' directory should exist at top-level", outPrivate.exists() && outPrivate.isDirectory)
        assertTrue("'settings' directory should exist at top-level", outSettings.exists() && outSettings.isDirectory)
        assertTrue(File(outPrivate, "wallet.dat").exists())
        assertTrue(File(outSettings, "config.json").exists())
    }
}

