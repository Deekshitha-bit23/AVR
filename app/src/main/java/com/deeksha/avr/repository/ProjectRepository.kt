package com.deeksha.avr.repository

import android.util.Log
import com.deeksha.avr.model.Project
import com.deeksha.avr.model.TemporaryApprover
import com.deeksha.avr.model.isExpired
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val temporaryApproverRepository: TemporaryApproverRepository
) {
    
    fun getUserProjects(userId: String): Flow<List<Project>> = callbackFlow {
        Log.d("ProjectRepository", "🔥 Starting to fetch projects for user: $userId")
        
        val listener = firestore.collection("projects")
            .whereArrayContains("teamMembers", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProjectRepository", "❌ Error fetching user projects: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                
                Log.d("ProjectRepository", "📊 Received ${snapshot?.documents?.size ?: 0} projects for user $userId from Firebase")
                
                val projects = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val projectData = doc.data ?: emptyMap()
                        
                        Project(
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
                        Log.e("ProjectRepository", "Error parsing user project document ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()
                
                Log.d("ProjectRepository", "🎯 Sending ${projects.size} projects for user $userId to UI")
                projects.forEach { project ->
                    Log.d("ProjectRepository", "  📋 User Project: ${project.name} (ID: ${project.id}, Budget: ${project.budget})")
                }
                
                trySend(projects)
            }
        
        awaitClose { 
            Log.d("ProjectRepository", "🔚 Closing user projects listener for user: $userId")
            listener.remove() 
        }
    }

    /**
     * Get projects where the user is a temporary approver
     */
    fun getTemporaryApproverProjects(userId: String): Flow<List<Project>> = callbackFlow {
        Log.d("ProjectRepository", "🔥 Starting to fetch temporary approver projects for user: $userId")
        Log.d("ProjectRepository", "📱 User ID type: ${userId::class.simpleName}, length: ${userId.length}")

        val listener = firestore.collection("projects")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProjectRepository", "❌ Error fetching temporary approver projects: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                Log.d("ProjectRepository", "📊 Received ${snapshot?.documents?.size ?: 0} projects for temporary approver check")

                val allProjects = snapshot?.documents?.mapNotNull { doc ->
                    Log.d("ProjectRepository", "🔍 Processing project document: ${doc.id}")
                    try {
                        val projectData = doc.data ?: emptyMap()
                        Project(
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
                            categories = (projectData["categories"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            temporaryApproverPhone = projectData["temporaryApproverPhone"] as? String ?: ""
                        ).also { project ->
                            Log.d("ProjectRepository", "📋 Project ${project.name}: temporaryApproverPhone='${project.temporaryApproverPhone}'")
                        }
                    } catch (e: Exception) {
                        Log.e("ProjectRepository", "Error parsing project document ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()

                // Filter projects where user is a temporary approver (only accepted assignments)
                val temporaryProjects = allProjects.filter { project ->
                    // Normalize phone numbers for comparison (remove +91, spaces, etc.)
                    val normalizedProjectPhone = project.temporaryApproverPhone.replace("+91", "").replace(" ", "").trim()
                    val normalizedUserId = userId.replace("+91", "").replace(" ", "").trim()
                    
                    val isMatch = normalizedProjectPhone == normalizedUserId
                    Log.d("ProjectRepository", "🔍 Checking project ${project.name}:")
                    Log.d("ProjectRepository", "   - temporaryApproverPhone='${project.temporaryApproverPhone}' -> normalized='$normalizedProjectPhone'")
                    Log.d("ProjectRepository", "   - userId='$userId' -> normalized='$normalizedUserId'")
                    Log.d("ProjectRepository", "   - Match: $isMatch")
                    isMatch
                }

                // Filter out rejected assignments - rejected assignments have empty temporaryApproverPhone
                // because the reject logic removes this field from the project document
                val acceptedTemporaryProjects = temporaryProjects.filter { project ->
                    val hasTemporaryApproverPhone = project.temporaryApproverPhone.isNotEmpty()
                    Log.d("ProjectRepository", "🔍 Project ${project.name}: hasTemporaryApproverPhone=$hasTemporaryApproverPhone")
                    hasTemporaryApproverPhone
                }
                
                // Filter out projects where delegation has expired
                // Note: We'll do this filtering in a different way since we can't call suspend functions in filter
                val activeTemporaryProjects = acceptedTemporaryProjects

                Log.d("ProjectRepository", "🎯 Sending ${activeTemporaryProjects.size} active temporary approver projects for user $userId to UI")
                activeTemporaryProjects.forEach { project ->
                    Log.d("ProjectRepository", "  📋 Active Temporary Project: ${project.name} (ID: ${project.id}, Budget: ${project.budget})")
                }
                
                trySend(activeTemporaryProjects)
            }
        
        awaitClose { 
            Log.d("ProjectRepository", "🔚 Closing temporary approver projects listener for user: $userId")
            listener.remove() 
        }
    }

    fun getApproverProjects(userId: String): Flow<List<Project>> = callbackFlow {
        Log.d("ProjectRepository", "🔥 Starting to fetch projects for user: $userId")

        val listener = firestore.collection("projects")
            .whereEqualTo("managerId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProjectRepository", "❌ Error fetching user projects: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                Log.d("ProjectRepository", "📊 Received ${snapshot?.documents?.size ?: 0} projects for user $userId from Firebase")

                val regularProjects = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val projectData = doc.data ?: emptyMap()

                        Project(
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
                            categories = (projectData["categories"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            temporaryApproverPhone = projectData["temporaryApproverPhone"] as? String ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("ProjectRepository", "Error parsing user project document ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()

                // For now, we'll only show regular projects
                // Temporary approver projects will be handled by a separate query
                val temporaryProjects = emptyList<Project>()

                // Combine regular and temporary projects
                val allProjects = (regularProjects + temporaryProjects).distinctBy { it.id }

                Log.d("ProjectRepository", "🎯 Sending ${allProjects.size} projects for user $userId to UI (${regularProjects.size} regular + ${temporaryProjects.size} temporary)")
                allProjects.forEach { project ->
                    val projectType = if (project.managerId == userId) "Regular" else "Temporary"
                    Log.d("ProjectRepository", "  📋 $projectType Project: ${project.name} (ID: ${project.id}, Budget: ${project.budget})")
                }
                
                trySend(allProjects)
            }
        
        awaitClose { 
            Log.d("ProjectRepository", "🔚 Closing user projects listener for user: $userId")
            listener.remove() 
        }
    }
    
    // Get a single project by ID
    suspend fun getProjectById(projectId: String): Project? {
        return try {
            Log.d("ProjectRepository", "🔍 Getting project by ID: $projectId")
            val document = firestore.collection("projects").document(projectId).get().await()
            
            if (document.exists()) {
                val projectData = document.data ?: emptyMap()
                
                val project = Project(
                    id = document.id,
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
                    categories = (projectData["categories"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    temporaryApproverPhone = projectData["temporaryApproverPhone"] as? String ?: ""
                )
                
                Log.d("ProjectRepository", "✅ Found project: ${project.name}")
                project
            } else {
                Log.d("ProjectRepository", "❌ Project not found with ID: $projectId")
                null
            }
        } catch (e: Exception) {
            Log.e("ProjectRepository", "❌ Error getting project by ID: ${e.message}")
            null
        }
    }
    
    suspend fun createProject(project: Project): Result<String> {
        return try {
            val docRef = firestore.collection("projects").add(project).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateProject(projectId: String, project: Project): Result<Unit> {
        return try {
            firestore.collection("projects").document(projectId).set(project).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update project approvers and production heads
    suspend fun updateProjectAssignments(
        projectId: String,
        approverIds: List<String>,
        productionHeadIds: List<String>
    ): Result<Unit> {
        return try {
            Log.d("ProjectRepository", "🔄 Updating project assignments for project: $projectId")
            Log.d("ProjectRepository", "📋 Approvers: $approverIds")
            Log.d("ProjectRepository", "📋 Production Heads: $productionHeadIds")
            
            val projectRef = firestore.collection("projects").document(projectId)
            projectRef.update(
                mapOf(
                    "approverIds" to approverIds,
                    "productionHeadIds" to productionHeadIds,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )
            ).await()
            
            Log.d("ProjectRepository", "✅ Successfully updated project assignments")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ProjectRepository", "❌ Error updating project assignments: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Direct method to get all projects for debugging purposes
    suspend fun getAllProjects(): List<Project> {
        return try {
            Log.d("ProjectRepository", "🔍 Getting all projects directly for debugging")
            val snapshot = firestore.collection("projects").get().await()
            
            val projects = snapshot.documents.mapNotNull { doc ->
                try {
                    val projectData = doc.data ?: emptyMap()
                    
                    Project(
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
                        categories = (projectData["categories"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        temporaryApproverPhone = projectData["temporaryApproverPhone"] as? String ?: ""
                    )
                } catch (e: Exception) {
                    Log.e("ProjectRepository", "Error parsing project document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            Log.d("ProjectRepository", "✅ Found ${projects.size} projects in total")
            projects.forEach { project ->
                Log.d("ProjectRepository", "  📋 Project: ${project.name} (ID: ${project.id}, Budget: ${project.budget})")
            }
            projects
        } catch (e: Exception) {
            Log.e("ProjectRepository", "❌ Error getting all projects: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    // Get all projects as a Flow for production heads
    fun getAllProjectsFlow(): Flow<List<Project>> = callbackFlow {
        Log.d("ProjectRepository", "🔥 Starting to fetch all projects for production head")
        
        val listener = firestore.collection("projects")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProjectRepository", "❌ Error fetching all projects: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                
                Log.d("ProjectRepository", "📊 Received ${snapshot?.documents?.size ?: 0} projects from Firebase")
                
                val projects = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val projectData = doc.data ?: emptyMap()
                        
                        Project(
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
                            categories = (projectData["categories"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            temporaryApproverPhone = projectData["temporaryApproverPhone"] as? String ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("ProjectRepository", "Error parsing project document ${doc.id}: ${e.message}")
                        null
                    }
                } ?: emptyList()
                
                Log.d("ProjectRepository", "🎯 Sending ${projects.size} projects to UI")
                projects.forEach { project ->
                    Log.d("ProjectRepository", "  📋 Project: ${project.name} (ID: ${project.id}, Budget: ${project.budget})")
                }
                
                trySend(projects)
            }
        
        awaitClose { 
            Log.d("ProjectRepository", "🔚 Closing all projects listener")
            listener.remove() 
        }
    }
    
    suspend fun updateProjectStatus(projectId: String, newStatus: String): Result<Unit> {
        return try {
            firestore.collection("projects").document(projectId)
                .update(
                    mapOf(
                        "status" to newStatus,
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if delegation is still active for a user in a project
     * This method can be called from UI layer to filter expired delegations
     */
    suspend fun isDelegationActiveForUser(projectId: String, userId: String): Boolean {
        return try {
            Log.d("ProjectRepository", "🔍 Checking delegation status for user $userId in project $projectId")
            
            // Get temporary approvers for this project
            val tempApprovers = temporaryApproverRepository.getTemporaryApproversForProject(projectId)
            
            // Find active delegation for this user
            val userDelegation = tempApprovers.find { approver ->
                approver.approverId == userId && approver.isActive && approver.status == "ACCEPTED"
            }
            
            if (userDelegation == null) {
                Log.d("ProjectRepository", "❌ No active delegation found for user $userId in project $projectId")
                return false
            }
            
            // Check if delegation has expired
            val isExpired = userDelegation.isExpired()
            val isActive = !isExpired
            
            Log.d("ProjectRepository", "📊 Delegation status for user $userId in project $projectId:")
            Log.d("ProjectRepository", "   - isActive: ${userDelegation.isActive}")
            Log.d("ProjectRepository", "   - status: ${userDelegation.status}")
            Log.d("ProjectRepository", "   - isExpired: $isExpired")
            Log.d("ProjectRepository", "   - final result: $isActive")
            
            isActive
            
        } catch (e: Exception) {
            Log.e("ProjectRepository", "❌ Error checking delegation status: ${e.message}", e)
            false // Return false on error to be safe
        }
    }
} 