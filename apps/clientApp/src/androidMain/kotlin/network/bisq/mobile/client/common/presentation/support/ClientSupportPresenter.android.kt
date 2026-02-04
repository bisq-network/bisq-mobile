package network.bisq.mobile.client.common.presentation.support

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

// TODO: Coverage exclusion rationale - Platform-specific clipboard code requires Android Context
// from Koin DI. Testing would require Robolectric or instrumented tests.
@ExcludeFromCoverage
actual fun copyToClipboard(text: String) {
    val context = ClipboardHelper.getContext()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    val clip = ClipData.newPlainText("Device Token", text)
    clipboard.setPrimaryClip(clip)
}

// TODO: Coverage exclusion rationale - Helper object uses Koin inject() for Android Context.
@ExcludeFromCoverage
private object ClipboardHelper : KoinComponent {
    fun getContext(): Context {
        val context: Context by inject()
        return context
    }
}
