package network.bisq.mobile.android.node.service.network.tor

import android.content.Context
import kotlinx.coroutines.flow.StateFlow
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
    }

    private val torConfigDir = File(baseDir, "tor")
    private val torConfigFile = File(torConfigDir, TOR_CONFIG_FILE)

    /**
     * Generate Tor configuration based on Bisq settings
     */
    fun generateTorConfig(
        socksPort: Int = DEFAULT_SOCKS_PORT,
        controlPort: Int = DEFAULT_CONTROL_PORT,
        enableHiddenService: Boolean = true,
        hiddenServicePort: Int = 8000
    ): String {
        val config = StringBuilder()

        // Basic Tor configuration
        config.appendLine("# Bisq Mobile Tor Configuration")
        config.appendLine("# Generated automatically - do not edit manually")
        config.appendLine()

        // Data directory
        config.appendLine("DataDirectory ${torConfigDir.absolutePath}")
        config.appendLine()

        // SOCKS proxy configuration
        config.appendLine("# SOCKS proxy configuration")
        config.appendLine("SocksPort $socksPort")
        config.appendLine("SocksPolicy accept *")
        config.appendLine()

        // Control port configuration
        config.appendLine("# Control port configuration")
        config.appendLine("ControlPort $controlPort")
        config.appendLine("CookieAuthentication 1")
        config.appendLine()

        // Logging configuration
        config.appendLine("# Logging configuration")
        config.appendLine("Log notice file ${File(torConfigDir, "tor.log").absolutePath}")
        config.appendLine("Log notice stdout")
        config.appendLine()

        // Mobile optimizations
        config.appendLine("# Mobile optimizations")
        config.appendLine("ConnLimit 1000")
        config.appendLine("MaxMemInQueues 512 MB")
        config.appendLine("DormantOnFirstStartup 0")
        config.appendLine("DisableNetwork 0")
        config.appendLine()

        // Circuit building optimizations
        config.appendLine("# Circuit building optimizations")
        config.appendLine("CircuitBuildTimeout 30")
        config.appendLine("LearnCircuitBuildTimeout 0")
        config.appendLine("MaxCircuitDirtiness 600")
        config.appendLine()

        // Hidden service configuration for Bisq node
        if (enableHiddenService) {
            config.appendLine("# Hidden service for Bisq node")
            val hiddenServiceDir = File(torConfigDir, "hidden_service")
            config.appendLine("HiddenServiceDir ${hiddenServiceDir.absolutePath}")
            config.appendLine("HiddenServicePort $hiddenServicePort 127.0.0.1:$hiddenServicePort")
            config.appendLine()
        }

        // Security settings
        config.appendLine("# Security settings")
        config.appendLine("AvoidDiskWrites 0")
        config.appendLine("ClientOnly 0")  // Allow hidden services
        config.appendLine()

        // Network settings
        config.appendLine("# Network settings")
        config.appendLine("EnforceDistinctSubnets 1")
        config.appendLine("StrictNodes 0")
        config.appendLine()

        return config.toString()
    }

    /**
     * Write Tor configuration to file
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

    /**
     * Get the path to the Tor configuration file
     */
    fun getTorConfigPath(): String = torConfigFile.absolutePath

    /**
     * Check if Tor configuration exists
     */
    fun hasTorConfig(): Boolean = torConfigFile.exists()

    /**
     * Read existing Tor configuration
     */
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
     * Update Bisq configuration to use Tor
     * Note: With Bisq2, the main configuration is done via external_tor.config file
     */
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

    /**
     * Get hidden service hostname if available
     */
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

    /**
     * Get hidden service private key if available
     */
    fun getHiddenServicePrivateKey(): String? {
        val hiddenServiceDir = File(torConfigDir, "hidden_service")
        val privateKeyFile = File(hiddenServiceDir, "hs_ed25519_secret_key")
        
        return try {
            if (privateKeyFile.exists()) {
                privateKeyFile.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            log.e(e) { "Failed to read hidden service private key" }
            null
        }
    }

    /**
     * Clean up Tor configuration and data
     */
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

    /**
     * Validate Tor configuration
     */
    fun validateTorConfig(config: String): Boolean {
        return try {
            // Basic validation - check for required directives
            val requiredDirectives = listOf("DataDirectory", "SocksPort", "ControlPort")
            val hasAllRequired = requiredDirectives.all { directive ->
                config.contains(directive)
            }
            
            if (!hasAllRequired) {
                log.w { "Tor configuration missing required directives" }
                return false
            }
            
            // Additional validation could be added here
            true
        } catch (e: Exception) {
            log.e(e) { "Failed to validate Tor configuration" }
            false
        }
    }

    /**
     * Get default Tor configuration for Bisq Mobile
     */
    fun getDefaultTorConfig(): String {
        return generateTorConfig(
            socksPort = DEFAULT_SOCKS_PORT,
            controlPort = DEFAULT_CONTROL_PORT,
            enableHiddenService = true,
            hiddenServicePort = 8000
        )
    }
}
