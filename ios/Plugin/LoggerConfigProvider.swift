import Foundation
#if canImport(Capacitor)
import Capacitor

class LoggerConfigProvider {
    func getConfig(_ plugin: CAPPlugin) -> [String: Any] {
        guard let bridgeConfig = plugin.bridge?.config else {
            return defaults()
        }
        let pluginConfig = bridgeConfig.getPluginConfig("PicaNetworkLogger")
        let enabled = pluginConfig.getBoolean("enabled", true)
        let notify = pluginConfig.getBoolean("notify", true)
        let maxBodySize = pluginConfig.getInt("maxBodySize", 131072)
        let redactHeaders = bridgeConfig.getPluginConfigValue("PicaNetworkLogger", "redactHeaders") as? [String] ?? []
        let redactJsonFields = bridgeConfig.getPluginConfigValue("PicaNetworkLogger", "redactJsonFields") as? [String] ?? []
        return [
            "enabled": enabled,
            "notify": notify,
            "maxBodySize": maxBodySize,
            "redactHeaders": redactHeaders,
            "redactJsonFields": redactJsonFields
        ]
    }

    private func defaults() -> [String: Any] {
        return [
            "enabled": true,
            "notify": true,
            "maxBodySize": 131072,
            "redactHeaders": [] as [String],
            "redactJsonFields": [] as [String]
        ]
    }
}
#endif
