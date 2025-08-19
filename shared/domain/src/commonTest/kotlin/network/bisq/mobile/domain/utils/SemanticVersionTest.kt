package network.bisq.mobile.domain.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SemanticVersionTest {

    @Test
    fun basicOrdering() {
        assertTrue(SemanticVersion.compare("1.0.0", "2.0.0") < 0)
        assertTrue(SemanticVersion.compare("2.1.0", "2.1.1") < 0)
        assertTrue(SemanticVersion.compare("2.1.1", "2.1.0") > 0)
        assertEquals(0, SemanticVersion.compare("1.2.3", "1.2.3"))
    }

    @Test
    fun preReleaseVsNormal() {
        assertTrue(SemanticVersion.compare("1.0.0-alpha", "1.0.0") < 0)
        assertTrue(SemanticVersion.compare("1.0.0", "1.0.0-alpha") > 0)
    }

    @Test
    fun preReleaseOrdering() {
        assertTrue(SemanticVersion.compare("1.0.0-alpha", "1.0.0-alpha.1") < 0)
        assertTrue(SemanticVersion.compare("1.0.0-alpha.1", "1.0.0-alpha.beta") < 0)
        assertTrue(SemanticVersion.compare("1.0.0-alpha.beta", "1.0.0-beta") < 0)
        assertTrue(SemanticVersion.compare("1.0.0-beta", "1.0.0-beta.2") < 0)
        assertTrue(SemanticVersion.compare("1.0.0-beta.2", "1.0.0-beta.11") < 0)
        assertTrue(SemanticVersion.compare("1.0.0-beta.11", "1.0.0-rc.1") < 0)
        assertTrue(SemanticVersion.compare("1.0.0-rc.1", "1.0.0") < 0)
    }

    @Test
    fun buildMetadataIgnored() {
        assertEquals(0, SemanticVersion.compare("1.0.0+build1", "1.0.0+build2"))
        assertEquals(0, SemanticVersion.compare("1.0.0-alpha+xyz", "1.0.0-alpha+abc"))
    }

    @Test
    fun numericVsAlphaPreRelease() {
        assertTrue(SemanticVersion.compare("1.0.0-1", "1.0.0-alpha") < 0)
    }

    @Test
    fun listSortingMatchesSemverSpec() {
        val sorted = listOf(
            "1.0.0-alpha",
            "1.0.0-alpha.1",
            "1.0.0-alpha.beta",
            "1.0.0-beta",
            "1.0.0-beta.2",
            "1.0.0-beta.11",
            "1.0.0-rc.1",
            "1.0.0",
            "2.0.0-0.3.7",
            "2.0.0"
        )
        val shuffled = sorted.shuffled().sortedWith(SemanticVersion.SEMVER_ORDER)
        assertEquals(sorted, shuffled)
    }

    @Test
    fun invalidVersionsThrow() {
        assertFailsWith<IllegalArgumentException> { SemanticVersion.parse("1") }
        assertFailsWith<IllegalArgumentException> { SemanticVersion.parse("1.0") }
        assertFailsWith<IllegalArgumentException> { SemanticVersion.parse("1.0.0-alpha..1") }
        assertFailsWith<IllegalArgumentException> { SemanticVersion.parse("a.b.c") }
        assertFailsWith<IllegalArgumentException> { SemanticVersion.parse("1.0.0-01") }
    }

    @Test
    fun buildMetadataAllowsLeadingZeros() {
        // Per SemVer 2.0.0, build metadata may contain numeric identifiers with leading zeros
        val version = SemanticVersion.parse("1.0.0+001")
        assertEquals(listOf("001"), version.build)
    }
}
