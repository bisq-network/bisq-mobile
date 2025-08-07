package network.bisq.mobile.domain.helper

import android.content.Context

fun getNotifResId(context: Context): Int {
    // we cannot get the identifier directly in this module
    /**
     * Use of this function is discouraged because resource reflection makes it harder to
     * perform build optimizations and compile-time verification of code.
     * It is much more efficient to retrieve resources by identifier (e.g. R.foo.bar) than by
     * name (e.g. getIdentifier("bar", "foo", null)
     */
    val iconResId = context.resources.getIdentifier(
        "ic_notification",
        "drawable",
        context.packageName
    )
    return if (iconResId != 0) iconResId else android.R.drawable.ic_notification_overlay
}