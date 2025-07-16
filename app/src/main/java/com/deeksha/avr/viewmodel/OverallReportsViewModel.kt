package com.deeksha.avr.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deeksha.avr.model.*
import com.deeksha.avr.repository.ExpenseRepository
import com.deeksha.avr.repository.ProjectRepository
import com.deeksha.avr.repository.ExportRepository
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class OverallReportData(
    val totalSpent: Double = 0.0,
    val totalBudget: Double = 0.0,
    val budgetUsagePercentage: Double = 0.0,
    val expensesByCategory: Map<String, Double> = emptyMap(),
    val expensesByDepartment: Map<String, Double> = emptyMap(),
    val expensesByProject: Map<String, Double> = emptyMap(),
    val detailedExpenses: List<DetailedExpenseWithProject> = emptyList(),
    val timeRange: String = "This Year",
    val selectedProject: String = "All Projects",
    val availableProjects: List<Project> = emptyList()
)

data class DetailedExpenseWithProject(
    val id: String = "",
    val date: Timestamp? = null,
    val invoice: String = "",
    val by: String = "",
    val amount: Double = 0.0,
    val department: String = "",
    val category: String = "",
    val modeOfPayment: String = "",
    val projectName: String = ""
)

data class ProjectFilter(
    val name: String,
    val value: String
)

@HiltViewModel
class OverallReportsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository,
    private val exportRepository: ExportRepository
) : ViewModel() {
    
    private val _reportData = MutableStateFlow(OverallReportData())
    val reportData: StateFlow<OverallReportData> = _reportData.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _selectedTimeRange = MutableStateFlow("this_year")
    val selectedTimeRange: StateFlow<String> = _selectedTimeRange.asStateFlow()
    
    private val _selectedProject = MutableStateFlow("all")
    val selectedProject: StateFlow<String> = _selectedProject.asStateFlow()
    
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()
    
    // Pagination for detailed expenses
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()
    
    private val _hasMoreExpenses = MutableStateFlow(false)
    val hasMoreExpenses: StateFlow<Boolean> = _hasMoreExpenses.asStateFlow()
    
    private val pageSize = 10
    
    private var allProjects: List<Project> = emptyList()
    private var allExpenses: List<Expense> = emptyList()
    private var filteredExpenses: List<DetailedExpenseWithProject> = emptyList()
    
    init {
        loadOverallReports()
    }
    
    fun loadOverallReports() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                android.util.Log.d("OverallReportsViewModel", "🔄 Loading overall reports...")
                
                // Load all projects first
                allProjects = projectRepository.getAllProjects()
                android.util.Log.d("OverallReportsViewModel", "📊 Loaded ${allProjects.size} projects")
                
                if (allProjects.isEmpty()) {
                    android.util.Log.w("OverallReportsViewModel", "⚠️ No projects found")
                    _error.value = "No projects found in the system"
                    return@launch
                }
                
                // Load all approved expenses across all projects
                allExpenses = loadAllApprovedExpenses()
                android.util.Log.d("OverallReportsViewModel", "💰 Loaded ${allExpenses.size} approved expenses")
                
                // Calculate total budget
                val totalBudget = allProjects.sumOf { it.budget }
                
                // Even if no expenses, show the report with budget info
                if (allExpenses.isEmpty()) {
                    android.util.Log.w("OverallReportsViewModel", "⚠️ No approved expenses found")
                    _reportData.value = OverallReportData(
                        totalSpent = 0.0,
                        totalBudget = totalBudget,
                        budgetUsagePercentage = 0.0,
                        timeRange = "This Year",
                        selectedProject = "All Projects",
                        availableProjects = allProjects,
                        expensesByProject = allProjects.associate { it.name to 0.0 }
                    )
                } else {
                    // Apply filters and update report with expense data
                    applyFiltersAndUpdateReport()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("OverallReportsViewModel", "❌ Failed to load overall reports: ${e.message}")
                e.printStackTrace()
                _error.value = "Failed to load overall reports: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateTimeRange(timeRange: String) {
        android.util.Log.d("OverallReportsViewModel", "📅 Updating time range to: $timeRange")
        _selectedTimeRange.value = timeRange
        
        // Reset pagination when time range changes
        viewModelScope.launch {
            _isLoading.value = true
            _currentPage.value = 0
            try {
                applyFiltersAndUpdateReport()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateProject(projectId: String) {
        android.util.Log.d("OverallReportsViewModel", "🎯 Updating project filter to: $projectId")
        _selectedProject.value = projectId
        
        // Force reload data when project changes
        viewModelScope.launch {
            _isLoading.value = true
            _currentPage.value = 0
            try {
                applyFiltersAndUpdateReport()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun applyFiltersAndUpdateReport() {
        viewModelScope.launch {
            try {
                android.util.Log.d("OverallReportsViewModel", "🔄 Applying filters...")
                android.util.Log.d("OverallReportsViewModel", "📅 Time range: ${_selectedTimeRange.value}")
                android.util.Log.d("OverallReportsViewModel", "🎯 Selected project: ${_selectedProject.value}")
                
                // Filter expenses by time range
                val timeFilteredExpenses = filterByTimeRange(allExpenses, _selectedTimeRange.value)
                android.util.Log.d("OverallReportsViewModel", "📊 After time filter: ${timeFilteredExpenses.size} expenses")
                
                // Filter by project
                val projectFilteredExpenses = if (_selectedProject.value == "all") {
                    timeFilteredExpenses
                } else {
                    timeFilteredExpenses.filter { it.projectId == _selectedProject.value }
                }
                android.util.Log.d("OverallReportsViewModel", "🎯 After project filter: ${projectFilteredExpenses.size} expenses")
                
                // Calculate total spent
                val totalSpent = projectFilteredExpenses.sumOf { it.amount }
                android.util.Log.d("OverallReportsViewModel", "💰 Total spent: ₹$totalSpent")
                
                // Calculate total budget based on selected projects
                val totalBudget = if (_selectedProject.value == "all") {
                    allProjects.sumOf { it.budget }
                } else {
                    allProjects.find { it.id == _selectedProject.value }?.budget ?: 0.0
                }
                android.util.Log.d("OverallReportsViewModel", "💼 Total budget: ₹$totalBudget")
                
                // Calculate budget usage percentage
                val budgetUsagePercentage = if (totalBudget > 0) {
                    (totalSpent / totalBudget) * 100
                } else {
                    0.0
                }
                android.util.Log.d("OverallReportsViewModel", "📈 Budget usage: $budgetUsagePercentage%")
                
                // Group by category
                val expensesByCategory = projectFilteredExpenses
                    .filter { it.category.isNotBlank() }
                    .groupBy { it.category }
                    .mapValues { it.value.sumOf { expense -> expense.amount } }
                android.util.Log.d("OverallReportsViewModel", "🏷️ Categories: ${expensesByCategory.keys}")
                android.util.Log.d("OverallReportsViewModel", "📊 Category totals: $expensesByCategory")
                
                // Group by department
                val expensesByDepartment = projectFilteredExpenses
                    .filter { it.department.isNotBlank() }
                    .groupBy { it.department }
                    .mapValues { it.value.sumOf { expense -> expense.amount } }
                android.util.Log.d("OverallReportsViewModel", "🏢 Departments: ${expensesByDepartment.keys}")
                
                // Group by project (only for All Projects view)
                val expensesByProject = if (_selectedProject.value == "all") {
                    val projectExpenses = projectFilteredExpenses.groupBy { it.projectId }
                        .mapValues { it.value.sumOf { expense -> expense.amount } }
                        .mapKeys { entry ->
                            allProjects.find { it.id == entry.key }?.name ?: "Unknown Project"
                        }
                    
                    // Ensure we show all projects even if no expenses
                    val allProjectsWithExpenses = allProjects.associate { project ->
                        project.name to (projectExpenses[project.name] ?: 0.0)
                    }
                    
                    allProjectsWithExpenses
                } else {
                    emptyMap()
                }
                android.util.Log.d("OverallReportsViewModel", "🎯 Projects: ${expensesByProject.keys}")
                
                // Convert to detailed expenses with project names
                filteredExpenses = projectFilteredExpenses.map { expense ->
                    val projectName = allProjects.find { it.id == expense.projectId }?.name ?: "Unknown Project"
                    DetailedExpenseWithProject(
                        id = expense.id,
                        date = expense.date,
                        invoice = expense.receiptNumber.ifEmpty { "N/A" },
                        by = expense.userName,
                        amount = expense.amount,
                        department = expense.department,
                        category = expense.category,
                        modeOfPayment = expense.modeOfPayment,
                        projectName = projectName
                    )
                }.sortedByDescending { it.date?.toDate()?.time ?: 0L }
                
                // Reset pagination when data changes
                _currentPage.value = 0
                updatePaginatedExpenses()
                
                android.util.Log.d("OverallReportsViewModel", "📋 Total filtered expenses: ${filteredExpenses.size}")
                
                // Create project filters
                val projectFilters = mutableListOf(
                    ProjectFilter("All Projects", "all")
                ).apply {
                    addAll(allProjects.map { ProjectFilter(it.name, it.id) })
                }
                
                val timeRangeDisplay = FilterOptions.timeRanges.find { it.value == _selectedTimeRange.value }?.displayName ?: "This Year"
                val selectedProjectDisplay = projectFilters.find { it.value == _selectedProject.value }?.name ?: "All Projects"
                
                // Get current page of expenses
                val currentPageExpenses = getCurrentPageExpenses()
                
                // Update state
                _reportData.value = OverallReportData(
                    totalSpent = totalSpent,
                    totalBudget = totalBudget,
                    budgetUsagePercentage = budgetUsagePercentage,
                    expensesByCategory = expensesByCategory,
                    expensesByDepartment = expensesByDepartment,
                    expensesByProject = expensesByProject,
                    detailedExpenses = currentPageExpenses,
                    timeRange = timeRangeDisplay,
                    selectedProject = selectedProjectDisplay,
                    availableProjects = allProjects
                )
                
                android.util.Log.d("OverallReportsViewModel", "✅ Report data updated successfully")
                android.util.Log.d("OverallReportsViewModel", "📊 Final summary: ₹$totalSpent / ₹$totalBudget ($budgetUsagePercentage%)")
                android.util.Log.d("OverallReportsViewModel", "🏷️ Categories found: ${expensesByCategory.size}")
                android.util.Log.d("OverallReportsViewModel", "📋 Expenses in overview: ${filteredExpenses.size}")
                
            } catch (e: Exception) {
                android.util.Log.e("OverallReportsViewModel", "❌ Error applying filters: ${e.message}")
                _error.value = "Failed to process report data: ${e.message}"
            }
        }
    }
    
    private suspend fun loadAllApprovedExpenses(): List<Expense> {
        return try {
            val allExpenses = mutableListOf<Expense>()
            var errorCount = 0
            
            android.util.Log.d("OverallReportsViewModel", "🔄 Loading approved expenses from ${allProjects.size} projects...")
            
            allProjects.forEach { project ->
                try {
                    android.util.Log.d("OverallReportsViewModel", "📊 Loading expenses for project: ${project.name} (${project.id})")
                    val projectExpenses = expenseRepository.getApprovedExpensesForProject(project.id)
                    android.util.Log.d("OverallReportsViewModel", "💰 Found ${projectExpenses.size} approved expenses for ${project.name}")
                    
                    if (projectExpenses.isNotEmpty()) {
                        allExpenses.addAll(projectExpenses)
                        // Log sample expense for verification
                        projectExpenses.firstOrNull()?.let { expense ->
                            android.util.Log.d("OverallReportsViewModel", "📝 Sample expense from ${project.name}: ${expense.category} - ₹${expense.amount}")
                        }
                    }
                } catch (e: Exception) {
                    errorCount++
                    android.util.Log.e("OverallReportsViewModel", "❌ Error loading expenses for project ${project.name}: ${e.message}")
                    e.printStackTrace()
                    // Continue with other projects
                }
            }
            
            if (errorCount > 0) {
                android.util.Log.w("OverallReportsViewModel", "⚠️ Failed to load expenses for $errorCount projects")
            }
            
            android.util.Log.d("OverallReportsViewModel", "✅ Successfully loaded ${allExpenses.size} total approved expenses")
            
            // Sort by date descending
            allExpenses.sortedByDescending { it.submittedAt?.toDate()?.time ?: 0L }
        } catch (e: Exception) {
            android.util.Log.e("OverallReportsViewModel", "❌ Critical error loading approved expenses: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun filterByTimeRange(expenses: List<Expense>, timeRange: String): List<Expense> {
        val now = Calendar.getInstance()
        val startDate = Calendar.getInstance()
        
        when (timeRange) {
            "this_month" -> {
                startDate.set(Calendar.DAY_OF_MONTH, 1)
                startDate.set(Calendar.HOUR_OF_DAY, 0)
                startDate.set(Calendar.MINUTE, 0)
                startDate.set(Calendar.SECOND, 0)
                startDate.set(Calendar.MILLISECOND, 0)
            }
            "this_year" -> {
                startDate.set(Calendar.DAY_OF_YEAR, 1)
                startDate.set(Calendar.HOUR_OF_DAY, 0)
                startDate.set(Calendar.MINUTE, 0)
                startDate.set(Calendar.SECOND, 0)
                startDate.set(Calendar.MILLISECOND, 0)
            }
            "last_6_months" -> {
                startDate.add(Calendar.MONTH, -6)
            }
            "last_12_months" -> {
                startDate.add(Calendar.MONTH, -12)
            }
            "all_time" -> {
                return expenses
            }
        }
        
        return expenses.filter { expense ->
            expense.date?.toDate()?.after(startDate.time) == true
        }
    }
    
    fun exportToPDF(onSuccess: (android.content.Intent?) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val exportData = ExportData(
                    totalSpent = _reportData.value.totalSpent,
                    timeRange = _reportData.value.timeRange,
                    department = _reportData.value.selectedProject,
                    categoryBreakdown = _reportData.value.expensesByCategory,
                    detailedExpenses = _reportData.value.detailedExpenses.map { detailedExpense ->
                        DetailedExpense(
                            id = detailedExpense.id,
                            date = detailedExpense.date,
                            invoice = detailedExpense.invoice,
                            by = detailedExpense.by,
                            amount = detailedExpense.amount,
                            department = detailedExpense.department,
                            modeOfPayment = detailedExpense.modeOfPayment
                        )
                    },
                    generatedAt = Timestamp.now()
                )
                
                val result = exportRepository.exportToPDF(exportData)
                result.fold(
                    onSuccess = { file ->
                        val shareIntent = exportRepository.shareFile(file, "application/pdf")
                        onSuccess(shareIntent)
                    },
                    onFailure = { exception ->
                        onError("Failed to export PDF: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                onError("Failed to export PDF: ${e.message}")
            } finally {
                _isExporting.value = false
            }
        }
    }
    
    fun exportToCSV(onSuccess: (android.content.Intent?) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isExporting.value = true
            try {
                val exportData = ExportData(
                    totalSpent = _reportData.value.totalSpent,
                    timeRange = _reportData.value.timeRange,
                    department = _reportData.value.selectedProject,
                    categoryBreakdown = _reportData.value.expensesByCategory,
                    detailedExpenses = _reportData.value.detailedExpenses.map { detailedExpense ->
                        DetailedExpense(
                            id = detailedExpense.id,
                            date = detailedExpense.date,
                            invoice = detailedExpense.invoice,
                            by = detailedExpense.by,
                            amount = detailedExpense.amount,
                            department = detailedExpense.department,
                            modeOfPayment = detailedExpense.modeOfPayment
                        )
                    },
                    generatedAt = Timestamp.now()
                )
                
                val result = exportRepository.exportToCSV(exportData)
                result.fold(
                    onSuccess = { file ->
                        val shareIntent = exportRepository.shareFile(file, "text/csv")
                        onSuccess(shareIntent)
                    },
                    onFailure = { exception ->
                        onError("Failed to export CSV: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                onError("Failed to export CSV: ${e.message}")
            } finally {
                _isExporting.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun getCategoryData(): List<CategoryData> {
        val categoryMap = _reportData.value.expensesByCategory
        return categoryMap.map { (category, amount) ->
            CategoryData(
                category = category,
                amount = amount,
                color = FilterOptions.categoryColors[category] ?: 0xFF9E9E9E
            )
        }.sortedByDescending { it.amount }
    }
    
    fun getCategoryDataWithBudget(): List<CategoryBudgetData> {
        val categoryMap = _reportData.value.expensesByCategory
        val totalBudget = _reportData.value.totalBudget
        val totalSpent = _reportData.value.totalSpent
        
        return categoryMap.map { (category, amount) ->
            // Calculate proportional budget allocation for this category
            val budgetAllocation = if (totalSpent > 0 && totalBudget > 0) {
                (amount / totalSpent) * totalBudget
            } else {
                totalBudget / maxOf(1, categoryMap.size) // Equal distribution if no spending
            }
            
            CategoryBudgetData(
                category = category,
                spent = amount,
                budgetAllocated = budgetAllocation,
                color = FilterOptions.categoryColors[category] ?: 0xFF9E9E9E
            )
        }.sortedByDescending { it.spent }
    }
    
    fun getProjectFilters(): List<ProjectFilter> {
        return listOf(ProjectFilter("All Projects", "all")) + 
               allProjects.map { ProjectFilter(it.name, it.id) }
    }
    
    private fun createSampleReportData() {
        android.util.Log.d("OverallReportsViewModel", "📝 Creating sample report data for demonstration")
        
        val totalBudget = if (allProjects.isNotEmpty()) {
            allProjects.sumOf { it.budget }
        } else {
            1000000.0 // Sample budget of 10 lakhs
        }
        
        // Filter sample data based on selected project
        val isAllProjects = _selectedProject.value == "all"
        val selectedProjectName = if (isAllProjects) "All Projects" else {
            allProjects.find { it.id == _selectedProject.value }?.name ?: "Unknown Project"
        }
        
        // Create sample expense data
        val sampleTotalSpent = if (isAllProjects) 250000.0 else 80000.0 // Adjust for specific project
        val budgetUsagePercentage = (sampleTotalSpent / totalBudget) * 100
        
        // Sample category breakdown (only for specific projects)
        val sampleCategoryExpenses = if (!isAllProjects) {
            mapOf(
                "Wages & Crew Payments" to 30000.0,
                "Equipment Rental" to 20000.0,
                "Catering & Food" to 10000.0,
                "Transportation" to 12000.0,
                "Costumes & Makeup" to 8000.0
            )
        } else {
            emptyMap()
        }
        
        // Sample department breakdown
        val sampleDepartmentExpenses = mapOf(
            "Production" to if (isAllProjects) 100000.0 else 35000.0,
            "Direction" to if (isAllProjects) 50000.0 else 20000.0,
            "Cinematography" to if (isAllProjects) 60000.0 else 25000.0,
            "Art Department" to if (isAllProjects) 25000.0 else 0.0,
            "Sound" to if (isAllProjects) 15000.0 else 0.0
        ).filter { it.value > 0 }
        
        // Sample project breakdown (only for All Projects)
        val sampleProjectExpenses = if (isAllProjects) {
            if (allProjects.isNotEmpty()) {
                if (allProjects.size > 1) {
                    allProjects.take(3).mapIndexed { index, project ->
                        project.name to when(index) {
                            0 -> 120000.0
                            1 -> 80000.0
                            else -> 50000.0
                        }
                    }.toMap()
                } else {
                    mapOf(allProjects.first().name to sampleTotalSpent)
                }
            } else {
                mapOf(
                    "Movie Production A" to 120000.0,
                    "Documentary Project B" to 80000.0,
                    "Commercial Ads" to 50000.0
                )
            }
        } else {
            emptyMap()
        }
        
        // Generate more sample detailed expenses
        val sampleDetailedExpenses = generateSampleExpenses(selectedProjectName, isAllProjects)
        
        // Store as filtered expenses for pagination
        filteredExpenses = sampleDetailedExpenses
        _currentPage.value = 0
        updatePaginatedExpenses()
        
        _reportData.value = OverallReportData(
            totalSpent = sampleTotalSpent,
            totalBudget = totalBudget,
            budgetUsagePercentage = budgetUsagePercentage,
            expensesByCategory = sampleCategoryExpenses,
            expensesByDepartment = sampleDepartmentExpenses,
            expensesByProject = sampleProjectExpenses,
            detailedExpenses = getCurrentPageExpenses(),
            timeRange = "This Year",
            selectedProject = selectedProjectName,
            availableProjects = allProjects
        )
        
        android.util.Log.d("OverallReportsViewModel", "✅ Sample data created: ₹$sampleTotalSpent / ₹$totalBudget ($budgetUsagePercentage%)")
        android.util.Log.d("OverallReportsViewModel", "📊 Sample categories: ${sampleCategoryExpenses.size}")
        android.util.Log.d("OverallReportsViewModel", "📋 Sample expenses: ${sampleDetailedExpenses.size}")
    }
    
    private fun generateSampleExpenses(projectName: String, isAllProjects: Boolean): List<DetailedExpenseWithProject> {
        val expenses = mutableListOf<DetailedExpenseWithProject>()
        val sampleProjects = if (isAllProjects) {
            listOf("Movie Production A", "Documentary Project B", "Commercial Ads")
        } else {
            listOf(projectName)
        }
        
        val departments = listOf("Production", "Direction", "Cinematography", "Art Department", "Sound", "Editing")
        val users = listOf("John Doe", "Jane Smith", "Mike Johnson", "Sarah Wilson", "Tom Brown", "Lisa Davis")
        val categories = listOf("Wages & Crew Payments", "Equipment Rental", "Catering & Food", "Transportation", "Costumes & Makeup")
        
        // Generate 25 sample expenses for better pagination testing
        for (i in 1..25) {
            expenses.add(
                DetailedExpenseWithProject(
                    id = "sample$i",
                    date = Timestamp.now(),
                    invoice = "INV${String.format("%03d", i)}",
                    by = users.random(),
                    amount = (5000..50000).random().toDouble(),
                    department = departments.random(),
                    category = categories.random(),
                    modeOfPayment = listOf("UPI", "Cash", "Check").random(),
                    projectName = sampleProjects.random()
                )
            )
        }
        
        return expenses.sortedByDescending { it.date?.toDate()?.time ?: 0L }
    }
    
    // Method to force reload without sample data (for testing with real data)
    fun loadRealDataOnly() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                android.util.Log.d("OverallReportsViewModel", "🔄 Loading REAL data only (no samples)...")
                
                // Load all projects first
                allProjects = projectRepository.getAllProjects()
                android.util.Log.d("OverallReportsViewModel", "📊 Loaded ${allProjects.size} projects")
                
                // Load all approved expenses across all projects
                allExpenses = loadAllApprovedExpenses()
                android.util.Log.d("OverallReportsViewModel", "💰 Loaded ${allExpenses.size} approved expenses")
                
                if (allExpenses.isEmpty()) {
                    // Show empty state with real budget info
                    val totalBudget = allProjects.sumOf { it.budget }
                    _reportData.value = OverallReportData(
                        totalSpent = 0.0,
                        totalBudget = totalBudget,
                        budgetUsagePercentage = 0.0,
                        timeRange = "This Year",
                        selectedProject = "All Projects",
                        availableProjects = allProjects
                    )
                } else {
                    // Apply current filters and update report data
                    applyFiltersAndUpdateReport()
                }
                
            } catch (e: Exception) {
                android.util.Log.e("OverallReportsViewModel", "❌ Failed to load real data: ${e.message}")
                _error.value = "Failed to load real data: ${e.message}"
            } finally {
                _isLoading.value = false
                         }
         }
     }
     
     // NEW: Real-time observation method for overall reports
     fun observeAllApprovedExpensesRealtime() {
         viewModelScope.launch {
             _isLoading.value = true
             _error.value = null
             
             try {
                 android.util.Log.d("OverallReportsViewModel", "🔄 Setting up real-time observation for all approved expenses...")
                 
                 // Load all projects first
                 allProjects = projectRepository.getAllProjects()
                 android.util.Log.d("OverallReportsViewModel", "📊 Loaded ${allProjects.size} projects")
                 
                 if (allProjects.isEmpty()) {
                     android.util.Log.w("OverallReportsViewModel", "⚠️ No projects found")
                     _error.value = "No projects found in the system"
                     return@launch
                 }
                 
                 // Set up real-time listeners for all projects
                 setupRealTimeListeners()
                 
             } catch (e: Exception) {
                 android.util.Log.e("OverallReportsViewModel", "❌ Failed to setup real-time observation: ${e.message}")
                 _error.value = "Failed to setup real-time observation: ${e.message}"
             } finally {
                 _isLoading.value = false
             }
         }
     }
     
     private fun setupRealTimeListeners() {
         viewModelScope.launch {
             try {
                 android.util.Log.d("OverallReportsViewModel", "🔥 Setting up real-time listeners for ${allProjects.size} projects...")
                 
                 // Create a map to store all expenses by project
                 val allExpensesMap = mutableMapOf<String, List<Expense>>()
                 
                 // Set up listeners for each project using launch for concurrent collection
                 allProjects.forEach { project ->
                     launch {
                         expenseRepository.getProjectExpenses(project.id)
                             .collect { projectExpenses ->
                                 // Filter for approved expenses only
                                 val approvedExpenses = projectExpenses.filter { it.status.name == "APPROVED" }
                                 
                                 android.util.Log.d("OverallReportsViewModel", "📊 Project ${project.name}: ${approvedExpenses.size} approved expenses")
                                 
                                 // Update the map
                                 allExpensesMap[project.id] = approvedExpenses
                                 
                                 // Combine all approved expenses
                                 allExpenses = allExpensesMap.values.flatten()
                                     .sortedByDescending { it.submittedAt?.toDate()?.time ?: 0L }
                                 
                                 android.util.Log.d("OverallReportsViewModel", "💰 Total approved expenses across all projects: ${allExpenses.size}")
                                 
                                 // Apply current filters and update report
                                 applyFiltersAndUpdateReport()
                             }
                     }
                 }
                 
             } catch (e: Exception) {
                 android.util.Log.e("OverallReportsViewModel", "❌ Error in real-time listeners: ${e.message}")
                 _error.value = "Failed to setup real-time listeners: ${e.message}"
             }
         }
     }
     
     // Pagination methods
     private fun updatePaginatedExpenses() {
         val totalExpenses = filteredExpenses.size
         val totalPages = (totalExpenses + pageSize - 1) / pageSize // Ceiling division
         _hasMoreExpenses.value = _currentPage.value < totalPages - 1
     }
     
     private fun getCurrentPageExpenses(): List<DetailedExpenseWithProject> {
         val startIndex = _currentPage.value * pageSize
         val endIndex = kotlin.math.min(startIndex + pageSize, filteredExpenses.size)
         return if (startIndex < filteredExpenses.size) {
             filteredExpenses.subList(startIndex, endIndex)
         } else {
             emptyList()
         }
     }
     
     fun loadNextPage() {
         if (_hasMoreExpenses.value) {
             _currentPage.value += 1
             updatePaginatedExpenses()
             
             // Update report data with new page
             val currentPageExpenses = getCurrentPageExpenses()
             val currentData = _reportData.value
             _reportData.value = currentData.copy(
                 detailedExpenses = currentData.detailedExpenses + currentPageExpenses
             )
             
             android.util.Log.d("OverallReportsViewModel", "📄 Loaded page ${_currentPage.value + 1}, showing ${currentData.detailedExpenses.size + currentPageExpenses.size} total expenses")
         }
     }
     
     fun resetPagination() {
         _currentPage.value = 0
         updatePaginatedExpenses()
         
         // Update report data with first page only
         val currentPageExpenses = getCurrentPageExpenses()
         val currentData = _reportData.value
         _reportData.value = currentData.copy(detailedExpenses = currentPageExpenses)
     }
     
     fun getTotalExpenseCount(): Int = filteredExpenses.size
     fun getCurrentPageNumber(): Int = _currentPage.value + 1
     fun getTotalPages(): Int = (filteredExpenses.size + pageSize - 1) / pageSize
     
     // Method to refresh data manually
     fun refreshData() {
         viewModelScope.launch {
             _isLoading.value = true
             try {
                 android.util.Log.d("OverallReportsViewModel", "🔄 Manual refresh triggered")
                 
                 // Reload projects
                 allProjects = projectRepository.getAllProjects()
                 
                 // Reload expenses
                 allExpenses = loadAllApprovedExpenses()
                 
                 // Apply filters and update
                 applyFiltersAndUpdateReport()
                 
                 android.util.Log.d("OverallReportsViewModel", "✅ Manual refresh completed")
             } catch (e: Exception) {
                 android.util.Log.e("OverallReportsViewModel", "❌ Manual refresh failed: ${e.message}")
                 _error.value = "Failed to refresh data: ${e.message}"
             } finally {
                 _isLoading.value = false
             }
         }
     }
     
     // Method to load data without real-time observation (for better performance)
     fun loadDataOnce() {
         viewModelScope.launch {
             _isLoading.value = true
             _error.value = null
             
             try {
                 android.util.Log.d("OverallReportsViewModel", "🔄 Loading data once (no real-time observation)...")
                 
                 // Load all projects first
                 allProjects = projectRepository.getAllProjects()
                 android.util.Log.d("OverallReportsViewModel", "📊 Loaded ${allProjects.size} projects")
                 
                 if (allProjects.isEmpty()) {
                     android.util.Log.w("OverallReportsViewModel", "⚠️ No projects found")
                     _error.value = "No projects found in the system"
                     return@launch
                 }
                 
                 // Check if there are any approved expenses in the system
                 val hasApprovedExpenses = expenseRepository.hasAnyApprovedExpenses()
                 android.util.Log.d("OverallReportsViewModel", "🔍 System has approved expenses: $hasApprovedExpenses")
                 
                 if (!hasApprovedExpenses) {
                     android.util.Log.w("OverallReportsViewModel", "⚠️ No approved expenses found in any project")
                     // Show empty state with budget info
                     val totalBudget = allProjects.sumOf { it.budget }
                     _reportData.value = OverallReportData(
                         totalSpent = 0.0,
                         totalBudget = totalBudget,
                         budgetUsagePercentage = 0.0,
                         timeRange = "This Year",
                         selectedProject = "All Projects",
                         availableProjects = allProjects,
                         expensesByProject = allProjects.associate { it.name to 0.0 }
                     )
                     return@launch
                 }
                 
                 // Load all approved expenses across all projects
                 allExpenses = loadAllApprovedExpenses()
                 android.util.Log.d("OverallReportsViewModel", "💰 Loaded ${allExpenses.size} approved expenses")
                 
                 // Apply current filters and update report data
                 applyFiltersAndUpdateReport()
                 
             } catch (e: Exception) {
                 android.util.Log.e("OverallReportsViewModel", "❌ Failed to load data: ${e.message}")
                 _error.value = "Failed to load data: ${e.message}"
             } finally {
                 _isLoading.value = false
             }
         }
     }
}  