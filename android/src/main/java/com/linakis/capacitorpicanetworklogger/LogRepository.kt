package com.linakis.capacitorpicanetworklogger

import com.getcapacitor.JSObject
import java.util.concurrent.ConcurrentHashMap

class LogRepository {
    private val logsById: MutableMap<String, LogEntry> = ConcurrentHashMap()

    fun attach(@Suppress("UNUSED_PARAMETER") context: android.content.Context) {
        // No-op: in-memory storage
    }

    fun startRequest(data: JSObject) {
        val id = data.getString("id") ?: return
        val url = data.getString("url") ?: return
        val method = data.getString("method") ?: "GET"
        val startTs = data.getLong("startTs") ?: System.currentTimeMillis()
        val headers = data.getJSObject("headers")?.toString()
        val reqBody = data.getString("requestBody")
        val reqBodyTruncated = data.getBoolean("requestBodyTruncated") ?: false
        val correlationId = data.getString("correlationId") ?: id

        val parsed = runCatching { java.net.URI(url) }.getOrNull()
        val entry = LogEntry(
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
        logsById[id] = entry
    }

    fun finishRequest(data: JSObject) {
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

        val existing = logsById[id]
        val entry = existing ?: LogEntry(
            id = id,
            startTs = System.currentTimeMillis(),
            method = data.getString("method") ?: "",
            url = data.getString("url") ?: "",
            platform = "android",
            correlationId = id
        )
        entry.durationMs = duration
        entry.resStatus = status
        entry.resHeadersJson = headers
        entry.resBody = resBody
        entry.resBodyTruncated = resBodyTruncated
        entry.protocol = protocol
        entry.ssl = ssl ?: entry.url.startsWith("https", ignoreCase = true)
        entry.error = error
        entry.errorMessage = errorMessage
        logsById[id] = entry

        val notifyMethod = entry.method
        val notifyUrl = entry.url
        LogRepositoryStore.notify(notifyMethod, notifyUrl, status?.toInt())
    }

    fun getLogs(): List<JSObject> {
        return getEntries().map { it.toJsObject() }
    }

    fun getLog(id: String?): JSObject? {
        if (id == null) return null
        return logsById[id]?.toJsObject()
    }

    fun getEntry(id: String?): LogEntry? {
        if (id == null) return null
        return logsById[id]
    }

    fun getEntries(): List<LogEntry> {
        return logsById.values.sortedByDescending { it.startTs }
    }

    fun clear() {
        logsById.clear()
    }
}

data class LogEntry(
    val id: String,
    val startTs: Long,
    val method: String,
    val url: String,
    val host: String? = null,
    val path: String? = null,
    val query: String? = null,
    val reqHeadersJson: String? = null,
    val reqBody: String? = null,
    val reqBodyTruncated: Boolean = false,
    val platform: String,
    val correlationId: String? = null
) {
    var durationMs: Long? = null
    var resStatus: Long? = null
    var resHeadersJson: String? = null
    var resBody: String? = null
    var resBodyTruncated: Boolean = false
    var protocol: String? = null
    var ssl: Boolean = false
    var error: Boolean = false
    var errorMessage: String? = null

    fun toJsObject(): JSObject {
        return JSObject().apply {
            put("id", id)
            put("startTs", startTs)
            put("durationMs", durationMs)
            put("method", method)
            put("url", url)
            put("host", host)
            put("path", path)
            put("query", query)
            put("reqHeaders", reqHeadersJson)
            put("reqBody", reqBody)
            put("reqBodyTruncated", reqBodyTruncated)
            put("resStatus", resStatus)
            put("resHeaders", resHeadersJson)
            put("resBody", resBody)
            put("resBodyTruncated", resBodyTruncated)
            put("protocol", protocol)
            put("ssl", ssl)
            put("error", error)
            put("errorMessage", errorMessage)
            put("platform", platform)
            put("correlationId", correlationId)
        }
    }
}
