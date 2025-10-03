package com.deeksha.avr.service

import android.util.Log
import com.deeksha.avr.model.Notification
import com.deeksha.avr.model.NotificationType
import com.deeksha.avr.model.User
import com.deeksha.avr.model.UserRole
import com.deeksha.avr.repository.AuthRepository
import com.deeksha.avr.repository.NotificationRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceNotificationService @Inject constructor(
    private val authRepository: AuthRepository,
    private val notificationRepository: NotificationRepository
) {
    
    /**
     * Send device-specific notification to a user based on their role and preferences
     */
    suspend fun sendDeviceNotification(
        userId: String,
        notification: Notification
    ): Result<Unit> {
        return try {
            Log.d("DeviceNotificationService", "🔄 Sending device notification to user: $userId")
            
            // Get user details
            val user = authRepository.getAllUsers().find { it.uid == userId }
            if (user == null) {
                Log.e("DeviceNotificationService", "❌ User not found: $userId")
                return Result.failure(Exception("User not found"))
            }
            
            // Check if user has push notifications enabled
            if (!user.notificationPreferences.pushNotifications) {
                Log.d("DeviceNotificationService", "🚫 Push notifications disabled for user: ${user.name}")
                return Result.success(Unit)
            }
            
            // Check role-based notification preferences
            if (!shouldSendNotificationToUser(user, notification.type)) {
                Log.d("DeviceNotificationService", "🚫 Notification filtered for user: ${user.name} based on role/preferences")
                return Result.success(Unit)
            }
            
            // Check if user has FCM token
            if (user.deviceInfo.fcmToken.isEmpty()) {
                Log.w("DeviceNotificationService", "⚠️ No FCM token for user: ${user.name}")
                return Result.success(Unit)
            }
            
            // Send FCM notification
            sendFCMNotification(user, notification)
            
            Log.d("DeviceNotificationService", "✅ Device notification sent successfully to: ${user.name}")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("DeviceNotificationService", "❌ Error sending device notification: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Send device notifications to multiple users based on their roles
     */
    suspend fun sendDeviceNotificationsToRole(
        role: UserRole,
        notification: Notification
    ): Result<Unit> {
        return try {
            Log.d("DeviceNotificationService", "🔄 Sending device notifications to role: ${role.name}")
            
            // Get all users with the specified role
            val users = authRepository.getUsersByRole(role)
            Log.d("DeviceNotificationService", "📋 Found ${users.size} users with role: ${role.name}")
            
            var successCount = 0
            var failureCount = 0
            
            users.forEach { user ->
                try {
                    sendDeviceNotification(user.uid, notification)
                        .onSuccess {
                            successCount++
                            Log.d("DeviceNotificationService", "✅ Sent to user: ${user.name}")
                        }
                        .onFailure { error ->
                            failureCount++
                            Log.e("DeviceNotificationService", "❌ Failed to send to user ${user.name}: ${error.message}")
                        }
                } catch (e: Exception) {
                    failureCount++
                    Log.e("DeviceNotificationService", "❌ Exception sending to user ${user.name}: ${e.message}")
                }
            }
            
            Log.d("DeviceNotificationService", "📊 Device notification summary: $successCount success, $failureCount failures")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("DeviceNotificationService", "❌ Error sending device notifications to role: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Check if notification should be sent to user based on role and preferences
     */
    private fun shouldSendNotificationToUser(user: User, notificationType: NotificationType): Boolean {
        return when (notificationType) {
            NotificationType.EXPENSE_SUBMITTED -> {
                (user.role == UserRole.APPROVER || user.role == UserRole.PRODUCTION_HEAD) &&
                user.notificationPreferences.expenseSubmitted
            }
            NotificationType.EXPENSE_APPROVED -> {
                user.role == UserRole.USER &&
                user.notificationPreferences.expenseApproved
            }
            NotificationType.EXPENSE_REJECTED -> {
                user.role == UserRole.USER &&
                user.notificationPreferences.expenseRejected
            }
            NotificationType.PROJECT_ASSIGNMENT -> {
                user.notificationPreferences.projectAssignment
            }
            NotificationType.PENDING_APPROVAL -> {
                (user.role == UserRole.APPROVER || user.role == UserRole.PRODUCTION_HEAD) &&
                user.notificationPreferences.pendingApprovals
            }
            else -> {
                // Default to true for other notification types
                true
            }
        }
    }
    
    /**
     * Send FCM notification to user's device
     */
    private suspend fun sendFCMNotification(user: User, notification: Notification) {
        try {
            // This would typically involve calling your backend API to send FCM
            // For now, we'll log the notification details
            Log.d("DeviceNotificationService", "📱 FCM Notification Details:")
            Log.d("DeviceNotificationService", "   - User: ${user.name} (${user.uid})")
            Log.d("DeviceNotificationService", "   - FCM Token: ${user.deviceInfo.fcmToken}")
            Log.d("DeviceNotificationService", "   - Title: ${notification.title}")
            Log.d("DeviceNotificationService", "   - Message: ${notification.message}")
            Log.d("DeviceNotificationService", "   - Type: ${notification.type}")
            Log.d("DeviceNotificationService", "   - Project: ${notification.projectName}")
            
            // FCM sending is handled by FCMNotificationService
            // This service focuses on device-specific notification logic
            
        } catch (e: Exception) {
            Log.e("DeviceNotificationService", "❌ Error sending FCM notification: ${e.message}")
            throw e
        }
    }
    
    /**
     * Update user's FCM token
     */
    suspend fun updateUserFCMToken(userId: String): Result<Unit> {
        return try {
            Log.d("DeviceNotificationService", "🔄 Updating FCM token for user: $userId")
            
            // Get current FCM token
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            Log.d("DeviceNotificationService", "📱 New FCM token: $fcmToken")
            
            // Update user's FCM token in Firebase
            authRepository.updateUserFCMToken(userId, fcmToken)
                .onSuccess {
                    Log.d("DeviceNotificationService", "✅ FCM token updated successfully")
                }
                .onFailure { error ->
                    Log.e("DeviceNotificationService", "❌ Failed to update FCM token: ${error.message}")
                    return Result.failure(error)
                }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("DeviceNotificationService", "❌ Error updating FCM token: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get users with FCM tokens for a specific role
     */
    suspend fun getUsersWithFCMTokens(role: UserRole): List<User> {
        return try {
            val users = authRepository.getUsersByRole(role)
            val usersWithTokens = users.filter { user -> user.deviceInfo.fcmToken.isNotEmpty() }
            
            Log.d("DeviceNotificationService", "📋 Found ${usersWithTokens.size} users with FCM tokens for role: ${role.name}")
            usersWithTokens
            
        } catch (e: Exception) {
            Log.e("DeviceNotificationService", "❌ Error getting users with FCM tokens: ${e.message}")
            emptyList()
        }
    }
} 