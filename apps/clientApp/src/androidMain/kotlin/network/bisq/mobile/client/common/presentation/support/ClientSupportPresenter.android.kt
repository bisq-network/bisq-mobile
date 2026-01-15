package network.bisq.mobile.client.common.presentation.support

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named

actual fun copyToClipboard(text: String) {
    val context = ClipboardHelper.getContext()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val clip = ClipData.newPlainText("Device Token", text)
    clipboard.setPrimaryClip(clip)
}

private object ClipboardHelper : KoinComponent {
    fun getContext(): Context {
        val context: Context by inject()
        return context
    }
}

