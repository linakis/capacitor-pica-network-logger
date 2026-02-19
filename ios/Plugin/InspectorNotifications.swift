import Foundation
import UserNotifications

class InspectorNotifications: NSObject, UNUserNotificationCenterDelegate {
    static let shared = InspectorNotifications()

    static func show(method: String, url: String, status: Int?) {
        let center = UNUserNotificationCenter.current()
        center.delegate = shared
        let content = UNMutableNotificationContent()
        if let status = status {
            content.title = "\(method) \(status)"
        } else {
            content.title = method
        }
        content.body = url
        content.sound = nil
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        center.add(request, withCompletionHandler: nil)
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([])
    }
}
