import Foundation
#if canImport(UserNotifications)
import UserNotifications

class InspectorNotifications: NSObject, UNUserNotificationCenterDelegate {
    static let shared = InspectorNotifications()

    static func show(title: String, body: String) {
        let center = UNUserNotificationCenter.current()
        center.delegate = shared
        let content = UNMutableNotificationContent()
        content.title = title.isEmpty ? "Network Inspector" : title
        content.body = body.isEmpty ? "Tap to open" : body
        content.sound = .default

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        let request = UNNotificationRequest(identifier: "cap_http_inspector", content: content, trigger: trigger)
        center.add(request, withCompletionHandler: nil)
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound, .badge])
    }
}
#endif
