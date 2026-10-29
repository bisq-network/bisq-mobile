package network.bisq.mobile.data.replicated.common.validation

/**
 * Port of bisq2's `bisq.common.validation.RequestedAmountValidation`: the bitcoin payment data a buyer sends may
 * request no amount or exactly the trade amount. The seller's node rejects anything else, so the buyer is told
 * before sending. Must keep matching bisq2's rules (bisq2 `docs/specifications/trade/bisq-easy-bitcoin-payment-data.md`).
 *
 * bisq2 compares with BigInteger/BigDecimal; common Kotlin has neither, so amounts are compared exactly through
 * divisibility and digit strings instead of multiplications that could overflow a Long.
 */
object RequestedAmountValidation {
    // A mainnet BOLT 11 invoice: "lnbc", an optional amount, the separator "1" and the bech32 data part, which does not
    // contain "1", "b", "i" or "o". The pattern only looks ahead at the data part, so that an invoice right after the
    // separator of another one is read too.
    private val lightningInvoice = Regex("lnbc([0-9]*)([munp]?)1(?=[02-9ac-hj-np-z])", RegexOption.IGNORE_CASE)
    private val bolt12OfferOrInvoice = Regex("ln[oi]1[02-9ac-hj-np-z]+", RegexOption.IGNORE_CASE)
    private val bitcoinUriAmount = Regex("[?&]amount=([^&]*)", RegexOption.IGNORE_CASE)

    // Whitespace, control and format characters, which a wallet can remove from the data, and "+", which form decoding
    // turns into a space
    private val removableCharacters = Regex("[\\s\\p{Z}\\p{Cc}\\p{Cf}+]")
    private val bitcoinAmount = Regex("[0-9]+(\\.[0-9]*)?|\\.[0-9]+")

    private const val MAX_DECODING_ROUNDS = 3
    private const val BITCOIN_DECIMALS = 8
    private const val SATOSHI_PER_BITCOIN = 100_000_000L
    private const val MILLISATOSHI_PER_SATOSHI = 1000L

    /**
     * Whether every amount which the payment data requests is the given amount. Data which requests no amount, like a
     * bitcoin address or an invoice without amount, lets the payer enter the amount. An amount which cannot be read
     * counts as another amount. A wallet can percent-decode the data and remove whitespace from it, in any order,
     * before it reads the amount, so each of these readings must request only the given amount.
     */
    fun requestsOnlyAmount(
        paymentData: String,
        amountInSatoshi: Long,
    ): Boolean = readings(paymentData).all { requestsOnlyAmountAsRead(it, amountInSatoshi) }

    // The data as written and percent-decoded up to MAX_DECODING_ROUNDS times, with the removable characters kept or
    // removed before and after each decoding
    private fun readings(paymentData: String): Set<String> {
        val readings = linkedSetOf(paymentData, withoutRemovableCharacters(paymentData))
        repeat(MAX_DECODING_ROUNDS) {
            val decodedReadings = linkedSetOf<String>()
            readings.forEach { reading ->
                val decoded = percentDecode(reading)
                decodedReadings += decoded
                decodedReadings += withoutRemovableCharacters(decoded)
            }
            readings += decodedReadings
        }
        return readings
    }

    private fun withoutRemovableCharacters(value: String): String = removableCharacters.replace(value, "")

    private fun requestsOnlyAmountAsRead(
        paymentData: String,
        amountInSatoshi: Long,
    ): Boolean {
        if (bolt12OfferOrInvoice.containsMatchIn(paymentData)) return false
        if (amountInSatoshi < 0 || amountInSatoshi > Long.MAX_VALUE / MILLISATOSHI_PER_SATOSHI) return false
        val amountInMillisatoshi = amountInSatoshi * MILLISATOSHI_PER_SATOSHI
        val invoicesOk =
            lightningInvoice.findAll(paymentData).all { match ->
                isInvoiceAmountOrNone(match.groupValues[1], match.groupValues[2], amountInMillisatoshi)
            }
        return invoicesOk &&
            bitcoinUriAmount.findAll(paymentData).all { match -> isBitcoinAmount(match.groupValues[1], amountInSatoshi) }
    }

    // BOLT 11: the number has no leading zeros, and with the multiplier p (a tenth of a millisatoshi) it ends with 0.
    private fun isInvoiceAmountOrNone(
        number: String,
        multiplier: String,
        amountInMillisatoshi: Long,
    ): Boolean {
        if (number.isEmpty()) return multiplier.isEmpty()
        if (number.startsWith("0")) return false
        val lowerCaseMultiplier = multiplier.lowercase()
        if (lowerCaseMultiplier == "p") {
            // number / 10 == amount and number % 10 == 0, read on the digits so no value overflows
            return number.endsWith("0") && number.dropLast(1).toLongOrNull() == amountInMillisatoshi
        }
        val millisatoshiPerUnit =
            when (lowerCaseMultiplier) {
                "m" -> 100_000_000L
                "u" -> 100_000L
                "n" -> 100L
                else -> 100_000_000_000L
            }
        // number * unit == amount, without the multiplication; a number too large for a Long is another amount
        val value = number.toLongOrNull() ?: return false
        return amountInMillisatoshi % millisatoshiPerUnit == 0L && value == amountInMillisatoshi / millisatoshiPerUnit
    }

    // BIP 21: the amount is a decimal number of bitcoin
    private fun isBitcoinAmount(
        value: String,
        amountInSatoshi: Long,
    ): Boolean {
        if (!bitcoinAmount.matches(value)) return false
        val wholeDigits = value.substringBefore('.')
        val fractionDigits = value.substringAfter('.', missingDelimiterValue = "")
        // Digits past the eighth decimal are below a satoshi and must be zeros
        if (fractionDigits.drop(BITCOIN_DECIMALS).any { it != '0' }) return false
        val whole = if (wholeDigits.isEmpty()) 0L else wholeDigits.toLongOrNull() ?: return false
        val fraction = fractionDigits.take(BITCOIN_DECIMALS).padEnd(BITCOIN_DECIMALS, '0').toLong()
        if (whole > (Long.MAX_VALUE - fraction) / SATOSHI_PER_BITCOIN) return false
        return whole * SATOSHI_PER_BITCOIN + fraction == amountInSatoshi
    }

    // Decodes each %XX as a byte of UTF-8, as for a URI. Other characters, also "+", stay as they are.
    private fun percentDecode(value: String): String {
        val bytes = ArrayList<Byte>(value.length)
        var index = 0
        while (index < value.length) {
            val char = value[index]
            val high = value.getOrNull(index + 1)?.digitToIntOrNull(16)
            val low = value.getOrNull(index + 2)?.digitToIntOrNull(16)
            if (char == '%' && high != null && low != null) {
                bytes += (high * 16 + low).toByte()
                index += 3
            } else {
                // A surrogate pair is one code point and is encoded together
                val length = if (char.isHighSurrogate() && value.getOrNull(index + 1)?.isLowSurrogate() == true) 2 else 1
                bytes += value.substring(index, index + length).encodeToByteArray().toList()
                index += length
            }
        }
        return bytes.toByteArray().decodeToString()
    }
}
