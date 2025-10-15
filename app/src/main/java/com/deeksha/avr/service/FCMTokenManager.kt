package com.deeksha.avr.service

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FCMTokenManager @Inject constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val messaging = FirebaseMessaging.getInstance()
    
    suspend fun getFCMToken(): String? {
        return try {
            val token = messaging.token.await()
            Log.d("FCMTokenManager", "FCM Token: $token")
            token
        } catch (e: Exception) {
            Log.e("FCMTokenManager", "Error getting FCM token: ${e.message}")
            null
        }
    }
    
    suspend fun saveFCMTokenToUser(userId: String) {
        try {
            val token = getFCMToken()
            if (token != null) {
                val userDoc = firestore.collection("users").document(userId)
                val deviceInfo = mapOf(
                    "fcmToken" to token,
                    "isOnline" to true,
                    "lastLoginAt" to System.currentTimeMillis()
                )
                
                userDoc.update("deviceInfo", deviceInfo).await()
                Log.d("FCMTokenManager", "FCM token saved for user: $userId")
            }
        } catch (e: Exception) {
            Log.e("FCMTokenManager", "Error saving FCM token: ${e.message}")
        }
    }
    
    suspend fun removeFCMTokenFromUser(userId: String) {
        try {
            val userDoc = firestore.collection("users").document(userId)
            val deviceInfo = mapOf(
                "fcmToken" to null,
                "isOnline" to false,
                "lastLoginAt" to System.currentTimeMillis()
            )
            
            userDoc.update("deviceInfo", deviceInfo).await()
            Log.d("FCMTokenManager", "FCM token removed for user: $userId")
        } catch (e: Exception) {
            Log.e("FCMTokenManager", "Error removing FCM token: ${e.message}")
        }
    }
}



















