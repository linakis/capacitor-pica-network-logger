package com.linakis.capacitorpicanetworklogger

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object InspectorNotifications {
    private const val CHANNEL_ID = "cap_http_inspector"
    private const val CHANNEL_NAME = "Network Inspector"

    fun show(context: Context, method: String, url: String, status: Int?) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(context, InspectorActivity::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pending = PendingIntent.getActivity(context, 0, intent, flags)

        val path = runCatching {
            val uri = java.net.URI(url)
            val rawPath = uri.rawPath ?: ""
            val rawQuery = uri.rawQuery
            if (rawQuery.isNullOrBlank()) rawPath else "$rawPath?$rawQuery"
        }.getOrNull().orEmpty()
        val titleParts = mutableListOf<String>()
        if (status != null) titleParts.add(status.toString())
        if (method.isNotBlank()) titleParts.add(method)
        if (path.isNotBlank()) titleParts.add(path)
        val summary = if (titleParts.isNotEmpty()) titleParts.joinToString(" ") else "Network Inspector"
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(summary.ifEmpty { "Network Inspector" })
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pending)
            .setAutoCancel(true)

        if (url.isNotBlank()) {
            builder.setContentText(url)
        }

        val notification = builder.build()

        manager.notify((System.currentTimeMillis() % 10000).toInt(), notification)
    }
}
