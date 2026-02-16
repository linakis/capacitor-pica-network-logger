package com.linakis.capacitorpicanetworklogger.kmp

import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSObject
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

fun InspectorViewController() = ComposeUIViewController {
    val repo = InspectorRepository(DatabaseDriverFactory(null))
    InspectorScreen(
        repo,
        shareText = { title, text ->
            val items = listOf(text) as List<NSObject>
            val activity = UIActivityViewController(items, null)
            val root = UIApplication.sharedApplication.keyWindow?.rootViewController
            root?.presentViewController(activity, true, null)
        },
        saveText = { fileName, text ->
            val items = listOf(text) as List<NSObject>
            val activity = UIActivityViewController(items, null)
            val root = UIApplication.sharedApplication.keyWindow?.rootViewController
            root?.presentViewController(activity, true, null)
        }
    )
}
