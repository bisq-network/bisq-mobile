package network.bisq.mobile.domain.logging

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The fixture mirrors the shapes a real bisq2 `bisq.log` contains: connection lines with onion
 * peers and key ids, `UserProfile.toString()` dumps, trade ids, hashes and base64 key material.
 */
class LogScrubberTest {
    private val onionA = "ygcd52prbkt5al4yscyj5oythgz65pdbzq2p36nrisxdhzcruueaxwid.onion"
    private val onionB = "pbprrgwwfhuuwzvvjynsolkcrgphxrmnmw357rwjst4ethyz26bshbad.onion"
    private val tradeId = "4c46d7a1-9f3e-4b7a-8c21-0d5e6f7a8b9c"
    private val profileId = "bbb3d54c0adcc93c39de58a0f423a1276c30614b"
    private val hash = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"
    private val nym = "Cruelly-Containable-Tackle-891"
    private val pubKey = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE7Qx3f2kq9h1c7mNb4vP6sTzL8wR0yU5jK2aD9eG3hI1nM4oP7qS0tV8wX2yZ5bC6dE9fG1hJ3kL5mN7oP9q"

    private val fixture =
        """
        Sept-14 12:23:57.883 [Connection.read-TOR-ygcd52p…-0] INFO  b.n.p.n.Connection: Close 'InboundConnection [peerAddress=$onionA:37802, keyId=$tradeId]'
        Sept-14 12:23:58.295 [Connection.read-TOR-zzkamjb…-0] INFO  b.n.p.n.Connection: Open 'OutboundConnection [peerAddress=$onionB:2736]'
        Sept-14 12:23:58.300 [main] INFO  b.u.p.UserProfileService: UserProfile{
                            nickName='Alice Example',
                            nym='$nym',
                            statement='I trade on tuesdays',
                            terms='',
                            pubKeyHash=$profileId,
                            proofOfWork=$hash,
                            pubKey=$pubKey}
        Sept-14 12:23:59.001 [main] INFO  b.t.TradeService: Trade $tradeId with $onionA:37802 moved to state BTC_CONFIRMED
        Sept-14 12:24:00.002 [main] WARN  b.n.Server: peer 192.168.1.20 timed out after 30s, version 2.1.13
        """.trimIndent()

    private fun scrubbed() = LogScrubber().scrub(fixture)

    @Test
    fun `nothing matching the raw identifiers survives`() {
        val out = scrubbed()

        for (raw in listOf(onionA, onionB, tradeId, profileId, hash, nym, pubKey, "Alice Example", "I trade on tuesdays", "192.168.1.20")) {
            assertFalse(out.contains(raw), "still contains $raw in:\n$out")
        }
    }

    @Test
    fun `placeholders are stable within one scrubber and numbered in first-seen order`() {
        val out = scrubbed()

        // Twice as a full host, once resolved from the thread-name prefix on the first line.
        assertEquals(3, Regex("<onion#1>").findAll(out).count(), out)
        assertEquals(1, Regex("<onion#2>").findAll(out).count(), out)
        assertEquals(2, Regex("<uuid#1>").findAll(out).count(), out)
        assertTrue(out.contains("<profile#1>"), out)
        assertTrue(out.contains("<hash#1>"), out)
        assertTrue(out.contains("<nym#1>"), out)
        assertTrue(out.contains("<ip#1>"), out)
        assertTrue(out.contains("<blob#1>"), out)
    }

    @Test
    fun `a truncated onion in a thread name maps to the full host when known and gets its own placeholder otherwise`() {
        val out = scrubbed().lines()

        // Thread name on the same line as the full host: resolved to that host's number.
        assertTrue(out[0].contains("[Connection.read-TOR-<onion#1>\u2026-0]"), out[0])
        // A prefix whose host never appears in full still does not leak.
        assertFalse(out[1].contains("zzkamjb"), out[1])
        assertTrue(out[1].contains("[Connection.read-TOR-<onion-prefix#1>\u2026-0]"), out[1])
    }

    @Test
    fun `a fresh scrubber numbers from one again`() {
        val first = LogScrubber().scrubLine("peer $onionB")
        val second = LogScrubber().scrubLine("peer $onionB")

        assertEquals("peer <onion#1>", first)
        assertEquals(first, second)
    }

    @Test
    fun `line structure timestamps levels and ports are preserved`() {
        val out = scrubbed().lines()

        assertEquals(fixture.lines().size, out.size)
        assertTrue(out[0].startsWith("Sept-14 12:23:57.883 [Connection.read-TOR-"), out[0])
        assertTrue(out[0].contains("peerAddress=<onion#1>:37802"), out[0])
        assertTrue(out.last().contains("version 2.1.13"), out.last())
        assertTrue(out.last().contains("timed out after 30s"), out.last())
    }

    @Test
    fun `profile text fields keep their name and quotes and skip empty values`() {
        val out = scrubbed()

        assertTrue(out.contains("nickName='<nickname#1>'"), out)
        assertTrue(out.contains("statement='<statement#1>'"), out)
        assertTrue(out.contains("terms=''"), out)
    }

    @Test
    fun `a quote inside a profile value does not end the redaction early`() {
        val out = LogScrubber().scrubLine("                    nickName='O'Brien the 2nd',")

        assertEquals("                    nickName='<nickname#1>',", out)
    }

    @Test
    fun `a profile value spanning several lines is redacted in full`() {
        val scrubber = LogScrubber()
        val lines =
            listOf(
                "                    statement='short',",
                "                    terms='Payment within 24h.",
                "No refunds after release; contact $onionA if unsure.",
                "Thanks, Alice',",
                "                    nym='$nym',",
            )

        val out = lines.map { scrubber.scrubLine(it) }

        assertEquals("                    statement='<statement#1>',", out[0])
        assertEquals("                    terms='<terms#1>", out[1])
        assertEquals("<terms#1>", out[2])
        assertEquals("<terms#1>',", out[3])
        assertEquals("                    nym='<nym#1>',", out[4])
        assertFalse(out.joinToString("\n").contains("Alice"))
        assertFalse(out.joinToString("\n").contains(onionA))
    }

    @Test
    fun `a compact single-line profile dump is redacted field by field and does not swallow later records`() {
        val scrubber = LogScrubber()
        val lines =
            listOf(
                "Sept-14 12:24:01.000 [main] INFO  b.u.p.UserProfileService: UserProfile{nickName='Alice', nym='$nym', statement='hi there', terms='', applicationVersion=2.1.13}",
                "Sept-14 12:24:02.000 [main] INFO  b.n.Server: peer $onionA:37802 connected",
                "Sept-14 12:24:03.000 [main] INFO  b.t.TradeService: Trade $tradeId confirmed",
            )

        val out = lines.map { scrubber.scrubLine(it) }

        assertTrue(out[0].contains("nickName='<nickname#1>', nym='<nym#1>', statement='<statement#1>', terms='', applicationVersion=2.1.13}"), out[0])
        assertEquals("Sept-14 12:24:02.000 [main] INFO  b.n.Server: peer <onion#1>:37802 connected", out[1])
        assertEquals("Sept-14 12:24:03.000 [main] INFO  b.t.TradeService: Trade <uuid#1> confirmed", out[2])
    }

    @Test
    fun `an open span ends at the next log record even without a closing quote`() {
        val scrubber = LogScrubber()
        val lines =
            listOf(
                "                    terms='never closed",
                "still part of the terms",
                "Sept-14 12:24:05.000 [main] INFO  b.n.Server: peer $onionA:37802 connected",
            )

        val out = lines.map { scrubber.scrubLine(it) }

        assertEquals("                    terms='<terms#1>", out[0])
        assertEquals("<terms#1>", out[1])
        assertEquals("Sept-14 12:24:05.000 [main] INFO  b.n.Server: peer <onion#1>:37802 connected", out[2])
    }

    @Test
    fun `record start detection matches the logback line shape only`() {
        assertTrue(LogScrubber.isRecordStart("Sept-14 12:23:48.827 [main] INFO  b.n.Server: hi"))
        assertTrue(LogScrubber.isRecordStart("May-02 08:01:02.003 [Connection.read-TOR-abc\u2026-0] WARN  x: y"))
        assertFalse(LogScrubber.isRecordStart("                    nickName='Alice',"))
        assertFalse(LogScrubber.isRecordStart("Thanks, Alice',"))
    }

    @Test
    fun `summary counts distinct values per class`() {
        val scrubber = LogScrubber()
        scrubber.scrub(fixture)

        assertEquals(2, scrubber.summary["onion"])
        assertEquals(1, scrubber.summary["uuid"])
        assertEquals(1, scrubber.summary["profile"])
        assertEquals(1, scrubber.summary["nym"])
        assertTrue(scrubber.summaryLine().startsWith("2 onion"), scrubber.summaryLine())
    }

    @Test
    fun `an empty scrubber reports that nothing needed redaction`() {
        val scrubber = LogScrubber()

        assertEquals("plain line", scrubber.scrubLine("plain line"))
        assertEquals("", scrubber.scrub(""))
        assertEquals("nothing needed redaction", scrubber.summaryLine())
    }

    @Test
    fun `a stack trace with a path and an email is scrubbed like the analytics payloads`() {
        val out = LogScrubber().scrub("java.io.IOException: /Users/alice/bisq/data.db locked by bob@example.com\n\tat a.b.C.run(C.kt:12)")

        assertFalse(out.contains("alice"), out)
        assertFalse(out.contains("bob@example.com"), out)
        assertTrue(out.contains("<path#1>"), out)
        assertTrue(out.contains("<email#1>"), out)
        assertTrue(out.contains("\tat a.b.C.run(C.kt:12)"), out)
    }
}
