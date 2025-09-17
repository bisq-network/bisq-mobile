package network.bisq.mobile.domain.helper

import android.content.ContentResolver
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.net.toUri
import java.util.Locale
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
        val normalizedName = name.lowercase(Locale.ENGLISH).replace('-', '_')

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
                normalizedName,
                type,
                context.packageName
            )
        }
    }

    fun getSoundUri(context: Context, sound: String?): Uri? {
        if (sound.isNullOrBlank()) {
            return null
        } else if (sound.contains("://")) {
            return sound.toUri()
        } else if (sound.equals("default", true)) {
            return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        } else {
            // User is attempting to get a sound by file name, verify it exists
            var soundResourceId = getResourceIdByName(context, sound, "raw")
            if (soundResourceId == 0 && sound.contains(".")) {
                soundResourceId = getResourceIdByName(context, sound.substring(0, sound.lastIndexOf('.')), "raw")
            }
            if (soundResourceId == 0) {
                return null;
            }
            // use the actual sound name to obtain a stable URI
            return context.resourceUri(soundResourceId)
        }
    }

    private fun Context.resourceUri(resourceId: Int): Uri = with(resources) {
        Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(getResourcePackageName(resourceId))
            .appendPath(getResourceTypeName(resourceId))
            .appendPath(getResourceEntryName(resourceId))
            .build()
    }
}
