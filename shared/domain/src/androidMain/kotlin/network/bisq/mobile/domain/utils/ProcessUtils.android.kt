package network.bisq.mobile.domain.utils

import android.app.Activity
import android.os.Process
import com.jakewharton.processphoenix.ProcessPhoenix
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

actual fun restartProcess(view: Any?) {
    val activity =
        view as? Activity ?: throw IllegalStateException("Passed view is not an Activity")

    val appContext = activity.applicationContext

    activity.runOnUiThread {
        ProcessPhoenix.triggerRebirth(appContext)
    }
}

actual fun killProcess(view: Any?) {
    val activity =
        view as? Activity

    activity?.finishAffinity() // triggers destroy lifecycle of activity for proper cleanup
    runBlocking {
        delay(200) // wait for cleanup to finish
    }
    Process.killProcess(Process.myPid())
    exitProcess(0)
}
