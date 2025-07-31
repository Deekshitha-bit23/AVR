package com.deeksha.avr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deeksha.avr.model.Project
import com.deeksha.avr.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {
    
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()
    
    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun loadProjects(userId: String? = null) {
        viewModelScope.launch {
            println("🚀 ProjectViewModel: Starting to load projects")
            println("🚀 ProjectViewModel: User ID: $userId")
            _isLoading.value = true
            _error.value = null
            
            try {
                if (userId != null) {
                    // Load projects for specific user using flow
                    projectRepository.getUserProjects(userId).collect { projectList ->
                        println("📦 ProjectViewModel: Received ${projectList.size} projects for user $userId from repository")
                        projectList.forEach { project ->
                            println("  📋 Project: ${project.name} (ID: ${project.id})")
                        }
                        _projects.value = projectList
                        _isLoading.value = false
                    }
                } else {
                    // Load all active projects (fallback) - this is a one-time call
                    val projectList = projectRepository.getAllProjects()
                    println("📦 ProjectViewModel: Received ${projectList.size} projects from repository")
                    projectList.forEach { project ->
                        println("  📋 Project: ${project.name} (ID: ${project.id})")
                    }
                    _projects.value = projectList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                println("❌ ProjectViewModel: Error loading projects: ${e.message}")
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }

    
    suspend fun createProject(project: Project): Result<String> {
        return projectRepository.createProject(project)
    }
    
    suspend fun updateProject(projectId: String, project: Project): Result<Unit> {
        return projectRepository.updateProject(projectId, project)
    }
    
    fun clearError() {
        _error.value = null
    }

    // Assign approvers and production heads to a project
    fun assignProjectMembers(
        projectId: String,
        approverIds: List<String>,
        productionHeadIds: List<String>
    ) {
        viewModelScope.launch {
            try {
                Log.d("ProjectViewModel", "🔄 Assigning project members to project: $projectId")
                
                val result = projectRepository.updateProjectAssignments(projectId, approverIds, productionHeadIds)
                
                if (result.isSuccess) {
                    Log.d("ProjectViewModel", "✅ Successfully assigned project members")
                    // Reload projects to get updated data
                    loadProjects()
                } else {
                    Log.e("ProjectViewModel", "❌ Failed to assign project members: ${result.exceptionOrNull()?.message}")
                    _error.value = "Failed to assign project members: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                Log.e("ProjectViewModel", "❌ Error assigning project members: ${e.message}")
                _error.value = "Error assigning project members: ${e.message}"
            }
        }
    }
} 