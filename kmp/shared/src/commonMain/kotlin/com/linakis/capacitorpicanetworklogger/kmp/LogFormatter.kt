package com.linakis.capacitorpicanetworklogger.kmp

import com.linakis.capacitorpicanetworklogger.kmp.db.Http_logs
import kotlinx.datetime.Instant

data class LogItem(
    val id: String,
    val method: String,
    val url: String,
    val status: Long?,
    val durationMs: Long?,
    val startTs: Long,
    val host: String,
    val path: String,
    val sizeBytes: Int
)

data class LogDetail(
    val id: String,
    val method: String,
    val url: String,
    val status: Long?,
    val durationMs: Long?,
    val startTs: Long,
    val host: String,
    val path: String,
    val reqHeadersJson: String?,
    val reqBody: String?,
    val resHeadersJson: String?,
    val resBody: String?,
    val protocol: String?,
    val ssl: Boolean,
    val error: Boolean,
    val errorMessage: String?
)

fun Http_logs.toLogItem(): LogItem {
    val parts = splitUrl(url)
    val size = (res_body?.length ?: 0)
    return LogItem(
        id = id,
        method = method,
        url = url,
        status = res_status,
        durationMs = duration_ms,
        startTs = start_ts,
        host = parts.first,
        path = parts.second,
        sizeBytes = size
    )
}

fun Http_logs.toLogDetail(): LogDetail {
    val parts = splitUrl(url)
    return LogDetail(
        id = id,
        method = method,
        url = url,
        status = res_status,
        durationMs = duration_ms,
        startTs = start_ts,
        host = parts.first,
        path = parts.second,
        reqHeadersJson = req_headers_json,
        reqBody = req_body,
        resHeadersJson = res_headers_json,
        resBody = res_body,
        protocol = protocol,
        ssl = ssl == 1L,
        error = error == 1L,
        errorMessage = error_message
    )
}

fun LogDetail.toCurl(): String {
    val sb = StringBuilder()
    sb.append("curl -X ").append(method).append(" '").append(url).append("'")
    if (!reqHeadersJson.isNullOrEmpty()) {
        val headerPart = formatHeadersForCurl(reqHeadersJson)
        if (headerPart.isNotEmpty()) {
            sb.append(" \\\n+  ").append(headerPart)
        }
    }
    if (!reqBody.isNullOrEmpty()) {
        sb.append(" \\\n+  --data '").append(escapeSingleQuotes(reqBody)).append("'")
    }
    return sb.toString()
}

fun LogDetail.toJson(): String {
    return buildJson(
        mapOf(
            "id" to id,
            "method" to method,
            "url" to url,
            "status" to status,
            "durationMs" to durationMs,
            "startTs" to startTs,
            "reqHeaders" to reqHeadersJson,
            "reqBody" to reqBody,
            "resHeaders" to resHeadersJson,
            "resBody" to resBody,
            "error" to error,
            "errorMessage" to errorMessage
        )
    )
}

fun LogDetail.toShareText(): String {
    val divider = "----------------------------------------"
    val requestHeaders = formatHeadersForShare(reqHeadersJson)
    val responseHeaders = formatHeadersForShare(resHeadersJson)
    val requestBody = formatBodyForShare(reqBody)
    val responseBody = formatBodyForShare(resBody)
    return buildString {
        append("URL\n")
        append(url)
        append("\n")
        append(divider)
        append("\nRequest Headers\n")
        append(requestHeaders.ifBlank { "-" })
        append("\n")
        append(divider)
        append("\nRequest Body\n")
        append(requestBody.ifBlank { "-" })
        append("\n")
        append(divider)
        append("\nResponse Headers\n")
        append(responseHeaders.ifBlank { "-" })
        append("\n")
        append(divider)
        append("\nResponse Body\n")
        append(responseBody.ifBlank { "-" })
    }
}

fun LogDetail.toHar(): String {
    val requestHeaders = headersJsonToMap(reqHeadersJson)
    val responseHeaders = headersJsonToMap(resHeadersJson)
    val started = isoTimestamp(startTs)
    val time = durationMs ?: 0
    val requestHeaderList = requestHeaders.entries.joinToString(",") { entry ->
        buildJson(mapOf("name" to entry.key, "value" to entry.value))
    }
    val responseHeaderList = responseHeaders.entries.joinToString(",") { entry ->
        buildJson(mapOf("name" to entry.key, "value" to entry.value))
    }
    val requestBody = reqBody ?: ""
    val responseBody = resBody ?: ""

    return buildJson(
        mapOf(
            "log" to buildJson(
                mapOf(
                    "version" to "1.2",
                    "creator" to buildJson(mapOf("name" to "capacitor-pica-network-logger", "version" to "0.1.0")),
                    "entries" to "[" + buildJson(
                        mapOf(
                            "startedDateTime" to started,
                            "time" to time,
                            "request" to buildJson(
                                mapOf(
                                    "method" to method,
                                    "url" to url,
                                    "httpVersion" to "HTTP/1.1",
                                    "headers" to "[${requestHeaderList}]",
                                    "queryString" to "[]",
                                    "headersSize" to -1,
                                    "bodySize" to requestBody.length,
                                    "postData" to buildJson(
                                        mapOf(
                                            "mimeType" to "text/plain",
                                            "text" to requestBody
                                        )
                                    )
                                )
                            ),
                            "response" to buildJson(
                                mapOf(
                                    "status" to (status ?: 0),
                                    "statusText" to "",
                                    "httpVersion" to "HTTP/1.1",
                                    "headers" to "[${responseHeaderList}]",
                                    "content" to buildJson(
                                        mapOf(
                                            "size" to responseBody.length,
                                            "mimeType" to "text/plain",
                                            "text" to responseBody
                                        )
                                    ),
                                    "redirectURL" to "",
                                    "headersSize" to -1,
                                    "bodySize" to responseBody.length
                                )
                            ),
                            "cache" to buildJson(emptyMap()),
                            "timings" to buildJson(mapOf("send" to 0, "wait" to time, "receive" to 0))
                        )
                    ) + "]"
                )
            )
        )
    )
}

fun List<LogDetail>.toHar(): String {
    val entries = joinToString(",") { it.toHarEntry() }
    return buildJson(
        mapOf(
            "log" to buildJson(
                mapOf(
                    "version" to "1.2",
                    "creator" to buildJson(mapOf("name" to "capacitor-pica-network-logger", "version" to "0.1.0")),
                    "entries" to "[" + entries + "]"
                )
            )
        )
    )
}

private fun LogDetail.toHarEntry(): String {
    val requestHeaders = headersJsonToMap(reqHeadersJson)
    val responseHeaders = headersJsonToMap(resHeadersJson)
    val started = isoTimestamp(startTs)
    val time = durationMs ?: 0
    val requestHeaderList = requestHeaders.entries.joinToString(",") { entry ->
        buildJson(mapOf("name" to entry.key, "value" to entry.value))
    }
    val responseHeaderList = responseHeaders.entries.joinToString(",") { entry ->
        buildJson(mapOf("name" to entry.key, "value" to entry.value))
    }
    val requestBody = reqBody ?: ""
    val responseBody = resBody ?: ""
    return buildJson(
        mapOf(
            "startedDateTime" to started,
            "time" to time,
            "request" to buildJson(
                mapOf(
                    "method" to method,
                    "url" to url,
                    "httpVersion" to "HTTP/1.1",
                    "headers" to "[${requestHeaderList}]",
                    "queryString" to "[]",
                    "headersSize" to -1,
                    "bodySize" to requestBody.length,
                    "postData" to buildJson(
                        mapOf(
                            "mimeType" to "text/plain",
                            "text" to requestBody
                        )
                    )
                )
            ),
            "response" to buildJson(
                mapOf(
                    "status" to (status ?: 0),
                    "statusText" to "",
                    "httpVersion" to "HTTP/1.1",
                    "headers" to "[${responseHeaderList}]",
                    "content" to buildJson(
                        mapOf(
                            "size" to responseBody.length,
                            "mimeType" to "text/plain",
                            "text" to responseBody
                        )
                    ),
                    "redirectURL" to "",
                    "headersSize" to -1,
                    "bodySize" to responseBody.length
                )
            ),
            "cache" to buildJson(emptyMap()),
            "timings" to buildJson(mapOf("send" to 0, "wait" to time, "receive" to 0))
        )
    )
}

private fun escapeSingleQuotes(value: String): String {
    return value.replace("'", "'\\''")
}

private fun buildJson(map: Map<String, Any?>): String {
    val sb = StringBuilder()
    sb.append("{")
    var first = true
    for ((key, value) in map) {
        if (!first) sb.append(",") else first = false
        sb.append("\"").append(escapeJson(key)).append("\":")
        sb.append(encodeJsonValue(value))
    }
    sb.append("}")
    return sb.toString()
}

private fun encodeJsonValue(value: Any?): String {
    return when (value) {
        null -> "null"
        is Number, is Boolean -> value.toString()
        else -> {
            val str = value.toString()
            if (str.startsWith("{") || str.startsWith("[") || str == "") {
                str
            } else {
                "\"${escapeJson(str)}\""
            }
        }
    }
}

private fun escapeJson(value: String): String {
    return buildString {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }
}

private fun headersJsonToMap(headersJson: String?): Map<String, String> {
    if (headersJson.isNullOrBlank()) return emptyMap()
    return try {
        val obj = org.json.JSONObject(headersJson)
        val keys = obj.keys()
        val map = mutableMapOf<String, String>()
        while (keys.hasNext()) {
            val key = keys.next()
            map[key] = obj.get(key).toString()
        }
        map
    } catch (_: Exception) {
        emptyMap()
    }
}

private fun formatHeadersForCurl(headersJson: String): String {
    val headers = headersJsonToMap(headersJson)
    if (headers.isEmpty()) return ""
    return headers.entries.joinToString(" \\\n+  ") { entry ->
        "-H '${escapeSingleQuotes(entry.key)}: ${escapeSingleQuotes(entry.value)}'"
    }
}

private fun formatHeadersForShare(headersJson: String?): String {
    if (headersJson.isNullOrBlank()) return ""
    return try {
        val obj = org.json.JSONObject(headersJson)
        val keys = obj.keys()
        val lines = mutableListOf<String>()
        while (keys.hasNext()) {
            val key = keys.next()
            lines.add("$key: ${obj.get(key)}")
        }
        lines.joinToString("\n")
    } catch (_: Exception) {
        headersJson
    }
}

private fun formatBodyForShare(body: String?): String {
    if (body.isNullOrBlank()) return ""
    return try {
        val obj = org.json.JSONObject(body)
        obj.toString(2)
    } catch (_: Exception) {
        body
    }
}

private fun isoTimestamp(epochMillis: Long): String {
    val seconds = epochMillis / 1000
    val millis = epochMillis % 1000
    val date = Instant.fromEpochSeconds(seconds, millis.toInt() * 1_000_000)
    return date.toString()
}

private fun splitUrl(raw: String): Pair<String, String> {
    if (raw.isBlank()) return "" to ""
    val schemeIndex = raw.indexOf("://")
    val start = if (schemeIndex >= 0) schemeIndex + 3 else 0
    val slashIndex = raw.indexOf('/', start)
    val host = if (slashIndex >= 0) raw.substring(start, slashIndex) else raw.substring(start)
    val path = if (slashIndex >= 0) raw.substring(slashIndex) else "/"
    return host to path
}
