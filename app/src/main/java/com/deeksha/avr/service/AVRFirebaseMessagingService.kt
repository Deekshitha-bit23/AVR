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
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AVRFirebaseMessagingService : FirebaseMessagingService() {
    
    companion object {
        private const val CHANNEL_ID = "message_notifications"
        private const val CHANNEL_NAME = "Message Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for new chat messages"
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        android.util.Log.d("AVRFirebaseMessagingService", "Message received: ${remoteMessage.data}")
        
        // Handle data payload
        val data = remoteMessage.data
        val senderName = data["senderName"] ?: "Unknown"
        val message = data["message"] ?: ""
        val projectId = data["projectId"] ?: ""
        val chatId = data["chatId"] ?: ""
        
        // Show notification
        showNotification(senderName, message, projectId, chatId)
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        android.util.Log.d("AVRFirebaseMessagingService", "New FCM token: $token")
        
        // TODO: Send token to your server
        // You can store this token in Firestore under user's deviceInfo
    }
    
    private fun showNotification(
        senderName: String,
        message: String,
        projectId: String,
        chatId: String
    ) {
        try {
            createNotificationChannel()
            
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("projectId", projectId)
                putExtra("chatId", chatId)
                putExtra("navigateToChat", true)
            }
            
            val pendingIntent = PendingIntent.getActivity(
                this,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("New message from $senderName")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 1000, 500, 1000))
                .build()
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            
            android.util.Log.d("AVRFirebaseMessagingService", "Notification shown for message from $senderName")
            
        } catch (e: Exception) {
            android.util.Log.e("AVRFirebaseMessagingService", "Error showing notification: ${e.message}", e)
        }
    }
    
    private fun createNotificationChannel() {
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
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
