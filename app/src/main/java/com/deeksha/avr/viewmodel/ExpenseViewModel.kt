package com.deeksha.avr.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deeksha.avr.model.Expense
import com.deeksha.avr.model.ExpenseStatus
import com.deeksha.avr.model.Project
import com.deeksha.avr.repository.ExpenseRepository
import com.deeksha.avr.repository.ProjectRepository
import com.deeksha.avr.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import com.deeksha.avr.model.ExpenseFormData
import com.deeksha.avr.model.StatusCounts
import com.deeksha.avr.model.ExpenseSummary

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository,
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _expenseSummary = MutableStateFlow(ExpenseSummary())
    val expenseSummary: StateFlow<ExpenseSummary> = _expenseSummary.asStateFlow()

    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()

    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject.asStateFlow()

    // Additional properties for TrackSubmissionsScreen compatibility
    private val _statusCounts = MutableStateFlow(StatusCounts())
    val statusCounts: StateFlow<StatusCounts> = _statusCounts.asStateFlow()

    private val _filteredExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val filteredExpenses: StateFlow<List<Expense>> = _filteredExpenses.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow<ExpenseStatus?>(null)
    val selectedStatusFilter: StateFlow<ExpenseStatus?> = _selectedStatusFilter.asStateFlow()

    // Form-related properties that UI screens expect
    private val _formData = MutableStateFlow(ExpenseFormData())
    val formData: StateFlow<ExpenseFormData> = _formData.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Categories and departments - these could be loaded from Firebase in real app
    val categories = listOf(
        "Wages & Crew Payments",
        "Equipment Rental",
        "Catering & Food",
        "Transportation",
        "Costumes & Makeup",
        "Post Production",
        "Marketing & Promotion",
        "Other"
    )

    val departments = listOf(
        "Production",
        "Direction",
        "Cinematography", 
        "Art Department",
        "Costumes",
        "Makeup",
        "Sound",
        "Editing",
        "VFX",
        "Administration"
    )

    val paymentModes = listOf(
        "cash" to "By Cash",
        "upi" to "By UPI", 
        "check" to "By Check"
    )

    fun loadUserExpenses(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d("ExpenseViewModel", "🔄 Loading expenses for user: $userId")
                
                // Load projects first  
                try {
                    val projects = projectRepository.getAllProjects()
                    _projects.value = projects
                } catch (exception: Exception) {
                    Log.e("ExpenseViewModel", "❌ Error loading projects: ${exception.message}")
                }
                
                // Load user expenses
                expenseRepository.getUserExpenses(userId)
                    .onEach { expenseList ->
                        _expenses.value = expenseList
                        calculateUserSummary(expenseList)
                        Log.d("ExpenseViewModel", "✅ Loaded ${expenseList.size} expenses for user")
                    }
                    .catch { exception ->
                        _error.value = "Failed to load expenses: ${exception.message}"
                        Log.e("ExpenseViewModel", "❌ Error loading user expenses: ${exception.message}")
                    }
                    .collect()
                
            } catch (e: Exception) {
                _error.value = "Failed to load user data: ${e.message}"
                Log.e("ExpenseViewModel", "❌ Error in loadUserExpenses: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadProjectExpenses(projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Load project details
                val project = projectRepository.getProjectById(projectId)
                _selectedProject.value = project
                
                // Load project expenses
                expenseRepository.getProjectExpenses(projectId)
                    .onEach { expenseList ->
                        _expenses.value = expenseList
                        _filteredExpenses.value = expenseList
                        calculateProjectSummary(expenseList)
                        Log.d("ExpenseViewModel", "✅ Loaded ${expenseList.size} expenses for project")
                    }
                    .catch { exception ->
                        _error.value = "Failed to load project expenses: ${exception.message}"
                        Log.e("ExpenseViewModel", "❌ Error loading project expenses: ${exception.message}")
                    }
                    .collect()
                
            } catch (e: Exception) {
                _error.value = "Failed to load project: ${e.message}"
                Log.e("ExpenseViewModel", "❌ Error in loadProjectExpenses: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateFormField(field: String, value: String) {
        val currentData = _formData.value
        _formData.value = when (field) {
            "date" -> currentData.copy(date = value)
            "amount" -> currentData.copy(amount = value)
            "department" -> currentData.copy(department = value)
            "category" -> currentData.copy(category = value)
            "description" -> currentData.copy(description = value)
            "modeOfPayment" -> currentData.copy(modeOfPayment = value)
            "attachmentUri" -> currentData.copy(attachmentUri = value)
            else -> currentData
        }
    }

    fun submitExpense(
        projectId: String,
        userId: String,
        userName: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            _error.value = null
            
            try {
                val formData = _formData.value
                
                // Validation
                if (formData.date.isEmpty()) {
                    _error.value = "Please select a date"
                    _isSubmitting.value = false
                    return@launch
                }
                
                if (formData.amount.isEmpty() || formData.amount.toDoubleOrNull() == null) {
                    _error.value = "Please enter a valid amount"
                    _isSubmitting.value = false
                    return@launch
                }
                
                if (formData.department.isEmpty()) {
                    _error.value = "Please select a department"
                    _isSubmitting.value = false
                    return@launch
                }
                
                if (formData.category.isEmpty()) {
                    _error.value = "Please select a category"
                    _isSubmitting.value = false
                    return@launch
                }
                
                // Parse date
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val parsedDate = try {
                    dateFormat.parse(formData.date)
                } catch (e: Exception) {
                    _error.value = "Invalid date format. Please use DD/MM/YYYY"
                    _isSubmitting.value = false
                    return@launch
                }
                
                val amount = formData.amount.toDouble()
                
                // Create expense object
                val expense = Expense(
                    projectId = projectId,
                    userId = userId,
                    userName = userName,
                    date = Timestamp(parsedDate!!),
                    amount = amount,
                    department = formData.department,
                    category = formData.category,
                    description = formData.description,
                    modeOfPayment = formData.modeOfPayment,
                    netAmount = amount, // For now, net amount = amount
                    attachmentUrl = formData.attachmentUri,
                    status = ExpenseStatus.PENDING
                )
                
                val result = expenseRepository.addExpense(expense)
                
                if (result.isSuccess) {
                    Log.d("ExpenseViewModel", "✅ Expense submitted successfully - will trigger real-time updates")
                    
                    // Send notifications using the expense data we created
                    sendExpenseSubmissionNotifications(expense, projectId)
                    
                    // Clear form and error
                    _formData.value = ExpenseFormData()
                    _error.value = null
                    _successMessage.value = "Expense submitted successfully! ✅"
                    _isSubmitting.value = false
                    
                    // Reduced delay for faster navigation while still showing success message
                    delay(1000) // Reduced from 3 seconds to 1 second
                    _successMessage.value = null
                    onSuccess()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to submit expense"
                    _isSubmitting.value = false
                }
                
            } catch (e: Exception) {
                _error.value = e.message ?: "An error occurred while submitting expense"
                _isSubmitting.value = false
            }
        }
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun resetForm() {
        _formData.value = ExpenseFormData()
        _error.value = null
        _successMessage.value = null
    }

    private fun calculateUserSummary(expenses: List<Expense>) {
        val approved = expenses.filter { it.status == ExpenseStatus.APPROVED }
        val pending = expenses.filter { it.status == ExpenseStatus.PENDING }
        val rejected = expenses.filter { it.status == ExpenseStatus.REJECTED }
        
        val totalApproved = approved.sumOf { it.amount }
        val totalPending = pending.sumOf { it.amount }
        val totalRejected = rejected.sumOf { it.amount }
        
        // Group approved expenses by category
        val expensesByCategory = approved
            .filter { it.category.isNotEmpty() }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        
        // Group approved expenses by project
        val projectMap = _projects.value.associateBy { it.id }
        val expensesByProject = approved
            .filter { it.projectId.isNotEmpty() }
            .groupBy { it.projectId }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .mapKeys { entry -> projectMap[entry.key]?.name ?: "Unknown Project" }
        
        // Get recent approved expenses
        val recentApproved = approved
            .sortedByDescending { it.reviewedAt?.toDate()?.time ?: 0L }
            .take(5)
        
        _expenseSummary.value = ExpenseSummary(
            totalApproved = totalApproved,
            totalPending = totalPending,
            totalRejected = totalRejected,
            approvedCount = approved.size,
            pendingCount = pending.size,
            rejectedCount = rejected.size,
            expensesByCategory = expensesByCategory,
            expensesByProject = expensesByProject,
            recentExpenses = recentApproved
        )
        
        Log.d("ExpenseViewModel", "📊 Summary - Approved: $totalApproved, Pending: $totalPending, Rejected: $totalRejected")
    }

    private fun calculateProjectSummary(expenses: List<Expense>) {
        val approved = expenses.filter { it.status == ExpenseStatus.APPROVED }
        val pending = expenses.filter { it.status == ExpenseStatus.PENDING }
        val rejected = expenses.filter { it.status == ExpenseStatus.REJECTED }
        
        // Only count approved expenses in the total - as per user requirement
        val totalExpenses = approved.sumOf { it.amount }
        
        // Group only approved expenses by category - as per user requirement
        val expensesByCategory = approved
            .filter { it.category.isNotEmpty() }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        
        _expenseSummary.value = ExpenseSummary(
            totalExpenses = totalExpenses,
            approvedCount = approved.size,
            pendingCount = pending.size,
            rejectedCount = rejected.size,
            expensesByCategory = expensesByCategory
        )
        
        _statusCounts.value = StatusCounts(
            approved = approved.size,
            pending = pending.size,
            rejected = rejected.size,
            total = expenses.size
        )
        
        Log.d("ExpenseViewModel", "📊 Project Summary - Total Approved: $totalExpenses, Approved: ${approved.size}, Pending: ${pending.size}, Rejected: ${rejected.size}")
    }

    fun clearError() {
        _error.value = null
    }

    fun refreshData(userId: String) {
        loadUserExpenses(userId)
    }
    
    fun refreshProjectData(projectId: String) {
        Log.d("ExpenseViewModel", "🔄 Manually refreshing project expenses for: $projectId")
        loadProjectExpenses(projectId)
    }
    
    fun loadUserExpensesForProject(projectId: String, userId: String) {
        viewModelScope.launch {
            Log.d("ExpenseViewModel", "🚀 STARTING loadUserExpensesForProject: project=$projectId, user=$userId")
            _isLoading.value = true
            _error.value = null
            
            // Clear previous data to avoid stale state
            _expenses.value = emptyList()
            _filteredExpenses.value = emptyList()
            _statusCounts.value = StatusCounts()
            _selectedStatusFilter.value = null
            
            try {
                // Load project details first
                Log.d("ExpenseViewModel", "🏗️ Loading project details for: $projectId")
                val project = projectRepository.getProjectById(projectId)
                _selectedProject.value = project
                Log.d("ExpenseViewModel", "🏗️ Project loaded: ${project?.name} (Budget: ₹${project?.budget})")
                
                if (project == null) {
                    Log.e("ExpenseViewModel", "❌ Project not found: $projectId")
                    _error.value = "Project not found"
                    _isLoading.value = false
                    return@launch
                }
                
                // Set up real-time listener for user's expenses in this project
                Log.d("ExpenseViewModel", "🔄 Setting up real-time listener for user expenses...")
                
                // Use a timeout to detect if the listener is not working
                var listenerTimeout: kotlinx.coroutines.Job? = null
                
                try {
                    expenseRepository.getUserExpensesForProject(projectId, userId)
                        .catch { exception ->
                            Log.e("ExpenseViewModel", "❌ Error in expenses flow: ${exception.message}")
                            exception.printStackTrace()
                            _error.value = "Failed to load expenses: ${exception.message}"
                            _isLoading.value = false
                            
                            // Try fallback method
                            loadUserExpensesForProjectFallback(projectId, userId)
                        }
                        .collect { expenses ->
                            Log.d("ExpenseViewModel", "📊 Received ${expenses.size} expenses from real-time listener")
                            
                            // Cancel timeout since we received data
                            listenerTimeout?.cancel()
                            
                            // Update expenses state
                            _expenses.value = expenses
                            
                            // Calculate status counts and update filtered expenses
                            calculateUserProjectSummary(expenses)
                            
                            // If no filter is applied, show all expenses
                            if (_selectedStatusFilter.value == null) {
                                _filteredExpenses.value = expenses
                                Log.d("ExpenseViewModel", "📋 Showing all ${expenses.size} expenses (no filter)")
                            } else {
                                // Apply current filter
                                val filteredExpenses = expenses.filter { it.status == _selectedStatusFilter.value }
                                _filteredExpenses.value = filteredExpenses
                                Log.d("ExpenseViewModel", "📋 Showing ${filteredExpenses.size} expenses (filter: ${_selectedStatusFilter.value})")
                            }
                            
                            // Log detailed status breakdown
                            val statusBreakdown = expenses.groupBy { it.status }
                                .mapValues { it.value.size }
                            Log.d("ExpenseViewModel", "📊 Status breakdown: $statusBreakdown")
                            
                            // Log current status counts for debugging
                            val currentCounts = _statusCounts.value
                            Log.d("ExpenseViewModel", "📊 Status counts: Approved=${currentCounts.approved}, Pending=${currentCounts.pending}, Rejected=${currentCounts.rejected}, Total=${currentCounts.total}")
                            
                            // Log sample expenses for debugging
                            if (expenses.isNotEmpty()) {
                                Log.d("ExpenseViewModel", "🎯 Sample expenses (first 3):")
                                expenses.take(3).forEach { expense ->
                                    Log.d("ExpenseViewModel", "  💰 ${expense.id}: ${expense.category} - ${expense.status} - ₹${expense.amount}")
                                }
                            } else {
                                Log.d("ExpenseViewModel", "📭 No expenses found for user $userId in project $projectId")
                            }
                            
                            _isLoading.value = false
                        }
                } catch (e: Exception) {
                    Log.e("ExpenseViewModel", "❌ Real-time listener failed: ${e.message}")
                    e.printStackTrace()
                    
                    // Try fallback method
                    loadUserExpensesForProjectFallback(projectId, userId)
                }
                
                // Set up timeout to detect if listener is not working
                listenerTimeout = launch {
                    delay(10000) // 10 seconds timeout
                    Log.w("ExpenseViewModel", "⏰ Real-time listener timeout - trying fallback method")
                    loadUserExpensesForProjectFallback(projectId, userId)
                }
                
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Error loading user expenses: ${e.message}")
                e.printStackTrace()
                _error.value = "Failed to load expenses: ${e.message}"
                _isLoading.value = false
                
                // Ensure we don't leave the UI in a hanging state
                _expenses.value = emptyList()
                _filteredExpenses.value = emptyList()
                _statusCounts.value = StatusCounts()
            }
        }
    }
    
    // Fallback method using direct query
    private fun loadUserExpensesForProjectFallback(projectId: String, userId: String) {
        viewModelScope.launch {
            Log.d("ExpenseViewModel", "🔄 Using fallback method for loading user expenses")
            _isLoading.value = true
            
            try {
                // Use direct query as fallback
                val expenses = expenseRepository.getUserExpensesForProjectDirect(projectId, userId)
                Log.d("ExpenseViewModel", "📊 Fallback method received ${expenses.size} expenses")
                
                // Update expenses state
                _expenses.value = expenses
                
                // Calculate status counts and update filtered expenses
                calculateUserProjectSummary(expenses)
                
                // If no filter is applied, show all expenses
                if (_selectedStatusFilter.value == null) {
                    _filteredExpenses.value = expenses
                    Log.d("ExpenseViewModel", "📋 Showing all ${expenses.size} expenses (no filter)")
                } else {
                    // Apply current filter
                    val filteredExpenses = expenses.filter { it.status == _selectedStatusFilter.value }
                    _filteredExpenses.value = filteredExpenses
                    Log.d("ExpenseViewModel", "📋 Showing ${filteredExpenses.size} expenses (filter: ${_selectedStatusFilter.value})")
                }
                
                // Log detailed status breakdown
                val statusBreakdown = expenses.groupBy { it.status }
                    .mapValues { it.value.size }
                Log.d("ExpenseViewModel", "📊 Fallback status breakdown: $statusBreakdown")
                
                // Log current status counts for debugging
                val currentCounts = _statusCounts.value
                Log.d("ExpenseViewModel", "📊 Fallback status counts: Approved=${currentCounts.approved}, Pending=${currentCounts.pending}, Rejected=${currentCounts.rejected}, Total=${currentCounts.total}")
                
                _isLoading.value = false
                
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Fallback method also failed: ${e.message}")
                e.printStackTrace()
                _error.value = "Failed to load expenses: ${e.message}"
                _isLoading.value = false
                
                // Ensure we don't leave the UI in a hanging state
                _expenses.value = emptyList()
                _filteredExpenses.value = emptyList()
                _statusCounts.value = StatusCounts()
            }
        }
    }

    private fun calculateUserProjectSummary(expenses: List<Expense>) {
        Log.d("ExpenseViewModel", "🧮 Calculating user project summary for ${expenses.size} expenses")
        
        // Filter expenses by status with detailed logging
        val approved = expenses.filter { it.status == ExpenseStatus.APPROVED }
        val pending = expenses.filter { it.status == ExpenseStatus.PENDING }
        val rejected = expenses.filter { it.status == ExpenseStatus.REJECTED }
        val draft = expenses.filter { it.status == ExpenseStatus.DRAFT }
        
        Log.d("ExpenseViewModel", "🔍 Status filtering results:")
        Log.d("ExpenseViewModel", "  ✅ Approved: ${approved.size} expenses")
        Log.d("ExpenseViewModel", "  ⏳ Pending: ${pending.size} expenses")
        Log.d("ExpenseViewModel", "  ❌ Rejected: ${rejected.size} expenses")
        Log.d("ExpenseViewModel", "  📝 Draft: ${draft.size} expenses")
        
        // Log sample expenses for each status
        if (approved.isNotEmpty()) {
            Log.d("ExpenseViewModel", "📋 Sample approved expenses:")
            approved.take(2).forEach { expense ->
                Log.d("ExpenseViewModel", "  ✅ ${expense.id}: ${expense.category} - ₹${expense.amount}")
            }
        }
        
        if (pending.isNotEmpty()) {
            Log.d("ExpenseViewModel", "📋 Sample pending expenses:")
            pending.take(2).forEach { expense ->
                Log.d("ExpenseViewModel", "  ⏳ ${expense.id}: ${expense.category} - ₹${expense.amount}")
            }
        }
        
        if (rejected.isNotEmpty()) {
            Log.d("ExpenseViewModel", "📋 Sample rejected expenses:")
            rejected.take(2).forEach { expense ->
                Log.d("ExpenseViewModel", "  ❌ ${expense.id}: ${expense.category} - ₹${expense.amount}")
            }
        }
        
        // Calculate totals
        val totalExpenses = expenses.sumOf { it.amount }
        val approvedAmount = approved.sumOf { it.amount }
        val pendingAmount = pending.sumOf { it.amount }
        val rejectedAmount = rejected.sumOf { it.amount }
        
        Log.d("ExpenseViewModel", "💰 Amount breakdown:")
        Log.d("ExpenseViewModel", "  💰 Total: ₹$totalExpenses")
        Log.d("ExpenseViewModel", "  ✅ Approved: ₹$approvedAmount")
        Log.d("ExpenseViewModel", "  ⏳ Pending: ₹$pendingAmount")
        Log.d("ExpenseViewModel", "  ❌ Rejected: ₹$rejectedAmount")
        
        // Group user's expenses by category
        val expensesByCategory = expenses
            .filter { it.category.isNotEmpty() }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        
        Log.d("ExpenseViewModel", "🏷️ Category breakdown: ${expensesByCategory.keys}")
        
        // Update expense summary
        _expenseSummary.value = ExpenseSummary(
            totalExpenses = totalExpenses,
            totalApproved = approvedAmount,
            totalPending = pendingAmount,
            totalRejected = rejectedAmount,
            approvedCount = approved.size,
            pendingCount = pending.size,
            rejectedCount = rejected.size,
            expensesByCategory = expensesByCategory
        )
        
        // Update status counts with detailed logging
        val newStatusCounts = StatusCounts(
            approved = approved.size,
            pending = pending.size,
            rejected = rejected.size,
            total = expenses.size
        )
        
        Log.d("ExpenseViewModel", "📊 UPDATING STATUS COUNTS:")
        Log.d("ExpenseViewModel", "  ✅ Approved: ${newStatusCounts.approved}")
        Log.d("ExpenseViewModel", "  ⏳ Pending: ${newStatusCounts.pending}")
        Log.d("ExpenseViewModel", "  ❌ Rejected: ${newStatusCounts.rejected}")
        Log.d("ExpenseViewModel", "  📊 Total: ${newStatusCounts.total}")
        
        _statusCounts.value = newStatusCounts
        
        // Verify the update was successful
        val verifyUpdate = _statusCounts.value
        Log.d("ExpenseViewModel", "🔍 VERIFICATION - Status counts after update:")
        Log.d("ExpenseViewModel", "  ✅ Approved: ${verifyUpdate.approved}")
        Log.d("ExpenseViewModel", "  ⏳ Pending: ${verifyUpdate.pending}")
        Log.d("ExpenseViewModel", "  ❌ Rejected: ${verifyUpdate.rejected}")
        Log.d("ExpenseViewModel", "  📊 Total: ${verifyUpdate.total}")
        
        Log.d("ExpenseViewModel", "✅ User Project Summary calculation complete")
    }

    fun clearFilter() {
        _selectedStatusFilter.value = null
        _filteredExpenses.value = _expenses.value
    }

    fun filterByStatus(status: ExpenseStatus) {
        _selectedStatusFilter.value = status
        _filteredExpenses.value = _expenses.value.filter { it.status == status }
    }
    
    fun clearData() {
        _expenses.value = emptyList()
        _filteredExpenses.value = emptyList()
        _statusCounts.value = StatusCounts()
        _selectedStatusFilter.value = null
        _error.value = null
        Log.d("ExpenseViewModel", "🧹 Cleared all expense data")
    }
    
    fun addDemoExpenses(projectId: String, userId: String, userName: String) {
        viewModelScope.launch {
            try {
                Log.d("ExpenseViewModel", "🎬 Adding demo expenses for testing...")
                
                val demoExpenses = listOf(
                    Expense(
                        id = "demo_1_${System.currentTimeMillis()}",
                        projectId = projectId,
                        userId = userId,
                        userName = userName,
                        date = Timestamp.now(),
                        amount = 5000.0,
                        department = "Production",
                        category = "Wages & Crew Payments",
                        description = "Camera operator payment",
                        modeOfPayment = "cash",
                        status = ExpenseStatus.APPROVED,
                        submittedAt = Timestamp.now(),
                        receiptNumber = "RCP001"
                    ),
                    Expense(
                        id = "demo_2_${System.currentTimeMillis() + 1}",
                        projectId = projectId,
                        userId = userId,
                        userName = userName,
                        date = Timestamp.now(),
                        amount = 3000.0,
                        department = "Art Department",
                        category = "Equipment Rental",
                        description = "Lighting equipment rental",
                        modeOfPayment = "upi",
                        status = ExpenseStatus.PENDING,
                        submittedAt = Timestamp.now(),
                        receiptNumber = "RCP002"
                    ),
                    Expense(
                        id = "demo_3_${System.currentTimeMillis() + 2}",
                        projectId = projectId,
                        userId = userId,
                        userName = userName,
                        date = Timestamp.now(),
                        amount = 1500.0,
                        department = "Catering",
                        category = "Catering & Food",
                        description = "Lunch for crew",
                        modeOfPayment = "cash",
                        status = ExpenseStatus.REJECTED,
                        submittedAt = Timestamp.now(),
                        reviewComments = "Receipt not clear, please resubmit",
                        receiptNumber = "RCP003"
                    ),
                    Expense(
                        id = "demo_4_${System.currentTimeMillis() + 3}",
                        projectId = projectId,
                        userId = userId,
                        userName = userName,
                        date = Timestamp.now(),
                        amount = 2500.0,
                        department = "Transportation",
                        category = "Transportation",
                        description = "Vehicle rental for location",
                        modeOfPayment = "check",
                        status = ExpenseStatus.APPROVED,
                        submittedAt = Timestamp.now(),
                        receiptNumber = "RCP004"
                    ),
                    Expense(
                        id = "demo_5_${System.currentTimeMillis() + 4}",
                        projectId = projectId,
                        userId = userId,
                        userName = userName,
                        date = Timestamp.now(),
                        amount = 800.0,
                        department = "Costumes",
                        category = "Costumes & Makeup",
                        description = "Costume materials",
                        modeOfPayment = "upi",
                        status = ExpenseStatus.PENDING,
                        submittedAt = Timestamp.now(),
                        receiptNumber = "RCP005"
                    )
                )
                
                // Update local state immediately for instant feedback
                _expenses.value = demoExpenses
                _filteredExpenses.value = demoExpenses
                calculateUserProjectSummary(demoExpenses)
                
                // Also save to Firebase in background
                var allSuccess = true
                var errorMessage: String? = null
                for (expense in demoExpenses) {
                    try {
                        val result = expenseRepository.addExpense(expense)
                        if (result.isFailure) {
                            allSuccess = false
                            errorMessage = result.exceptionOrNull()?.message
                            Log.e("ExpenseViewModel", "❌ Error saving to Firebase: $errorMessage")
                        }
                    } catch (e: Exception) {
                        allSuccess = false
                        errorMessage = e.message
                        Log.e("ExpenseViewModel", "❌ Exception saving to Firebase: $errorMessage")
                    }
                }
                if (allSuccess) {
                    _successMessage.value = "Demo expenses added successfully!"
                    Log.d("ExpenseViewModel", "✅ Added ${demoExpenses.size} demo expenses to Firestore")
                } else {
                    _error.value = "Failed to add some demo expenses: $errorMessage"
                }
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Error adding demo expenses: ${e.message}")
                _error.value = "Failed to add demo expenses: ${e.message}"
            }
        }
    }
    
    private fun sendExpenseSubmissionNotifications(expense: Expense, projectId: String) {
        viewModelScope.launch {
            try {
                // Get project details to find approvers
                val project = projectRepository.getProjectById(projectId)
                val projectName = project?.name ?: "Unknown Project"
                
                // Get approvers for the project (manager and any other approvers)
                val approverIds = mutableListOf<String>()
                project?.managerId?.let { approverIds.add(it) }
                
                // Send notifications to all approvers
                if (approverIds.isNotEmpty()) {
                    notificationRepository.createExpenseSubmissionNotification(
                        projectId = projectId,
                        projectName = projectName,
                        expenseId = expense.id,
                        submittedBy = expense.userName,
                        amount = expense.amount,
                        approverIds = approverIds
                    )
                    
                    Log.d("ExpenseViewModel", "✅ Sent expense submission notifications to ${approverIds.size} approvers")
                }
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "❌ Error sending expense submission notifications: ${e.message}")
            }
        }
    }
} 