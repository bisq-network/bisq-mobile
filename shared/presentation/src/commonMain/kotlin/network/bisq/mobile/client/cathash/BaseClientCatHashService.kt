package network.bisq.mobile.client.cathash

import com.ionspin.kotlin.bignum.integer.BigInteger
import com.ionspin.kotlin.bignum.integer.Sign
import kotlinx.datetime.Clock
import network.bisq.mobile.client.replicated_model.user.profile.UserProfile
import network.bisq.mobile.client.user_profile.ClientCatHashService
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.utils.Logging
import network.bisq.mobile.utils.concat
import network.bisq.mobile.utils.hexToByteArray
import network.bisq.mobile.utils.toHex
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM

abstract class BaseClientCatHashService(private val baseDirPath: String) :
    ClientCatHashService<PlatformImage?>, Logging {
    companion object {
        const val SIZE_OF_CACHED_ICONS = 60
        const val MAX_CACHE_SIZE = 5000
        const val CATHASH_ICONS_PATH = "db/cache/cat_hash_icons"
    }

    private val fileSystem: FileSystem = FileSystem.SYSTEM
    private val cache = mutableMapOf<BigInteger, PlatformImage>()

    protected abstract fun composeImage(paths: Array<String>, size: Int): PlatformImage?
    protected abstract fun writeRawImage(image: PlatformImage, iconFilePath: String)
    protected abstract fun readRawImage(iconFilePath: String): PlatformImage?

    fun getImage(userProfile: UserProfile, size: Int): PlatformImage? {
        try {
            val pubKeyHash = userProfile.pubKeyHash!!.hexToByteArray()
            return getImage(
                pubKeyHash,
                userProfile.proofOfWork!!.solution,
                userProfile.avatarVersion!!,
                size
            )
        } catch (e: Exception) {
            log.e(e) { "Failed to get image from profile" }
            return null
        }
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

            val useCache = size <= SIZE_OF_CACHED_ICONS
            if (useCache) {
                cache[catHashInput]?.let {
                    return it
                }

                if (!fileSystem.exists(iconsDir)) {
                    fileSystem.createDirectories(iconsDir)
                }

                if (fileSystem.exists(iconFilePath)) {
                    try {
                        val image = readRawImage(iconFilePath.toString())
                        if (image != null && cache.size < MAX_CACHE_SIZE) {
                            cache[catHashInput] = image
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
            val image = composeImage(pathsList, SIZE_OF_CACHED_ICONS * 2)

            val passed = Clock.System.now().toEpochMilliseconds() - ts
            log.i("Creating user profile icon for $userProfileId took $passed ms.")
            if (image != null && useCache && cache.size < MAX_CACHE_SIZE) {
                cache[catHashInput] = image
                try {
                    writeRawImage(image, iconFilePath.toString())
                } catch (e: Exception) {
                    log.e("Error writing image", e)
                }
            }
            return image
        } catch (e: Exception) {
            log.e { e.toString() }
            throw e;
        }
    }

    fun pruneOutdatedProfileIcons(userProfiles: Collection<UserProfile>) {
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

    fun currentAvatarsVersion(): Int = BucketConfig.CURRENT_VERSION

    private fun getBucketConfig(avatarVersion: Int): BucketConfig {
        return when (avatarVersion) {
            0 -> BucketConfigV0()
            else -> throw IllegalArgumentException("Unsupported avatarVersion: $avatarVersion")
        }
    }
}

