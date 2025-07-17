package com.deeksha.avr.service

import android.util.Log
import com.deeksha.avr.model.Notification
import com.deeksha.avr.model.NotificationType
import com.deeksha.avr.model.Project
import com.deeksha.avr.model.User
import com.deeksha.avr.repository.NotificationRepository
import com.deeksha.avr.repository.ProjectRepository
import com.deeksha.avr.repository.AuthRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationService @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val projectRepository: ProjectRepository,
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) {
    
    /**
     * Send expense submission notification to project approvers and production heads
     */
    suspend fun sendExpenseSubmissionNotification(
        projectId: String,
        expenseId: String,
        submittedBy: String,
        amount: Double,
        category: String
    ): Result<Unit> {
        return try {
            Log.d("NotificationService", "🔄 Sending expense submission notification for project: $projectId, expense: $expenseId")
            
            // Get project details
            val project = projectRepository.getProjectById(projectId)
            if (project == null) {
                Log.e("NotificationService", "❌ Project not found: $projectId")
                return Result.failure(Exception("Project not found"))
            }
            
            Log.d("NotificationService", "📋 Project: ${project.name}")
            Log.d("NotificationService", "📋 Project approverIds: ${project.approverIds}")
            Log.d("NotificationService", "📋 Project productionHeadIds: ${project.productionHeadIds}")
            Log.d("NotificationService", "📋 Project managerId: ${project.managerId}")
            
            // Get all users to find approvers and production heads
            val allUsers = authRepository.getAllUsers()
            Log.d("NotificationService", "📋 Total users found: ${allUsers.size}")
            
            // Collect approver and production head IDs
            val approverIds = mutableListOf<String>()
            val productionHeadIds = mutableListOf<String>()
            
            // Add project-specific approvers
            project.approverIds.forEach { approverId ->
                Log.d("NotificationService", "🔍 Checking approver ID: $approverId")
                val user = allUsers.find { it.uid == approverId }
                if (user?.role == com.deeksha.avr.model.UserRole.APPROVER) {
                    approverIds.add(approverId)
                    Log.d("NotificationService", "✅ Added approver: ${user.name} (${user.uid})")
                } else {
                    Log.w("NotificationService", "⚠️ User not found or not approver: $approverId")
                }
            }
            
            // Add project-specific production heads
            project.productionHeadIds.forEach { productionHeadId ->
                Log.d("NotificationService", "🔍 Checking production head ID: $productionHeadId")
                val user = allUsers.find { it.uid == productionHeadId }
                if (user?.role == com.deeksha.avr.model.UserRole.PRODUCTION_HEAD) {
                    productionHeadIds.add(productionHeadId)
                    Log.d("NotificationService", "✅ Added production head: ${user.name} (${user.uid})")
                } else {
                    Log.w("NotificationService", "⚠️ User not found or not production head: $productionHeadId")
                }
            }
            
            // Add project manager as approver if not already included
            if (project.managerId.isNotEmpty() && !approverIds.contains(project.managerId)) {
                Log.d("NotificationService", "🔍 Checking manager ID: ${project.managerId}")
                val manager = allUsers.find { it.uid == project.managerId }
                if (manager?.role == com.deeksha.avr.model.UserRole.APPROVER) {
                    approverIds.add(project.managerId)
                    Log.d("NotificationService", "✅ Added manager as approver: ${manager.name} (${manager.uid})")
                } else {
                    Log.w("NotificationService", "⚠️ Manager not found or not approver: ${project.managerId}")
                }
            }
            
            Log.d("NotificationService", "📋 Final: ${approverIds.size} approvers and ${productionHeadIds.size} production heads")
            
            // If no approvers or production heads found, log a warning
            if (approverIds.isEmpty() && productionHeadIds.isEmpty()) {
                Log.w("NotificationService", "⚠️ No approvers or production heads found for project: ${project.name}")
                Log.w("NotificationService", "⚠️ This means notifications won't be sent to anyone!")
                
                // TEMPORARY FIX: Send to all approvers and production heads if project-specific ones are not set
                Log.d("NotificationService", "🔄 Using fallback: sending to all approvers and production heads")
                
                allUsers.forEach { user ->
                    when (user.role) {
                        com.deeksha.avr.model.UserRole.APPROVER -> {
                            if (!approverIds.contains(user.uid)) {
                                approverIds.add(user.uid)
                                Log.d("NotificationService", "✅ Added fallback approver: ${user.name} (${user.uid})")
                            }
                        }
                        com.deeksha.avr.model.UserRole.PRODUCTION_HEAD -> {
                            if (!productionHeadIds.contains(user.uid)) {
                                productionHeadIds.add(user.uid)
                                Log.d("NotificationService", "✅ Added fallback production head: ${user.name} (${user.uid})")
                            }
                        }
                        else -> {}
                    }
                }
                
                Log.d("NotificationService", "📋 After fallback: ${approverIds.size} approvers and ${productionHeadIds.size} production heads")
            }
            
            // Send notifications to approvers
            approverIds.forEach { approverId ->
                if (approverId.isNotEmpty()) {
                    val notification = Notification(
                        recipientId = approverId,
                        recipientRole = "APPROVER",
                        title = "New Expense Submitted",
                        message = "New expense of ₹${String.format("%.2f", amount)} submitted by $submittedBy in ${project.name} (Category: $category)",
                        type = NotificationType.EXPENSE_SUBMITTED,
                        projectId = projectId,
                        projectName = project.name,
                        relatedId = expenseId,
                        actionRequired = true,
                        navigationTarget = "pending_approvals/$projectId"
                    )
                    
                    notificationRepository.createNotification(notification).onSuccess { notificationId ->
                        Log.d("NotificationService", "✅ Sent notification to approver $approverId: $notificationId")
                    }.onFailure { error ->
                        Log.e("NotificationService", "❌ Failed to send notification to approver $approverId: ${error.message}")
                    }
                } else {
                    Log.w("NotificationService", "⚠️ Skipping notification for empty approver ID")
                }
            }
            
            // Send notifications to production heads
            productionHeadIds.forEach { productionHeadId ->
                if (productionHeadId.isNotEmpty()) {
                    val notification = Notification(
                        recipientId = productionHeadId,
                        recipientRole = "PRODUCTION_HEAD",
                        title = "New Expense Submitted",
                        message = "New expense of ₹${String.format("%.2f", amount)} submitted by $submittedBy in ${project.name} (Category: $category)",
                        type = NotificationType.EXPENSE_SUBMITTED,
                        projectId = projectId,
                        projectName = project.name,
                        relatedId = expenseId,
                        actionRequired = true,
                        navigationTarget = "pending_approvals/$projectId"
                    )
                    
                    notificationRepository.createNotification(notification).onSuccess { notificationId ->
                        Log.d("NotificationService", "✅ Sent notification to production head $productionHeadId: $notificationId")
                    }.onFailure { error ->
                        Log.e("NotificationService", "❌ Failed to send notification to production head $productionHeadId: ${error.message}")
                    }
                } else {
                    Log.w("NotificationService", "⚠️ Skipping notification for empty production head ID")
                }
            }
            
            // TODO: Send push notifications via FCM
            // For now, we're only storing in Firestore
            // In a real implementation, you would send FCM messages here
            
            Log.d("NotificationService", "✅ Successfully sent expense submission notifications")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("NotificationService", "❌ Error sending expense submission notification: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Send expense approval/rejection notification to the user who submitted the expense
     */
    suspend fun sendExpenseStatusNotification(
        expenseId: String,
        projectId: String,
        submittedByUserId: String,
        isApproved: Boolean,
        amount: Double,
        reviewerName: String,
        comments: String = ""
    ): Result<Unit> {
        return try {
            Log.d("NotificationService", "🔄 Sending expense status notification for expense: $expenseId")
            Log.d("NotificationService", "📋 Submitted by user ID: $submittedByUserId")
            Log.d("NotificationService", "📋 Reviewer name: $reviewerName")
            Log.d("NotificationService", "📋 Amount: $amount")
            Log.d("NotificationService", "📋 Is approved: $isApproved")
            
            // Get project details
            val project = projectRepository.getProjectById(projectId)
            if (project == null) {
                Log.e("NotificationService", "❌ Project not found: $projectId")
                return Result.failure(Exception("Project not found"))
            }
            
            // Get user details to include in notification
            val allUsers = authRepository.getAllUsers()
            val submittedByUser = allUsers.find { it.uid == submittedByUserId }
            val submittedByUserName = submittedByUser?.name ?: "User"
            
            Log.d("NotificationService", "📋 Found user: $submittedByUserName (ID: $submittedByUserId)")
            
            // Create more detailed notification message
            val notificationTitle = if (isApproved) "✅ Expense Approved" else "❌ Expense Rejected"
            val notificationMessage = if (isApproved) {
                "Your expense of ₹${String.format("%.2f", amount)} in ${project.name} has been approved by $reviewerName"
            } else {
                val baseMessage = "Your expense of ₹${String.format("%.2f", amount)} in ${project.name} has been rejected by $reviewerName"
                if (comments.isNotEmpty()) {
                    "$baseMessage - Reason: $comments"
                } else {
                    baseMessage
                }
            }
            
            Log.d("NotificationService", "📋 Notification title: $notificationTitle")
            Log.d("NotificationService", "📋 Notification message: $notificationMessage")
            
            val notification = Notification(
                recipientId = submittedByUserId,
                recipientRole = "USER",
                title = notificationTitle,
                message = notificationMessage,
                type = if (isApproved) NotificationType.EXPENSE_APPROVED else NotificationType.EXPENSE_REJECTED,
                projectId = projectId,
                projectName = project.name,
                relatedId = expenseId,
                actionRequired = false, // User doesn't need to take action, just informational
                navigationTarget = "expense_list/$projectId" // Navigate to expense list for the specific project
            )
            
            // Validate that recipient ID is not empty
            if (submittedByUserId.isEmpty()) {
                Log.e("NotificationService", "❌ Cannot send notification: recipient ID is empty")
                return Result.failure(Exception("Recipient ID is empty"))
            }
            
            notificationRepository.createNotification(notification).onSuccess { notificationId ->
                Log.d("NotificationService", "✅ Successfully sent expense status notification to $submittedByUserName: $notificationId")
                Log.d("NotificationService", "✅ Notification details: title='$notificationTitle', message='$notificationMessage'")
                Log.d("NotificationService", "✅ Navigation target: ${notification.navigationTarget}")
            }.onFailure { error ->
                Log.e("NotificationService", "❌ Failed to send expense status notification to $submittedByUserId: ${error.message}")
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("NotificationService", "❌ Error sending expense status notification: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    /**
     * Send project assignment notification
     */
    suspend fun sendProjectAssignmentNotification(
        projectId: String,
        userId: String,
        assignedRole: String
    ): Result<Unit> {
        return try {
            Log.d("NotificationService", "🔄 Sending project assignment notification for user: $userId")
            
            // Get project details
            val project = projectRepository.getProjectById(projectId)
            if (project == null) {
                Log.e("NotificationService", "❌ Project not found: $projectId")
                return Result.failure(Exception("Project not found"))
            }
            
            // Get user details
            val user = authRepository.getAllUsers().find { it.uid == userId }
            if (user == null) {
                Log.e("NotificationService", "❌ User not found: $userId")
                return Result.failure(Exception("User not found"))
            }
            
            val notification = Notification(
                recipientId = userId,
                recipientRole = user.role.name,
                title = "New Project Assignment",
                message = "You have been assigned as $assignedRole to project: ${project.name}",
                type = NotificationType.PROJECT_ASSIGNMENT,
                projectId = projectId,
                projectName = project.name,
                actionRequired = true,
                navigationTarget = when (user.role) {
                    com.deeksha.avr.model.UserRole.USER -> "user_project_dashboard/$projectId"
                    com.deeksha.avr.model.UserRole.APPROVER -> "approver_project_dashboard/$projectId"
                    com.deeksha.avr.model.UserRole.PRODUCTION_HEAD -> "production_head_project_dashboard/$projectId"
                    else -> "project_selection"
                }
            )
            
            notificationRepository.createNotification(notification).onSuccess { notificationId ->
                Log.d("NotificationService", "✅ Sent project assignment notification: $notificationId")
            }.onFailure { error ->
                Log.e("NotificationService", "❌ Failed to send project assignment notification: ${error.message}")
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("NotificationService", "❌ Error sending project assignment notification: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Send pending approval notification to production heads
     */
    suspend fun sendPendingApprovalNotification(
        projectId: String,
        pendingCount: Int
    ): Result<Unit> {
        return try {
            Log.d("NotificationService", "🔄 Sending pending approval notification for project: $projectId")
            
            // Get project details
            val project = projectRepository.getProjectById(projectId)
            if (project == null) {
                Log.e("NotificationService", "❌ Project not found: $projectId")
                return Result.failure(Exception("Project not found"))
            }
            
            // Get all users to find production heads
            val allUsers = authRepository.getAllUsers()
            
            // Send to project-specific production heads
            project.productionHeadIds.forEach { productionHeadId ->
                val user = allUsers.find { it.uid == productionHeadId }
                if (user?.role == com.deeksha.avr.model.UserRole.PRODUCTION_HEAD) {
                    val notification = Notification(
                        recipientId = productionHeadId,
                        recipientRole = "PRODUCTION_HEAD",
                        title = "Pending Approvals",
                        message = "$pendingCount expenses awaiting approval in ${project.name}",
                        type = NotificationType.PENDING_APPROVAL,
                        projectId = projectId,
                        projectName = project.name,
                        actionRequired = true,
                        navigationTarget = "pending_approvals/$projectId"
                    )
                    
                    notificationRepository.createNotification(notification).onSuccess { notificationId ->
                        Log.d("NotificationService", "✅ Sent pending approval notification to production head $productionHeadId: $notificationId")
                    }.onFailure { error ->
                        Log.e("NotificationService", "❌ Failed to send pending approval notification to production head $productionHeadId: ${error.message}")
                    }
                }
            }
            
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e("NotificationService", "❌ Error sending pending approval notification: ${e.message}")
            Result.failure(e)
        }
    }
} 