package com.linakis.capacitorpicanetworklogger.kmp

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@Suppress("UNCHECKED_CAST")
fun InspectorViewController(
    onClose: (() -> Unit)? = null
) = ComposeUIViewController {
    val repo = InspectorRepository(DatabaseDriverFactory(null))
    InspectorScreen(
        repo,
        shareText = { _, text ->
            val items = listOf(text) as List<Any>
            val activity = UIActivityViewController(items, null)
            val root = UIApplication.sharedApplication.keyWindow?.rootViewController
            root?.presentViewController(activity, true, null)
        },
        saveText = { _, text ->
            val items = listOf(text) as List<Any>
            val activity = UIActivityViewController(items, null)
            val root = UIApplication.sharedApplication.keyWindow?.rootViewController
            root?.presentViewController(activity, true, null)
        },
        onClose = onClose
    )
}
