package com.linakis.capacitorpicanetworklogger.kmp

import android.os.Bundle
import androidx.activity.ComponentActivity
import android.content.Intent
import androidx.activity.compose.setContent
import android.os.Build
import android.view.WindowInsetsController
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme

class InspectorActivity : ComponentActivity() {
    private var pendingSave: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val repo = InspectorRepository(DatabaseDriverFactory(this))
            InspectorScreen(
                repo,
                shareText = { title, text ->
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_SUBJECT, title)
                    intent.putExtra(Intent.EXTRA_TEXT, text)
                    startActivity(Intent.createChooser(intent, "Share"))
                },
                saveText = { fileName, text ->
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TITLE, fileName)
                    pendingSave = text
                    startActivityForResult(intent, 7001)
                },
                colorSchemeProvider = { isDark ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        if (isDark) dynamicDarkColorScheme(this) else dynamicLightColorScheme(this)
                    } else {
                        null
                    }
                },
                onThemeChange = { isDark ->
                    val window = window
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val controller = window.insetsController
                        if (controller != null) {
                            val appearance = if (isDark) 0 else WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                            controller.setSystemBarsAppearance(appearance, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS)
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val flags = window.decorView.systemUiVisibility
                        @Suppress("DEPRECATION")
                        window.decorView.systemUiVisibility = if (isDark) {
                            flags and android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                        } else {
                            flags or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                        }
                    }
                },
                onClose = {
                    finish()
                }
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 7001) return
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data ?: return
        val content = pendingSave ?: return
        contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(content.toByteArray())
        }
        pendingSave = null
    }
}
