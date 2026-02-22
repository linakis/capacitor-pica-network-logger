package com.linakis.capacitorpicanetworklogger

import com.getcapacitor.JSObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class LogRepository {
    private val logsById: MutableMap<String, LogEntry> = ConcurrentHashMap()
    private var database: LogDatabase? = null

    fun attach(@Suppress("UNUSED_PARAMETER") context: android.content.Context) {
        if (database != null) return
        val dbFile = File(context.applicationContext.filesDir, "cap-pica-logger.sqlite")
        database = LogDatabase(dbFile)
        database?.readAll()?.forEach { entry ->
            logsById[entry.id] = entry
        }
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
        database?.upsert(entry)
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
        database?.upsert(entry)

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
        database?.clear()
    }
}

private class LogDatabase(private val file: File) {
    private val driver = DatabaseDriver(file)

    init {
        driver.open()
        driver.exec(
            """
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
            """.trimIndent()
        )
    }

    fun upsert(entry: LogEntry) {
        driver.exec(
            """
            INSERT OR REPLACE INTO logs (
                id, startTs, durationMs, method, url, host, path, query, reqHeadersJson, reqBody,
                reqBodyTruncated, resStatus, resHeadersJson, resBody, resBodyTruncated, protocol,
                ssl, error, errorMessage, platform, correlationId
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
            """.trimIndent(),
            listOf(
                entry.id,
                entry.startTs,
                entry.durationMs,
                entry.method,
                entry.url,
                entry.host,
                entry.path,
                entry.query,
                entry.reqHeadersJson,
                entry.reqBody,
                if (entry.reqBodyTruncated) 1 else 0,
                entry.resStatus,
                entry.resHeadersJson,
                entry.resBody,
                if (entry.resBodyTruncated) 1 else 0,
                entry.protocol,
                if (entry.ssl) 1 else 0,
                if (entry.error) 1 else 0,
                entry.errorMessage,
                entry.platform,
                entry.correlationId
            )
        )
    }

    fun readAll(): List<LogEntry> {
        val rows = driver.query(
            """
            SELECT id, startTs, durationMs, method, url, host, path, query, reqHeadersJson, reqBody,
                   reqBodyTruncated, resStatus, resHeadersJson, resBody, resBodyTruncated, protocol,
                   ssl, error, errorMessage, platform, correlationId
            FROM logs
            """.trimIndent()
        )
        return rows.mapNotNull { row ->
            val id = row[0] as? String ?: return@mapNotNull null
            val startTs = (row[1] as? Number)?.toLong() ?: return@mapNotNull null
            val method = row[3] as? String ?: return@mapNotNull null
            val url = row[4] as? String ?: return@mapNotNull null
            val platform = row[19] as? String ?: return@mapNotNull null
            val entry = LogEntry(
                id = id,
                startTs = startTs,
                method = method,
                url = url,
                host = row[5] as? String,
                path = row[6] as? String,
                query = row[7] as? String,
                reqHeadersJson = row[8] as? String,
                reqBody = row[9] as? String,
                reqBodyTruncated = (row[10] as? Number)?.toInt() == 1,
                platform = platform,
                correlationId = row[20] as? String
            )
            entry.durationMs = (row[2] as? Number)?.toLong()
            entry.resStatus = (row[11] as? Number)?.toLong()
            entry.resHeadersJson = row[12] as? String
            entry.resBody = row[13] as? String
            entry.resBodyTruncated = (row[14] as? Number)?.toInt() == 1
            entry.protocol = row[15] as? String
            entry.ssl = (row[16] as? Number)?.toInt() == 1
            entry.error = (row[17] as? Number)?.toInt() == 1
            entry.errorMessage = row[18] as? String
            entry
        }
    }

    fun clear() {
        driver.exec("DELETE FROM logs")
    }
}

private class DatabaseDriver(private val file: File) {
    private var database: android.database.sqlite.SQLiteDatabase? = null

    fun open() {
        if (database != null) return
        database = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(file, null)
    }

    fun exec(sql: String, args: List<Any?> = emptyList()) {
        val db = database ?: return
        if (args.isEmpty()) {
            db.execSQL(sql)
        } else {
            db.execSQL(sql, args.toTypedArray())
        }
    }

    fun query(sql: String): List<List<Any?>> {
        val db = database ?: return emptyList()
        val cursor = db.rawQuery(sql, null)
        val rows = mutableListOf<List<Any?>>()
        cursor.use {
            val count = cursor.columnCount
            while (cursor.moveToNext()) {
                val row = ArrayList<Any?>(count)
                for (i in 0 until count) {
                    row.add(
                        when (cursor.getType(i)) {
                            android.database.Cursor.FIELD_TYPE_NULL -> null
                            android.database.Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i)
                            android.database.Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i)
                            android.database.Cursor.FIELD_TYPE_BLOB -> cursor.getBlob(i)
                            else -> cursor.getString(i)
                        }
                    )
                }
                rows.add(row)
            }
        }
        return rows
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
