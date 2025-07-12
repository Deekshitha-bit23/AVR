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
            val result = notificationsCollection
                .whereEqualTo("recipientId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            result.documents.mapNotNull { doc ->
                doc.toObject(Notification::class.java)
            }
        } catch (e: Exception) {
            Log.e("NotificationRepository", "❌ Error getting notifications: ${e.message}")
            emptyList()
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
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            result.documents.mapNotNull { doc ->
                doc.toObject(Notification::class.java)
            }
        } catch (e: Exception) {
            Log.e("NotificationRepository", "❌ Error getting project notifications: ${e.message}")
            emptyList()
        }
    }

    // Mark notification as read
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            notificationsCollection.document(notificationId)
                .update("isRead", true)
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
                .whereEqualTo("isRead", false)
                .get()
                .await()

            notifications.documents.forEach { doc ->
                doc.reference.update("isRead", true)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotificationRepository", "❌ Error marking all notifications as read: ${e.message}")
            Result.failure(e)
        }
    }

    // Get notification badge count
    suspend fun getNotificationBadge(userId: String): NotificationBadge {
        return try {
            val unreadNotifications = notificationsCollection
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("isRead", false)
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

    // Create project assignment notification
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


} 