package network.bisq.mobile.presentation.common.ui.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AndroidCurrentTimeProvider : TimeProvider {
    override fun getCurrentTime(): String {
        val currentTimeMillis = System.currentTimeMillis()
        val date = Date(currentTimeMillis)
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        return formatter.format(date)
    }
}
