package network.bisq.mobile.presentation.common.share

/**
 * Writes UTF-8 text to a temp file and opens the OS share sheet.
 */
interface ShareFileService {
    suspend fun shareUtf8TextFile(
        content: String,
        fileName: String,
    ): Result<Unit>
}
