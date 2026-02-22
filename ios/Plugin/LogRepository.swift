import Foundation
import SQLite3

private let sqliteTransient = unsafeBitCast(-1, to: sqlite3_destructor_type.self)

class LogRepository {
    static let shared = LogRepository()

    private var logsById: [String: LogEntry] = [:]
    private let lock = NSLock()
    private var db: OpaquePointer?
    private let dbPath: String?

    init() {
        dbPath = LogRepository.databasePath()
        if let path = dbPath {
            openDatabase(path)
            createTableIfNeeded()
            loadPersistedLogs()
        }
    }

    deinit {
        if let db = db {
            sqlite3_close(db)
        }
    }

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

        upsert(entry)
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

        upsert(entry)
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

        deleteAll()
    }

    private static func databasePath() -> String? {
        let fileManager = FileManager.default
        guard let baseUrl = fileManager.urls(for: .applicationSupportDirectory, in: .userDomainMask).first else {
            return nil
        }
        let directory = baseUrl.appendingPathComponent("CapacitorPicaNetworkLogger", isDirectory: true)
        if !fileManager.fileExists(atPath: directory.path) {
            try? fileManager.createDirectory(at: directory, withIntermediateDirectories: true)
        }
        return directory.appendingPathComponent("logs.sqlite").path
    }

    private func openDatabase(_ path: String) {
        if sqlite3_open(path, &db) != SQLITE_OK {
            db = nil
        }
    }

    private func createTableIfNeeded() {
        guard let db = db else { return }
        let sql = """
        CREATE TABLE IF NOT EXISTS logs (
            id TEXT PRIMARY KEY,
            startTs INTEGER,
            durationMs INTEGER,
            method TEXT,
            url TEXT,
            host TEXT,
            path TEXT,
            query TEXT,
            reqHeadersJson TEXT,
            reqBody TEXT,
            reqBodyTruncated INTEGER,
            resStatus INTEGER,
            resHeadersJson TEXT,
            resBody TEXT,
            resBodyTruncated INTEGER,
            protocol TEXT,
            ssl INTEGER,
            error INTEGER,
            errorMessage TEXT,
            platform TEXT,
            correlationId TEXT
        );
        """
        sqlite3_exec(db, sql, nil, nil, nil)
    }

    private func loadPersistedLogs() {
        guard let db = db else { return }
        let sql = """
        SELECT id, startTs, durationMs, method, url, host, path, query, reqHeadersJson, reqBody,
               reqBodyTruncated, resStatus, resHeadersJson, resBody, resBodyTruncated, protocol,
               ssl, error, errorMessage, platform, correlationId
        FROM logs
        """
        var statement: OpaquePointer?
        if sqlite3_prepare_v2(db, sql, -1, &statement, nil) != SQLITE_OK {
            return
        }
        defer { sqlite3_finalize(statement) }

        while sqlite3_step(statement) == SQLITE_ROW {
            guard let id = columnText(statement, index: 0),
                  let method = columnText(statement, index: 3),
                  let url = columnText(statement, index: 4),
                  let platform = columnText(statement, index: 19) else {
                continue
            }
            let startTs = sqlite3_column_int64(statement, 1)
            let entry = LogEntry(
                id: id,
                startTs: startTs,
                method: method,
                url: url,
                host: columnText(statement, index: 5),
                path: columnText(statement, index: 6),
                query: columnText(statement, index: 7),
                reqHeadersJson: columnText(statement, index: 8),
                reqBody: columnText(statement, index: 9),
                reqBodyTruncated: sqlite3_column_int64(statement, 10) != 0,
                platform: platform,
                correlationId: columnText(statement, index: 20)
            )
            if sqlite3_column_type(statement, 2) != SQLITE_NULL {
                entry.durationMs = sqlite3_column_int64(statement, 2)
            }
            if sqlite3_column_type(statement, 11) != SQLITE_NULL {
                entry.resStatus = sqlite3_column_int64(statement, 11)
            }
            entry.resHeadersJson = columnText(statement, index: 12)
            entry.resBody = columnText(statement, index: 13)
            entry.resBodyTruncated = sqlite3_column_int64(statement, 14) != 0
            entry.protocol = columnText(statement, index: 15)
            entry.ssl = sqlite3_column_int64(statement, 16) != 0
            entry.error = sqlite3_column_int64(statement, 17) != 0
            entry.errorMessage = columnText(statement, index: 18)

            logsById[id] = entry
        }
    }

    private func upsert(_ entry: LogEntry) {
        guard let db = db else { return }
        let sql = """
        INSERT OR REPLACE INTO logs (
            id, startTs, durationMs, method, url, host, path, query, reqHeadersJson, reqBody,
            reqBodyTruncated, resStatus, resHeadersJson, resBody, resBodyTruncated, protocol,
            ssl, error, errorMessage, platform, correlationId
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
        """
        var statement: OpaquePointer?
        if sqlite3_prepare_v2(db, sql, -1, &statement, nil) != SQLITE_OK {
            return
        }
        defer { sqlite3_finalize(statement) }

        sqlite3_bind_text(statement, 1, entry.id, -1, sqliteTransient)
        sqlite3_bind_int64(statement, 2, entry.startTs)
        bindOptionalInt64(statement, index: 3, value: entry.durationMs)
        sqlite3_bind_text(statement, 4, entry.method, -1, sqliteTransient)
        sqlite3_bind_text(statement, 5, entry.url, -1, sqliteTransient)
        bindOptionalText(statement, index: 6, value: entry.host)
        bindOptionalText(statement, index: 7, value: entry.path)
        bindOptionalText(statement, index: 8, value: entry.query)
        bindOptionalText(statement, index: 9, value: entry.reqHeadersJson)
        bindOptionalText(statement, index: 10, value: entry.reqBody)
        sqlite3_bind_int(statement, 11, entry.reqBodyTruncated ? 1 : 0)
        bindOptionalInt64(statement, index: 12, value: entry.resStatus)
        bindOptionalText(statement, index: 13, value: entry.resHeadersJson)
        bindOptionalText(statement, index: 14, value: entry.resBody)
        sqlite3_bind_int(statement, 15, entry.resBodyTruncated ? 1 : 0)
        bindOptionalText(statement, index: 16, value: entry.protocol)
        sqlite3_bind_int(statement, 17, entry.ssl ? 1 : 0)
        sqlite3_bind_int(statement, 18, entry.error ? 1 : 0)
        bindOptionalText(statement, index: 19, value: entry.errorMessage)
        sqlite3_bind_text(statement, 20, entry.platform, -1, sqliteTransient)
        bindOptionalText(statement, index: 21, value: entry.correlationId)

        sqlite3_step(statement)
    }

    private func deleteAll() {
        guard let db = db else { return }
        sqlite3_exec(db, "DELETE FROM logs", nil, nil, nil)
    }

    private func columnText(_ statement: OpaquePointer?, index: Int32) -> String? {
        guard let cString = sqlite3_column_text(statement, index) else { return nil }
        return String(cString: cString)
    }

    private func bindOptionalText(_ statement: OpaquePointer?, index: Int32, value: String?) {
        if let value = value {
            sqlite3_bind_text(statement, index, value, -1, sqliteTransient)
        } else {
            sqlite3_bind_null(statement, index)
        }
    }

    private func bindOptionalInt64(_ statement: OpaquePointer?, index: Int32, value: Int64?) {
        if let value = value {
            sqlite3_bind_int64(statement, index, value)
        } else {
            sqlite3_bind_null(statement, index)
        }
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
