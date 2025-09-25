package com.deeksha.avr.model

import com.google.firebase.Timestamp

data class Notification(
    val id: String = "",
    val recipientId: String = "",
    val recipientRole: String = "", // USER, APPROVER, PRODUCTION_HEAD
    val title: String = "",
    val message: String = "",
    val type: NotificationType = NotificationType.INFO,
    val projectId: String = "",
    val projectName: String = "",
    val relatedId: String = "", // Could be expenseId, userId, etc.
    val isRead: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val actionRequired: Boolean = false,
    val navigationTarget: String = "" // Where to navigate when clicked
)

enum class NotificationType {
    PROJECT_ASSIGNMENT,     // When assigned to a project
    PROJECT_CHANGED,        // When project details are modified
    EXPENSE_SUBMITTED,      // When expense is submitted (for approvers)
    EXPENSE_APPROVED,       // When expense is approved (for users)
    EXPENSE_REJECTED,       // When expense is rejected (for users)
    PENDING_APPROVAL,       // General pending approval notifications
    ROLE_ASSIGNMENT,        // When role is assigned/changed
    TEMPORARY_APPROVER_ASSIGNMENT, // When assigned as temporary approver
    INFO                    // General information
}

data class NotificationBadge(
    val count: Int = 0,
    val hasUnread: Boolean = false
)

data class ProjectNotificationSummary(
    val projectId: String,
    val projectName: String,
    val totalNotifications: Int = 0,
    val unreadCount: Int = 0,
    val latestNotification: Notification? = null,
    val pendingApprovals: Int = 0,
    val expenseUpdates: Int = 0
)

 