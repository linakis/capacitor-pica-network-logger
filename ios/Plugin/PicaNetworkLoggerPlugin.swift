import Foundation
#if canImport(Capacitor)
import Capacitor
import UIKit
import UserNotifications

@objc(PicaNetworkLoggerPlugin)
public class PicaNetworkLoggerPlugin: CAPPlugin, CAPBridgedPlugin {
    public let identifier = "PicaNetworkLoggerPlugin"
    public let jsName = "PicaNetworkLogger"
    public let pluginMethods: [CAPPluginMethod] = [
        CAPPluginMethod(name: "startRequest", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "finishRequest", returnType: CAPPluginReturnPromise),
        CAPPluginMethod(name: "openInspector", returnType: CAPPluginReturnPromise)
    ]
    private let configProvider = LoggerConfigProvider()

    @objc func startRequest(_ call: CAPPluginCall) {
        let id = UUID().uuidString
        let method = call.getString("method") ?? "GET"
        let url = call.getString("url") ?? ""
        let headers = call.getObject("headers")
        let body = call.getString("body")
        InspectorLogger.shared.logStart(
            id: id,
            method: method,
            url: url,
            headers: headers,
            body: body
        )
        call.resolve(["id": id])
    }

    @objc func finishRequest(_ call: CAPPluginCall) {
        guard let id = call.getString("id") else {
            call.reject("Missing id")
            return
        }
        let status = call.getInt("status")
        let headers = call.getObject("headers")
        let body = call.getString("body")
        let error = call.getString("error")
        let ssl = call.getBool("ssl")
        InspectorLogger.shared.logFinish(
            id: id,
            status: status,
            headers: headers,
            body: body,
            error: error,
            ssl: ssl
        )
        call.resolve()
    }

    public override func load() {
        super.load()
        let config = configProvider.getConfig(self)
        if let size = config["maxBodySize"] as? Int {
            InspectorLogger.shared.setMaxBodySize(size)
        }
        let headers = config["redactHeaders"] as? [String] ?? []
        let jsonFields = config["redactJsonFields"] as? [String] ?? []
        InspectorLogger.shared.setRedaction(headers: headers, jsonFields: jsonFields)
        let notify = config["notify"] as? Bool ?? true
        InspectorLogger.shared.setNotify(enabled: notify)
        requestNotificationPermissionIfNeeded(enabled: notify)
    }

    private func requestNotificationPermissionIfNeeded(enabled: Bool) {
        guard enabled else { return }
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in
        }
    }

    @objc func openInspector(_ call: CAPPluginCall) {
        #if canImport(UIKit)
        DispatchQueue.main.async { [weak self] in
            let inspector = UINavigationController(rootViewController: InspectorViewController())
            inspector.modalPresentationStyle = .fullScreen
            if let root = self?.bridge?.viewController {
                root.present(inspector, animated: true)
            }
        }
        #endif
        call.resolve()
    }
}
#endif
