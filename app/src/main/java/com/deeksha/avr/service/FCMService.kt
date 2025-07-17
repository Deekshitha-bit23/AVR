package com.deeksha.avr.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.deeksha.avr.MainActivity
import com.deeksha.avr.R
import com.deeksha.avr.model.Notification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.util.Log
import javax.inject.Inject

class FCMService : FirebaseMessagingService() {
    
    companion object {
        private const val CHANNEL_ID = "expense_notifications"
        private const val CHANNEL_NAME = "Expense Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for expense updates and approvals"
        
        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    enableLights(true)
                }
                
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCMService", "🔄 New FCM token: $token")
        // TODO: Send token to server to associate with user
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d("FCMService", "📨 Received FCM message: ${remoteMessage.data}")
        
        // Extract notification data
        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "New Notification"
        val message = remoteMessage.data["message"] ?: remoteMessage.notification?.body ?: ""
        val projectId = remoteMessage.data["projectId"] ?: ""
        val expenseId = remoteMessage.data["expenseId"] ?: ""
        val notificationType = remoteMessage.data["type"] ?: "INFO"
        
        Log.d("FCMService", "📋 Notification data: title=$title, message=$message, projectId=$projectId, expenseId=$expenseId")
        
        // Create and show notification
        showNotification(title, message, projectId, expenseId, notificationType)
    }
    
    private fun showNotification(
        title: String,
        message: String,
        projectId: String,
        expenseId: String,
        notificationType: String
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create intent for notification tap
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("projectId", projectId)
            putExtra("expenseId", expenseId)
            putExtra("notificationType", notificationType)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()
        
        // Show notification
        val notificationId = System.currentTimeMillis().toInt()
        notificationManager.notify(notificationId, notification)
        
        Log.d("FCMService", "✅ Notification displayed with ID: $notificationId")
    }
} 