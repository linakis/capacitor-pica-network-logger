package com.linakis.capacitorpicanetworklogger.kmp

import app.cash.sqldelight.db.SqlDriver

expect class DatabaseDriverFactory(context: Any?) {
    fun createDriver(): SqlDriver
}
