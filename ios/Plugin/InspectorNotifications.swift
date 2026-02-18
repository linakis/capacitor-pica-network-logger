import Foundation
#if canImport(UserNotifications)
import UserNotifications

class InspectorNotifications: NSObject, UNUserNotificationCenterDelegate {
    static let shared = InspectorNotifications()

    static func show(method: String, url: String, status: Int?) {
        let center = UNUserNotificationCenter.current()
        center.delegate = shared
        let content = UNMutableNotificationContent()
        var titleParts: [String] = []
        if let status = status {
            titleParts.append("\(status)")
        }
        if !method.isEmpty {
            titleParts.append(method)
        }
        let path: String = {
            guard let urlObj = URL(string: url) else { return "" }
            let rawPath = urlObj.path
            let rawQuery = urlObj.query
            if let rawQuery = rawQuery, !rawQuery.isEmpty {
                return "\(rawPath)?\(rawQuery)"
            }
            return rawPath
        }()
        if !path.isEmpty {
            titleParts.append(path)
        }
        content.title = titleParts.isEmpty ? "Network Inspector" : titleParts.joined(separator: " ")
        if !url.isEmpty {
            content.body = url
        }
        content.sound = nil

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        let request = UNNotificationRequest(identifier: "cap_http_inspector", content: content, trigger: trigger)
        center.add(request, withCompletionHandler: nil)
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([])
    }
}
#endif
