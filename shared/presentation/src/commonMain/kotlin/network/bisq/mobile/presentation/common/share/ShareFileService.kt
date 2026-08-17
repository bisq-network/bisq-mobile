package network.bisq.mobile.presentation.common.share

/**
 * Writes UTF-8 text to a temp file and opens the OS share sheet.
 */
interface ShareFileService {
    /**
     * @param shareText optional plain text handed to receivers that only read text (notes, chat
     *   apps) and would otherwise get an empty share. Ignored on iOS, where mixing a text item
     *   into the activity items removes "Save to Files".
     */
    suspend fun shareUtf8TextFile(
        content: String,
        fileName: String,
        shareText: String? = null,
    ): Result<Unit>

    /**
     * Shares a file that already exists on disk, streamed rather than read into memory, so a
     * multi-MB log file can be shared whole. The receiver sees the file's own name.
     *
     * @param path absolute path of the file to share.
     */
    suspend fun shareFile(path: String): Result<Unit>
}
