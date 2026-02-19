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
    private var requestStartTs: [String: Int64] = [:]

    func logStart(
        id: String,
        method: String,
        url: String,
        headers: [String: Any]?,
        body: String?
    ) {
        let startTs = Int64(Date().timeIntervalSince1970 * 1000)
        var data: [String: Any] = [
            "id": id,
            "method": method,
            "url": url,
            "startTs": startTs
        ]

        requestMethods[id] = method
        requestUrls[id] = url
        requestStartTs[id] = startTs

        if let headers = headers as? [String: String] {
            data["headers"] = redact(headers)
        }

        if let body = body {
            let truncated = truncate(redactJson(body))
            data["requestBody"] = truncated.value
            data["requestBodyTruncated"] = truncated.truncated
        }

        repository.startRequest(data)
    }

    func logFinish(
        id: String,
        status: Int?,
        headers: [String: Any]?,
        body: String?,
        error: String?,
        ssl: Bool?
    ) {
        var payload: [String: Any] = ["id": id]
        payload["method"] = requestMethods[id] ?? ""
        payload["url"] = requestUrls[id] ?? ""
        if let startTs = requestStartTs[id] {
            payload["durationMs"] = Int64(Date().timeIntervalSince1970 * 1000) - startTs
        }
        if let status = status {
            payload["status"] = status
        }
        if let headers = headers as? [String: String] {
            payload["headers"] = redact(headers)
        }
        if let body = body {
            let truncated = truncate(redactJson(body))
            payload["responseBody"] = truncated.value
            payload["responseBodyTruncated"] = truncated.truncated
        }
        if let error = error {
            payload["error"] = error
        }
        if let ssl = ssl {
            payload["ssl"] = ssl
        }
        if payload["ssl"] == nil {
            payload["ssl"] = (payload["url"] as? String)?.hasPrefix("https") == true
        }
        repository.finishRequest(payload)

        requestMethods.removeValue(forKey: id)
        requestUrls.removeValue(forKey: id)
        requestStartTs.removeValue(forKey: id)

        if notifyEnabled {
            let method = payload["method"] as? String ?? ""
            let url = payload["url"] as? String ?? ""
            let status = payload["status"] as? Int
            InspectorNotifications.show(method: method, url: url, status: status)
        }

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
}
