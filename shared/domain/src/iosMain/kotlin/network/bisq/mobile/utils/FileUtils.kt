package network.bisq.mobile.utils

import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask


fun getFilesDir(): String {
    val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
    val documentsDirectory = paths.firstOrNull() as? String
        ?: throw IllegalStateException("Could not get documents directory")

    // println("documentsDirectory="+documentsDirectory)
    return documentsDirectory
}