package com.nousresearch.hermes.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nousresearch.hermes.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Hermes",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Hermes approvals, task completions, and background status"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun approvalNeeded(command: String) {
        notify(
            id = 1001,
            title = "Hermes approval needed",
            text = command.take(120),
            priority = NotificationCompat.PRIORITY_HIGH,
        )
    }

    fun taskCompleted(summary: String) {
        notify(
            id = 1002,
            title = "Hermes task complete",
            text = summary.take(120),
            priority = NotificationCompat.PRIORITY_DEFAULT,
        )
    }

    private fun notify(id: Int, title: String, text: String, priority: Int) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(priority)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = "hermes_notifications"
    }
}
