package com.deeksha.avr.repository

import android.util.Log
import com.deeksha.avr.model.Project
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    
    fun getActiveProjects(): Flow<List<Project>> = callbackFlow {
        Log.d("ProjectRepository", "🔥 Starting to fetch projects from Firebase")
        
        // First try to get all projects, then filter
        val listener = firestore.collection("projects")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProjectRepository", "❌ Error fetching projects: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                
                Log.d("ProjectRepository", "📊 Received ${snapshot?.documents?.size ?: 0} documents from Firebase")
                
                if (snapshot?.documents?.isEmpty() == true) {
                    Log.w("ProjectRepository", "⚠️ No documents found in 'projects' collection")
                }
                
                val projects = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        Log.d("ProjectRepository", "📄 Processing document ${doc.id}: ${doc.data}")
                        
                        // Get the project data
                        val projectData = doc.data ?: emptyMap()
                        
                        // Check if document has required fields
                        val name = projectData["name"] as? String ?: ""
                        val description = projectData["description"] as? String ?: ""
                        val budget = (projectData["budget"] as? Number)?.toDouble() ?: 0.0
                        val spent = (projectData["spent"] as? Number)?.toDouble() ?: 0.0
                        val status = projectData["status"] as? String ?: "ACTIVE"
                        val managerId = projectData["managerId"] as? String ?: ""
                        val code = projectData["code"] as? String ?: ""
                        val teamMembers = projectData["teamMembers"] as? List<String> ?: emptyList()
                        
                        // Create project with manual mapping to handle missing fields
                        val project = Project(
                            id = doc.id,
                            name = name,
                            description = description,
                            budget = budget,
                            spent = spent,
                            startDate = projectData["startDate"] as? com.google.firebase.Timestamp,
                            endDate = projectData["endDate"] as? com.google.firebase.Timestamp,
                            status = status,
                            managerId = managerId,
                            approverIds = projectData["approverIds"] as? List<String> ?: emptyList(),
                            productionHeadIds = projectData["productionHeadIds"] as? List<String> ?: emptyList(),
                            teamMembers = teamMembers,
                            createdAt = projectData["createdAt"] as? com.google.firebase.Timestamp,
                            updatedAt = projectData["updatedAt"] as? com.google.firebase.Timestamp,
                            code = code,
                            departmentBudgets = (projectData["departmentBudgets"] as? Map<String, Any>)?.mapValues { 
                                (it.value as? Number)?.toDouble() ?: 0.0 
                            } ?: emptyMap(),
                            categories = (projectData["categories"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                        )
                        
                        Log.d("ProjectRepository", "✅ Successfully parsed project: ${project.name} (Status: ${project.status}, Departments: ${project.departmentBudgets.keys})")
                        
                        // Filter active projects (including null/empty status as active)
                        if (project.status.equals("ACTIVE", ignoreCase = true) || 
                            project.status.isEmpty() || 
                            project.name.isNotEmpty()) { // Include any project with a name
                            project
                        } else {
                            Log.d("ProjectRepository", "⏭️ Skipping project with status: ${project.status}")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("ProjectRepository", "❌ Error parsing project document ${doc.id}: ${e.message}")
                        e.printStackTrace()
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
            Log.d("ProjectRepository", "🔚 Closing projects listener")
            listener.remove() 
        }
    }
    
    fun getUserProjects(userId: String): Flow<List<Project>> = callbackFlow {
        val listener = firestore.collection("projects")
            .whereArrayContains("teamMembers", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ProjectRepository", "❌ Error fetching user projects: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }
                
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
                
                trySend(projects)
            }
        
        awaitClose { listener.remove() }
    }
    
    suspend fun getProjectById(projectId: String): Project? {
        return try {
            Log.d("ProjectRepository", "🔍 Fetching project by ID: $projectId")
            val document = firestore.collection("projects").document(projectId).get().await()
            
            if (document.exists()) {
                Log.d("ProjectRepository", "📄 Found document: ${document.data}")
                
                val projectData = document.data ?: return null
                
                // Manual mapping to handle field consistency
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
                    categories = (projectData["categories"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                )
                
                Log.d("ProjectRepository", "✅ Successfully parsed project: ${project.name} (Budget: ${project.budget}, Departments: ${project.departmentBudgets.keys})")
                project
            } else {
                Log.w("ProjectRepository", "❌ Project document not found for ID: $projectId")
                null
            }
        } catch (e: Exception) {
            Log.e("ProjectRepository", "❌ Error fetching project by ID $projectId: ${e.message}")
            e.printStackTrace()
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
                        categories = (projectData["categories"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                    )
                } catch (e: Exception) {
                    Log.e("ProjectRepository", "Error parsing project document ${doc.id}: ${e.message}")
                    null
                }
            }
            
                            Log.d("ProjectRepository", "✅ Found ${projects.size} projects in total")
                projects.forEach { project ->
                    Log.d("ProjectRepository", "  📋 Project: ${project.name} (Departments: ${project.departmentBudgets.keys})")
                }
            projects
        } catch (e: Exception) {
            Log.e("ProjectRepository", "❌ Error getting all projects: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
} 