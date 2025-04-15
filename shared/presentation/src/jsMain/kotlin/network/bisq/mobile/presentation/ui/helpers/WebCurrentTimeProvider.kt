package network.bisq.mobile.presentation.ui.helpers

import kotlin.js.Date

class WebCurrentTimeProvider : TimeProvider {
    override fun getCurrentTime(): String {
        val date = Date()
        val hours = date.getHours()
        val minutes = date.getMinutes()
        
        // Format time as "h:mm a" (e.g., "3:30 PM")
        val hour = if (hours % 12 == 0) 12 else hours % 12
        val amPm = if (hours >= 12) "PM" else "AM"
        val minutesStr = if (minutes < 10) "0$minutes" else "$minutes"
        
        return "$hour:$minutesStr $amPm"
    }
}