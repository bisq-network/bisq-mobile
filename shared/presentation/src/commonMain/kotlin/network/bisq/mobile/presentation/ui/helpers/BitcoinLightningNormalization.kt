package network.bisq.mobile.presentation.ui.helpers

/**
 * Normalizes Camera scanned Bitcoin/Lightning payloads safely:
 * - Lightning (BOLT11): canonical lowercase ("lightning:" prefix preserved if present)
 * - Bitcoin bech32/bech32m (bc1/tb1/bcrt1): lowercase address (BIP-21 scheme preserved; query kept as-is)
 * - Legacy Base58 (1..., 3...): unchanged (case-sensitive)
 * - Anything else: unchanged
 */
object BitcoinLightningNormalization {
    fun normalizeScan(input: String): String {
        val raw = input.trim()
        if (raw.isEmpty()) return raw

        val lowerPrefixes = listOf("bc1", "tb1", "bcrt1")
        val isBech32 = { s: String -> lowerPrefixes.any { s.startsWith(it, ignoreCase = true) } }

        return when {
            // lightning:... or raw lnbc/lntb/lnbcrt
            raw.startsWith("lightning:", ignoreCase = true) -> {
                "lightning:" + raw.substring(10).lowercase()
            }
            raw.startsWith("lnbc", ignoreCase = true) ||
            raw.startsWith("lntb", ignoreCase = true) ||
            raw.startsWith("lnbcrt", ignoreCase = true) -> raw.lowercase()

            // bitcoin:BIP21 (preserve query case; only lowercase bech32 address part)
            raw.startsWith("bitcoin:", ignoreCase = true) -> {
                val rest = raw.substring(8)
                val addr = rest.substringBefore("?")
                val restQ = rest.substringAfter("?", missingDelimiterValue = "")
                val normAddr = if (isBech32(addr)) addr.lowercase() else addr
                buildString {
                    append("bitcoin:")
                    append(normAddr)
                    if (restQ.isNotEmpty()) append('?').append(restQ)
                }
            }

            // raw bech32 address
            isBech32(raw) -> raw.lowercase()

            // legacy base58 or anything else: leave unchanged
            else -> raw
        }
    }

    /**
     * Cleans a scanned/typed payload for validation:
     * - Applies normalizeScan first (case handling for bech32/BOLT11)
     * - Strips leading scheme (bitcoin:, lightning:) case-insensitively
     * - Drops any leading slashes after the scheme (handles bitcoin:// and lightning://)
     * - Removes query and fragment starting at '?' or '#'
     */
    fun cleanForValidation(input: String): String {
        val normalized = normalizeScan(input)
        val s = normalized.trim()
        val afterScheme = when {
            s.startsWith("bitcoin:", ignoreCase = true) -> s.substringAfter(":")
            s.startsWith("lightning:", ignoreCase = true) -> s.substringAfter(":")
            else -> s
        }
        val withoutSlashes = afterScheme.trim().trimStart('/')
        return withoutSlashes.substringBefore('?').substringBefore('#')
    }

}

