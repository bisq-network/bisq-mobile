package network.bisq.mobile.presentation.ui.helpers

import platform.Foundation.*

class IOSCurrentTimeProvider : TimeProvider {
    override fun getCurrentTime(): String {
        val dateFormatter = NSDateFormatter()
        dateFormatter.dateFormat = "h:mm a" // 12-hour format with AM/PM
        val currentDate = NSDate()
        return dateFormatter.stringFromDate(currentDate)
    }
}
