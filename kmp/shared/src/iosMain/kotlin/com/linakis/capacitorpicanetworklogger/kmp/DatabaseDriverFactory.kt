package com.linakis.capacitorpicanetworklogger.kmp

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.linakis.capacitorpicanetworklogger.kmp.db.InspectorDatabase

actual class DatabaseDriverFactory actual constructor(private val context: Any?) {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(InspectorDatabase.Schema, "inspector.db")
    }
}
