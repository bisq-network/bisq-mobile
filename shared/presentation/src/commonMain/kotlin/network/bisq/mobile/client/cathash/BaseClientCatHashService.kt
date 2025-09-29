package network.bisq.mobile.client.cathash

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Clock
import network.bisq.mobile.client.service.user_profile.ClientCatHashService
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.service.BaseService
import network.bisq.mobile.domain.utils.Logging
import network.bisq.mobile.domain.utils.concat
import network.bisq.mobile.domain.utils.toHex
import okio.ByteString.Companion.decodeBase64
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import kotlin.io.encoding.ExperimentalEncodingApi

abstract class BaseClientCatHashService(private val baseDirPath: String) :
    BaseService(), ClientCatHashService<PlatformImage?>, Logging {
    companion object {
        const val MAX_CACHE_SIZE = 500
        const val CATHASH_ICONS_PATH = "db/cache/cat_hash_icons"
    }

    private val fileSystem: FileSystem = FileSystem.SYSTEM

    // In-memory cache with userProfileId as key.
    // Guarded by a non-suspending Mutex tryLock to avoid blocking in non-suspend getImage().
    private val cacheLock = Mutex()
    private val cache = mutableMapOf<String, PlatformImage>()

    protected abstract fun composeImage(paths: Array<String>, size: Int): PlatformImage?
    protected abstract fun writeRawImage(image: PlatformImage, iconFilePath: String)
    protected abstract fun readRawImage(iconFilePath: String): PlatformImage?

    @OptIn(ExperimentalEncodingApi::class)
    override fun getImage(userProfile: UserProfileVO, size: Int): PlatformImage? {
        // We get the data Base 64 encoded form the Webservice/Rest API backend
        // As the data are verified at the network layer, the decodeBase64() cannot return null,
        // but to cover the case we fall back to 0.
        val pubKeyHash = userProfile.networkId.pubKey.hash.decodeBase64()?.toByteArray() ?: ByteArray(0)
        val powSolution = userProfile.proofOfWork.solutionEncoded.decodeBase64()?.toByteArray() ?: ByteArray(0)
        return getImage(
            pubKeyHash,
            powSolution,
            userProfile.avatarVersion,
            size
        )
    }

    override fun getImage(
        pubKeyHash: ByteArray,
        powSolution: ByteArray,
        avatarVersion: Int,
        size: Int
    ): PlatformImage? {
        try {
            val combined = concat(powSolution, pubKeyHash)
            val catHashInput = BigInteger.fromByteArray(combined, sign = Sign.POSITIVE)
            val userProfileId = pubKeyHash.toHex()
            val subPath = "db/cache/cat_hash_icons/v$avatarVersion"
            val iconsDir = baseDirPath.toPath().resolve(subPath.toPath())
            val iconFilePath = iconsDir.resolve("$userProfileId.raw")

            val profileId = pubKeyHash.toHex()
            val cacheKey = "$profileId-v$avatarVersion"
            val useCache = size <= getSizeOfCachedIcons()
            if (useCache) {
                // Fast path: attempt non-blocking cache read; if lock is contested, skip cache read
                if (cacheLock.tryLock()) {
                    try {
                        cache[cacheKey]?.let { return it }
                    } finally {
                        cacheLock.unlock()
                    }
                }

                if (!fileSystem.exists(iconsDir)) {
                    fileSystem.createDirectories(iconsDir)
                }

                if (fileSystem.exists(iconFilePath)) {
                    try {
                        val image = readRawImage(iconFilePath.toString())
                        if (image != null) {
                            if (cacheLock.tryLock()) {
                                try {
                                    if (cache.size < MAX_CACHE_SIZE) {
                                        cache[cacheKey] = image
                                    }
                                } finally {
                                    cacheLock.unlock()
                                }
                            }
                        }
                        return image
                    } catch (e: Exception) {
                        log.e("Error reading image", e)
                    }
                }
            }

            val ts = Clock.System.now().toEpochMilliseconds()
            val bucketConfig = getBucketConfig(avatarVersion)
            val bucketSizes = bucketConfig.bucketSizes
            val buckets = BucketEncoder.encode(catHashInput, bucketSizes)
            val paths: Array<String?> = BucketEncoder.toPaths(buckets, bucketConfig.pathTemplates)
            val pathsList: Array<String> = paths.filterNotNull().toTypedArray()
            val image = composeImage(pathsList, size)
            val passed = Clock.System.now().toEpochMilliseconds() - ts
            if (passed > 100) { // Only log if it takes more than 100ms
                log.i("Creating user profile icon for $userProfileId took $passed ms.")
            }

            if (image != null && useCache) {
                if (cacheLock.tryLock()) {
                    try {
                        if (cache.size < MAX_CACHE_SIZE) {
                            cache[cacheKey] = image
                        }
                    } finally {
                        cacheLock.unlock()
                    }
                }
                writeAsync(image, iconFilePath)
            }
            return image
        } catch (e: Exception) {
            log.e { e.toString() }
            throw e
        }
    }

    open fun getSizeOfCachedIcons(): Int {
        return ClientCatHashService.DEFAULT_SIZE
    }

    private fun writeAsync(image: PlatformImage, iconFilePath: Path) {
        launchIO {
            try {
                writeRawImage(image, iconFilePath.toString())
            } catch (e: Exception) {
                log.e("Error writing image", e)
            }
        }
    }

    fun pruneOutdatedProfileIcons(userProfiles: Collection<UserProfileVO>) {
        if (userProfiles.isEmpty()) return

        val iconsDirectory = baseDirPath.toPath().resolve(CATHASH_ICONS_PATH)
        val versionDirs =
            fileSystem.listOrNull(iconsDirectory)?.filter { fileSystem.metadata(it).isDirectory }
                ?: return

        val userProfilesByVersion = userProfiles.groupBy { it.avatarVersion }

        versionDirs.forEach { versionDir ->
            val version = versionDir.name.removePrefix("v").toIntOrNull() ?: return@forEach
            val fromDisk = fileSystem.list(versionDir).map { it.name }.toSet()
            val fromData =
                userProfilesByVersion[version]?.map { "${it.id}.raw" }?.toSet() ?: emptySet()
            val toRemove = fromDisk - fromData

            log.i("Removing outdated profile icons: $toRemove")
            toRemove.forEach { fileName ->
                val fileToDelete = versionDir.resolve(fileName)
                try {
                    fileSystem.delete(fileToDelete)
                } catch (e: Exception) {
                    log.e("Failed to remove file $fileToDelete", e)
                }
            }
        }
    }

    private fun getBucketConfig(avatarVersion: Int): BucketConfig {
        return when (avatarVersion) {
            0 -> BucketConfigV0()
            else -> throw IllegalArgumentException("Unsupported avatarVersion: $avatarVersion")
        }
    }
}

