import Foundation
#if canImport(PicaNetworkLoggerShared)
import PicaNetworkLoggerShared
import UserNotifications
#endif

class InspectorLogger {
    static let shared = InspectorLogger()
    #if canImport(PicaNetworkLoggerShared)
    private let repository = LogRepository()
    private var maxBodySize: Int = 131072
    private var redactHeaders: Set<String> = ["authorization", "cookie"]
    private var redactJsonFields: Set<String> = ["password", "token"]
    private var notifyEnabled: Bool = true
    private var requestMethods: [String: String] = [:]
    private var requestUrls: [String: String] = [:]
    #endif

    func logStart(request: URLRequest) -> String {
        let id = request.value(forHTTPHeaderField: "X-Cap-ReqId") ?? UUID().uuidString
        var data: [String: Any] = [
            "id": id,
            "method": request.httpMethod ?? "GET",
            "url": request.url?.absoluteString ?? "",
            "startTs": Int64(Date().timeIntervalSince1970 * 1000)
        ]

        #if canImport(PicaNetworkLoggerShared)
        requestMethods[id] = data["method"] as? String ?? ""
        requestUrls[id] = data["url"] as? String ?? ""
        #endif

        if let headers = request.allHTTPHeaderFields {
            data["headers"] = redact(headers)
        }

        if let body = request.httpBody, let bodyString = String(data: body, encoding: .utf8) {
            let truncated = truncate(redactJson(bodyString))
            data["requestBody"] = truncated.value
            data["requestBodyTruncated"] = truncated.truncated
        }

        #if canImport(PicaNetworkLoggerShared)
        repository.startRequest(data)
        #endif
        return id
    }

    func logFinish(id: String, response: HTTPURLResponse?, data: Data?, error: Error?, protocol proto: String? = nil) {
        var payload: [String: Any] = ["id": id]
        #if canImport(PicaNetworkLoggerShared)
        payload["method"] = requestMethods[id] ?? ""
        payload["url"] = requestUrls[id] ?? ""
        #endif
        if let response = response {
            payload["status"] = response.statusCode
            payload["headers"] = redact(response.allHeaderFields)
        }
        if let data = data, let bodyString = String(data: data, encoding: .utf8) {
            let truncated = truncate(redactJson(bodyString))
            payload["responseBody"] = truncated.value
            payload["responseBodyTruncated"] = truncated.truncated
        }
        if let error = error {
            payload["error"] = error.localizedDescription
        }
        if let proto = proto {
            payload["protocol"] = proto
        }
        payload["ssl"] = (payload["url"] as? String)?.hasPrefix("https") == true
        #if canImport(PicaNetworkLoggerShared)
        repository.finishRequest(payload)
        #endif

        #if canImport(PicaNetworkLoggerShared)
        requestMethods.removeValue(forKey: id)
        requestUrls.removeValue(forKey: id)
        #endif

        #if canImport(PicaNetworkLoggerShared)
        if notifyEnabled {
            let method = payload["method"] as? String ?? ""
            let url = payload["url"] as? String ?? ""
            let status = payload["status"] as? Int ?? 0
            let path: String = {
                guard let urlObj = URL(string: url) else { return "" }
                let rawPath = urlObj.path
                let rawQuery = urlObj.query
                if let rawQuery = rawQuery, !rawQuery.isEmpty {
                    return "\(rawPath)?\(rawQuery)"
                }
                return rawPath
            }()
            var titleParts: [String] = []
            if status > 0 { titleParts.append("\(status)") }
            if !method.isEmpty { titleParts.append(method) }
            if !path.isEmpty { titleParts.append(path) }
            let title = titleParts.isEmpty ? "Network Inspector" : titleParts.joined(separator: " ")
            let body = url.isEmpty ? "Tap to open" : url
            InspectorNotifications.show(title: title, body: body)
        }
        #endif
    }

    func setMaxBodySize(_ size: Int) {
        #if canImport(PicaNetworkLoggerShared)
        maxBodySize = size
        #endif
    }

    func setRedaction(headers: [String], jsonFields: [String]) {
        #if canImport(PicaNetworkLoggerShared)
        redactHeaders = Set(headers.map { $0.lowercased() })
        redactJsonFields = Set(jsonFields.map { $0.lowercased() })
        #endif
    }

    func setNotify(enabled: Bool) {
        #if canImport(PicaNetworkLoggerShared)
        notifyEnabled = enabled
        #endif
    }

    private func truncate(_ value: String) -> (value: String, truncated: Bool) {
        #if canImport(PicaNetworkLoggerShared)
        if value.count <= maxBodySize {
            return (value, false)
        }
        let index = value.index(value.startIndex, offsetBy: maxBodySize)
        return (String(value[..<index]), true)
        #else
        return (value, false)
        #endif
    }

    private func redact(_ headers: [AnyHashable: Any]) -> [AnyHashable: Any] {
        #if canImport(PicaNetworkLoggerShared)
        var output: [AnyHashable: Any] = headers
        for (key, value) in headers {
            if let keyString = (key as? String)?.lowercased(), redactHeaders.contains(keyString) {
                output[key] = "[REDACTED]"
            } else {
                output[key] = value
            }
        }
        return output
        #else
        return headers
        #endif
    }

    private func redact(_ headers: [String: String]) -> [String: String] {
        #if canImport(PicaNetworkLoggerShared)
        var output = headers
        for (key, value) in headers {
            if redactHeaders.contains(key.lowercased()) {
                output[key] = "[REDACTED]"
            } else {
                output[key] = value
            }
        }
        return output
        #else
        return headers
        #endif
    }

    private func redactJson(_ value: String) -> String {
        #if canImport(PicaNetworkLoggerShared)
        guard let data = value.data(using: .utf8),
              let object = try? JSONSerialization.jsonObject(with: data, options: []),
              var json = object as? [String: Any] else {
            return value
        }
        for key in json.keys {
            if redactJsonFields.contains(key.lowercased()) {
                json[key] = "[REDACTED]"
            }
        }
        guard let out = try? JSONSerialization.data(withJSONObject: json, options: []) else {
            return value
        }
        return String(data: out, encoding: .utf8) ?? value
        #else
        return value
        #endif
    }
}
