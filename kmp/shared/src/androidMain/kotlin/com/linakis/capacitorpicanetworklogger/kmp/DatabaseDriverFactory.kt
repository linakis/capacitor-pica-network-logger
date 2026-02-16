package com.linakis.capacitorpicanetworklogger.kmp

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.linakis.capacitorpicanetworklogger.kmp.db.InspectorDatabase

actual class DatabaseDriverFactory actual constructor(private val context: Any?) {
    actual fun createDriver(): SqlDriver {
        val appContext = context as Context
        return AndroidSqliteDriver(InspectorDatabase.Schema, appContext, "inspector_v2.db")
    }
}
