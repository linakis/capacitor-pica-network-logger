package com.linakis.capacitorpicanetworklogger

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin

class LoggerConfigProvider {
    fun getConfig(plugin: Plugin): JSObject {
        val config = plugin.bridge.config.getPluginConfiguration("PicaNetworkLogger")
        val output = JSObject()
        output.put("maxBodySize", config.getInt("maxBodySize", 131072))
        output.put("redactHeaders", config.getArray("redactHeaders"))
        output.put("redactJsonFields", config.getArray("redactJsonFields"))
        output.put("notify", config.getBoolean("notify", true))
        return output
    }
}
