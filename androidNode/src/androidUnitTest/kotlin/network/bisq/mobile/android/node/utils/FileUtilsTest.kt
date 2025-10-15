package network.bisq.mobile.android.node.utils

import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.nio.file.Files

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
}

