package network.bisq.mobile.presentation.ui.helpers

import android.content.Context
import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class AndroidCurrentTimeProvider() : TimeProvider {
    override fun getCurrentTime(): String {
        val currentTimeMillis = System.currentTimeMillis()
        val date = Date(currentTimeMillis)
        val formatter = SimpleDateFormat("h:mm a", Locale.getDefault())
        return formatter.format(date)
    }
}
