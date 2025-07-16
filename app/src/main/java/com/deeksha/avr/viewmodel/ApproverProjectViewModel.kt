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
    
    fun loadProjects() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d("ApproverProjectVM", "🔄 Loading projects...")
                projectRepository.getActiveProjects().collect { projectList ->
                    Log.d("ApproverProjectVM", "✅ Loaded ${projectList.size} projects: ${projectList.map { it.name }}")
                    _projects.value = projectList
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("ApproverProjectVM", "❌ Error loading projects: ${e.message}")
                _error.value = e.message
                _isLoading.value = false
            }
        }
    }
    
    fun selectProject(project: Project) {
        Log.d("ApproverProjectVM", "🎯 Project selected: ${project.name} (ID: ${project.id})")
        _selectedProject.value = project
        loadProjectBudgetSummary(project.id)
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
                    
                    // Calculate department breakdown
                    val departmentBreakdown = calculateDepartmentBreakdown(expenses, totalBudget)
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
    
    private fun calculateDepartmentBreakdown(expenses: List<Expense>, totalBudget: Double): List<DepartmentBudgetBreakdown> {
        Log.d("ApproverProjectVM", "🔄 Calculating dynamic department breakdown for ${expenses.size} expenses")
        
        // Get approved expenses and group by department
        val approvedExpenses = expenses.filter { it.status.name == "APPROVED" }
        val expensesByDepartment = approvedExpenses.groupBy { it.department.trim() }
            .filter { it.key.isNotEmpty() } // Remove empty departments
        
        Log.d("ApproverProjectVM", "📊 Found ${expensesByDepartment.size} departments with expenses: ${expensesByDepartment.keys}")
        
        if (expensesByDepartment.isEmpty()) {
            Log.d("ApproverProjectVM", "✅ No departments with expenses found")
            return emptyList()
        }
        
        // Calculate total spent across all departments
        val totalSpent = approvedExpenses.sumOf { it.amount }
        
        // Create dynamic department budget breakdown
        val result = expensesByDepartment.map { (department, departmentExpenses) ->
            val spent = departmentExpenses.sumOf { it.amount }
            
            // Calculate allocated budget proportionally based on actual spending
            // If no expenses yet, use equal distribution; otherwise use proportional allocation
            val allocatedBudget = if (totalSpent > 0) {
                // Proportional allocation based on spending pattern
                val spendingRatio = spent / totalSpent
                totalBudget * spendingRatio
            } else {
                // Equal distribution if no spending yet
                totalBudget / expensesByDepartment.size
            }
            
            val remaining = maxOf(0.0, allocatedBudget - spent) // Ensure non-negative
            val usedPercentage = if (allocatedBudget > 0) (spent / allocatedBudget) * 100 else 0.0
            
            Log.d("ApproverProjectVM", "📈 $department: allocated=$allocatedBudget, spent=$spent, remaining=$remaining, ${departmentExpenses.size} expenses")
            
            DepartmentBudgetBreakdown(
                department = department,
                budgetAllocated = allocatedBudget,
                spent = spent,
                remaining = remaining,
                percentage = usedPercentage
            )
        }.sortedByDescending { it.spent } // Sort by highest spending first
        
        Log.d("ApproverProjectVM", "✅ Dynamic department breakdown complete: ${result.size} departments")
        return result
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun refreshProjectData() {
        _selectedProject.value?.let { project ->
            Log.d("ApproverProjectVM", "🔄 Manually refreshing project data for: ${project.name}")
            loadProjectBudgetSummary(project.id)
        }
    }
} 