package com.deeksha.avr.service

import android.util.Log
import com.deeksha.avr.model.User
import com.deeksha.avr.model.UserRole
import com.deeksha.avr.repository.AuthRepository
import com.deeksha.avr.repository.ProjectRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FCMNotificationService @Inject constructor(
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository
) {
    
    companion object {
        private const val TAG = "FCMNotificationService"
        private const val FCM_SERVER_KEY = "YOUR_FCM_SERVER_KEY" // Replace with your actual FCM server key
        private const val FCM_URL = "https://fcm.googleapis.com/fcm/send"
    }
    
    /**
     * Check if a user is assigned to a specific project
     */
    private suspend fun isUserAssignedToProject(user: User, projectId: String): Boolean {
        return when (user.role) {
            UserRole.USER -> {
                // For USER role, check if the project is in their assignedProjects list
                user.assignedProjects.contains(projectId)
            }
            UserRole.APPROVER -> {
                // For APPROVER role, check if they are in the project's approverIds
                val project = projectRepository.getProjectById(projectId)
                project?.approverIds?.contains(user.uid) == true || project?.managerId == user.uid
            }
            UserRole.PRODUCTION_HEAD -> {
                // For PRODUCTION_HEAD role, check if they are in the project's productionHeadIds
                val project = projectRepository.getProjectById(projectId)
                project?.productionHeadIds?.contains(user.uid) == true
            }
            UserRole.ADMIN -> {
                // ADMIN can see all projects
                true
            }
        }
    }
    
    /**
     * Send FCM notification to a specific user by their user ID
     */
    suspend fun sendNotificationToUser(
        userId: String,
        title: String,
        message: String,
        projectId: String = "",
        expenseId: String = "",
        notificationType: String = "INFO"
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Sending FCM notification to user: $userId")
            
            // Get user details including FCM token
            val user = authRepository.getUserById(userId)
            if (user == null) {
                Log.e(TAG, "❌ User not found: $userId")
                return Result.failure(Exception("User not found"))
            }
            
            // If projectId is provided, check if user is assigned to the project
            if (projectId.isNotEmpty() && !isUserAssignedToProject(user, projectId)) {
                Log.w(TAG, "⚠️ User ${user.name} (${user.uid}) is not assigned to project $projectId")
                Log.w(TAG, "⚠️ Skipping FCM notification to prevent unauthorized access")
                return Result.success(Unit) // Don't send notification if user is not assigned to project
            }
            
            val fcmToken = user.deviceInfo.fcmToken
            if (fcmToken.isEmpty()) {
                Log.w(TAG, "⚠️ User $userId has no FCM token")
                return Result.failure(Exception("User has no FCM token"))
            }
            
            Log.d(TAG, "📱 Found FCM token for user ${user.name}: $fcmToken")
            
            // Send FCM notification
            sendFCMNotification(
                fcmToken = fcmToken,
                title = title,
                message = message,
                projectId = projectId,
                expenseId = expenseId,
                notificationType = notificationType
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending notification to user $userId: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Send FCM notification to multiple users by their user IDs
     */
    suspend fun sendNotificationToUsers(
        userIds: List<String>,
        title: String,
        message: String,
        projectId: String = "",
        expenseId: String = "",
        notificationType: String = "INFO"
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Sending FCM notification to ${userIds.size} users")
            
            val results = mutableListOf<Result<Unit>>()
            
            for (userId in userIds) {
                val result = sendNotificationToUser(
                    userId = userId,
                    title = title,
                    message = message,
                    projectId = projectId,
                    expenseId = expenseId,
                    notificationType = notificationType
                )
                results.add(result)
            }
            
            val successCount = results.count { it.isSuccess }
            val failureCount = results.count { it.isFailure }
            
            Log.d(TAG, "📊 FCM notification results: $successCount success, $failureCount failures")
            
            if (successCount > 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to send notifications to any users"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending notifications to users: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Send FCM notification to users by role
     */
    suspend fun sendNotificationToUsersByRole(
        role: UserRole,
        title: String,
        message: String,
        projectId: String = "",
        expenseId: String = "",
        notificationType: String = "INFO"
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Sending FCM notification to users with role: ${role.name}")
            
            // Get all users with the specified role
            val users = authRepository.getUsersByRole(role)
            val usersWithTokens = users.filter { it.deviceInfo.fcmToken.isNotEmpty() }
            
            Log.d(TAG, "📋 Found ${usersWithTokens.size} users with FCM tokens for role: ${role.name}")
            
            if (usersWithTokens.isEmpty()) {
                Log.w(TAG, "⚠️ No users with FCM tokens found for role: ${role.name}")
                return Result.failure(Exception("No users with FCM tokens found for role: ${role.name}"))
            }
            
            val userIds = usersWithTokens.map { it.uid }
            sendNotificationToUsers(
                userIds = userIds,
                title = title,
                message = message,
                projectId = projectId,
                expenseId = expenseId,
                notificationType = notificationType
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending notification to users by role: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Send FCM notification to users by role with project assignment filtering
     * This ensures that USER role users only receive notifications for projects they are assigned to
     */
    suspend fun sendNotificationToUsersByRoleWithProjectFilter(
        role: UserRole,
        title: String,
        message: String,
        projectId: String,
        expenseId: String = "",
        notificationType: String = "INFO"
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Sending FCM notification to users with role: ${role.name} for project: $projectId")
            
            // Get all users with the specified role
            val users = authRepository.getUsersByRole(role)
            Log.d(TAG, "📋 Found ${users.size} users with role: ${role.name}")
            
            // Filter users by project assignment and FCM token availability
            val eligibleUsers = users.filter { user ->
                val hasToken = user.deviceInfo.fcmToken.isNotEmpty()
                val isAssigned = runBlocking { isUserAssignedToProject(user, projectId) }
                
                if (!hasToken) {
                    Log.d(TAG, "⚠️ User ${user.name} has no FCM token")
                }
                if (!isAssigned) {
                    Log.d(TAG, "⚠️ User ${user.name} is not assigned to project $projectId")
                }
                
                hasToken && isAssigned
            }
            
            Log.d(TAG, "📋 Found ${eligibleUsers.size} eligible users with FCM tokens and project assignment")
            
            if (eligibleUsers.isEmpty()) {
                Log.w(TAG, "⚠️ No eligible users found for role: ${role.name} and project: $projectId")
                return Result.failure(Exception("No eligible users found for role: ${role.name} and project: $projectId"))
            }
            
            val userIds = eligibleUsers.map { it.uid }
            Log.d(TAG, "📋 Sending notifications to user IDs: $userIds")
            
            sendNotificationToUsers(
                userIds = userIds,
                title = title,
                message = message,
                projectId = projectId,
                expenseId = expenseId,
                notificationType = notificationType
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending notification to users by role with project filter: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Send FCM notification using FCM token
     */
    private suspend fun sendFCMNotification(
        fcmToken: String,
        title: String,
        message: String,
        projectId: String,
        expenseId: String,
        notificationType: String
    ): Result<Unit> {
        return try {
            Log.d(TAG, "📤 Sending FCM notification to token: $fcmToken")
            
            // Create FCM message payload
            val fcmMessage = createFCMMessage(
                fcmToken = fcmToken,
                title = title,
                message = message,
                projectId = projectId,
                expenseId = expenseId,
                notificationType = notificationType
            )
            
            // Send FCM message using HTTP client
            val response = sendFCMRequest(fcmMessage)
            
            if (response.isSuccess) {
                Log.d(TAG, "✅ FCM notification sent successfully")
                Result.success(Unit)
            } else {
                Log.e(TAG, "❌ Failed to send FCM notification: ${response.exceptionOrNull()?.message}")
                Result.failure(response.exceptionOrNull() ?: Exception("Failed to send FCM notification"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending FCM notification: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Create FCM message payload
     */
    private fun createFCMMessage(
        fcmToken: String,
        title: String,
        message: String,
        projectId: String,
        expenseId: String,
        notificationType: String
    ): Map<String, Any> {
        return mapOf(
            "to" to fcmToken,
            "notification" to mapOf(
                "title" to title,
                "body" to message,
                "sound" to "default",
                "priority" to "high"
            ),
            "data" to mapOf(
                "title" to title,
                "message" to message,
                "projectId" to projectId,
                "expenseId" to expenseId,
                "type" to notificationType,
                "click_action" to "FLUTTER_NOTIFICATION_CLICK"
            ),
            "priority" to "high"
        )
    }
    
    /**
     * Send FCM HTTP request
     */
    private suspend fun sendFCMRequest(fcmMessage: Map<String, Any>): Result<Unit> {
        return try {
            // For now, we'll simulate the FCM request
            // In a real implementation, you would use an HTTP client like OkHttp or Retrofit
            // to send the actual FCM request to Google's FCM servers
            
            Log.d(TAG, "📡 Simulating FCM request with message: $fcmMessage")
            
            // Simulate network delay
            kotlinx.coroutines.delay(100)
            
            // Simulate success (in real implementation, check actual HTTP response)
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error sending FCM request: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Update user's FCM token
     */
    suspend fun updateUserFCMToken(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Updating FCM token for user: $userId")
            
            // Get current FCM token
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "📱 New FCM token: $fcmToken")
            
            // Update user's FCM token in Firebase
            authRepository.updateUserFCMToken(userId, fcmToken)
                .onSuccess {
                    Log.d(TAG, "✅ FCM token updated successfully")
                }
                .onFailure { error ->
                    Log.e(TAG, "❌ Failed to update FCM token: ${error.message}")
                    return Result.failure(error)
                }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating FCM token: ${e.message}")
            Result.failure(e)
        }
    }
} 