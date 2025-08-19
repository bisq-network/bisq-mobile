package network.bisq.mobile.domain.utils

import com.ionspin.kotlin.bignum.integer.toBigInteger

/**
 * Semantic Version per https://semver.org/spec/v2.0.0.html
 */
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val preRelease: List<String> = emptyList(),
    val build: List<String> = emptyList()
) : Comparable<SemanticVersion> {

    override fun compareTo(other: SemanticVersion): Int {
        major.compareTo(other.major).let { if (it != 0) return it }
        minor.compareTo(other.minor).let { if (it != 0) return it }
        patch.compareTo(other.patch).let { if (it != 0) return it }

        val aHasPre = preRelease.isNotEmpty()
        val bHasPre = other.preRelease.isNotEmpty()
        if (aHasPre && !bHasPre) return -1
        if (!aHasPre && bHasPre) return 1
        if (!aHasPre) return 0

        val len = minOf(preRelease.size, other.preRelease.size)
        for (i in 0 until len) {
            val a = preRelease[i]
            val b = other.preRelease[i]
            val aNum = a.isNumeric()
            val bNum = b.isNumeric()
            if (aNum && bNum) {
                val cmp = a.toBigInteger().compareTo(b.toBigInteger())
                if (cmp != 0) return cmp
            } else if (aNum != bNum) {
                return if (aNum) -1 else 1
            } else {
                val cmp = a.compareTo(b)
                if (cmp != 0) return cmp
            }
        }
        return preRelease.size.compareTo(other.preRelease.size)
    }

    override fun toString(): String {
        val core = "$major.$minor.$patch"
        val pre = if (preRelease.isNotEmpty()) "-${preRelease.joinToString(".")}" else ""
        val bld = if (build.isNotEmpty()) "+${build.joinToString(".")}" else ""
        return "$core$pre$bld"
    }

    companion object {
        private val corePattern = Regex("^(0|[1-9]\\d*)\$")
        private val idPattern = Regex("^[0-9A-Za-z-]+\$")
        private val numericPattern = Regex("^(0|[1-9]\\d*)\$")


        /** Simple creator from MAJOR.MINOR.PATCH only (no validation for semver extras) */
        fun from(version: String): SemanticVersion {
            val v = version.trim()
            val parts = v.split(".")
            require(parts.size == 3) { "Version must have format MAJOR.MINOR.PATCH" }
            require(parts.all { corePattern.matches(it) }) { "Invalid version format: $v" }
            return SemanticVersion(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
        }

        fun parse(input: String): SemanticVersion {
            val s = input.trim()
            require(s.isNotBlank()) { "Version string cannot be blank" }
            val (coreAndPre, buildMeta) = s.split("+", limit = 2).let {
                it[0] to it.getOrNull(1)
            }
            val build = buildMeta?.splitBuild() ?: emptyList()

            val (core, pre) = coreAndPre.split("-", limit = 2).let {
                it[0] to it.getOrNull(1)
            }
            val preRelease = pre?.splitPreRelease() ?: emptyList()

            val coreParts = core.split(".")
            require(coreParts.size == 3 &&
                    coreParts.all { corePattern.matches(it) }
            ) { "Invalid core version: $s" }

            return SemanticVersion(
                major = coreParts[0].toInt(),
                minor = coreParts[1].toInt(),
                patch = coreParts[2].toInt(),
                preRelease = preRelease,
                build = build
            )
        }

        val SEMVER_ORDER: Comparator<String> = Comparator { a, b ->
            parse(a).compareTo(parse(b))
        }

        fun compare(a: String, b: String): Int =
            parse(a).compareTo(parse(b))

        private fun String.splitPreRelease(): List<String> =
            split(".").map {
                require(idPattern.matches(it)) { "Invalid pre-release identifier: $it" }
                if (numericPattern.matches(it) && it.length > 1 && it.startsWith("0")) {
                    throw IllegalArgumentException(
                        "Numeric pre-release identifier must not have leading zeros: $it"
                    )
                }
                it
            }

        private fun String.splitBuild(): List<String> =
            split(".").map {
                require(idPattern.matches(it)) { "Invalid build identifier: $it" }
                it
            }

        private fun String.isNumeric(): Boolean =
            numericPattern.matches(this)
    }
}
