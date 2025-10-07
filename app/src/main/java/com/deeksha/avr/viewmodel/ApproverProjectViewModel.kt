package com.deeksha.avr.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deeksha.avr.model.Expense
import com.deeksha.avr.model.Project
import com.deeksha.avr.repository.ExpenseRepository
import com.deeksha.avr.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.deeksha.avr.model.CategoryBudget
import com.deeksha.avr.model.DepartmentBudgetBreakdown
import com.deeksha.avr.model.ProjectBudgetSummary

@HiltViewModel
class ApproverProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {
    
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()
    
    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject.asStateFlow()
    
    private val _projectBudgetSummary = MutableStateFlow(ProjectBudgetSummary())
    val projectBudgetSummary: StateFlow<ProjectBudgetSummary> = _projectBudgetSummary.asStateFlow()
    
    private val _projectExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val projectExpenses: StateFlow<List<Expense>> = _projectExpenses.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun loadProjects(userId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d("ApproverProjectLoading", "🔄 Starting to load projects for approver")
                Log.d("ApproverProjectLoading", "📱 User ID provided: $userId")
                
                if (userId != null && userId.isNotEmpty() && userId != "1234567891") {
                    Log.d("ApproverProjectLoading", "✅ Valid user ID provided: $userId")
                    Log.d("ApproverProjectLoading", "🔄 Loading projects for specific approver using flow")
                    
                    // Load both regular and temporary projects using combine
                    combine(
                        projectRepository.getApproverProjects(userId),
                        projectRepository.getTemporaryApproverProjects(userId)
                    ) { regularList, temporaryList ->
                        Log.d("ApproverProjectLoading", "📊 Received ${regularList.size} regular projects")
                        Log.d("ApproverProjectLoading", "📊 Received ${temporaryList.size} temporary projects")
                        
                        // Combine and deduplicate projects
                        val allProjects = (regularList + temporaryList).distinctBy { it.id }
                        Log.d("ApproverProjectLoading", "🎯 Total projects after combining: ${allProjects.size}")
                        
                        allProjects
                    }.collect { allProjects ->
                        _projects.value = allProjects
                        _isLoading.value = false
                    }
                } else {
                    Log.d("ApproverProjectLoading", "⚠️ Invalid or empty user ID: '$userId'")
                    Log.d("ApproverProjectLoading", "🔄 Loading all active projects as fallback")
                    
                    // Load all active projects (fallback) - this is a one-time call
                    val projectList = projectRepository.getAllProjects()
                    Log.d("ApproverProjectLoading", "📊 Received ${projectList.size} projects from fallback")
                    _projects.value = projectList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("ApproverProjectLoading", "❌ Error loading projects: ${e.message}", e)
                _error.value = "Failed to load projects: ${e.message}"
                _isLoading.value = false
            }
        }
    }
    
    fun selectProject(project: Project) {
        Log.d("ApproverProjectVM", "🎯 Project selected: ${project.name} (ID: ${project.id})")
        _selectedProject.value = project
        loadProjectBudgetSummary(project.id)
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun loadProjectBudgetSummary(projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d("ApproverProjectVM", "🔄 Loading project budget summary for ID: $projectId")
                
                // First, try to find project in cached list
                var project = _projects.value.find { it.id == projectId }
                
                // If not found in cache, load directly from repository
                if (project == null) {
                    Log.d("ApproverProjectVM", "🔍 Project not in cache, loading from repository...")
                    project = projectRepository.getProjectById(projectId)
                    
                    if (project == null) {
                        Log.e("ApproverProjectVM", "❌ Project not found in repository for ID: $projectId")
                        _error.value = "Project not found"
                        _isLoading.value = false
                        return@launch
                    }
                    
                    Log.d("ApproverProjectVM", "✅ Found project: ${project.name}")
                    _selectedProject.value = project
                }
                
                Log.d("ApproverProjectVM", "📊 Loading expenses for project: ${project.name}")
                
                // Load project expenses
                expenseRepository.getProjectExpenses(projectId).collect { expenses ->
                    Log.d("ApproverProjectVM", "✅ Loaded ${expenses.size} expenses")
                    _projectExpenses.value = expenses
                    
                    // Calculate budget summary
                    val totalBudget = project.budget
                    val approvedExpenses = expenses.filter { it.status.name == "APPROVED" }
                    val totalSpent = approvedExpenses.sumOf { it.amount }
                    val totalRemaining = totalBudget - totalSpent
                    val spentPercentage = if (totalBudget > 0) (totalSpent / totalBudget) * 100 else 0.0
                    
                    Log.d("ApproverProjectVM", "💰 Budget: $totalBudget, Spent: $totalSpent, Remaining: $totalRemaining")
                    
                    // Calculate category breakdown
                    val categoryBreakdown = calculateCategoryBreakdown(expenses, totalBudget)
                    Log.d("ApproverProjectVM", "📈 Category breakdown: ${categoryBreakdown.size} categories")
                    
                    // Calculate department breakdown using explicit project department budgets
                    val departmentBreakdown = calculateDepartmentBreakdown(expenses, project)
                    Log.d("ApproverProjectVM", "🏢 Department breakdown: ${departmentBreakdown.size} departments")
                    
                    // Get recent expenses (last 5)
                    val recentExpenses = expenses.sortedByDescending { 
                        it.submittedAt?.toDate()?.time ?: 0L 
                    }.take(5)
                    
                    // Count pending and approved
                    val pendingCount = expenses.count { it.status.name == "PENDING" }
                    val approvedCount = expenses.count { it.status.name == "APPROVED" }
                    
                    val summary = ProjectBudgetSummary(
                        project = project,
                        totalBudget = totalBudget,
                        totalSpent = totalSpent,
                        totalRemaining = totalRemaining,
                        spentPercentage = spentPercentage,
                        categoryBreakdown = categoryBreakdown,
                        departmentBreakdown = departmentBreakdown,
                        recentExpenses = recentExpenses,
                        pendingApprovalsCount = pendingCount,
                        approvedExpensesCount = approvedCount
                    )
                    
                    Log.d("ApproverProjectVM", "✅ Budget summary created successfully")
                    _projectBudgetSummary.value = summary
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("ApproverProjectVM", "❌ Error loading project budget summary: ${e.message}")
                _error.value = e.message ?: "Failed to load project data"
                _isLoading.value = false
            }
        }
    }
    
    private fun calculateCategoryBreakdown(expenses: List<Expense>, totalBudget: Double): List<CategoryBudget> {
        Log.d("ApproverProjectVM", "🔄 Calculating dynamic category breakdown for ${expenses.size} expenses")
        
        // Get approved expenses and group by category
        val approvedExpenses = expenses.filter { it.status.name == "APPROVED" }
        val expensesByCategory = approvedExpenses.groupBy { it.category.trim() }
            .filter { it.key.isNotEmpty() } // Remove empty categories
        
        Log.d("ApproverProjectVM", "📊 Found ${expensesByCategory.size} categories with expenses: ${expensesByCategory.keys}")
        
        if (expensesByCategory.isEmpty()) {
            Log.d("ApproverProjectVM", "✅ No categories with expenses found")
            return emptyList()
        }
        
        // Calculate total spent across all categories
        val totalSpent = approvedExpenses.sumOf { it.amount }
        
        // Create dynamic category budget breakdown
        val result = expensesByCategory.map { (category, categoryExpenses) ->
            val spent = categoryExpenses.sumOf { it.amount }
            
            // Calculate allocated budget proportionally based on actual spending
            // If no expenses yet, use equal distribution; otherwise use proportional allocation
            val allocatedBudget = if (totalSpent > 0) {
                // Proportional allocation based on spending pattern
                val spendingRatio = spent / totalSpent
                totalBudget * spendingRatio
            } else {
                // Equal distribution if no spending yet
                totalBudget / expensesByCategory.size
            }
            
            val remaining = maxOf(0.0, allocatedBudget - spent) // Ensure non-negative
            val usedPercentage = if (allocatedBudget > 0) (spent / allocatedBudget) * 100 else 0.0
            
            Log.d("ApproverProjectVM", "📈 $category: allocated=$allocatedBudget, spent=$spent, remaining=$remaining, ${categoryExpenses.size} expenses")
            
            CategoryBudget(
                category = category,
                budgetAllocated = allocatedBudget,
                spent = spent,
                remaining = remaining,
                percentage = usedPercentage
            )
        }.sortedByDescending { it.spent } // Sort by highest spending first
        
        Log.d("ApproverProjectVM", "✅ Dynamic category breakdown complete: ${result.size} categories")
        return result
    }
    
    private fun calculateDepartmentBreakdown(expenses: List<Expense>, project: Project): List<DepartmentBudgetBreakdown> {
        Log.d("ApproverProjectVM", "🔄 Calculating department breakdown using project department budgets for ${expenses.size} expenses")

        // Map of approved spend by department
        val approvedExpenses = expenses.filter { it.status.name == "APPROVED" }
        val spendByDepartment: Map<String, Double> = approvedExpenses
            .groupBy { it.department.trim() }
            .filter { it.key.isNotEmpty() }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        // Build breakdown strictly from project's configured departmentBudgets
        val breakdownFromProjectBudgets = project.departmentBudgets.entries.map { (department, allocatedBudget) ->
            val spent = spendByDepartment[department] ?: 0.0
            val remaining = maxOf(0.0, allocatedBudget - spent)
            val usedPercentage = if (allocatedBudget > 0) (spent / allocatedBudget) * 100 else 0.0
            Log.d("ApproverProjectVM", "📈 $department: allocated=$allocatedBudget, spent=$spent, remaining=$remaining")
            DepartmentBudgetBreakdown(
                department = department,
                budgetAllocated = allocatedBudget,
                spent = spent,
                remaining = remaining,
                percentage = usedPercentage
            )
        }

        // Optionally include departments that have spend but no configured budget (allocated = 0)
        val extras = spendByDepartment.keys
            .filter { it.isNotEmpty() && !project.departmentBudgets.containsKey(it) }
            .map { department ->
                val spent = spendByDepartment[department] ?: 0.0
                DepartmentBudgetBreakdown(
                    department = department,
                    budgetAllocated = 0.0,
                    spent = spent,
                    remaining = 0.0,
                    percentage = 0.0
                )
            }

        val result = (breakdownFromProjectBudgets + extras)
            .sortedByDescending { it.spent }

        Log.d("ApproverProjectVM", "✅ Department breakdown computed: ${result.size} departments")
        return result
    }
    
    fun refreshProjectData() {
        _selectedProject.value?.let { project ->
            Log.d("ApproverProjectVM", "🔄 Manually refreshing project data for: ${project.name}")
            loadProjectBudgetSummary(project.id)
        }
    }
} 