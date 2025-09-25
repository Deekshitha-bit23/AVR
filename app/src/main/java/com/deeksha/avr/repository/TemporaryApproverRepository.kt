package com.deeksha.avr.repository

import android.util.Log
import com.deeksha.avr.model.TemporaryApprover
import com.deeksha.avr.model.isExpired
import com.deeksha.avr.model.Notification
import com.deeksha.avr.model.NotificationType
import com.deeksha.avr.service.NotificationService
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemporaryApproverRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val notificationService: NotificationService?
) {
    
    companion object {
        private const val TAG = "TempApproverRepository"
        private const val COLLECTION_PROJECTS = "projects"
        private const val SUBCOLLECTION_TEMP_APPROVERS = "temporaryApprovers"
    }
    
    /**
     * Add a temporary approver to a project
     */
    suspend fun addTemporaryApprover(
        projectId: String,
        approverId: String,
        approverName: String,
        approverPhone: String,
        expiringDate: Timestamp,
        assignedBy: String,
        assignedByName: String
    ): Result<TemporaryApprover> {
        return try {
            Log.d(TAG, "🔄 Adding temporary approver $approverName to project $projectId")
            
            val tempApprover = TemporaryApprover(
                id = "", // Will be set by Firestore
                projectId = projectId,
                approverId = approverId,
                approverName = approverName,
                approverPhone = approverPhone,
                assignedDate = Timestamp.now(),
                expiringDate = expiringDate,
                isActive = true,
                assignedBy = assignedBy,
                assignedByName = assignedByName,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            
            val docRef = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .add(tempApprover)
                .await()
            
            val savedTempApprover = tempApprover.copy(id = docRef.id)
            
            // Also update the project document with the temporary approver phone number
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .update("temporaryApproverPhone", approverPhone)
                .await()
            
            Log.d(TAG, "✅ Temporary approver added successfully with ID: ${docRef.id}")
            Log.d(TAG, "✅ Project document updated with temporary approver phone: $approverPhone")
            
            // Send notification to the temporary approver
            notificationService?.let { service ->
                sendTemporaryApproverNotification(projectId, approverId, approverName, approverPhone, expiringDate, service)
            }
            
            Result.success(savedTempApprover)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to add temporary approver", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get all temporary approvers for a project
     */
    suspend fun getTemporaryApprovers(projectId: String): Result<List<TemporaryApprover>> {
        return try {
            Log.d(TAG, "🔍 Getting temporary approvers for project: $projectId")
            
            val snapshot = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .get()
                .await()
            
            val tempApprovers = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    TemporaryApprover(
                        id = doc.id,
                        projectId = data["projectId"] as? String ?: "",
                        approverId = data["approverId"] as? String ?: "",
                        approverName = data["approverName"] as? String ?: "",
                        approverPhone = data["approverPhone"] as? String ?: "",
                        assignedDate = data["assignedDate"] as? Timestamp ?: Timestamp.now(),
                        expiringDate = data["expiringDate"] as? Timestamp,
                        isActive = data["isActive"] as? Boolean ?: true,
                        assignedBy = data["assignedBy"] as? String ?: "",
                        assignedByName = data["assignedByName"] as? String ?: "",
                        createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                        updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing temporary approver document: ${doc.id}", e)
                    null
                }
            }
            
            Log.d(TAG, "✅ Found ${tempApprovers.size} temporary approvers")
            Result.success(tempApprovers)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get temporary approvers", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get active temporary approvers for a project (not expired and isActive = true)
     */
    suspend fun getActiveTemporaryApprovers(projectId: String): Result<List<TemporaryApprover>> {
        return try {
            val result = getTemporaryApprovers(projectId)
            if (result.isSuccess) {
                val allTempApprovers = result.getOrNull() ?: emptyList()
                val activeTempApprovers = allTempApprovers.filter { tempApprover ->
                    tempApprover.isActive && !tempApprover.isExpired()
                }
                Log.d(TAG, "✅ Found ${activeTempApprovers.size} active temporary approvers")
                Result.success(activeTempApprovers)
            } else {
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get active temporary approvers", e)
            Result.failure(e)
        }
    }
    
    /**
     * Deactivate a temporary approver
     */
    suspend fun deactivateTemporaryApprover(projectId: String, tempApproverId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Deactivating temporary approver: $tempApproverId")
            
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .document(tempApproverId)
                .update(
                    mapOf(
                        "isActive" to false,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            
            // Clear the temporary approver phone from project document when deactivated
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .update("temporaryApproverPhone", "")
                .await()
            
            Log.d(TAG, "✅ Temporary approver deactivated successfully")
            Log.d(TAG, "✅ Project document cleared of temporary approver phone")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to deactivate temporary approver", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update expiring date of a temporary approver
     */
    suspend fun updateExpiringDate(
        projectId: String, 
        tempApproverId: String, 
        newExpiringDate: Timestamp
    ): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Updating expiring date for temporary approver: $tempApproverId")
            
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .document(tempApproverId)
                .update(
                    mapOf(
                        "expiringDate" to newExpiringDate,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            
            Log.d(TAG, "✅ Expiring date updated successfully")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to update expiring date", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete a temporary approver
     */
    suspend fun deleteTemporaryApprover(projectId: String, tempApproverId: String): Result<Unit> {
        return try {
            Log.d(TAG, "🔄 Deleting temporary approver: $tempApproverId")
            
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .document(tempApproverId)
                .delete()
                .await()
            
            // Clear the temporary approver phone from project document when deleted
            firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .update("temporaryApproverPhone", "")
                .await()
            
            Log.d(TAG, "✅ Temporary approver deleted successfully")
            Log.d(TAG, "✅ Project document cleared of temporary approver phone")
            Result.success(Unit)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to delete temporary approver", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check and deactivate expired temporary approvers for a project
     */
    suspend fun deactivateExpiredApprovers(projectId: String): Result<Int> {
        return try {
            Log.d(TAG, "🔄 Checking for expired temporary approvers in project: $projectId")
            
            val result = getTemporaryApprovers(projectId)
            if (result.isFailure) {
                return result.map { 0 }
            }
            
            val allTempApprovers = result.getOrNull() ?: emptyList()
            val expiredApprovers = allTempApprovers.filter { tempApprover ->
                tempApprover.isActive && tempApprover.isExpired()
            }
            
            var deactivatedCount = 0
            for (expiredApprover in expiredApprovers) {
                val deactivateResult = deactivateTemporaryApprover(projectId, expiredApprover.id)
                if (deactivateResult.isSuccess) {
                    deactivatedCount++
                    Log.d(TAG, "✅ Deactivated expired temporary approver: ${expiredApprover.approverName}")
                }
            }
            
            Log.d(TAG, "✅ Deactivated $deactivatedCount expired temporary approvers")
            Result.success(deactivatedCount)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to deactivate expired approvers", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if a user is a temporary approver for a project
     */
    suspend fun isTemporaryApprover(projectId: String, userId: String): Result<Boolean> {
        return try {
            val result = getActiveTemporaryApprovers(projectId)
            if (result.isSuccess) {
                val activeTempApprovers = result.getOrNull() ?: emptyList()
                val isTemporaryApprover = activeTempApprovers.any { it.approverId == userId }
                Result.success(isTemporaryApprover)
            } else {
                Result.success(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to check temporary approver status", e)
            Result.failure(e)
        }
    }
    
    /**
     * Send notification to temporary approver about their assignment
     */
    private suspend fun sendTemporaryApproverNotification(
        projectId: String,
        approverId: String,
        approverName: String,
        approverPhone: String,
        expiringDate: Timestamp,
        notificationService: NotificationService
    ) {
        try {
            Log.d(TAG, "🔄 Sending temporary approver notification to: $approverName ($approverPhone)")
            
            // Get project details for the notification
            val projectDoc = firestore.collection(COLLECTION_PROJECTS)
                .document(projectId)
                .get()
                .await()
            
            if (!projectDoc.exists()) {
                Log.e(TAG, "❌ Project not found for notification: $projectId")
                return
            }
            
            val projectData = projectDoc.data ?: return
            val projectName = projectData["name"] as? String ?: "Unknown Project"
            
            // Format the expiry date
            val dateFormat = java.text.SimpleDateFormat("dd MMM yyyy 'at' HH:mm", java.util.Locale.getDefault())
            val formattedExpiryDate = dateFormat.format(expiringDate.toDate())
            
            // Create notification - Use phone number as recipientId to match notification filtering
            val notification = Notification(
                recipientId = approverPhone, // Use phone number instead of UID
                recipientRole = "APPROVER",
                title = "Temporary Approver Assignment",
                message = "You have been assigned as a temporary approver to project '$projectName' until $formattedExpiryDate. You can now access and manage this project.",
                type = NotificationType.TEMPORARY_APPROVER_ASSIGNMENT,
                projectId = projectId,
                projectName = projectName,
                actionRequired = true,
                navigationTarget = "approver_project_dashboard/$projectId"
            )
            
            Log.d(TAG, "📋 Created notification:")
            Log.d(TAG, "   - recipientId: $approverPhone (phone number)")
            Log.d(TAG, "   - recipientRole: APPROVER")
            Log.d(TAG, "   - title: ${notification.title}")
            Log.d(TAG, "   - message: ${notification.message}")
            Log.d(TAG, "   - projectId: $projectId")
            Log.d(TAG, "   - projectName: $projectName")
            
            // Send the notification
            val notificationResult = notificationService.sendNotification(notification)
            
            if (notificationResult.isSuccess) {
                Log.d(TAG, "✅ Temporary approver notification sent successfully to: $approverName")
            } else {
                Log.e(TAG, "❌ Failed to send notification: ${notificationResult.exceptionOrNull()?.message}")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send temporary approver notification", e)
            // Don't fail the main operation if notification fails
        }
    }
}


