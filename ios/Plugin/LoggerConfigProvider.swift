import Foundation
#if canImport(Capacitor)
import Capacitor

class LoggerConfigProvider {
    func getConfig(_ plugin: CAPPlugin) -> [String: Any] {
        let raw = plugin.getConfig() as? [AnyHashable: Any] ?? [:]
        let config = raw.reduce(into: [String: Any]()) { dict, item in
            if let key = item.key as? String {
                dict[key] = item.value
            }
        }
        return [
            "enabled": config["enabled"] as? Bool ?? true,
            "maxBodySize": config["maxBodySize"] as? Int ?? 131072,
            "redactHeaders": config["redactHeaders"] as? [String] ?? [],
            "redactJsonFields": config["redactJsonFields"] as? [String] ?? []
        ]
    }
}
#endif
