package network.bisq.mobile.domain.helper

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.net.toUri
import java.util.concurrent.ConcurrentHashMap

object ResourceUtils {
    private val resourceIdCache by lazy {
        ConcurrentHashMap<String, Int>()
    }

    fun getNotifResId(context: Context): Int {
        val iconResId = getImageResourceId(context, "ic_notification")
        return if (iconResId != 0) iconResId else android.R.drawable.ic_notification_overlay
    }

    /**
     * Gets a resource id by name
     *
     * @param resourceName
     * @return int or 0 if not found
     */
    fun getImageResourceId(context: Context, resourceName: String?): Int {
        var resourceId = getResourceIdByName(context, resourceName, "drawable")
        if (resourceId == 0) {
            resourceId = getResourceIdByName(context, resourceName, "mipmap")
        }
        return resourceId
    }

    fun getResourceIdByName(context: Context, name: String?, type: String): Int {
        if (name.isNullOrBlank()) {
            return 0
        }
        val normalizedName = name.lowercase().replace('-', '_')

        val key = "${normalizedName}_${type}"

        return resourceIdCache.computeIfAbsent(key) {
            // we cannot get the identifier directly in this module
            /**
             * Use of this function is discouraged because resource reflection makes it harder to
             * perform build optimizations and compile-time verification of code.
             * It is much more efficient to retrieve resources by identifier (e.g. R.foo.bar) than by
             * name (e.g. getIdentifier("bar", "foo", null)
             */
            context.resources.getIdentifier(
                name,
                type,
                context.packageName
            )
        }
    }
}