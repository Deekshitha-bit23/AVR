package com.deeksha.avr.repository

import android.util.Log
import com.deeksha.avr.model.Notification
import com.deeksha.avr.model.NotificationBadge
import com.deeksha.avr.model.NotificationType
import com.deeksha.avr.model.ProjectNotificationSummary
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val notificationsCollection = firestore.collection("notifications")

    // Create a new notification
    suspend fun createNotification(notification: Notification): Result<String> {
        return try {
            val docRef = notificationsCollection.document()
            val notificationWithId = notification.copy(id = docRef.id)
            docRef.set(notificationWithId).await()
            Log.d("NotificationRepository", "✅ Created notification: ${notification.title}")
            Log.d("NotificationRepository", "📋 Notification details:")
            Log.d("NotificationRepository", "   - ID: ${docRef.id}")
            Log.d("NotificationRepository", "   - recipientId: '${notification.recipientId}'")
            Log.d("NotificationRepository", "   - recipientRole: '${notification.recipientRole}'")
            Log.d("NotificationRepository", "   - type: '${notification.type}'")
            Log.d("NotificationRepository", "   - projectId: '${notification.projectId}'")
            Log.d("NotificationRepository", "   - projectName: '${notification.projectName}'")
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "❌ Error creating notification: ${e.message}")
            Result.failure(e)
        }
    }

    // Get notifications for a specific user
    suspend fun getNotificationsForUser(
        userId: String,
        limit: Int = 50
    ): List<Notification> {
        return try {
            Log.d("NotificationRepository", "🔄 Getting notifications for user: $userId")
            
            val result = notificationsCollection
                .whereEqualTo("recipientId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val notifications = result.documents
                .filter { doc ->
                    // Only include unread notifications (isRead is false or missing)
                    val isRead = doc.getBoolean("isRead") ?: false
                    !isRead
                }
                .mapNotNull { doc ->
                    doc.toObject(Notification::class.java)
                }
            
            Log.d("NotificationRepository", "📋 Found ${notifications.size} unread notifications for user: $userId")
            notifications.forEach { notification ->
                Log.d("NotificationRepository", "📋 Unread Notification: ${notification.title} - Recipient: ${notification.recipientId} - Project: ${notification.projectName}")
            }
            
            notifications
        } catch (e: Exception) {
            Log.e("NotificationRepository", "❌ Error getting notifications: ${e.message}")
            emptyList()
        }
    }

    // Get notifications for a specific user with real-time updates
    fun getNotificationsForUserRealtime(
        userId: String,
        limit: Int = 50
    ): Flow<List<Notification>> {
        return callbackFlow {
            try {
                Log.d("NotificationRepository", "🔄 Setting up real-time notifications for user: $userId")
                Log.d("NotificationRepository", "🔍 Querying notifications with recipientId: $userId")
                Log.d("NotificationRepository", "🔍 User ID type: ${userId::class.simpleName}, length: ${userId.length}")

                val query = notificationsCollection
                    .whereEqualTo("recipientId", userId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(limit.toLong())

                // Listen for real-time updates
                val listener = query.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("NotificationRepository", "❌ Error in real-time listener: ${error.message}")
                        Log.e("NotificationRepository", "❌ Error details: ${error}")
                        return@addSnapshotListener
                    }

                    val notifications = snapshot?.documents
                        ?.filter { doc ->
                            // Only include unread notifications (isRead is false or missing)
                            val isRead = doc.getBoolean("isRead") ?: false
                            !isRead
                        }
                        ?.mapNotNull { doc ->
                            try {
                                doc.toObject(Notification::class.java)
                            } catch (e: Exception) {
                                Log.e("NotificationRepository", "❌ Error parsing notification document: ${e.message}")
                                null
                            }
                        } ?: emptyList()

                    Log.d("NotificationRepository", "📡 Real-time update: ${notifications.size} unread notifications for user: $userId")
                    if (notifications.isEmpty()) {
                        Log.d("NotificationRepository", "⚠️ No unread notifications found for user: $userId")
                        Log.d("NotificationRepository", "🔍 Checking if there are any notifications in the database for this user...")
                    } else {
                        notifications.forEach { notification ->
                            Log.d("NotificationRepository", "📋 Unread Notification: ${notification.title} - Recipient: '${notification.recipientId}' - Project: ${notification.projectName} - Read: ${notification.isRead} - Type: ${notification.type}")
                        }
                    }

                    try {
                        trySend(notifications)
                    } catch (e: Exception) {
                        Log.e("NotificationRepository", "❌ Error sending notifications: ${e.message}")
                    }
                }

                // Clean up listener when flow is cancelled
                awaitClose {
                    Log.d("NotificationRepository", "🔄 Cleaning up real-time listener for user: $userId")
                    listener.remove()
                }

            } catch (e: Exception) {
                Log.e("NotificationRepository", "❌ Error setting up real-time notifications: ${e.message}")
                close(e)
            }

        }
    }

    // Get notifications for a specific project
    suspend fun getNotificationsForProject(
        projectId: String,
        userId: String
    ): List<Notification> {
        return try {
            val result = notificationsCollection
                .whereEqualTo("projectId", projectId)
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("read", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            Log.d("NotificationRepositoryHistory", "Got the notification for project : ${projectId} userid :${userId}")
            result.documents.mapNotNull { doc ->
                doc.toObject(Notification::class.java)
            }
        } catch (e: Exception) {
            Log.e("NotificationRepository", "❌ Error getting project notifications: ${e.message}")
            emptyList()
        }
    }

    // Get project-specific notifications for a user
    suspend fun getProjectNotificationsForUser(
        userId: String,
        projectId: String,
        userRole: String
    ): List<Notification> {
        return try {
            Log.d("NotificationRepository", "🔄 Getting project notifications for user: $userId, project: $projectId, role: $userRole")
            
            val result = notificationsCollection
                .whereEqualTo("projectId", projectId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val notifications = result.documents.mapNotNull { doc ->
                doc.toObject(Notification::class.java)
            }
            
            // Apply role-based filtering - only show unread notifications for all roles
            val filteredNotifications = when (userRole) {
                "USER" -> {
                    // For USER role, only show unread notifications that will vanish when read
                    val unreadNotifications = notifications.filter { !it.isRead }
                    Log.d("NotificationRepository", "👤 USER role: showing ${unreadNotifications.size} unread notifications out of ${notifications.size} total")
                    unreadNotifications
                }
                "APPROVER", "PRODUCTION_HEAD", "ADMIN" -> {
                    // For other roles, also only show unread notifications
                    val unreadNotifications = notifications.filter { !it.isRead }
                    Log.d("NotificationRepository", "👥 ${userRole} role: showing ${unreadNotifications.size} unread notifications out of ${notifications.size} total")
                    unreadNotifications
                }
                else -> {
                    // Default to showing only unread notifications for unknown roles
                    val unreadNotifications = notifications.filter { !it.isRead }
                    Log.d("NotificationRepository", "❓ Unknown role '$userRole': showing ${unreadNotifications.size} unread notifications out of ${notifications.size} total")
                    unreadNotifications
                }
            }
            
            Log.d("NotificationRepository", "📋 Found ${notifications.size} total project notifications, ${filteredNotifications.size} filtered for user: $userId, project: $projectId, role: $userRole")
            filteredNotifications
        } catch (e: Exception) {
            Log.e("NotificationRepository", "❌ Error getting project notifications for user: ${e.message}")
            emptyList()
        }
    }

    // Get ALL project notifications for a user (including read ones) - for history/overview purposes
    suspend fun getAllProjectNotificationsForUser(
        userId: String,
        projectId: String,
        userRole: String
    ): List<Notification> {
        return try {
            Log.d("NotificationRepository", "🔄 Getting ALL project notifications for user: $userId, project: $projectId, role: $userRole")
            
            val result = notificationsCollection
                .whereEqualTo("projectId", projectId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val notifications = result.documents.mapNotNull { doc ->
                doc.toObject(Notification::class.java)
            }
            
            Log.d("NotificationRepository", "📋 Found ${notifications.size} total project notifications for user: $userId")
            notifications
        } catch (e: Exception) {
            Log.e("NotificationRepository", "❌ Error getting all project notifications for user: ${e.message}")
            emptyList()
        }
    }

    // Get project-specific notifications for a user with real-time updates (only unread)
    fun getProjectNotificationsForUserRealtime(
        userId: String,
        projectId: String,
        userRole: String
    ): Flow<List<Notification>> {
        return callbackFlow {
            try {
                Log.d("NotificationRepository", "🔄 Setting up real-time project notifications for user: $userId, project: $projectId, role: $userRole")
                
                // Query for all notifications for the project
                // This allows users to see all notifications related to their project
                val query = notificationsCollection
                    .whereEqualTo("projectId", projectId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                
                // Listen for real-time updates
                val listener = query.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("NotificationRepository", "❌ Error in real-time project listener: ${error.message}")
                        return@addSnapshotListener
                    }
                    
                    val notifications = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Notification::class.java)
                    } ?: emptyList()
                    
                    // Apply role-based filtering - only show unread notifications for all roles
                    val filteredNotifications = when (userRole) {
                        "USER" -> {
                            // For USER role, only show unread notifications that will vanish when read
                            val unreadNotifications = notifications.filter { !it.isRead }
                            Log.d("NotificationRepository", "👤 USER role: showing ${unreadNotifications.size} unread notifications out of ${notifications.size} total")
                            unreadNotifications
                        }
                        "APPROVER", "PRODUCTION_HEAD", "ADMIN" -> {
                            // For other roles, also only show unread notifications
                            val unreadNotifications = notifications.filter { !it.isRead }
                            Log.d("NotificationRepository", "👥 ${userRole} role: showing ${unreadNotifications.size} unread notifications out of ${notifications.size} total")
                            unreadNotifications
                        }
                        else -> {
                            // Default to showing only unread notifications for unknown roles
                            val unreadNotifications = notifications.filter { !it.isRead }
                            Log.d("NotificationRepository", "❓ Unknown role '$userRole': showing ${unreadNotifications.size} unread notifications out of ${notifications.size} total")
                            unreadNotifications
                        }
                    }
                    
                    Log.d("NotificationRepository", "📡 Real-time project update: ${notifications.size} total notifications, ${filteredNotifications.size} filtered for user: $userId, project: $projectId, role: $userRole")
                    
                    // Send the filtered notifications through the channel
                    try {
                        trySend(filteredNotifications)
                    } catch (e: Exception) {
                        Log.e("NotificationRepository", "❌ Error sending project notifications: ${e.message}")
                    }
                }
                
                // Clean up listener when flow is cancelled
                awaitClose {
                    Log.d("NotificationRepository", "🔄 Cleaning up real-time project listener for user: $userId, project: $projectId")
                    listener.remove()
                }
                
            } catch (e: Exception) {
                Log.e("NotificationRepository", "❌ Error setting up real-time project notifications: ${e.message}")
                close(e)
            }
        }
    }

    // Get ALL project notifications for a user with real-time updates (including read ones) - for history/overview purposes
    fun getAllProjectNotificationsForUserRealtime(
        userId: String,
        projectId: String,
        userRole: String
    ): Flow<List<Notification>> {
        return callbackFlow {
            try {
                Log.d("NotificationRepository", "🔄 Setting up real-time ALL project notifications for user: $userId, project: $projectId, role: $userRole")
                
                // Query for all notifications for the project (including read ones)
                val query = notificationsCollection
                    .whereEqualTo("projectId", projectId)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                
                // Listen for real-time updates
                val listener = query.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("NotificationRepository", "❌ Error in real-time all project listener: ${error.message}")
                        return@addSnapshotListener
                    }
                    
                    val notifications = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Notification::class.java)
                    } ?: emptyList()
                    
                    Log.d("NotificationRepository", "📡 Real-time ALL project update: ${notifications.size} notifications for user: $userId, project: $projectId")
                    
                    // Send all notifications through the channel
                    try {
                        trySend(notifications)
                    } catch (e: Exception) {
                        Log.e("NotificationRepository", "❌ Error sending all project notifications: ${e.message}")
                    }
                }
                
                // Clean up listener when flow is cancelled
                awaitClose {
                    Log.d("NotificationRepository", "🔄 Cleaning up real-time all project listener for user: $userId, project: $projectId")
                    listener.remove()
                }
                
            } catch (e: Exception) {
                Log.e("NotificationRepository", "❌ Error setting up real-time all project notifications: ${e.message}")
                close(e)
            }
        }
    }

    // Mark notification as read
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId)
                .update(
                    mapOf(
                        "read" to true,
                        "isRead" to true
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "❌ Error marking notification as read: ${e.message}")
            Result.failure(e)
        }
    }

    // Mark all notifications as read for a user
    suspend fun markAllNotificationsAsRead(userId: String): Result<Unit> {
        return try {
            val notifications = notificationsCollection
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("read", false)
                .get()
                .await()

            notifications.documents.forEach { doc ->
                doc.reference.update("read", true)
                doc.reference.update("isRead", true)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepositorychecking", "❌ Error marking all notifications as read: ${e.message}")
            Result.failure(e)
        }
    }

    // Mark all project notifications as read for a user
    suspend fun markAllProjectNotificationsAsRead(userId: String, projectId: String): Result<Unit> {
        return try {
            val notifications = notificationsCollection
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("projectId", projectId)
                .whereEqualTo("read", false)
                .get()
                .await()

            notifications.documents.forEach { doc ->
                doc.reference.update("read", true)
                doc.reference.update("isRead", true)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "❌ Error marking all project notifications as read: ${e.message}")
            Result.failure(e)
        }
    }

    // Get notification badge count
    suspend fun getNotificationBadge(userId: String): NotificationBadge {
        return try {
            val unreadNotifications = notificationsCollection
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("read", false)
                .get()
                .await()

            val count = unreadNotifications.size()
            NotificationBadge(
                count = count,
                hasUnread = count > 0
            )
        } catch (e: Exception) {
            Log.e("NotificationRepository", "❌ Error getting notification badge: ${e.message}")
            NotificationBadge()
        }
    }

    // Get project notification summaries
    suspend fun getProjectNotificationSummaries(
        userId: String,
        projectIds: List<String>
    ): List<ProjectNotificationSummary> {
        return try {
            val summaries = mutableListOf<ProjectNotificationSummary>()

            for (projectId in projectIds) {
                val notifications = getNotificationsForProject(projectId, userId)
                val unreadCount = notifications.count { !it.isRead }
                val pendingApprovals = notifications.count { 
                    it.type == NotificationType.PENDING_APPROVAL && !it.isRead 
                }
                val expenseUpdates = notifications.count { 
                    (it.type == NotificationType.EXPENSE_APPROVED || 
                     it.type == NotificationType.EXPENSE_REJECTED) && !it.isRead 
                }

                summaries.add(
                    ProjectNotificationSummary(
                        projectId = projectId,
                        projectName = notifications.firstOrNull()?.projectName ?: "",
                        totalNotifications = notifications.size,
                        unreadCount = unreadCount,
                        latestNotification = notifications.firstOrNull(),
                        pendingApprovals = pendingApprovals,
                        expenseUpdates = expenseUpdates
                    )
                )
            }

            summaries
        } catch (e: Exception) {
            Log.e("NotificationRepository", "❌ Error getting project summaries: ${e.message}")
            emptyList()
        }
    }

    // Create new project notification (for approvers and team members)
    suspend fun createNewProjectNotification(
        recipientId: String,
        recipientRole: String,
        projectId: String,
        projectName: String,
        assignedRole: String
    ): Result<String> {
        val notification = Notification(
            recipientId = recipientId,
            recipientRole = recipientRole,
            title = "New Project Created",
            message = "You have been assigned as $assignedRole to the newly created project: $projectName",
            type = NotificationType.PROJECT_ASSIGNMENT,
            projectId = projectId,
            projectName = projectName,
            actionRequired = true,
            navigationTarget = when (recipientRole) {
                "USER" -> "user_project_dashboard/$projectId"
                "APPROVER" -> "approver_project_dashboard/$projectId"
                "PRODUCTION_HEAD" -> "production_head_project_dashboard/$projectId"
                else -> "project_selection"
            }
        )

        return createNotification(notification)
    }

    // Create project change notification (for approvers and team members)
    suspend fun createProjectChangeNotification(
        recipientId: String,
        recipientRole: String,
        projectId: String,
        projectName: String,
        changeDescription: String,
        changedBy: String
    ): Result<String> {
        val notification = Notification(
            recipientId = recipientId,
            recipientRole = recipientRole,
            title = "Project Updated",
            message = "Project '$projectName' has been updated by $changedBy. $changeDescription",
            type = NotificationType.PROJECT_CHANGED,
            projectId = projectId,
            projectName = projectName,
            actionRequired = false,
            navigationTarget = when (recipientRole) {
                "USER" -> "user_project_dashboard/$projectId"
                "APPROVER" -> "approver_project_dashboard/$projectId"
                "PRODUCTION_HEAD" -> "production_head_project_dashboard/$projectId"
                else -> "project_selection"
            }
        )

        return createNotification(notification)
    }

    // Create project assignment notification (for approvers)
    suspend fun createProjectAssignmentNotification(
        recipientId: String,
        recipientRole: String,
        projectId: String,
        projectName: String,
        assignedRole: String
    ): Result<String> {
        val notification = Notification(
            recipientId = recipientId,
            recipientRole = recipientRole,
            title = "New Project Assignment",
            message = "You have been assigned as $assignedRole to project: $projectName",
            type = NotificationType.PROJECT_ASSIGNMENT,
            projectId = projectId,
            projectName = projectName,
            actionRequired = true,
            navigationTarget = when (recipientRole) {
                "USER" -> "user_project_dashboard/$projectId"
                "APPROVER" -> "approver_project_dashboard/$projectId"
                "PRODUCTION_HEAD" -> "production_head_project_dashboard/$projectId"
                else -> "project_selection"
            }
        )

        return createNotification(notification)
    }

    // Create expense submission notification (for approvers)
    suspend fun createExpenseSubmissionNotification(
        projectId: String,
        projectName: String,
        expenseId: String,
        submittedBy: String,
        amount: Double,
        approverIds: List<String>
    ): Result<List<String>> {
        val results = mutableListOf<String>()

        for (approverId in approverIds) {
            val notification = Notification(
                recipientId = approverId,
                recipientRole = "APPROVER",
                title = "New Expense Submitted",
                message = "New expense of ₹$amount submitted by $submittedBy in $projectName",
                type = NotificationType.EXPENSE_SUBMITTED,
                projectId = projectId,
                projectName = projectName,
                relatedId = expenseId,
                actionRequired = true,
                navigationTarget = "pending_approvals/$projectId"
            )

            createNotification(notification).onSuccess { notificationId ->
                results.add(notificationId)
            }
        }

        return Result.success(results)
    }

    // Create expense status update notification (for users)
    suspend fun createExpenseStatusNotification(
        recipientId: String,
        projectId: String,
        projectName: String,
        expenseId: String,
        isApproved: Boolean,
        amount: Double,
        approverName: String
    ): Result<String> {
        val notification = Notification(
            recipientId = recipientId,
            recipientRole = "USER",
            title = if (isApproved) "Expense Approved" else "Expense Rejected",
            message = if (isApproved) {
                "Your expense of ₹$amount has been approved by $approverName in $projectName"
            } else {
                "Your expense of ₹$amount has been rejected by $approverName in $projectName"
            },
            type = if (isApproved) NotificationType.EXPENSE_APPROVED else NotificationType.EXPENSE_REJECTED,
            projectId = projectId,
            projectName = projectName,
            relatedId = expenseId,
            navigationTarget = "expense_list/$projectId"
        )

        return createNotification(notification)
    }

    // Create pending approval notification (for production heads)
    suspend fun createPendingApprovalNotification(
        productionHeadId: String,
        projectId: String,
        projectName: String,
        pendingCount: Int
    ): Result<String> {
        val notification = Notification(
            recipientId = productionHeadId,
            recipientRole = "PRODUCTION_HEAD",
            title = "Pending Approvals",
            message = "$pendingCount expenses awaiting approval in $projectName",
            type = NotificationType.PENDING_APPROVAL,
            projectId = projectId,
            projectName = projectName,
            actionRequired = true,
            navigationTarget = "pending_approvals/$projectId"
        )

        return createNotification(notification)
    }

    // Delete old notifications (cleanup)
    suspend fun deleteOldNotifications(daysOld: Int = 30): Result<Int> {
        return try {
            val cutoffDate = Timestamp(System.currentTimeMillis() / 1000 - (daysOld * 24 * 60 * 60), 0)
            
            val oldNotifications = notificationsCollection
                .whereLessThan("createdAt", cutoffDate)
                .get()
                .await()

            var deletedCount = 0
            oldNotifications.documents.forEach { doc ->
                doc.reference.delete()
                deletedCount++
            }

            Log.d("NotificationRepository", "🗑️ Deleted $deletedCount old notifications")
            Result.success(deletedCount)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "❌ Error deleting old notifications: ${e.message}")
            Result.failure(e)
        }
    }

    // Update notification recipient (for fixing empty recipient IDs)
    suspend fun updateNotificationRecipient(notificationId: String, newRecipientId: String): Result<Unit> {
        return try {
            Log.d("NotificationRepository", "🔄 Updating notification recipient: $notificationId -> $newRecipientId")
            
            notificationsCollection.document(notificationId)
                .update("recipientId", newRecipientId)
                .await()
                
            Log.d("NotificationRepository", "✅ Updated notification recipient")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "❌ Error updating notification recipient: ${e.message}")
            Result.failure(e)
        }
    }

    // Get all notifications (for debugging)
    suspend fun getAllNotifications(): List<Notification> {
        return try {
            Log.d("NotificationRepository", "🔄 Getting all notifications for debugging")
            
            val result = notificationsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val notifications = result.documents.mapNotNull { doc ->
                doc.toObject(Notification::class.java)
            }
            
            Log.d("NotificationRepository", "📋 Found ${notifications.size} total notifications")
            notifications.forEach { notification ->
                Log.d("NotificationRepository", "📋 Notification: ${notification.title} - Recipient: '${notification.recipientId}' - Project: ${notification.projectName}")
            }
            
            notifications
        } catch (e: Exception) {
            Log.e("NotificationRepository", "❌ Error getting all notifications: ${e.message}")
            emptyList()
        }
    }


} 