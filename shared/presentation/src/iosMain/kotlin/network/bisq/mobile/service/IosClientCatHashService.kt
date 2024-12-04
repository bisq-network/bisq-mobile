package network.bisq.mobile.service

import kotlinx.io.IOException
import network.bisq.mobile.client.cathash.BaseClientCatHashService
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.utils.getFilesDir
import platform.Foundation.*
import platform.UIKit.*

const val PATH_TO_DRAWABLE ="compose-resources/composeResources/bisqapps.shared.presentation.generated.resources/drawable/"
const val CAT_HASH_PATH = PATH_TO_DRAWABLE + "cathash/"

class IosClientCatHashService : BaseClientCatHashService("${getFilesDir()}/Bisq2_mobile") {

    override fun composeImage(paths: Array<String>, size: Int): PlatformImage? {
        return IosImageUtil.composeImage(
            CAT_HASH_PATH,
            paths,
            size,
            size
        )?.let { PlatformImage(it) }
    }

    override fun writeRawImage(image: PlatformImage, iconFilePath: String) {
        val uiImage = image.image
        val data = UIImagePNGRepresentation(uiImage)
        val filePath = NSString.stringWithString(iconFilePath)
        if (data != null) {
            data.writeToFile(filePath, atomically = true)
        } else {
            throw IOException("Failed to convert image to PNG data.")
        }
    }

    override fun readRawImage(iconFilePath: String): PlatformImage? {
        val filePath = NSString.stringWithString(iconFilePath)
        val data = NSData.dataWithContentsOfFile(filePath)

        return data?.let {
            val uiImage = UIImage(data = it)
            PlatformImage(uiImage)
        }
    }
}


