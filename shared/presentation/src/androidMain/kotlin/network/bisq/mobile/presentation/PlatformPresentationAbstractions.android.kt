package network.bisq.mobile.presentation

import android.content.res.Resources
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.presentation.ui.helpers.AndroidCurrentTimeProvider
import network.bisq.mobile.presentation.ui.helpers.TimeProvider

actual fun getPlatformPainter(platformImage: PlatformImage): Painter {
    return BitmapPainter(platformImage.bitmap)
}

actual fun getPlatformCurrentTimeProvider(): TimeProvider = AndroidCurrentTimeProvider()

actual fun exitApp(view: Any?) {
    if (view == null) {
        android.os.Process.killProcess(android.os.Process.myPid())
    } else {
        if (view is android.app.Activity) {
//            (view as android.app.Activity).finish()
            // Move task to the background (similar to pressing Home button)
            val homeIntent = android.content.Intent(android.content.Intent.ACTION_MAIN)
            homeIntent.addCategory(android.content.Intent.CATEGORY_HOME)
            homeIntent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            view.startActivity(homeIntent)
        }
    }
}

actual fun getScreenWidthDp(): Int {
    return Resources.getSystem().displayMetrics.widthPixels / Resources.getSystem().displayMetrics.density.toInt()
}