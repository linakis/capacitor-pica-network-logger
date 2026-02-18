import Foundation

class InspectorLogger {
    static let shared = InspectorLogger()
    private let repository = LogRepository.shared
    private var maxBodySize: Int = 131072
    private var redactHeaders: Set<String> = ["authorization", "cookie"]
    private var redactJsonFields: Set<String> = ["password", "token"]
    private var notifyEnabled: Bool = true
    private var requestMethods: [String: String] = [:]
    private var requestUrls: [String: String] = [:]

    func logStart(request: URLRequest) -> String {
        let id = request.value(forHTTPHeaderField: "X-Cap-ReqId") ?? UUID().uuidString
        var data: [String: Any] = [
            "id": id,
            "method": request.httpMethod ?? "GET",
            "url": request.url?.absoluteString ?? "",
            "startTs": Int64(Date().timeIntervalSince1970 * 1000)
        ]

        requestMethods[id] = data["method"] as? String ?? ""
        requestUrls[id] = data["url"] as? String ?? ""

        if let headers = request.allHTTPHeaderFields {
            data["headers"] = redact(headers)
        }

        if let body = request.httpBody, let bodyString = String(data: body, encoding: .utf8) {
            let truncated = truncate(redactJson(bodyString))
            data["requestBody"] = truncated.value
            data["requestBodyTruncated"] = truncated.truncated
        }

        repository.startRequest(data)
        return id
    }

    func logFinish(id: String, response: HTTPURLResponse?, data: Data?, error: Error?, protocol proto: String? = nil) {
        var payload: [String: Any] = ["id": id]
        payload["method"] = requestMethods[id] ?? ""
        payload["url"] = requestUrls[id] ?? ""
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
        repository.finishRequest(payload)

        requestMethods.removeValue(forKey: id)
        requestUrls.removeValue(forKey: id)

        if notifyEnabled {
            let method = payload["method"] as? String ?? ""
            let url = payload["url"] as? String ?? ""
            let status = payload["status"] as? Int
            InspectorNotifications.show(method: method, url: url, status: status)
        }
    }

    func setMaxBodySize(_ size: Int) {
        maxBodySize = size
    }

    func setRedaction(headers: [String], jsonFields: [String]) {
        redactHeaders = Set(headers.map { $0.lowercased() })
        redactJsonFields = Set(jsonFields.map { $0.lowercased() })
    }

    func setNotify(enabled: Bool) {
        notifyEnabled = enabled
    }

    private func truncate(_ value: String) -> (value: String, truncated: Bool) {
        if value.count <= maxBodySize {
            return (value, false)
        }
        let index = value.index(value.startIndex, offsetBy: maxBodySize)
        return (String(value[..<index]), true)
    }

    private func redact(_ headers: [AnyHashable: Any]) -> [AnyHashable: Any] {
        var output: [AnyHashable: Any] = headers
        for (key, value) in headers {
            if let keyString = (key as? String)?.lowercased(), redactHeaders.contains(keyString) {
                output[key] = "[REDACTED]"
            } else {
                output[key] = value
            }
        }
        return output
    }

    private func redact(_ headers: [String: String]) -> [String: String] {
        var output = headers
        for (key, value) in headers {
            if redactHeaders.contains(key.lowercased()) {
                output[key] = "[REDACTED]"
            } else {
                output[key] = value
            }
        }
        return output
    }

    private func redactJson(_ value: String) -> String {
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
    }
}
