package network.bisq.mobile.domain.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files

class EncryptionUtilsTest {

    @Test
    fun encryptWritesHeaderAndDecryptsRoundtrip() {
        val tmpDir = Files.createTempDirectory("encTest").toFile()
        val input = File(tmpDir, "in.zip").apply { writeText("hello world\n12345") }
        val enc = File(tmpDir, "out.enc")
        val password = "pass123"

        encrypt(input, enc, password)
        assertTrue(enc.exists() && enc.length() > 0)

        // Verify header bytes at start
        val headerExpected = "BISQENC|AES256GCM|PBKDF2|v1\n".toByteArray()
        FileInputStream(enc).use { fis ->
            val headerBuf = ByteArray(headerExpected.size)
            val n = fis.read(headerBuf)
            assertEquals(headerExpected.size, n)
            assertTrue(headerBuf.contentEquals(headerExpected))
        }

        // Roundtrip decrypt
        val decrypted = FileInputStream(enc).use { decrypt(it, password) }
        val decryptedText = decrypted.readText()
        assertEquals(input.readText(), decryptedText)
        decrypted.delete()
    }

    @Test
    fun decryptFailsWithoutHeader() {
        val tmpDir = Files.createTempDirectory("encNoHdr").toFile()
        val input = File(tmpDir, "in.zip").apply { writeText("abc") }
        val enc = File(tmpDir, "out.enc")
        val noHdr = File(tmpDir, "out_no_hdr.enc")
        val password = "pass123"

        encrypt(input, enc, password)

        // Remove header bytes to simulate legacy/invalid format
        val headerLen = "BISQENC|AES256GCM|PBKDF2|v1\n".toByteArray().size
        val bytes = enc.readBytes()
        noHdr.writeBytes(bytes.copyOfRange(headerLen, bytes.size))

        assertFailsWith<IOException> {
            FileInputStream(noHdr).use { decrypt(it, password) }
        }
    }
}

