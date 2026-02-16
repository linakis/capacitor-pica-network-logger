package com.linakis.capacitorpicanetworklogger

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class InspectorActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val targetClassName = "com.linakis.capacitorpicanetworklogger.kmp.InspectorActivity"
        val targetClass = runCatching { Class.forName(targetClassName) }.getOrNull()

        if (targetClass != null) {
            try {
                startActivity(android.content.Intent(this, targetClass))
                finish()
                return
            } catch (err: Exception) {
                // Fall through to fallback UI
            }
        }

        val fallback = TextView(this)
        fallback.text = "Inspector UI not available."
        setContentView(fallback)
    }
}
