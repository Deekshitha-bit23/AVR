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
                    
                    // Send notifications to approvers about the new project
                    sendNewProjectNotifications(project, managerId, teamMemberIds)
                    
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
                
                // Store original project data for comparison
                val originalProject = _editingProject.value
                
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
                    
                    // Send notifications for project changes
                    if (originalProject != null) {
                        sendProjectChangeNotifications(originalProject, updatedProject)
                    }
                    

                    
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
    
    fun updateProjectStatus(projectId: String, newStatus: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                android.util.Log.d("ProductionHeadViewModel", "🔄 Updating project status: $projectId to $newStatus")
                
                val result = projectRepository.updateProjectStatus(projectId, newStatus)
                if (result.isSuccess) {
                    android.util.Log.d("ProductionHeadViewModel", "✅ Project status updated successfully")
                    
                    // Send status change notifications
                    sendProjectStatusChangeNotifications(projectId, newStatus)
                    
                    _successMessage.value = "Project status updated to $newStatus!"
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to update project status"
                    android.util.Log.e("ProductionHeadViewModel", "❌ Project status update failed: $errorMsg")
                    _error.value = errorMsg
                }
            } catch (e: Exception) {
                android.util.Log.e("ProductionHeadViewModel", "❌ Error updating project status: ${e.message}", e)
                _error.value = "Error updating project status: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun sendProjectStatusChangeNotifications(projectId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                // Get project details
                val project = projectRepository.getProjectById(projectId)
                if (project == null) {
                    android.util.Log.e("ProductionHeadViewModel", "❌ Project not found for status notifications: $projectId")
                    return@launch
                }
                
                // Get all users to determine their roles
                val allUsers = authRepository.getAllUsers()
                val currentUser = authRepository.getCurrentUser()
                val changedBy = currentUser?.name ?: "Production Head"
                
                val statusMessage = when (newStatus.uppercase()) {
                    "ACTIVE" -> "Project has been activated"
                    "PAUSED" -> "Project has been paused"
                    "COMPLETED" -> "Project has been marked as completed"
                    "CANCELLED" -> "Project has been cancelled"
                    else -> "Project status changed to $newStatus"
                }
                
                // Send notification to manager (approver)
                val manager = allUsers.find { it.phone == project.managerId }
                if (manager != null) {
                    notificationRepository.createProjectChangeNotification(
                        recipientId = manager.phone,
                        recipientRole = manager.role.name,
                        projectId = project.id,
                        projectName = project.name,
                        changeDescription = statusMessage,
                        changedBy = changedBy
                    )
                }
                
                // Send notifications to approvers
                project.approverIds.forEach { approverId ->
                    val approver = allUsers.find { it.phone == approverId }
                    if (approver != null) {
                        notificationRepository.createProjectChangeNotification(
                            recipientId = approverId,
                            recipientRole = approver.role.name,
                            projectId = project.id,
                            projectName = project.name,
                            changeDescription = statusMessage,
                            changedBy = changedBy
                        )
                    }
                }
                
                // Send notifications to team members
                project.teamMembers.forEach { memberId ->
                    val member = allUsers.find { it.phone == memberId }
                    if (member != null) {
                        notificationRepository.createProjectChangeNotification(
                            recipientId = memberId,
                            recipientRole = member.role.name,
                            projectId = project.id,
                            projectName = project.name,
                            changeDescription = statusMessage,
                            changedBy = changedBy
                        )
                    }
                }
                
                android.util.Log.d("ProductionHeadViewModel", "✅ Sent status change notifications for project: ${project.name}")
            } catch (e: Exception) {
                android.util.Log.e("ProductionHeadViewModel", "❌ Error sending status change notifications: ${e.message}")
            }
        }
    }



    private fun sendNewProjectNotifications(
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
                    notificationRepository.createNewProjectNotification(
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
                        notificationRepository.createNewProjectNotification(
                            recipientId = memberId,
                            recipientRole = member.role.name,
                            projectId = project.id,
                            projectName = project.name,
                            assignedRole = "Team Member"
                        )
                    }
                }
                
                // Send notifications to approvers
                project.approverIds.forEach { approverId ->
                    val approver = allUsers.find { it.phone == approverId }
                    if (approver != null) {
                        notificationRepository.createNewProjectNotification(
                            recipientId = approverId,
                            recipientRole = approver.role.name,
                            projectId = project.id,
                            projectName = project.name,
                            assignedRole = "Approver"
                        )
                    }
                }
                
                android.util.Log.d("ProductionHeadViewModel", "✅ Sent new project notifications for project: ${project.name}")
            } catch (e: Exception) {
                android.util.Log.e("ProductionHeadViewModel", "❌ Error sending new project notifications: ${e.message}")
            }
        }
    }

    private fun sendProjectChangeNotifications(originalProject: Project, updatedProject: Project) {
        viewModelScope.launch {
            try {
                // Get all users to determine their roles
                val allUsers = authRepository.getAllUsers()
                
                // Detect what changes were made
                val changes = mutableListOf<String>()
                
                if (originalProject.name != updatedProject.name) {
                    changes.add("Project name changed from '${originalProject.name}' to '${updatedProject.name}'")
                }
                
                if (originalProject.description != updatedProject.description) {
                    changes.add("Project description updated")
                }
                
                if (originalProject.budget != updatedProject.budget) {
                    changes.add("Budget changed from ₹${originalProject.budget} to ₹${updatedProject.budget}")
                }
                
                if (originalProject.startDate != updatedProject.startDate) {
                    changes.add("Start date updated")
                }
                
                if (originalProject.endDate != updatedProject.endDate) {
                    changes.add("End date updated")
                }
                
                if (originalProject.managerId != updatedProject.managerId) {
                    changes.add("Project manager changed")
                }
                
                if (originalProject.teamMembers != updatedProject.teamMembers) {
                    changes.add("Team members updated")
                }
                
                if (originalProject.departmentBudgets != updatedProject.departmentBudgets) {
                    changes.add("Department budgets updated")
                }
                
                if (originalProject.categories != updatedProject.categories) {
                    changes.add("Project categories updated")
                }
                
                // If no changes detected, don't send notifications
                if (changes.isEmpty()) {
                    android.util.Log.d("ProductionHeadViewModel", "ℹ️ No significant changes detected, skipping notifications")
                    return@launch
                }
                
                val changeDescription = changes.joinToString(". ")
                val currentUser = authRepository.getCurrentUser()
                val changedBy = currentUser?.name ?: "Production Head"
                
                // Send notification to manager (approver)
                val manager = allUsers.find { it.phone == updatedProject.managerId }
                if (manager != null) {
                    notificationRepository.createProjectChangeNotification(
                        recipientId = manager.phone,
                        recipientRole = manager.role.name,
                        projectId = updatedProject.id,
                        projectName = updatedProject.name,
                        changeDescription = changeDescription,
                        changedBy = changedBy
                    )
                }
                
                // Send notifications to approvers
                updatedProject.approverIds.forEach { approverId ->
                    val approver = allUsers.find { it.phone == approverId }
                    if (approver != null) {
                        notificationRepository.createProjectChangeNotification(
                            recipientId = approverId,
                            recipientRole = approver.role.name,
                            projectId = updatedProject.id,
                            projectName = updatedProject.name,
                            changeDescription = changeDescription,
                            changedBy = changedBy
                        )
                    }
                }
                
                // Send notifications to team members
                updatedProject.teamMembers.forEach { memberId ->
                    val member = allUsers.find { it.phone == memberId }
                    if (member != null) {
                        notificationRepository.createProjectChangeNotification(
                            recipientId = memberId,
                            recipientRole = member.role.name,
                            projectId = updatedProject.id,
                            projectName = updatedProject.name,
                            changeDescription = changeDescription,
                            changedBy = changedBy
                        )
                    }
                }
                
                // Send special budget change notifications to approvers if budget was modified
                if (originalProject.budget != updatedProject.budget) {
                    sendBudgetChangeNotifications(updatedProject, originalProject.budget, updatedProject.budget, changedBy)
                }
                
                android.util.Log.d("ProductionHeadViewModel", "✅ Sent change notifications for project: ${updatedProject.name}")
                android.util.Log.d("ProductionHeadViewModel", "📝 Changes detected: $changeDescription")
            } catch (e: Exception) {
                android.util.Log.e("ProductionHeadViewModel", "❌ Error sending change notifications: ${e.message}")
            }
        }
    }
    
    private fun sendBudgetChangeNotifications(
        project: Project,
        oldBudget: Double,
        newBudget: Double,
        changedBy: String
    ) {
        viewModelScope.launch {
            try {
                val allUsers = authRepository.getAllUsers()
                val budgetChangeMessage = "Budget changed from ₹$oldBudget to ₹$newBudget"
                
                // Send to all approvers
                project.approverIds.forEach { approverId ->
                    val approver = allUsers.find { it.phone == approverId }
                    if (approver != null) {
                        notificationRepository.createProjectChangeNotification(
                            recipientId = approverId,
                            recipientRole = approver.role.name,
                            projectId = project.id,
                            projectName = project.name,
                            changeDescription = budgetChangeMessage,
                            changedBy = changedBy
                        )
                    }
                }
                
                // Send to project manager if they're an approver
                val manager = allUsers.find { it.phone == project.managerId }
                if (manager != null && manager.role == UserRole.APPROVER) {
                    notificationRepository.createProjectChangeNotification(
                        recipientId = project.managerId,
                        recipientRole = manager.role.name,
                        projectId = project.id,
                        projectName = project.name,
                        changeDescription = budgetChangeMessage,
                        changedBy = changedBy
                    )
                }
                
                android.util.Log.d("ProductionHeadViewModel", "💰 Sent budget change notifications for project: ${project.name}")
            } catch (e: Exception) {
                android.util.Log.e("ProductionHeadViewModel", "❌ Error sending budget change notifications: ${e.message}")
            }
        }
    }

    fun clearEditState() {
        _editingProject.value = null
        _isEditMode.value = false
        clearProjectForm()
    }
    

} 