package com.deeksha.avr.service

import android.content.Context
import android.util.Log
import androidx.work.*
import com.deeksha.avr.repository.TemporaryApproverRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Background service to automatically check and deactivate expired temporary approvers
 */
class TemporaryApproverExpirationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "TempApproverExpiration"
        const val WORK_NAME = "temporary_approver_expiration_check"
        
        /**
         * Schedule periodic expiration checks
         */
        fun schedulePeriodicCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val periodicWorkRequest = PeriodicWorkRequestBuilder<TemporaryApproverExpirationWorker>(
                repeatInterval = 6, // Check every 6 hours
                repeatIntervalTimeUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()
            
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    periodicWorkRequest
                )
            
            Log.d(TAG, "✅ Scheduled periodic temporary approver expiration checks")
        }
        
        /**
         * Schedule an immediate one-time check
         */
        fun scheduleImmediateCheck(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val oneTimeWorkRequest = OneTimeWorkRequestBuilder<TemporaryApproverExpirationWorker>()
                .setConstraints(constraints)
                .build()
            
            WorkManager.getInstance(context)
                .enqueue(oneTimeWorkRequest)
            
            Log.d(TAG, "✅ Scheduled immediate temporary approver expiration check")
        }
        
        /**
         * Cancel all scheduled expiration checks
         */
        fun cancelExpirationChecks(context: Context) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(WORK_NAME)
            
            Log.d(TAG, "✅ Cancelled temporary approver expiration checks")
        }
    }
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            Log.d(TAG, "🔄 Starting temporary approver expiration check...")
            
            // Create repository instance
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            // For expiration service, we don't need notifications, so we'll create a minimal notification service
            val notificationRepository = com.deeksha.avr.repository.NotificationRepository(firestore)
            val authRepository = com.deeksha.avr.repository.AuthRepository(firestore)
            val notificationService = com.deeksha.avr.service.NotificationService(
                notificationRepository,
                authRepository,
                firestore
            )
            val temporaryApproverRepository = TemporaryApproverRepository(firestore, notificationService)
            val projectRepository = com.deeksha.avr.repository.ProjectRepository(firestore, temporaryApproverRepository)
            
            // Get all projects
            val allProjects = getAllProjects()
            Log.d(TAG, "📋 Found ${allProjects.size} projects to check")
            
            var totalDeactivated = 0
            var projectsChecked = 0
            
            // Check each project for expired temporary approvers
            for (project in allProjects) {
                try {
                    val deactivatedCount = temporaryApproverRepository.deactivateExpiredApprovers(project.id)
                    
                    if (deactivatedCount.isSuccess) {
                        val count = deactivatedCount.getOrNull() ?: 0
                        totalDeactivated += count
                        projectsChecked++
                        
                        if (count > 0) {
                            Log.d(TAG, "✅ Deactivated $count expired temporary approvers in project: ${project.name}")
                        }
                    } else {
                        Log.w(TAG, "⚠️ Failed to check project ${project.name}: ${deactivatedCount.exceptionOrNull()?.message}")
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error checking project ${project.name}", e)
                }
            }
            
            Log.d(TAG, "✅ Expiration check completed - $totalDeactivated approvers deactivated across $projectsChecked projects")
            
            // If we deactivated any approvers, we could send notifications to production heads
            if (totalDeactivated > 0) {
                Log.d(TAG, "📧 Consider sending notification about $totalDeactivated expired temporary approvers")
                // Notify production heads about expired approvers
            }
            
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Temporary approver expiration check failed", e)
            Result.failure()
        }
    }
    
    /**
     * Get all projects from the repository
     */
    private suspend fun getAllProjects(): List<com.deeksha.avr.model.Project> {
        return try {
            // Since ProjectRepository doesn't have a simple getAllProjects method,
            // we'll query Firestore directly
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("projects").get().await()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    val projectData = doc.data ?: return@mapNotNull null
                    
                    com.deeksha.avr.model.Project(
                        id = doc.id,
                        name = projectData["name"] as? String ?: "",
                        description = projectData["description"] as? String ?: "",
                        budget = (projectData["budget"] as? Number)?.toDouble() ?: 0.0,
                        spent = (projectData["spent"] as? Number)?.toDouble() ?: 0.0,
                        startDate = projectData["startDate"] as? com.google.firebase.Timestamp,
                        endDate = projectData["endDate"] as? com.google.firebase.Timestamp,
                        status = projectData["status"] as? String ?: "ACTIVE",
                        managerId = projectData["managerId"] as? String ?: "",
                        approverIds = projectData["approverIds"] as? List<String> ?: emptyList(),
                        productionHeadIds = projectData["productionHeadIds"] as? List<String> ?: emptyList(),
                        teamMembers = projectData["teamMembers"] as? List<String> ?: emptyList(),
                        createdAt = projectData["createdAt"] as? com.google.firebase.Timestamp,
                        updatedAt = projectData["updatedAt"] as? com.google.firebase.Timestamp,
                        code = projectData["code"] as? String ?: "",
                        departmentBudgets = (projectData["departmentBudgets"] as? Map<String, Any>)?.mapValues { 
                            (it.value as? Number)?.toDouble() ?: 0.0 
                        } ?: emptyMap(),
                        categories = (projectData["categories"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parsing project document ${doc.id}", e)
                    null
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting all projects", e)
            emptyList()
        }
    }
}

/**
 * Utility class to manage temporary approver expiration service
 */
object TemporaryApproverExpirationManager {
    
    private const val TAG = "TempApproverExpirationMgr"
    
    /**
     * Initialize the expiration service - call this when app starts
     */
    fun initialize(context: Context) {
        try {
            Log.d(TAG, "🔄 Initializing temporary approver expiration service...")
            
            // Schedule periodic checks
            TemporaryApproverExpirationWorker.schedulePeriodicCheck(context)
            
            // Schedule an immediate check
            TemporaryApproverExpirationWorker.scheduleImmediateCheck(context)
            
            Log.d(TAG, "✅ Temporary approver expiration service initialized")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to initialize temporary approver expiration service", e)
        }
    }
    
    /**
     * Manually trigger an expiration check
     */
    fun triggerExpirationCheck(context: Context) {
        Log.d(TAG, "🔄 Manually triggering expiration check...")
        TemporaryApproverExpirationWorker.scheduleImmediateCheck(context)
    }
    
    /**
     * Stop the expiration service
     */
    fun shutdown(context: Context) {
        Log.d(TAG, "🔄 Shutting down temporary approver expiration service...")
        TemporaryApproverExpirationWorker.cancelExpirationChecks(context)
        Log.d(TAG, "✅ Temporary approver expiration service shutdown")
    }
}
