package com.pcare.shared

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Firebase Cloud Messaging helpers shared by the patient and agent apps. */
object Fcm {
    const val CHANNEL_ID = "pcare_events"

    fun ensureChannel(ctx: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Care updates", NotificationManager.IMPORTANCE_HIGH)
                        .apply { description = "Case, pickup and care notifications" })
            }
        }
    }

    /** Fetch the current FCM token and register it with the backend for the logged-in user. No-op if Firebase isn't set up. */
    fun registerCurrentToken(ctx: Context, session: Session) {
        if (!session.isLoggedIn) return
        ensureChannel(ctx)
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) return@addOnCompleteListener
                val token = task.result ?: return@addOnCompleteListener
                CoroutineScope(Dispatchers.IO).launch {
                    try { ApiClient.service(session).registerDeviceToken(DeviceTokenRequest(token)) } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) { /* Firebase not configured (no google-services.json) — ignore */ }
    }

    fun showLocal(ctx: Context, title: String?, body: String?) {
        ensureChannel(ctx)
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(ctx.applicationInfo.icon)
            .setContentTitle(title ?: "360 Patient Care")
            .setContentText(body ?: "")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            ctx.getSystemService(NotificationManager::class.java)
                ?.notify((System.currentTimeMillis() % 100000).toInt(), n)
        } catch (_: Exception) {}
    }
}

/** Receives FCM pushes: re-registers refreshed tokens and shows a notification when one arrives in the foreground. */
class PcareMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        try {
            val session = Session(applicationContext)
            if (session.isLoggedIn) {
                CoroutineScope(Dispatchers.IO).launch {
                    try { ApiClient.service(session).registerDeviceToken(DeviceTokenRequest(token)) } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        val title = msg.notification?.title ?: msg.data["title"]
        val body = msg.notification?.body ?: msg.data["body"]
        Fcm.showLocal(applicationContext, title, body)
    }
}
