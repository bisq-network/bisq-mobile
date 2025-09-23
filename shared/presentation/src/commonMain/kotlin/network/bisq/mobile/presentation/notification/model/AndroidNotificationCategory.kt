package network.bisq.mobile.presentation.notification.model

enum class AndroidNotificationCategory {
    /**
     * Notification category: incoming call (voice or video) or similar synchronous communication request.
     */
    CATEGORY_CALL,

    /**
     * Notification category: map turn-by-turn navigation.
     */
    CATEGORY_NAVIGATION,

    /**
     * Notification category: incoming direct message (SMS, instant message, etc.).
     */
    CATEGORY_MESSAGE,

    /**
     * Notification category: asynchronous bulk message (email).
     */
    CATEGORY_EMAIL,

    /**
     * Notification category: calendar event.
     */
    CATEGORY_EVENT,

    /**
     * Notification category: promotion or advertisement.
     */
    CATEGORY_PROMO,

    /**
     * Notification category: alarm or timer.
     */
    CATEGORY_ALARM,

    /**
     * Notification category: progress of a long-running background operation.
     */
    CATEGORY_PROGRESS,

    /**
     * Notification category: social network or sharing update.
     */
    CATEGORY_SOCIAL,

    /**
     * Notification category: error in background operation or authentication status.
     */
    CATEGORY_ERROR,

    /**
     * Notification category: media transport control for playback.
     */
    CATEGORY_TRANSPORT,

    /**
     * Notification category: system or device status update.  Reserved for system use.
     */
    CATEGORY_SYSTEM,

    /**
     * Notification category: indication of running background service.
     */
    CATEGORY_SERVICE,

    /**
     * Notification category: user-scheduled reminder.
     */
    CATEGORY_REMINDER,

    /**
     * Notification category: a specific, timely recommendation for a single thing.
     * For example, a news app might want to recommend a news story it believes the user will
     * want to read next.
     */
    CATEGORY_RECOMMENDATION,

    /**
     * Notification category: ongoing information about device or contextual status.
     */
    CATEGORY_STATUS ,

    /**
     * Notification category: tracking a user's workout.
     */
    CATEGORY_WORKOUT ,

    /**
     * Notification category: temporarily sharing location.
     */
    CATEGORY_LOCATION_SHARING ,

    /**
     * Notification category: running stopwatch.
     */
    CATEGORY_STOPWATCH ,

    /**
     * Notification category: missed call.
     */
    CATEGORY_MISSED_CALL ,

    /**
     * Notification category: voicemail.
     */
    CATEGORY_VOICEMAIL ,
}