package network.bisq.mobile.presentation.common.ui.utils

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.text.AnnotatedString

actual fun AnnotatedString?.toClipEntry(): ClipEntry? {
    if (this == null) return null
    return ClipEntry(ClipData.newPlainText(this.text, this.text))
}

actual suspend fun ClipEntry.readText(): String? {
    val clipData = this.clipData
    val stringBuilder = StringBuilder()
    for (i in 0 until clipData.itemCount) {
        val item = clipData.getItemAt(i)
        item.text?.let {
            stringBuilder.append(it)
        }
    }
    return stringBuilder.toString().ifEmpty { null }
}
