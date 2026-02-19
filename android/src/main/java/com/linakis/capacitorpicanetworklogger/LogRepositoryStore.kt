package com.linakis.capacitorpicanetworklogger

import com.getcapacitor.JSObject

object LogRepositoryStore {
    private var repository: LogRepository? = null
    private var appContext: android.content.Context? = null
    private var maxBodySize: Int = 131072
    private var redactHeaders: Set<String> = setOf("authorization", "cookie")
    private var redactJsonFields: Set<String> = setOf("password", "token")
    private var notifyEnabled: Boolean = true
    private val requestStartTs: MutableMap<String, Long> = mutableMapOf()

    fun attach(context: android.content.Context, repo: LogRepository? = null, maxBodySize: Int? = null) {
        val instance = repo ?: LogRepository()
        instance.attach(context)
        repository = instance
        appContext = context.applicationContext
        if (maxBodySize != null) {
            this.maxBodySize = maxBodySize
        }
    }

    fun updateRedaction(headers: List<String>?, jsonFields: List<String>?) {
        if (headers != null) {
            redactHeaders = headers.map { it.lowercase() }.toSet()
        }
        if (jsonFields != null) {
            redactJsonFields = jsonFields.map { it.lowercase() }.toSet()
        }
    }

    fun updateNotify(enabled: Boolean?) {
        if (enabled != null) {
            notifyEnabled = enabled
        }
    }

    fun notify(method: String, url: String, status: Int?) {
        if (!notifyEnabled) return
        val context = appContext ?: return
        InspectorNotifications.show(context, method, url, status)
    }

    fun getRepository(): LogRepository? = repository

    fun logStart(
        id: String,
        method: String,
        url: String,
        headers: Map<String, String>?,
        body: String?
    ) {
        val data = JSObject()
        data.put("id", id)
        data.put("method", method)
        data.put("url", url)
        val startTs = System.currentTimeMillis()
        data.put("startTs", startTs)
        data.put("headers", headers?.redactedHeaders()?.toJsObject())
        val truncated = truncate(body?.redactJson())
        data.put("requestBody", truncated.value)
        data.put("requestBodyTruncated", truncated.truncated)
        requestStartTs[id] = startTs
        repository?.startRequest(data)
    }

    fun logFinish(
        id: String,
        status: Int?,
        headers: Map<String, String>?,
        body: String?,
        error: String?,
        protocol: String? = null,
        ssl: Boolean? = null
    ) {
        val data = JSObject()
        data.put("id", id)
        if (status != null) data.put("status", status)
        data.put("headers", headers?.redactedHeaders()?.toJsObject())
        val truncated = truncate(body?.redactJson())
        data.put("responseBody", truncated.value)
        data.put("responseBodyTruncated", truncated.truncated)
        if (error != null) data.put("error", error)
        if (protocol != null) data.put("protocol", protocol)
        if (ssl != null) data.put("ssl", ssl)
        val startTs = requestStartTs[id]
        if (startTs != null) {
            data.put("durationMs", System.currentTimeMillis() - startTs)
        }
        repository?.finishRequest(data)
        requestStartTs.remove(id)
    }

    private fun truncate(value: String?): Truncated {
        if (value == null) return Truncated(null, false)
        if (value.length <= maxBodySize) return Truncated(value, false)
        return Truncated(value.substring(0, maxBodySize), true)
    }

    private fun Map<String, String>.redactedHeaders(): Map<String, String> {
        return mapValues { (key, value) ->
            if (redactHeaders.contains(key.lowercase())) "[REDACTED]" else value
        }
    }

    private fun String.redactJson(): String {
        return try {
            val json = org.json.JSONObject(this)
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (redactJsonFields.contains(key.lowercase())) {
                    json.put(key, "[REDACTED]")
                }
            }
            json.toString()
        } catch (_: Exception) {
            this
        }
    }

    private data class Truncated(val value: String?, val truncated: Boolean)
}

private fun Map<String, String>.toJsObject(): JSObject {
    val obj = JSObject()
    forEach { (key, value) ->
        obj.put(key, value)
    }
    return obj
}
