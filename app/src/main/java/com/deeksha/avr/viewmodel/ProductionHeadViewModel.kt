package com.deeksha.avr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deeksha.avr.model.DepartmentBudget
import com.deeksha.avr.model.Project
import com.deeksha.avr.model.User
import com.deeksha.avr.model.UserRole
import com.deeksha.avr.repository.AuthRepository
import com.deeksha.avr.repository.ProjectRepository
import com.deeksha.avr.repository.NotificationRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductionHeadViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val projectRepository: ProjectRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    
    // UI States
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    // User Creation States
    private val _availableUsers = MutableStateFlow<List<User>>(emptyList())
    val availableUsers: StateFlow<List<User>> = _availableUsers.asStateFlow()
    
    private val _availableApprovers = MutableStateFlow<List<User>>(emptyList())
    val availableApprovers: StateFlow<List<User>> = _availableApprovers.asStateFlow()
    
    // Project Creation States
    private val _departmentBudgets = MutableStateFlow<List<DepartmentBudget>>(emptyList())
    val departmentBudgets: StateFlow<List<DepartmentBudget>> = _departmentBudgets.asStateFlow()
    
    private val _totalBudget = MutableStateFlow(0.0)
    val totalBudget: StateFlow<Double> = _totalBudget.asStateFlow()
    
    private val _totalAllocated = MutableStateFlow(0.0)
    val totalAllocated: StateFlow<Double> = _totalAllocated.asStateFlow()
    
    // Project Edit States
    private val _editingProject = MutableStateFlow<Project?>(null)
    val editingProject: StateFlow<Project?> = _editingProject.asStateFlow()
    
    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()
    
    init {
        loadUsers()
    }
    
    fun loadUsers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                android.util.Log.d("ProductionHeadViewModel", "🔄 Loading users...")
                val users = authRepository.getAllUsers()
                android.util.Log.d("ProductionHeadViewModel", "📊 Found ${users.size} total users")
                
                val regularUsers = users.filter { it.role == UserRole.USER }
                val approvers = users.filter { it.role == UserRole.APPROVER }
                
                android.util.Log.d("ProductionHeadViewModel", "👥 Regular users: ${regularUsers.size}")
                android.util.Log.d("ProductionHeadViewModel", "✅ Approvers: ${approvers.size}")
                
                regularUsers.forEach { user ->
                    android.util.Log.d("ProductionHeadViewModel", "  👤 User: ${user.name} (${user.uid})")
                }
                
                approvers.forEach { approver ->
                    android.util.Log.d("ProductionHeadViewModel", "  ✅ Approver: ${approver.name} (${approver.uid})")
                }
                
                _availableUsers.value = regularUsers
                _availableApprovers.value = approvers
                
                android.util.Log.d("ProductionHeadViewModel", "✅ Successfully loaded users")
            } catch (e: Exception) {
                android.util.Log.e("ProductionHeadViewModel", "❌ Error loading users: ${e.message}", e)
                _error.value = "Failed to load users: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun createUser(phoneNumber: String, fullName: String, role: UserRole) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val user = User(
                    uid = "", // Will be set by Firebase
                    phone = phoneNumber.replace("+91", "").trim(),
                    name = fullName,
                    role = role,
                    email = "",
                    createdAt = System.currentTimeMillis(),
                    isActive = true
                )
                
                val result = authRepository.createUserByAdmin(user)
                if (result.isSuccess) {
                    _successMessage.value = "User created successfully!"
                    loadUsers() // Refresh user list
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to create user"
                }
            } catch (e: Exception) {
                _error.value = "Error creating user: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun createProject(
        projectName: String,
        description: String,
        startDate: Timestamp,
        endDate: Timestamp?,
        totalBudget: Double,
        managerId: String,
        teamMemberIds: List<String>,
        departmentBudgets: List<DepartmentBudget>,
        categories: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                android.util.Log.d("ProductionHeadViewModel", "🚀 Creating project: $projectName")
                android.util.Log.d("ProductionHeadViewModel", "📋 Manager ID: $managerId")
                android.util.Log.d("ProductionHeadViewModel", "👥 Team members: ${teamMemberIds.size}")
                android.util.Log.d("ProductionHeadViewModel", "💰 Budget: $totalBudget")
                
                // Validate inputs
                if (projectName.isBlank()) {
                    throw Exception("Project name cannot be empty")
                }
                
                if (managerId.isBlank()) {
                    throw Exception("Manager ID cannot be empty")
                }
                
                if (teamMemberIds.isEmpty()) {
                    throw Exception("At least one team member must be selected")
                }
                
                val budgetMap = departmentBudgets.associate { 
                    it.departmentName to it.allocatedBudget 
                }
                
                val project = Project(
                    id = "", // Will be generated by Firestore
                    name = projectName,
                    description = description,
                    budget = totalBudget,
                    spent = 0.0,
                    startDate = startDate,
                    endDate = endDate,
                    status = "ACTIVE",
                    managerId = managerId,
                    teamMembers = teamMemberIds,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now(),
                    code = generateProjectCode(projectName),
                    departmentBudgets = budgetMap,
                    categories = categories
                )
                
                android.util.Log.d("ProductionHeadViewModel", "📦 Project object created, sending to repository")
                
                val result = projectRepository.createProject(project)
                if (result.isSuccess) {
                    android.util.Log.d("ProductionHeadViewModel", "✅ Project created successfully with ID: ${result.getOrNull()}")
                    
                    // Send notifications to assigned team members using the project data we created
                    sendProjectAssignmentNotifications(project, managerId, teamMemberIds)
                    
                    _successMessage.value = "Project created successfully!"
                    clearProjectForm()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to create project"
                    android.util.Log.e("ProductionHeadViewModel", "❌ Project creation failed: $errorMsg")
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                android.util.Log.e("ProductionHeadViewModel", "❌ Error creating project: ${e.message}", e)
                _error.value = "Error creating project: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun addDepartmentBudget(departmentName: String, budget: Double) {
        val currentList = _departmentBudgets.value.toMutableList()
        
        // Check if department already exists
        val existingIndex = currentList.indexOfFirst { it.departmentName == departmentName }
        if (existingIndex >= 0) {
            currentList[existingIndex] = DepartmentBudget(departmentName, budget)
        } else {
            currentList.add(DepartmentBudget(departmentName, budget))
        }
        
        _departmentBudgets.value = currentList
        calculateTotalAllocated()
    }
    
    fun removeDepartmentBudget(departmentName: String) {
        val currentList = _departmentBudgets.value.toMutableList()
        currentList.removeAll { it.departmentName == departmentName }
        _departmentBudgets.value = currentList
        calculateTotalAllocated()
    }
    
    fun updateTotalBudget(budget: Double) {
        _totalBudget.value = budget
    }
    
    private fun calculateTotalAllocated() {
        val total = _departmentBudgets.value.sumOf { it.allocatedBudget }
        _totalAllocated.value = total
    }
    
    private fun generateProjectCode(projectName: String): String {
        val words = projectName.split(" ")
        return if (words.size >= 2) {
            "${words[0].take(2).uppercase()}${words[1].take(1).uppercase()}"
        } else {
            projectName.take(3).uppercase()
        }
    }
    
    private fun clearProjectForm() {
        _departmentBudgets.value = emptyList()
        _totalBudget.value = 0.0
        _totalAllocated.value = 0.0
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    fun refreshUsers() {
        loadUsers()
    }
    
    // Project Edit Methods
    fun loadProjectForEdit(projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val project = projectRepository.getProjectById(projectId)
                if (project != null) {
                    _editingProject.value = project
                    _isEditMode.value = true
                    
                    // Load project data into form
                    _totalBudget.value = project.budget
                    
                    // Convert department budgets map to list
                    val deptBudgets = project.departmentBudgets.map { (dept, budget) ->
                        DepartmentBudget(dept, budget)
                    }
                    _departmentBudgets.value = deptBudgets
                    calculateTotalAllocated()
                    
                    android.util.Log.d("ProductionHeadViewModel", "✅ Loaded project for editing: ${project.name}")
                } else {
                    _error.value = "Project not found"
                }
            } catch (e: Exception) {
                android.util.Log.e("ProductionHeadViewModel", "❌ Error loading project for edit: ${e.message}")
                _error.value = "Error loading project: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateProject(
        projectId: String,
        projectName: String,
        description: String,
        startDate: Timestamp,
        endDate: Timestamp?,
        totalBudget: Double,
        managerId: String,
        teamMemberIds: List<String>,
        departmentBudgets: List<DepartmentBudget>,
        categories: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                android.util.Log.d("ProductionHeadViewModel", "🔄 Updating project: $projectName")
                
                // Validate inputs
                if (projectName.isBlank()) {
                    throw Exception("Project name cannot be empty")
                }
                
                if (managerId.isBlank()) {
                    throw Exception("Manager ID cannot be empty")
                }
                
                if (teamMemberIds.isEmpty()) {
                    throw Exception("At least one team member must be selected")
                }
                
                val budgetMap = departmentBudgets.associate { 
                    it.departmentName to it.allocatedBudget 
                }
                
                val updatedProject = Project(
                    id = projectId,
                    name = projectName,
                    description = description,
                    budget = totalBudget,
                    spent = _editingProject.value?.spent ?: 0.0, // Preserve spent amount
                    startDate = startDate,
                    endDate = endDate,
                    status = _editingProject.value?.status ?: "ACTIVE", // Preserve status
                    managerId = managerId,
                    approverIds = _editingProject.value?.approverIds ?: emptyList(), // Preserve approvers
                    productionHeadIds = _editingProject.value?.productionHeadIds ?: emptyList(), // Preserve production heads
                    teamMembers = teamMemberIds,
                    createdAt = _editingProject.value?.createdAt, // Preserve creation date
                    updatedAt = Timestamp.now(),
                    code = _editingProject.value?.code ?: generateProjectCode(projectName), // Preserve or regenerate code
                    departmentBudgets = budgetMap,
                    categories = categories
                )
                
                android.util.Log.d("ProductionHeadViewModel", "📦 Updated project object created, sending to repository")
                
                val result = projectRepository.updateProject(projectId, updatedProject)
                if (result.isSuccess) {
                    android.util.Log.d("ProductionHeadViewModel", "✅ Project updated successfully")
                    
                    // Send notifications to newly assigned team members
                    sendProjectUpdateNotifications(updatedProject, managerId, teamMemberIds)
                    
                    _successMessage.value = "Project updated successfully!"
                    clearEditState()
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to update project"
                    android.util.Log.e("ProductionHeadViewModel", "❌ Project update failed: $errorMsg")
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                android.util.Log.e("ProductionHeadViewModel", "❌ Error updating project: ${e.message}", e)
                _error.value = "Error updating project: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearEditState() {
        _editingProject.value = null
        _isEditMode.value = false
        clearProjectForm()
    }
    
    private fun sendProjectUpdateNotifications(
        project: Project,
        managerId: String,
        teamMemberIds: List<String>
    ) {
        viewModelScope.launch {
            try {
                // Get all users to determine their roles
                val allUsers = authRepository.getAllUsers()
                
                // Send notification to manager (approver)
                val manager = allUsers.find { it.phone == managerId }
                if (manager != null) {
                    notificationRepository.createProjectAssignmentNotification(
                        recipientId = managerId,
                        recipientRole = manager.role.name,
                        projectId = project.id,
                        projectName = project.name,
                        assignedRole = "Project Manager"
                    )
                }
                
                // Send notifications to team members
                teamMemberIds.forEach { memberId ->
                    val member = allUsers.find { it.phone == memberId }
                    if (member != null) {
                        notificationRepository.createProjectAssignmentNotification(
                            recipientId = memberId,
                            recipientRole = member.role.name,
                            projectId = project.id,
                            projectName = project.name,
                            assignedRole = "Team Member"
                        )
                    }
                }
                
                android.util.Log.d("ProductionHeadViewModel", "✅ Sent update notifications for project: ${project.name}")
            } catch (e: Exception) {
                android.util.Log.e("ProductionHeadViewModel", "❌ Error sending update notifications: ${e.message}")
            }
        }
    }

    private fun sendProjectAssignmentNotifications(
        project: Project,
        managerId: String,
        teamMemberIds: List<String>
    ) {
        viewModelScope.launch {
            try {
                // Get all users to determine their roles
                val allUsers = authRepository.getAllUsers()
                
                // Send notification to manager (approver)
                val manager = allUsers.find { it.phone == managerId }
                if (manager != null) {
                    notificationRepository.createProjectAssignmentNotification(
                        recipientId = managerId,
                        recipientRole = manager.role.name,
                        projectId = project.id,
                        projectName = project.name,
                        assignedRole = "Project Manager"
                    )
                }
                
                // Send notifications to team members
                teamMemberIds.forEach { memberId ->
                    val member = allUsers.find { it.phone == memberId }
                    if (member != null) {
                        notificationRepository.createProjectAssignmentNotification(
                            recipientId = memberId,
                            recipientRole = member.role.name,
                            projectId = project.id,
                            projectName = project.name,
                            assignedRole = "Team Member"
                        )
                    }
                }
                
                android.util.Log.d("ProductionHeadViewModel", "✅ Sent assignment notifications for project: ${project.name}")
            } catch (e: Exception) {
                android.util.Log.e("ProductionHeadViewModel", "❌ Error sending assignment notifications: ${e.message}")
            }
        }
    }
} 