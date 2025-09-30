package com.deeksha.avr.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.deeksha.avr.R
import com.deeksha.avr.MainActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageNotificationService @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val messaging = FirebaseMessaging.getInstance()
    
    companion object {
        private const val CHANNEL_ID = "message_notifications"
        private const val CHANNEL_NAME = "Message Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for new chat messages"
        private const val NOTIFICATION_ID = 1001
    }
    
    private fun createNotificationChannel(context: Context) {
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
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    suspend fun sendMessageNotification(
        context: Context,
        projectId: String,
        chatId: String,
        senderId: String,
        senderName: String,
        message: String,
        recipientIds: List<String>
    ) {
        try {
            createNotificationChannel(context)
            android.util.Log.d("MessageNotificationService", "Sending notification for message from $senderName to ${recipientIds.size} recipients")
            
            // Get FCM tokens for all recipients
            val recipientTokens = mutableListOf<String>()
            
            for (recipientId in recipientIds) {
                val token = getFCMToken(recipientId)
                if (token != null) {
                    recipientTokens.add(token)
                    android.util.Log.d("MessageNotificationService", "Found FCM token for $recipientId")
                } else {
                    android.util.Log.w("MessageNotificationService", "No FCM token found for $recipientId")
                }
            }
            
            if (recipientTokens.isNotEmpty()) {
            // Send FCM notification
            sendFCMNotification(
                context = context,
                tokens = recipientTokens,
                senderName = senderName,
                message = message,
                projectId = projectId,
                chatId = chatId
            )
            } else {
                android.util.Log.w("MessageNotificationService", "No FCM tokens found for any recipients")
            }
            
        } catch (e: Exception) {
            android.util.Log.e("MessageNotificationService", "Error sending notification: ${e.message}", e)
        }
    }
    
    private suspend fun getFCMToken(userId: String): String? {
        return try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val deviceInfo = userDoc.get("deviceInfo") as? Map<String, Any>
            deviceInfo?.get("fcmToken") as? String
        } catch (e: Exception) {
            android.util.Log.e("MessageNotificationService", "Error getting FCM token for $userId: ${e.message}")
            null
        }
    }
    
    private suspend fun sendFCMNotification(
        context: Context,
        tokens: List<String>,
        senderName: String,
        message: String,
        projectId: String,
        chatId: String
    ) {
        try {
            // For now, we'll use a simple approach
            // In a real app, you'd send this to your backend server
            // which would then send FCM notifications
            
            android.util.Log.d("MessageNotificationService", "Would send FCM notification to ${tokens.size} devices")
            android.util.Log.d("MessageNotificationService", "Message: $senderName: $message")
            
            // For immediate testing, show a local notification
            showLocalNotification(context, senderName, message, projectId, chatId)
            
        } catch (e: Exception) {
            android.util.Log.e("MessageNotificationService", "Error sending FCM notification: ${e.message}", e)
        }
    }
    
    private fun showLocalNotification(
        context: Context,
        senderName: String,
        message: String,
        projectId: String,
        chatId: String
    ) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("projectId", projectId)
                putExtra("chatId", chatId)
                putExtra("navigateToChat", true)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // You might want to create a chat icon
                .setContentTitle("New message from $senderName")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 1000, 500, 1000))
                .build()
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            android.util.Log.d("MessageNotificationService", "Local notification shown")
            
        } catch (e: Exception) {
            android.util.Log.e("MessageNotificationService", "Error showing local notification: ${e.message}", e)
        }
    }
}
