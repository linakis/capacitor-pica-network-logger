package com.linakis.capacitorpicanetworklogger

import com.getcapacitor.JSObject
import com.linakis.capacitorpicanetworklogger.kmp.DatabaseDriverFactory
import com.linakis.capacitorpicanetworklogger.kmp.InspectorRepository

class LogRepository {
    private var repository: InspectorRepository? = null

    fun attach(context: android.content.Context) {
        if (repository == null) {
            repository = InspectorRepository(DatabaseDriverFactory(context))
        }
    }

    fun startRequest(data: JSObject) {
        val repo = repository ?: return
        val id = data.getString("id") ?: return
        val url = data.getString("url") ?: return
        val method = data.getString("method") ?: "GET"
        val startTs = data.getLong("startTs") ?: System.currentTimeMillis()
        val headers = data.getJSObject("headers")?.toString()
        val reqBody = data.getString("requestBody")
        val reqBodyTruncated = data.getBoolean("requestBodyTruncated") ?: false
        val correlationId = data.getString("id")

        val parsed = runCatching { java.net.URI(url) }.getOrNull()
        repo.insertStart(
            id = id,
            startTs = startTs,
            method = method,
            url = url,
            host = parsed?.host,
            path = parsed?.path,
            query = parsed?.query,
            reqHeadersJson = headers,
            reqBody = reqBody,
            reqBodyTruncated = reqBodyTruncated,
            platform = "android",
            correlationId = correlationId
        )
    }

    fun finishRequest(data: JSObject) {
        val repo = repository ?: return
        val id = data.getString("id") ?: return
        val duration = data.getLong("durationMs")
        val status = data.getInteger("status")?.toLong()
        val headersObj = data.getJSObject("headers")
        val headers = headersObj?.toString()
        val resBody = data.getString("responseBody")
        val resBodyTruncated = data.getBoolean("responseBodyTruncated") ?: false
        val errorMessage = data.getString("error")
        val error = errorMessage != null
        val protocol = data.getString("protocol") ?: headersObj?.getString("X-Android-Selected-Protocol")
        val ssl = if (data.has("ssl")) data.optBoolean("ssl", false) else null

        val existing = repo.getLog(id)
        val finishMethod = existing?.method ?: data.getString("method")
        val finishUrl = existing?.url ?: data.getString("url")
        val resolvedSsl = ssl ?: (finishUrl?.startsWith("https", ignoreCase = true) == true)

        repo.updateFinish(
            id = id,
            durationMs = duration,
            status = status,
            resHeadersJson = headers,
            resBody = resBody,
            resBodyTruncated = resBodyTruncated,
            protocol = protocol,
            ssl = resolvedSsl,
            error = error,
            errorMessage = errorMessage
        )

        val log = repo.getLog(id)
        val notifyMethod = log?.method ?: data.getString("method") ?: ""
        val notifyUrl = log?.url ?: data.getString("url") ?: ""
        LogRepositoryStore.notify(notifyMethod, notifyUrl, status?.toInt())
    }

    fun getLogs(): List<JSObject> {
        val repo = repository ?: return emptyList()
        return repo.getLogs().map { log ->
            JSObject().apply {
                put("id", log.id)
                put("startTs", log.start_ts)
                put("durationMs", log.duration_ms)
                put("method", log.method)
                put("url", log.url)
                put("host", log.host)
                put("path", log.path)
                put("query", log.query)
                put("reqHeaders", log.req_headers_json)
                put("reqBody", log.req_body)
                put("reqBodyTruncated", log.req_body_truncated == 1L)
                put("resStatus", log.res_status)
                put("resHeaders", log.res_headers_json)
                put("resBody", log.res_body)
                put("resBodyTruncated", log.res_body_truncated == 1L)
            put("protocol", log.protocol)
            put("ssl", log.ssl == 1L)
            put("error", log.error == 1L)
                put("errorMessage", log.error_message)
                put("platform", log.platform)
                put("correlationId", log.correlation_id)
            }
        }
    }

    fun getLog(id: String?): JSObject? {
        val repo = repository ?: return null
        if (id == null) return null
        val log = repo.getLog(id) ?: return null
        return JSObject().apply {
            put("id", log.id)
            put("startTs", log.start_ts)
            put("durationMs", log.duration_ms)
            put("method", log.method)
            put("url", log.url)
            put("host", log.host)
            put("path", log.path)
            put("query", log.query)
            put("reqHeaders", log.req_headers_json)
            put("reqBody", log.req_body)
            put("reqBodyTruncated", log.req_body_truncated == 1L)
            put("resStatus", log.res_status)
            put("resHeaders", log.res_headers_json)
            put("resBody", log.res_body)
            put("resBodyTruncated", log.res_body_truncated == 1L)
            put("protocol", log.protocol)
            put("ssl", log.ssl == 1L)
            put("error", log.error == 1L)
            put("errorMessage", log.error_message)
            put("platform", log.platform)
            put("correlationId", log.correlation_id)
        }
    }

    fun clear() {
        repository?.clear()
    }
}
