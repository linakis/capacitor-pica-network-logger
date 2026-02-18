import Foundation
#if canImport(PicaNetworkLoggerShared)
import PicaNetworkLoggerShared
#endif

class LogRepository {
    #if canImport(PicaNetworkLoggerShared)
    private lazy var repository = InspectorRepository(driverFactory: DatabaseDriverFactory(context: nil))
    #endif

    func startRequest(_ data: [String: Any]) {
        #if canImport(PicaNetworkLoggerShared)
        guard let id = data["id"] as? String,
              let url = data["url"] as? String else { return }
        let method = (data["method"] as? String) ?? "GET"
        let startTs = (data["startTs"] as? NSNumber)?.int64Value ?? Int64(Date().timeIntervalSince1970 * 1000)
        let headers = data["headers"]
        let reqBody = data["requestBody"] as? String
        let reqBodyTruncated = (data["requestBodyTruncated"] as? Bool) ?? false
        let correlationId = id

        let parsed = URL(string: url)
        repository.insertStart(
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
        #endif
    }

    func finishRequest(_ data: [String: Any]) {
        #if canImport(PicaNetworkLoggerShared)
        guard let id = data["id"] as? String else { return }
        let duration = (data["durationMs"] as? NSNumber)?.int64Value
        let status = (data["status"] as? NSNumber)?.int64Value
        let headers = data["headers"]
        let resBody = data["responseBody"] as? String
        let resBodyTruncated = (data["responseBodyTruncated"] as? Bool) ?? false
        let errorMessage = data["error"] as? String
        let proto = data["protocol"] as? String
        let ssl = data["ssl"] as? Bool ?? false
        let error = errorMessage != nil

        repository.updateFinish(
            id: id,
            durationMs: duration.map { KotlinLong(value: $0) },
            status: status.map { KotlinLong(value: $0) },
            resHeadersJson: jsonString(headers),
            resBody: resBody,
            resBodyTruncated: resBodyTruncated,
            protocol: proto,
            ssl: ssl,
            error: error,
            errorMessage: errorMessage
        )
        #endif
    }

    func getLogs() -> [[String: Any]] {
        #if canImport(PicaNetworkLoggerShared)
        return repository.getLogs().map { log in
            [
                "id": log.id,
                "startTs": log.start_ts,
                "durationMs": log.duration_ms as Any,
                "method": log.method,
                "url": log.url,
                "host": log.host as Any,
                "path": log.path as Any,
                "query": log.query as Any,
                "reqHeaders": log.req_headers_json as Any,
                "reqBody": log.req_body as Any,
                "reqBodyTruncated": log.req_body_truncated == 1,
                "resStatus": log.res_status as Any,
                "resHeaders": log.res_headers_json as Any,
                "resBody": log.res_body as Any,
                "resBodyTruncated": log.res_body_truncated == 1,
                "protocol": log.protocol as Any,
                "ssl": log.ssl == 1,
                "error": log.error == 1,
                "errorMessage": log.error_message as Any,
                "platform": log.platform as Any,
                "correlationId": log.correlation_id as Any
            ]
        }
        #else
        return []
        #endif
    }

    func getLog(_ id: String?) -> [String: Any]? {
        #if canImport(PicaNetworkLoggerShared)
        guard let id = id, let log = repository.getLog(id: id) else { return nil }
        return [
            "id": log.id,
            "startTs": log.start_ts,
            "durationMs": log.duration_ms as Any,
            "method": log.method,
            "url": log.url,
            "host": log.host as Any,
            "path": log.path as Any,
            "query": log.query as Any,
            "reqHeaders": log.req_headers_json as Any,
            "reqBody": log.req_body as Any,
            "reqBodyTruncated": log.req_body_truncated == 1,
            "resStatus": log.res_status as Any,
            "resHeaders": log.res_headers_json as Any,
            "resBody": log.res_body as Any,
            "resBodyTruncated": log.res_body_truncated == 1,
            "protocol": log.protocol as Any,
            "ssl": log.ssl == 1,
            "error": log.error == 1,
            "errorMessage": log.error_message as Any,
            "platform": log.platform as Any,
            "correlationId": log.correlation_id as Any
        ]
        #else
        return nil
        #endif
    }

    func clear() {
        #if canImport(PicaNetworkLoggerShared)
        repository.clear()
        #endif
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
