import Foundation
#if canImport(Capacitor)
import Capacitor

class LoggerConfigProvider {
    func getConfig(_ plugin: CAPPlugin) -> [String: Any] {
        let config = plugin.bridge?.config?.getPluginConfiguration("PicaNetworkLogger")
        return [
            "enabled": config?.getBoolean("enabled", true) ?? true,
            "maxBodySize": config?.getInt("maxBodySize", 131072) ?? 131072,
            "redactHeaders": config?.getArray("redactHeaders") ?? [],
            "redactJsonFields": config?.getArray("redactJsonFields") ?? []
        ]
    }
}
#endif
