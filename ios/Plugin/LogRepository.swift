import Foundation

class LogRepository {
    static let shared = LogRepository()

    private var logsById: [String: LogEntry] = [:]
    private let lock = NSLock()

    func startRequest(_ data: [String: Any]) {
        guard let id = data["id"] as? String,
              let url = data["url"] as? String else { return }
        let method = (data["method"] as? String) ?? "GET"
        let startTs = (data["startTs"] as? NSNumber)?.int64Value ?? Int64(Date().timeIntervalSince1970 * 1000)
        let headers = data["headers"]
        let reqBody = data["requestBody"] as? String
        let reqBodyTruncated = (data["requestBodyTruncated"] as? Bool) ?? false
        let correlationId = (data["correlationId"] as? String) ?? id

        let parsed = URL(string: url)
        let entry = LogEntry(
            id: id,
            startTs: startTs,
            method: method,
            url: url,
            host: parsed?.host,
            path: parsed?.path,
            query: parsed?.query,
            reqHeadersJson: jsonString(headers),
            reqBody: reqBody,
            reqBodyTruncated: reqBodyTruncated,
            platform: "ios",
            correlationId: correlationId
        )
        lock.lock()
        logsById[id] = entry
        lock.unlock()
    }

    func finishRequest(_ data: [String: Any]) {
        guard let id = data["id"] as? String else { return }
        let duration = (data["durationMs"] as? NSNumber)?.int64Value
        let status = (data["status"] as? NSNumber)?.int64Value
        let headers = data["headers"]
        let resBody = data["responseBody"] as? String
        let resBodyTruncated = (data["responseBodyTruncated"] as? Bool) ?? false
        let errorMessage = data["error"] as? String
        let proto = data["protocol"] as? String
        let ssl = data["ssl"] as? Bool
        let error = errorMessage != nil

        lock.lock()
        let existing = logsById[id]
        let entry = existing ?? LogEntry(id: id, startTs: Int64(Date().timeIntervalSince1970 * 1000), method: data["method"] as? String ?? "", url: data["url"] as? String ?? "", platform: "ios", correlationId: id)
        entry.durationMs = duration
        entry.resStatus = status
        entry.resHeadersJson = jsonString(headers)
        entry.resBody = resBody
        entry.resBodyTruncated = resBodyTruncated
        entry.protocol = proto
        entry.ssl = ssl ?? (entry.url.hasPrefix("https"))
        entry.error = error
        entry.errorMessage = errorMessage
        logsById[id] = entry
        lock.unlock()
    }

    func getLogs() -> [[String: Any]] {
        return getLogEntries().map { $0.toDictionary() }
    }

    func getLog(_ id: String?) -> [String: Any]? {
        guard let id = id else { return nil }
        lock.lock()
        let entry = logsById[id]
        lock.unlock()
        return entry?.toDictionary()
    }

    func getLogEntries() -> [LogEntry] {
        lock.lock()
        let entries = Array(logsById.values)
        lock.unlock()
        return entries.sorted { $0.startTs > $1.startTs }
    }

    func clear() {
        lock.lock()
        logsById.removeAll()
        lock.unlock()
    }

    private func jsonString(_ value: Any?) -> String? {
        guard let value = value else { return nil }
        if let string = value as? String { return string }
        if !JSONSerialization.isValidJSONObject(value) { return nil }
        if let data = try? JSONSerialization.data(withJSONObject: value, options: []) {
            return String(data: data, encoding: .utf8)
        }
        return nil
    }
}

final class LogEntry {
    let id: String
    let startTs: Int64
    let method: String
    let url: String
    let host: String?
    let path: String?
    let query: String?
    let platform: String
    let correlationId: String?
    let reqHeadersJson: String?
    let reqBody: String?
    let reqBodyTruncated: Bool
    var durationMs: Int64?
    var resStatus: Int64?
    var resHeadersJson: String?
    var resBody: String?
    var resBodyTruncated: Bool = false
    var `protocol`: String?
    var ssl: Bool = false
    var error: Bool = false
    var errorMessage: String?

    init(
        id: String,
        startTs: Int64,
        method: String,
        url: String,
        host: String? = nil,
        path: String? = nil,
        query: String? = nil,
        reqHeadersJson: String? = nil,
        reqBody: String? = nil,
        reqBodyTruncated: Bool = false,
        platform: String,
        correlationId: String?
    ) {
        self.id = id
        self.startTs = startTs
        self.method = method
        self.url = url
        self.host = host
        self.path = path
        self.query = query
        self.reqHeadersJson = reqHeadersJson
        self.reqBody = reqBody
        self.reqBodyTruncated = reqBodyTruncated
        self.platform = platform
        self.correlationId = correlationId
    }

    func toDictionary() -> [String: Any] {
        return [
            "id": id,
            "startTs": startTs,
            "durationMs": durationMs as Any,
            "method": method,
            "url": url,
            "host": host as Any,
            "path": path as Any,
            "query": query as Any,
            "reqHeaders": reqHeadersJson as Any,
            "reqBody": reqBody as Any,
            "reqBodyTruncated": reqBodyTruncated,
            "resStatus": resStatus as Any,
            "resHeaders": resHeadersJson as Any,
            "resBody": resBody as Any,
            "resBodyTruncated": resBodyTruncated,
            "protocol": `protocol` as Any,
            "ssl": ssl,
            "error": error,
            "errorMessage": errorMessage as Any,
            "platform": platform,
            "correlationId": correlationId as Any
        ]
    }
}
