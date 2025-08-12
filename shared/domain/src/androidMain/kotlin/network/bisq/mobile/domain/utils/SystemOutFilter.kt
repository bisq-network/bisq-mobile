package network.bisq.mobile.domain.utils

import java.io.OutputStream
import java.io.PrintStream

/**
 * Custom PrintStream that filters System.out calls from Bisq2 JARs.
 * This provides more granular control than completely redirecting System.out.
 * 
 * Can be used by any Android app (androidNode, androidClient, etc.) to filter
 * verbose System.out.println() calls that bypass the normal logging framework.
 */
class SystemOutFilter(
    private val originalStream: PrintStream,
    private val isDebugBuild: Boolean,
    private val tag: String = "SystemOutFilter"
) : PrintStream(NullOutputStream()) {

    companion object {
        // Patterns to filter out (case-insensitive)
        // These are common verbose outputs from Bisq2 JARs
        private val FILTER_PATTERNS = listOf(
            // Market price data (verbose)
            "marketPriceByCurrencyMap",
            "MarketPrice{",
            "PriceQuote(",
            "Monetary(",
            "baseSideMonetary=",
            "quoteSideMonetary=",
            "marketPriceProvider=",
            "timestamp=",
            "priceQuote=",
            "displayName=",
            "precision=",
            "lowPrecision=",
            "market=",
            "source=null",

            // Protobuf performance warnings (not critical)
            "Missing inline cache for",
            "getSerializedSize()",
            "bisq.network.protobuf",
            "bisq.chat.protobuf",
            "bisq.offer.protobuf",
            "AuthenticatedData.getSerializedSize",
            "ChatMessage.getSerializedSize",
            "Offer.getSerializedSize",

            // Add more patterns as needed for other verbose Bisq2 outputs
        )

        private val FILTER_REGEX = FILTER_PATTERNS.joinToString("|") { Regex.escape(it) }
            .toRegex(RegexOption.IGNORE_CASE)
        
        /**
         * Sets up System.out/err filtering for the application.
         * Call this early in Application.onCreate() or similar.
         * 
         * @param isDebugBuild Whether this is a debug build
         * @param completeBlockInRelease If true, completely blocks all System.out in release builds.
         *                              If false, uses smart filtering in both debug and release.
         */
        fun setupSystemOutFiltering(
            isDebugBuild: Boolean, 
            completeBlockInRelease: Boolean = true
        ) {
            try {
                val originalOut = System.out
                val originalErr = System.err
                if (!isDebugBuild && completeBlockInRelease) {
                    // Release builds: Complete blocking approach
                    val nullStream = object : OutputStream() {
                        override fun write(b: Int) { /* discard */ }
                        override fun write(b: ByteArray?) { /* discard */ }
                        override fun write(b: ByteArray?, off: Int, len: Int) { /* discard */ }
                    }
                    
                    System.setOut(PrintStream(nullStream))
                    System.setErr(PrintStream(nullStream))

                    // Note: print before redirect so it’s visible
                    originalOut.println("SystemOutFilter: System.out/err completely blocked for release build")
                } else {
                    // Debug builds or smart filtering: Filter specific patterns
                    val filteredOut = SystemOutFilter(originalOut, isDebugBuild, "SystemOut")
                    val filteredErr = SystemOutFilter(originalErr, isDebugBuild, "SystemErr")
                    
                    System.setOut(filteredOut)
                    System.setErr(filteredErr)

                    val mode = if (isDebugBuild) "debug" else "release"
                    // Use the original stream before we redirect it
                    originalOut.println("SystemOutFilter: System.out/err smart filtering enabled for $mode build")
                }
            } catch (e: Exception) {
                // Use System.err for error reporting since System.out might be redirected
                System.err.println("SystemOutFilter: Failed to setup System.out filtering: ${e.message}")
            }
        }
    }

    // normalize all print calls
    override fun print(x: Boolean) = print(x.toString())
    override fun print(x: Char) = print(x.toString())
    override fun print(x: Int) = print(x.toString())
    override fun print(x: Long) = print(x.toString())
    override fun print(x: Float) = print(x.toString())
    override fun print(x: Double) = print(x.toString())
    override fun print(x: CharArray?) = print(x?.concatToString())
    override fun print(x: Any?) = print(x?.toString())
    override fun println(x: Boolean) = println(x.toString())
    override fun println(x: Char) = println(x.toString())
    override fun println(x: Int) = println(x.toString())
    override fun println(x: Long) = println(x.toString())
    override fun println(x: Float) = println(x.toString())
    override fun println(x: Double) = println(x.toString())
    override fun println(x: CharArray?) = println(x?.concatToString())

    override fun println(x: String?) {
        if (shouldFilter(x)) {
            if (isDebugBuild) {
                // In debug builds, show filtered content in the same logcat stream
                originalStream.println("[$tag][FILTERED] $x")
            }
            // In release builds, completely ignore
        } else {
            // Allow non-filtered content through
            originalStream.println(x)
        }
    }

    override fun print(x: String?) {
        if (shouldFilter(x)) {
            if (isDebugBuild) {
                // In debug builds, show filtered content in the same logcat stream
                originalStream.print("[$tag][FILTERED] $x")
            }
        } else {
            originalStream.print(x)
        }
    }

    override fun println() {
        originalStream.println()
    }
    override fun println(x: Any?) {
        val str = x?.toString()
        if (shouldFilter(str)) {
            if (isDebugBuild) {
                // In debug builds, show filtered content in the same logcat stream
                originalStream.println("[$tag][FILTERED] $str")
            }
        } else {
            originalStream.println(x)
        }
    }

    private fun shouldFilter(content: String?): Boolean {
        if (content == null) return false
        return FILTER_REGEX.containsMatchIn(content)
    }
}

/**
 * OutputStream that discards all data written to it.
 */
private class NullOutputStream : OutputStream() {
    override fun write(b: Int) {
        // Do nothing
    }
    
    override fun write(b: ByteArray?) {
        // Do nothing
    }
    
    override fun write(b: ByteArray?, off: Int, len: Int) {
        // Do nothing
    }
}
