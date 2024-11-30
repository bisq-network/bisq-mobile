package network.bisq.mobile.service

import network.bisq.mobile.PlatformImage
import network.bisq.mobile.client.cathash.ClientCatHashService
import network.bisq.mobile.utils.getFilesDir

const val PATH_TO_DRAWABLE ="compose-resources/composeResources/bisqapps.shared.presentation.generated.resources/drawable/"
const val CAT_HASH_PATH = PATH_TO_DRAWABLE + "cathash/"

class IosClientCatHashService : ClientCatHashService<PlatformImage?>("${getFilesDir()}/Bisq2_mobile") {

    override fun composeImage(paths: Array<String>, size: Int): PlatformImage? {
        return IosImageUtil.composeImage(
            CAT_HASH_PATH,
            paths,
            size,
            size
        )
    }

    override fun writeRawImage(image: PlatformImage?, iconFilePath: String) {
        //todo
    }

    override fun readRawImage(iconFilePath: String): PlatformImage?? {
        //todo
        return null
    }
}


