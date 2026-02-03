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
        val sorted =
            listOf(
                "1.0.0-alpha",
                "1.0.0-alpha.1",
                "1.0.0-alpha.beta",
                "1.0.0-beta",
                "1.0.0-beta.2",
                "1.0.0-beta.11",
                "1.0.0-rc.1",
                "1.0.0",
                "2.0.0-0.3.7",
                "2.0.0",
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

    @Test
    fun toStringFormatsCorrectly() {
        val version = SemanticVersion(1, 2, 3)
        assertEquals("1.2.3", version.toString())
    }

    @Test
    fun toStringWithPreRelease() {
        val version = SemanticVersion(1, 2, 3, listOf("alpha", "1"))
        assertEquals("1.2.3-alpha.1", version.toString())
    }

    @Test
    fun toStringWithBuildMetadata() {
        val version = SemanticVersion(1, 2, 3, build = listOf("build", "123"))
        assertEquals("1.2.3+build.123", version.toString())
    }

    @Test
    fun toStringWithPreReleaseAndBuild() {
        val version = SemanticVersion(1, 2, 3, listOf("beta"), listOf("456"))
        assertEquals("1.2.3-beta+456", version.toString())
    }

    @Test
    fun fromParsesSimpleVersion() {
        val version = SemanticVersion.from("1.2.3")
        assertEquals(1, version.major)
        assertEquals(2, version.minor)
        assertEquals(3, version.patch)
    }

    @Test
    fun fromThrowsForInvalidFormat() {
        assertFailsWith<IllegalArgumentException> { SemanticVersion.from("1.2") }
        assertFailsWith<IllegalArgumentException> { SemanticVersion.from("1.2.3.4") }
        assertFailsWith<IllegalArgumentException> { SemanticVersion.from("a.b.c") }
    }

    @Test
    fun compareToReturnsZeroForEqual() {
        val v1 = SemanticVersion(1, 2, 3)
        val v2 = SemanticVersion(1, 2, 3)
        assertEquals(0, v1.compareTo(v2))
    }

    @Test
    fun compareToHandlesMajorDifference() {
        val v1 = SemanticVersion(1, 0, 0)
        val v2 = SemanticVersion(2, 0, 0)
        assertTrue(v1 < v2)
    }

    @Test
    fun compareToHandlesMinorDifference() {
        val v1 = SemanticVersion(1, 1, 0)
        val v2 = SemanticVersion(1, 2, 0)
        assertTrue(v1 < v2)
    }

    @Test
    fun compareToHandlesPatchDifference() {
        val v1 = SemanticVersion(1, 0, 1)
        val v2 = SemanticVersion(1, 0, 2)
        assertTrue(v1 < v2)
    }
}
