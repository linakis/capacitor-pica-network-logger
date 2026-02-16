package com.linakis.capacitorpicanetworklogger.kmp

import com.linakis.capacitorpicanetworklogger.kmp.db.InspectorDatabase
import com.linakis.capacitorpicanetworklogger.kmp.db.Http_logs

class InspectorRepository(driverFactory: DatabaseDriverFactory) {
    private val database = InspectorDatabase(driverFactory.createDriver())
    private val queries = database.inspectorDatabaseQueries

    fun insertStart(
        id: String,
        startTs: Long,
        method: String,
        url: String,
        host: String?,
        path: String?,
        query: String?,
        reqHeadersJson: String?,
        reqBody: String?,
        reqBodyTruncated: Boolean,
        platform: String?,
        correlationId: String?
    ) {
        queries.insertStart(
            id = id,
            start_ts = startTs,
            method = method,
            url = url,
            host = host,
            path = path,
            query = query,
            req_headers_json = reqHeadersJson,
            req_body = reqBody,
            req_body_truncated = if (reqBodyTruncated) 1 else 0,
            platform = platform,
            correlation_id = correlationId
        )
    }

    fun updateFinish(
        id: String,
        durationMs: Long?,
        status: Long?,
        resHeadersJson: String?,
        resBody: String?,
        resBodyTruncated: Boolean,
        protocol: String?,
        ssl: Boolean,
        error: Boolean,
        errorMessage: String?
    ) {
        queries.updateFinish(
            duration_ms = durationMs,
            res_status = status,
            res_headers_json = resHeadersJson,
            res_body = resBody,
            res_body_truncated = if (resBodyTruncated) 1 else 0,
            protocol = protocol,
            ssl = if (ssl) 1 else 0,
            error = if (error) 1 else 0,
            error_message = errorMessage,
            id = id
        )
    }

    fun getLogs(): List<Http_logs> {
        return queries.selectAll().executeAsList()
    }

    fun getLog(id: String): Http_logs? {
        return queries.selectById(id).executeAsOneOrNull()
    }

    fun clear() {
        queries.deleteAll()
    }
}
