package network.bisq.mobile.android.node

import bisq.common.network.Address
import bisq.common.network.TransportConfig
import bisq.common.network.TransportType
import bisq.network.NetworkServiceConfig
import bisq.network.p2p.ServiceNode
import bisq.network.p2p.node.Feature
import bisq.network.p2p.node.authorization.AuthorizationService
import bisq.network.p2p.services.data.inventory.InventoryService
import bisq.network.p2p.services.peer_group.PeerGroupManager
import bisq.network.p2p.services.peer_group.PeerGroupService
import bisq.network.p2p.services.peer_group.exchange.PeerExchangeStrategy
import bisq.network.p2p.services.peer_group.keep_alive.KeepAliveService
import bisq.network.p2p.node.transport.ClearNetTransportService
import bisq.network.p2p.node.transport.I2PTransportService
import com.typesafe.config.Config
import com.typesafe.config.ConfigValueType
import java.nio.file.Path
import java.util.*
import java.util.stream.Collectors

/**
 * Mobile-specific NetworkServiceConfig that handles external Tor configuration properly.
 * This bypasses the validation issues in the original NetworkServiceConfig.
 */
object MobileNetworkServiceConfig {

    /**
     * Factory method to create the actual bisq2 TorTransportConfig
     * This bypasses the need for our own implementation
     */
    private fun createTorTransportConfig(dataDir: Path, config: Config): TransportConfig {
        // Use reflection to create the actual bisq2 TorTransportConfig
        return try {
            val torTransportConfigClass = Class.forName("bisq.network.tor.TorTransportConfig")
            val fromMethod = torTransportConfigClass.getMethod("from", Path::class.java, Config::class.java)
            fromMethod.invoke(null, dataDir, config) as TransportConfig
        } catch (e: Exception) {
            // Fallback to our custom implementation if the bisq2 class is not available
            MobileTorTransportConfig.from(dataDir, config)
        }
    }

    /**
     * Fallback TorTransportConfig implementation for when bisq2's TorTransportConfig is not available
     */
    class MobileTorTransportConfig(
        private val dataDir: Path,
        private val nodePort: Int,
        val bootstrapTimeout: Int,
        val hsUploadTimeout: Int,
        private val nodeSocketTimeout: Int,
        private val userSocketTimeout: Int,
        val testNetwork: Boolean,
        val directoryAuthorities: List<String>,
        private val sendThrottleTime: Int,
        private val receiveThrottleTime: Int,
        val useExternalTor: Boolean,
        val torrcOverrides: Map<String, Any>
    ) : TransportConfig {

        override fun getDataDir(): Path = dataDir
        override fun getDefaultNodePort(): Int = nodePort
        override fun getDefaultNodeSocketTimeout(): Int = nodeSocketTimeout
        override fun getUserNodeSocketTimeout(): Int = userSocketTimeout
        override fun getSendMessageThrottleTime(): Int = sendThrottleTime
        override fun getReceiveMessageThrottleTime(): Int = receiveThrottleTime

        companion object {
            fun from(dataDir: Path, config: Config): MobileTorTransportConfig {
                return MobileTorTransportConfig(
                    dataDir = dataDir,
                    nodePort = if (config.hasPath("defaultNodePort")) {
                        config.getInt("defaultNodePort")
                    } else {
                        8000 // Default Tor port for Bisq
                    },
                    bootstrapTimeout = config.getInt("bootstrapTimeout"),
                    hsUploadTimeout = config.getInt("hsUploadTimeout"),
                    nodeSocketTimeout = config.getInt("defaultNodeSocketTimeout"),
                    userSocketTimeout = config.getInt("userNodeSocketTimeout"),
                    testNetwork = config.getBoolean("testNetwork"),
                    directoryAuthorities = config.getStringList("directoryAuthorities"),
                    sendThrottleTime = config.getInt("sendMessageThrottleTime"),
                    receiveThrottleTime = config.getInt("receiveMessageThrottleTime"),
                    useExternalTor = config.getBoolean("useExternalTor"),
                    torrcOverrides = if (config.hasPath("torrcOverrides")) {
                        config.getConfig("torrcOverrides").entrySet().associate {
                            it.key to it.value.unwrapped()
                        }
                    } else {
                        emptyMap()
                    }
                )
            }
        }
    }



    fun from(baseDir: Path, config: Config): NetworkServiceConfig {
        val serviceNodeConfig = ServiceNode.Config.from(config.getConfig("serviceNode"))
        val inventoryServiceConfig = InventoryService.Config.from(config.getConfig("inventory"))
        val authorizationServiceConfig =
            AuthorizationService.Config.from(config.getConfig("authorization"))

        // Handle seed addresses without validation
        val seedConfig = config.getConfig("seedAddressByTransportType")
        val supportedTransportTypes =
            HashSet(config.getEnumList(TransportType::class.java, "supportedTransportTypes"))

        val seedAddressesByTransport = supportedTransportTypes.associateWith { transportType ->
            getSeedAddressesWithoutValidation(transportType, seedConfig)
        }

        val features = HashSet(config.getEnumList(Feature::class.java, "features"))

        val peerGroupConfig = PeerGroupService.Config.from(config.getConfig("peerGroup"))
        val peerExchangeStrategyConfig =
            PeerExchangeStrategy.Config.from(config.getConfig("peerExchangeStrategy"))
        val keepAliveServiceConfig = KeepAliveService.Config.from(config.getConfig("keepAlive"))

        val defaultConf = PeerGroupManager.Config.from(
            peerGroupConfig,
            peerExchangeStrategyConfig,
            keepAliveServiceConfig,
            config.getConfig("defaultPeerGroup")
        )

        val clearNetConf = PeerGroupManager.Config.from(
            peerGroupConfig,
            peerExchangeStrategyConfig,
            keepAliveServiceConfig,
            config.getConfig("clearNetPeerGroup")
        )

        val peerGroupServiceConfigByTransport = mapOf(
            TransportType.TOR to defaultConf,
            TransportType.I2P to defaultConf,
            TransportType.CLEAR to clearNetConf
        )

        val defaultPortByTransportType = createDefaultPortByTransportType(config)
        val configByTransportType = createConfigByTransportType(config, baseDir)

        return NetworkServiceConfig(
            baseDir.toAbsolutePath().toString(),
            config.getInt("version"),
            supportedTransportTypes,
            features,
            configByTransportType,
            serviceNodeConfig,
            inventoryServiceConfig,
            authorizationServiceConfig,
            peerGroupServiceConfigByTransport,
            defaultPortByTransportType,
            seedAddressesByTransport,
            Optional.empty()
        )
    }

    private fun getSeedAddressesWithoutValidation(
        transportType: TransportType,
        config: Config
    ): Set<Address> {
        return when (transportType) {
            TransportType.TOR -> {
                // Handle the complex structure: tor: [{ external: [...] }]
                if (config.hasPath("tor")) {
                    val torList = config.getList("tor")
                    if (torList.isNotEmpty()) {
                        val firstEntry = torList[0] as com.typesafe.config.ConfigObject
                        val torConfig = firstEntry.toConfig()
                        if (torConfig.hasPath("external")) {
                            torConfig.getStringList("external").stream()
                                .map { Address.fromFullAddress(it) }
                                .collect(Collectors.toSet())
                        } else {
                            emptySet()
                        }
                    } else {
                        emptySet()
                    }
                } else {
                    emptySet()
                }
            }

            TransportType.I2P -> {
                // Handle similar structure for I2P if it exists
                if (config.hasPath("i2p")) {
                    val i2pList = config.getList("i2p")
                    if (i2pList.isNotEmpty()) {
                        val firstEntry = i2pList[0] as com.typesafe.config.ConfigObject
                        val i2pConfig = firstEntry.toConfig()
                        if (i2pConfig.hasPath("external")) {
                            i2pConfig.getStringList("external").stream()
                                .map { Address.fromFullAddress(it) }
                                .collect(Collectors.toSet())
                        } else {
                            emptySet()
                        }
                    } else {
                        emptySet()
                    }
                } else {
                    emptySet()
                }
            }

            TransportType.CLEAR -> {
                // Handle CLEAR addresses - they can be either simple string arrays or nested structures
                if (config.hasPath("clear")) {
                    val clearList = config.getList("clear")
                    if (clearList.isNotEmpty()) {
                        val firstEntry = clearList[0]

                        // Check if it's a simple string (direct address format)
                        if (firstEntry is com.typesafe.config.ConfigValue && firstEntry.valueType() == com.typesafe.config.ConfigValueType.STRING) {
                            // Simple string array format: ["10.0.2.2:8000", "10.0.2.2:8001"]
                            config.getStringList("clear").stream()
                                .map { Address.fromFullAddress(it) }
                                .collect(Collectors.toSet())
                        } else {
                            // Complex nested format: [{ external: [...] }]
                            try {
                                val firstEntryObj = firstEntry as com.typesafe.config.ConfigObject
                                val clearConfig = firstEntryObj.toConfig()
                                if (clearConfig.hasPath("external")) {
                                    clearConfig.getStringList("external").stream()
                                        .map { Address.fromFullAddress(it) }
                                        .collect(Collectors.toSet())
                                } else {
                                    emptySet()
                                }
                            } catch (e: ClassCastException) {
                                // Fallback: treat as empty if we can't parse
                                emptySet()
                            }
                        }
                    } else {
                        emptySet()
                    }
                } else {
                    emptySet()
                }
            }
        }
    }

    private fun createDefaultPortByTransportType(config: Config): Map<TransportType, Int> {
        val map = HashMap<TransportType, Int>()
        val configByTransportType = config.getConfig("configByTransportType")

        if (configByTransportType.hasPath("tor")) {
            val torConfig = configByTransportType.getConfig("tor")
            if (torConfig.hasPath("defaultNodePort")) {
                map[TransportType.TOR] = torConfig.getInt("defaultNodePort")
            }
        }

        if (configByTransportType.hasPath("i2p")) {
            val i2pConfig = configByTransportType.getConfig("i2p")
            if (i2pConfig.hasPath("defaultNodePort")) {
                map[TransportType.I2P] = i2pConfig.getInt("defaultNodePort")
            }
        }

        if (configByTransportType.hasPath("clear")) {
            val clearConfig = configByTransportType.getConfig("clear")
            if (clearConfig.hasPath("defaultNodePort")) {
                map[TransportType.CLEAR] = clearConfig.getInt("defaultNodePort")
            }
        }

        return map
    }

    private fun createConfigByTransportType(
        config: Config,
        baseDir: Path
    ): Map<TransportType, TransportConfig> {
        val map = HashMap<TransportType, TransportConfig>()
        map[TransportType.CLEAR] = createTransportConfig(TransportType.CLEAR, config, baseDir)
        map[TransportType.TOR] = createTransportConfig(TransportType.TOR, config, baseDir)
        map[TransportType.I2P] = createTransportConfig(TransportType.I2P, config, baseDir)
        return map
    }

    private fun createTransportConfig(
        transportType: TransportType,
        config: Config,
        baseDir: Path
    ): TransportConfig {
        val transportConfig =
            config.getConfig("configByTransportType.${transportType.name.lowercase()}")

        return when (transportType) {
            TransportType.TOR -> {
                val dataDir = baseDir.resolve("tor")
                createTorTransportConfig(dataDir, transportConfig)
            }

            TransportType.I2P -> {
                val dataDir = baseDir.resolve("i2p")
                I2PTransportService.Config.from(dataDir, transportConfig)
            }

            TransportType.CLEAR -> {
                ClearNetTransportService.Config.from(baseDir, transportConfig)
            }
        }
    }
}
