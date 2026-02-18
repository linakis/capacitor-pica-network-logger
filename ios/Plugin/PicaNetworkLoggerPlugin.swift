import Foundation
#if canImport(Capacitor)
import Capacitor
#if canImport(PicaNetworkLoggerShared)
import PicaNetworkLoggerShared
#endif

@objc(PicaNetworkLoggerPlugin)
public class PicaNetworkLoggerPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "PicaNetworkLoggerPlugin"
    public let jsName = "PicaNetworkLogger"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "startRequest", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "finishRequest", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getLogs", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getLog", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "clearLogs", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "getConfig", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "openInspector", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "showNotification", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "requestNotificationPermission", returnType: CAPPluginReturnPromise)
    ]
    private let repository = LogRepository()
    private let configProvider = LoggerConfigProvider()

    @objc func startRequest(_ call: CAPPluginCall) {
        guard let id = call.getString("id") else {
            call.reject("Missing id")
            return
        }
        if let options = call.options as? [AnyHashable: Any] {
            let normalized = options.reduce(into: [String: Any]()) { dict, item in
                if let key = item.key as? String {
                    dict[key] = item.value
                }
            }
            repository.startRequest(normalized)
        }
        call.resolve(["id": id])
    }

    @objc func finishRequest(_ call: CAPPluginCall) {
        if let options = call.options as? [AnyHashable: Any] {
            let normalized = options.reduce(into: [String: Any]()) { dict, item in
                if let key = item.key as? String {
                    dict[key] = item.value
                }
            }
            repository.finishRequest(normalized)
        }
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
        #if canImport(PicaNetworkLoggerShared)
        DispatchQueue.main.async { [weak self] in
            let inspector = InspectorViewController()
            inspector.modalPresentationStyle = .fullScreen
            if let root = self?.bridge?.viewController {
                root.present(inspector, animated: true)
            }
        }
        #endif
        call.resolve()
    }

    @objc func showNotification(_ call: CAPPluginCall) {
        InspectorNotifications.show(method: "", url: "", status: nil)
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
