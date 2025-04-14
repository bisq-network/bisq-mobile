package network.bisq.mobile.client.web.services

import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.service.notifications.controller.NotificationServiceController
import network.bisq.mobile.domain.service.trades.TradesServiceFacade

class WebNotificationService(
    notificationServiceController: NotificationServiceController,
    tradesServiceFacade: TradesServiceFacade)
    : OpenTradesNotificationService(notificationServiceController, tradesServiceFacade) {

    override fun launchNotificationService() {
        // Web implementation for notifications
        // Could use the Web Notifications API
        js("""
            if (Notification.permission !== 'granted') {
                Notification.requestPermission().then(function(permission) {
                    if (permission !== 'granted') {
                        console.log('Notification permission denied');
                        // Consider alternative notification strategy
                    }
                }).catch(function(error) {
                    console.error('Error requesting notification permission:', error);
                });
            }
        """)
    }
    
    override fun stopNotificationService() {
        // Clean up if needed
    }
    
    // Helper method to show a web notification
    fun showNotification(title: String, body: String) {
        js("""
            if (Notification.permission === 'granted') {
                new Notification(title, { body: body });
            }
        """)
    }
}