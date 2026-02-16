package com.linakis.capacitorpicanetworklogger

import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

@CapacitorPlugin(name = "PicaNetworkLogger")
class PicaNetworkLoggerPlugin : Plugin() {
    private val repository = LogRepository()
    private val configProvider = LoggerConfigProvider()

    override fun load() {
        super.load()
        repository.attach(bridge.context)
        LogRepositoryStore.attach(bridge.context, repository, configProvider.getConfig(this).getInt("maxBodySize"))
        LogRepositoryStore.updateNotify(configProvider.getConfig(this).getBoolean("notify", true))
        requestNotificationPermissionIfNeeded()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < 33) {
            return
        }
        val notifyEnabled = configProvider.getConfig(this).getBoolean("notify", true)
        if (notifyEnabled != true) {
            return
        }
        val activity = activity ?: return
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
    }

    @PluginMethod
    fun requestNotificationPermission(call: PluginCall) {
        if (Build.VERSION.SDK_INT < 33) {
            call.resolve()
            return
        }
        val activity = activity ?: run {
            call.reject("No activity")
            return
        }
        if (ActivityCompat.checkSelfPermission(activity, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            call.resolve()
            return
        }
        ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
        call.resolve()
    }


    @PluginMethod
    fun startRequest(call: PluginCall) {
        val id = call.getString("id") ?: return call.reject("Missing id")
        repository.startRequest(call.data)
        val ret = JSObject()
        ret.put("id", id)
        call.resolve(ret)
    }

    @PluginMethod
    fun finishRequest(call: PluginCall) {
        repository.finishRequest(call.data)
        call.resolve()
    }

    @PluginMethod
    fun getLogs(call: PluginCall) {
        val ret = JSObject()
        ret.put("logs", repository.getLogs())
        call.resolve(ret)
    }

    @PluginMethod
    fun getLog(call: PluginCall) {
        val id = call.getString("id")
        val ret = JSObject()
        ret.put("log", repository.getLog(id))
        call.resolve(ret)
    }

    @PluginMethod
    fun clearLogs(call: PluginCall) {
        repository.clear()
        call.resolve()
    }

    @PluginMethod
    fun getConfig(call: PluginCall) {
        val ret = configProvider.getConfig(this)
        val maxBodySize = ret.getInt("maxBodySize")
        if (maxBodySize != null) {
            LogRepositoryStore.updateMaxBodySize(maxBodySize)
        }
        LogRepositoryStore.updateNotify(ret.getBoolean("notify", true))
        val redactHeaders = ret.get("redactHeaders")?.let { value ->
            when (value) {
                is org.json.JSONArray -> (0 until value.length()).map { index -> value.get(index).toString() }
                else -> null
            }
        }
        val redactJsonFields = ret.get("redactJsonFields")?.let { value ->
            when (value) {
                is org.json.JSONArray -> (0 until value.length()).map { index -> value.get(index).toString() }
                else -> null
            }
        }
        LogRepositoryStore.updateRedaction(redactHeaders, redactJsonFields)
        call.resolve(ret)
    }

    @PluginMethod
    fun openInspector(call: PluginCall) {
        val context = bridge.context
        val intent = android.content.Intent(context, InspectorActivity::class.java)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        call.resolve()
    }

    @PluginMethod
    fun showNotification(call: PluginCall) {
        val method = call.getString("method") ?: ""
        val url = call.getString("url") ?: ""
        val status = call.getInt("status")
        InspectorNotifications.show(bridge.context, method, url, status)
        call.resolve()
    }
}
