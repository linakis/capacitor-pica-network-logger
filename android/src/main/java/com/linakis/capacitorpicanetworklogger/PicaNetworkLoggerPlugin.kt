package com.linakis.capacitorpicanetworklogger

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import org.json.JSONObject

@CapacitorPlugin(name = "PicaNetworkLogger")
class PicaNetworkLoggerPlugin : Plugin() {
    private val repository = LogRepository()
    private val configProvider = LoggerConfigProvider()

    override fun load() {
        super.load()
        repository.attach(bridge.context)
        val config = configProvider.getConfig(this)
        LogRepositoryStore.attach(bridge.context, repository, config.getInt("maxBodySize"))
        val redactHeaders = config.get("redactHeaders")?.let { value ->
            when (value) {
                is org.json.JSONArray -> (0 until value.length()).map { index -> value.get(index).toString() }
                else -> null
            }
        }
        val redactJsonFields = config.get("redactJsonFields")?.let { value ->
            when (value) {
                is org.json.JSONArray -> (0 until value.length()).map { index -> value.get(index).toString() }
                else -> null
            }
        }
        LogRepositoryStore.updateRedaction(redactHeaders, redactJsonFields)
        LogRepositoryStore.updateNotify(config.optBoolean("notify", true))
        requestNotificationPermissionIfNeeded(config.optBoolean("notify", true))
    }

    private fun requestNotificationPermissionIfNeeded(notifyEnabled: Boolean) {
        if (Build.VERSION.SDK_INT < 33) {
            return
        }
        if (!notifyEnabled) {
            return
        }
        val activity = activity ?: return
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
    }


    @PluginMethod
    fun startRequest(call: PluginCall) {
        val method = call.getString("method") ?: "GET"
        val url = call.getString("url") ?: ""
        val headers = call.getObject("headers")?.let { obj ->
            obj.keys().asSequence().associateWith { key -> obj.getString(key) ?: "" }
        }
        val body = call.data.opt("body")?.let { if ( it == JSONObject.NULL) null else it.toString() }
        val id = java.util.UUID.randomUUID().toString()
        LogRepositoryStore.logStart(id, method, url, headers, body)
        val ret = JSObject()
        ret.put("id", id)
        call.resolve(ret)
    }

    @PluginMethod
    fun finishRequest(call: PluginCall) {
        val id = call.getString("id") ?: return call.reject("Missing id")
        val status = call.getInt("status")
        val headers = call.getObject("headers")?.let { obj ->
            obj.keys().asSequence().associateWith { key -> obj.getString(key) ?: "" }
        }
        val body = call.data.opt("body")?.let { if ( it == JSONObject.NULL) null else it.toString() }
        val error = call.getString("error")
        LogRepositoryStore.logFinish(id, status, headers, body, error, null, null)
        call.resolve()
    }

    @PluginMethod
    fun openInspector(call: PluginCall) {
        val context = bridge.context
        val intent = android.content.Intent(context, InspectorActivity::class.java)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        call.resolve()
    }

}
