import Foundation
import UserNotifications

class InspectorNotifications: NSObject {
    static let shared = InspectorNotifications()

    static func show(method: String, url: String, status: Int?) {
        let content = UNMutableNotificationContent()
        if let status = status {
            content.title = "\(method) \(status)"
        } else {
            content.title = method
        }
        content.body = url
        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request, withCompletionHandler: nil)
    }
}
