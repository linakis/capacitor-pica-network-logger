import Foundation
#if canImport(Capacitor)
import Capacitor

@objc(PicaNetworkLoggerPlugin)
public class PicaNetworkLoggerPlugin: CAPPlugin {
    private let repository = LogRepository()
    private let configProvider = LoggerConfigProvider()

    @objc func startRequest(_ call: CAPPluginCall) {
        guard let id = call.getString("id") else {
            call.reject("Missing id")
            return
        }
        repository.startRequest(call.options)
        call.resolve(["id": id])
    }

    @objc func finishRequest(_ call: CAPPluginCall) {
        repository.finishRequest(call.options)
        call.resolve()
    }

    @objc func getLogs(_ call: CAPPluginCall) {
        call.resolve(["logs": repository.getLogs()])
    }

    @objc func getLog(_ call: CAPPluginCall) {
        let id = call.getString("id")
        call.resolve(["log": repository.getLog(id)])
    }

    @objc func clearLogs(_ call: CAPPluginCall) {
        repository.clear()
        call.resolve()
    }

    @objc func getConfig(_ call: CAPPluginCall) {
        let config = configProvider.getConfig(self)
        if let size = config["maxBodySize"] as? Int {
            InspectorLogger.shared.setMaxBodySize(size)
        }
        let headers = config["redactHeaders"] as? [String] ?? []
        let jsonFields = config["redactJsonFields"] as? [String] ?? []
        InspectorLogger.shared.setRedaction(headers: headers, jsonFields: jsonFields)
        let notify = config["notify"] as? Bool ?? true
        InspectorLogger.shared.setNotify(enabled: notify)
        call.resolve(config)
    }

    @objc func openInspector(_ call: CAPPluginCall) {
        #if canImport(UIKit)
        if let root = bridge?.viewController {
            let inspector = InspectorViewController()
            root.present(inspector, animated: true)
        }
        #endif
        call.resolve()
    }

    @objc func showNotification(_ call: CAPPluginCall) {
        InspectorNotifications.show()
        call.resolve()
    }

    @objc func requestNotificationPermission(_ call: CAPPluginCall) {
        #if canImport(UserNotifications)
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
            call.resolve(["granted": granted])
        }
        #else
        call.resolve(["granted": false])
        #endif
    }

    public override func load() {
        super.load()
        requestNotificationPermissionImplicitly()
        #if canImport(UIKit)
        URLProtocol.registerClass(InspectorURLProtocol.self)
        #endif
    }

    private func requestNotificationPermissionImplicitly() {
        #if canImport(UserNotifications)
        let config = configProvider.getConfig(self)
        let notify = config["notify"] as? Bool ?? true
        if notify == false {
            return
        }
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in
        }
        #endif
    }
}
#endif
