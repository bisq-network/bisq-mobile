package network.bisq.mobile.client.common.presentation.support

import platform.UIKit.UIPasteboard

actual fun copyToClipboard(text: String) {
    UIPasteboard.generalPasteboard().string = text
}
