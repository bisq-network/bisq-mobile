package network.bisq.mobile.android.node.service.network.tor

import android.content.Context
import network.bisq.mobile.domain.utils.Logging
import java.io.File

/**
 * Manages Tor configuration for Bisq Mobile Android Node
 * Integrates with existing Bisq configuration system
 */
class TorConfigurationManager(
    private val context: Context,
    private val baseDir: File
) : Logging {

    companion object {
        const val DEFAULT_SOCKS_PORT = 9050
        const val DEFAULT_CONTROL_PORT = 9051
        const val TOR_CONFIG_FILE = "torrc"
        const val TOR_CIRCUIT_TIMEOUT = 60
    }

    private val torConfigDir = File(baseDir, "tor")
    private val torConfigFile = File(torConfigDir, TOR_CONFIG_FILE)

    fun readTorConfig(): String? {
        return try {
            if (torConfigFile.exists()) {
                torConfigFile.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to read Tor configuration" }
            null
        }
    }

    /**
     * @throws Exception if writing fails
     */
    fun writeTorConfig(config: String) {
        try {
            torConfigDir.mkdirs()
            torConfigFile.writeText(config)
            log.i { "Tor configuration written to ${torConfigFile.absolutePath}" }
        } catch (e: Exception) {
            log.e(e) { "Failed to write Tor configuration" }
            throw e
        }
    }

    fun hasTorConfig(): Boolean = torConfigFile.exists()

    fun updateBisqConfigForTor(socksPort: Int, enableTor: Boolean = true) {
        log.i { "Updating Bisq configuration for Tor - SOCKS port: $socksPort, enabled: $enableTor" }

        if (enableTor) {
            log.i { "Bisq2 should be configured with:" }
            log.i { "  - Transport type: TOR" }
            log.i { "  - SOCKS proxy: 127.0.0.1:$socksPort" }
            log.i { "  - External Tor: enabled via external_tor.config file" }
            log.i { "  - Configuration method: external_tor.config with UseExternalTor 1" }
        } else {
            log.i { "Bisq2 should be configured with:" }
            log.i { "  - Transport type: CLEAR" }
            log.i { "  - No SOCKS proxy" }
            log.i { "  - External Tor: disabled" }
        }
    }

    fun getHiddenServiceHostname(): String? {
        val hiddenServiceDir = File(torConfigDir, "hidden_service")
        val hostnameFile = File(hiddenServiceDir, "hostname")

        return try {
            if (hostnameFile.exists()) {
                hostnameFile.readText().trim()
            } else {
                null
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to read hidden service hostname" }
            null
        }
    }

    fun cleanup() {
        try {
            if (torConfigDir.exists()) {
                torConfigDir.deleteRecursively()
                log.i { "Tor configuration directory cleaned up" }
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to clean up Tor configuration" }
        }
    }

    fun validateTorConfig(config: String): Boolean {
        return try {
            val requiredDirectives = listOf("DataDirectory", "SocksPort", "ControlPort")
            val hasAllRequired = requiredDirectives.all { directive ->
                config.contains(directive)
            }

            if (!hasAllRequired) {
                log.w { "Tor configuration missing required directives" }
                return false
            }

            // Validate SOCKS policy security
            if (!isSocksPolicySecure(config)) {
                log.w { "Tor configuration has insecure SOCKS policy" }
                return false
            }

            true
        } catch (e: Exception) {
            log.e(e) { "Failed to validate Tor configuration" }
            false
        }
    }



    /**
     * Get device-appropriate connection limit based on available memory
     * @return recommended connection limit for the device
     */
    private fun calculateDeviceAppropriateConnLimit(): Int {
        val runtime = Runtime.getRuntime()
        val maxMemoryMB = runtime.maxMemory() / (1024 * 1024)

        return when {
            maxMemoryMB < 2048 -> 100   // Low-end devices (2GB RAM)
            maxMemoryMB < 4096 -> 250   // Mid-range devices (4GB RAM)
            maxMemoryMB < 8192 -> 500   // High-end devices (8GB RAM)
            else -> 750                  // Premium devices (8GB+ RAM)
        }
    }

    /**
     * Get device-appropriate memory limit for Tor queues
     * @return recommended memory limit in MB
     */
    private fun calculateDeviceAppropriateMemoryLimit(): Int {
        val runtime = Runtime.getRuntime()
        val maxMemoryMB = runtime.maxMemory() / (1024 * 1024)

        return when {
            maxMemoryMB < 2048 -> 64    // Low-end devices (2GB RAM)
            maxMemoryMB < 4096 -> 128   // Mid-range devices (4GB RAM)
            maxMemoryMB < 8192 -> 256   // High-end devices (8GB RAM)
            else -> 512                  // Premium devices (8GB+ RAM)
        }
    }

    /**
     * Log device capabilities and Tor configuration for debugging
     */
    private fun logDeviceCapabilities() {
        val runtime = Runtime.getRuntime()
        val maxMemoryMB = runtime.maxMemory() / (1024 * 1024)
        val connLimit = calculateDeviceAppropriateConnLimit()
        val memLimit = calculateDeviceAppropriateMemoryLimit()

        log.i { "Device Capabilities:" }
        log.i { "  - Max Memory: ${maxMemoryMB}MB" }
        log.i { "  - Recommended ConnLimit: $connLimit" }
        log.i { "  - Recommended MaxMemInQueues: ${memLimit}MB" }
        log.i { "  - Device Category: ${getDeviceCategory(maxMemoryMB)}" }
    }

    /**
     * Get device category based on memory
     */
    private fun getDeviceCategory(maxMemoryMB: Long): String {
        return when {
            maxMemoryMB < 2048 -> "Low-end (2GB RAM)"
            maxMemoryMB < 4096 -> "Mid-range (4GB RAM)"
            maxMemoryMB < 8192 -> "High-end (8GB RAM)"
            else -> "Premium (8GB+ RAM)"
        }
    }

    /**
     * Validate that the SOCKS policy is secure (restricted to localhost only)
     * @param config the Tor configuration string
     * @return true if the SOCKS policy is secure, false otherwise
     */
    private fun isSocksPolicySecure(config: String): Boolean {
        val lines = config.lines()
        val socksPolicyLines = lines.filter { it.trim().startsWith("SocksPolicy") }
        
        // Check for insecure policies
        val hasInsecurePolicy = socksPolicyLines.any { line ->
            line.contains("accept *") || line.contains("accept 0.0.0.0")
        }
        
        if (hasInsecurePolicy) {
            log.w { "Insecure SOCKS policy detected: accepts all connections" }
            return false
        }
        
        // Check for secure localhost-only policies
        val hasLocalhostPolicy = socksPolicyLines.any { line ->
            line.contains("accept 127.0.0.1") || line.contains("accept ::1")
        }
        
        if (!hasLocalhostPolicy) {
            log.w { "No localhost SOCKS policy found" }
            return false
        }
        
        // Check for explicit reject all policy
        val hasRejectAll = socksPolicyLines.any { line ->
            line.contains("reject *")
        }
        
        if (!hasRejectAll) {
            log.w { "No explicit reject all SOCKS policy found" }
            return false
        }
        
        log.d { "SOCKS policy validation passed - secure localhost-only configuration" }
        return true
    }

    /**
     * Get the current SOCKS policy from the configuration for debugging
     * @param config the Tor configuration string
     * @return a list of SOCKS policy lines
     */
    fun getSocksPolicy(config: String): List<String> {
        return config.lines()
            .filter { it.trim().startsWith("SocksPolicy") }
            .map { it.trim() }
    }

    fun getDefaultTorConfig(): String {
        return generateTorConfig(
            socksPort = DEFAULT_SOCKS_PORT,
            controlPort = DEFAULT_CONTROL_PORT,
            enableHiddenService = true,
            hiddenServicePort = 8000
        )
    }

    private fun generateTorConfig(
        socksPort: Int = DEFAULT_SOCKS_PORT,
        controlPort: Int = DEFAULT_CONTROL_PORT,
        enableHiddenService: Boolean = true,
        hiddenServicePort: Int = 8000
    ): String {
        val config = StringBuilder()

        logDeviceCapabilities()

        config.appendLine("# Bisq Mobile Tor Configuration")
        config.appendLine("# Generated automatically - do not edit manually")
        config.appendLine()

        config.appendLine("DataDirectory ${torConfigDir.absolutePath}")
        config.appendLine()

        config.appendLine("# SOCKS proxy configuration")
        config.appendLine("SocksPort $socksPort")
        config.appendLine("# Security: Restrict SOCKS access to localhost only for mobile security")
        config.appendLine("SocksPolicy accept 127.0.0.1/32")
        config.appendLine("SocksPolicy accept ::1/128")
        config.appendLine("SocksPolicy reject *")
        config.appendLine()

        config.appendLine("# Control port configuration")
        config.appendLine("ControlPort $controlPort")
        config.appendLine("CookieAuthentication 1")
        config.appendLine()

        config.appendLine("# Logging configuration")
        config.appendLine("Log notice file ${File(torConfigDir, "tor.log").absolutePath}")
        config.appendLine("Log notice stdout")
        config.appendLine()

        config.appendLine("# Mobile optimizations")
        config.appendLine("ConnLimit ${calculateDeviceAppropriateConnLimit()}")
        config.appendLine("MaxMemInQueues ${calculateDeviceAppropriateMemoryLimit()} MB")
        config.appendLine("DormantOnFirstStartup 0")
        config.appendLine("DisableNetwork 0")
        config.appendLine()

        config.appendLine("# Circuit building optimizations")
        config.appendLine("CircuitBuildTimeout $TOR_CIRCUIT_TIMEOUT")
        config.appendLine("LearnCircuitBuildTimeout 0")
        config.appendLine("MaxCircuitDirtiness 600")
        config.appendLine()

        if (enableHiddenService) {
            config.appendLine("# Hidden service for Bisq node")
            val hiddenServiceDir = File(torConfigDir, "hidden_service")
            config.appendLine("HiddenServiceDir ${hiddenServiceDir.absolutePath}")
            config.appendLine("HiddenServicePort $hiddenServicePort 127.0.0.1:$hiddenServicePort")
            config.appendLine()
        }

        config.appendLine("# Security settings")
        config.appendLine("AvoidDiskWrites 0")
        config.appendLine("ClientOnly 0")
        config.appendLine()

        config.appendLine("# Network settings")
        config.appendLine("EnforceDistinctSubnets 1")
        config.appendLine("StrictNodes 0")
        config.appendLine()

        return config.toString()
    }
}
