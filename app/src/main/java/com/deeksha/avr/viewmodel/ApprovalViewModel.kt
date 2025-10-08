package com.deeksha.avr.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deeksha.avr.model.Expense
import com.deeksha.avr.model.Project
import com.deeksha.avr.repository.ExpenseRepository
import com.deeksha.avr.repository.ProjectRepository
import com.deeksha.avr.repository.NotificationRepository
import com.deeksha.avr.repository.ChatRepository
import com.deeksha.avr.service.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

data class ApprovalSummary(
    val totalPendingCount: Int = 0,
    val totalPendingAmount: Double = 0.0,
    val recentSubmissions: List<Expense> = emptyList()
)

@HiltViewModel
class ApprovalViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository,
    private val notificationRepository: NotificationRepository,
    private val notificationService: NotificationService,
    private val chatRepository: ChatRepository
) : ViewModel() {
    
    private val _pendingExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val pendingExpenses: StateFlow<List<Expense>> = _pendingExpenses.asStateFlow()
    
    private val _approvalSummary = MutableStateFlow(ApprovalSummary())
    val approvalSummary: StateFlow<ApprovalSummary> = _approvalSummary.asStateFlow()
    
    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    // Add selectedExpense state for ReviewExpenseScreen
    private val _selectedExpense = MutableStateFlow<Expense?>(null)
    val selectedExpense: StateFlow<Expense?> = _selectedExpense.asStateFlow()
    
    // Track the current context - whether we're viewing project-specific or all pending approvals
    private val _currentProjectId = MutableStateFlow<String?>(null)
    val currentProjectId: StateFlow<String?> = _currentProjectId.asStateFlow()
    
    fun loadPendingApprovals() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _currentProjectId.value = null // Reset to indicate we're viewing all approvals
            
            try {
                // Use ONLY direct query for stability - no real-time listeners that cause blinking
                val directExpenses = expenseRepository.getPendingExpensesDirectly()
                
                // Always update UI with found data
                _pendingExpenses.value = directExpenses
                
                val totalAmount = directExpenses.sumOf { it.amount }
                val recentSubmissions = directExpenses.sortedByDescending { 
                    it.submittedAt?.toDate()?.time ?: 0L 
                }.take(5)
                
                _approvalSummary.value = ApprovalSummary(
                    totalPendingCount = directExpenses.size,
                    totalPendingAmount = totalAmount,
                    recentSubmissions = recentSubmissions
                )
                
            } catch (e: Exception) {
                _error.value = "Failed to load pending approvals: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadPendingApprovalsForProject(projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _currentProjectId.value = projectId // Track that we're viewing project-specific approvals
            
            try {
                // Load project details first
                val project = projectRepository.getProjectById(projectId)
                _currentProject.value = project
                
                // Use direct query for project-specific expenses - more stable
                val directExpenses = expenseRepository.getPendingExpensesDirectly()
                    .filter { it.projectId == projectId }
                
                // Always update UI with found data
                _pendingExpenses.value = directExpenses
                
                // Calculate approval summary for this project
                val totalAmount = directExpenses.sumOf { it.amount }
                val recentSubmissions = directExpenses.sortedByDescending { 
                    it.submittedAt?.toDate()?.time ?: 0L 
                }.take(5)
                
                _approvalSummary.value = ApprovalSummary(
                    totalPendingCount = directExpenses.size,
                    totalPendingAmount = totalAmount,
                    recentSubmissions = recentSubmissions
                )
                
            } catch (e: Exception) {
                _error.value = "Failed to load project details: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Helper method to refresh data based on current context
    private fun refreshCurrentData() {
        val projectId = _currentProjectId.value
        if (projectId != null) {
            // We're viewing project-specific data, refresh that
            loadPendingApprovalsForProject(projectId)
        } else {
            // We're viewing all data, refresh that
            loadPendingApprovals()
        }
    }
    
    // Function to fetch individual expense by ID for ReviewExpenseScreen
    fun fetchExpenseById(expenseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val expense = expenseRepository.getExpenseById(expenseId)
                _selectedExpense.value = expense
                
                if (expense == null) {
                    _error.value = "Expense not found"
                }
                
            } catch (e: Exception) {
                _error.value = "Failed to load expense: ${e.message}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun approveExpense(expense: Expense, reviewerName: String, comments: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            
            try {
                expenseRepository.updateExpenseStatus(
                    projectId = expense.projectId,
                    expenseId = expense.id,
                    status = com.deeksha.avr.model.ExpenseStatus.APPROVED,
                    reviewedBy = reviewerName,
                    reviewComments = comments,
                    reviewedAt = com.google.firebase.Timestamp.now()
                )
                
                // Send notification to expense submitter
                sendExpenseStatusNotification(expense, true, reviewerName)
                
            } catch (e: Exception) {
                _error.value = "Failed to approve expense: ${e.message}"
                e.printStackTrace()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun approveExpenseWithAmount(
        expense: Expense, 
        newAmount: Double, 
        reviewerName: String, 
        comments: String
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            
            try {
                expenseRepository.updateExpenseAmountAndStatus(
                    projectId = expense.projectId,
                    expenseId = expense.id,
                    newAmount = newAmount,
                    status = com.deeksha.avr.model.ExpenseStatus.APPROVED,
                    reviewedBy = reviewerName,
                    reviewComments = comments,
                    reviewedAt = com.google.firebase.Timestamp.now()
                )
                
                // Send notification to expense submitter with updated amount
                val updatedExpense = expense.copy(amount = newAmount)
                sendExpenseStatusNotification(updatedExpense, true, reviewerName)
                
            } catch (e: Exception) {
                _error.value = "Failed to approve expense with amount change: ${e.message}"
                e.printStackTrace()
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    fun rejectExpense(expense: Expense, reviewerName: String, comments: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            
            try {
                expenseRepository.updateExpenseStatus(
                    projectId = expense.projectId,
                    expenseId = expense.id,
                    status = com.deeksha.avr.model.ExpenseStatus.REJECTED,
                    reviewedBy = reviewerName,
                    reviewComments = comments,
                    reviewedAt = com.google.firebase.Timestamp.now()
                )
                
                // Send notification to expense submitter
                sendExpenseStatusNotification(expense, false, reviewerName)
                
            } catch (e: Exception) {
                _error.value = "Failed to reject expense: ${e.message}"
                e.printStackTrace()
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun rejectExpenseWithAmount(
        expense: Expense, 
        newAmount: Double, 
        reviewerName: String, 
        comments: String
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            
            try {
                expenseRepository.updateExpenseAmountAndStatus(
                    projectId = expense.projectId,
                    expenseId = expense.id,
                    newAmount = newAmount,
                    status = com.deeksha.avr.model.ExpenseStatus.REJECTED,
                    reviewedBy = reviewerName,
                    reviewComments = comments,
                    reviewedAt = com.google.firebase.Timestamp.now()
                )
                
                // Send notification to expense submitter with updated amount
                val updatedExpense = expense.copy(amount = newAmount)
                sendExpenseStatusNotification(updatedExpense, false, reviewerName)
                
            } catch (e: Exception) {
                _error.value = "Failed to reject expense with amount change: ${e.message}"
                e.printStackTrace()
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    fun approveSelectedExpenses(expenseIds: List<String>, reviewerName: String, comments: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            
            try {
                val currentExpenses = _pendingExpenses.value
                var successCount = 0
                var failCount = 0
                
                // Process each expense sequentially to ensure proper updates
                for (expenseId in expenseIds) {
                    try {
                        val expense = currentExpenses.find { it.id == expenseId }
                        if (expense != null) {
                            expenseRepository.updateExpenseStatus(
                                projectId = expense.projectId,
                                expenseId = expenseId,
                                status = com.deeksha.avr.model.ExpenseStatus.APPROVED,
                                reviewedBy = reviewerName,
                                reviewComments = comments,
                                reviewedAt = com.google.firebase.Timestamp.now()
                            )
                            
                            // Send notification for individual expense
                            sendExpenseStatusNotification(expense, true, reviewerName)
                            
                            successCount++
                            
                            // Small delay between each update
                            delay(100)
                            
                        } else {
                            failCount++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        failCount++
                    }
                }
                
                if (failCount > 0) {
                    _error.value = "Some expenses could not be approved ($failCount failed)"
                }
                
                // Give Firebase time to process updates
                delay(1000)
                
            } catch (e: Exception) {
                _error.value = "Failed to approve selected expenses: ${e.message}"
                e.printStackTrace()
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    fun rejectSelectedExpenses(expenseIds: List<String>, reviewerName: String, comments: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            
            try {
                val currentExpenses = _pendingExpenses.value
                var successCount = 0
                var failCount = 0
                
                // Process each expense sequentially to ensure proper updates
                for (expenseId in expenseIds) {
                    try {
                        val expense = currentExpenses.find { it.id == expenseId }
                        if (expense != null) {
                            expenseRepository.updateExpenseStatus(
                                projectId = expense.projectId,
                                expenseId = expenseId,
                                status = com.deeksha.avr.model.ExpenseStatus.REJECTED,
                                reviewedBy = reviewerName,
                                reviewComments = comments,
                                reviewedAt = com.google.firebase.Timestamp.now()
                            )
                            
                            // Send notification for individual expense
                            sendExpenseStatusNotification(expense, false, reviewerName)
                            
                            successCount++
                            
                            // Small delay between each update
                            delay(100)
                            
                        } else {
                            failCount++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        failCount++
                    }
                }
                
                if (failCount > 0) {
                    _error.value = "Some expenses could not be rejected ($failCount failed)"
                }
                
                // Give Firebase time to process updates
                delay(1000)
                
            } catch (e: Exception) {
                _error.value = "Failed to reject selected expenses: ${e.message}"
                e.printStackTrace()
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    
    // Debug method to test Firebase connectivity and data structure
    fun debugFirebaseConnection() {
        viewModelScope.launch {
            try {
                // Test 1: Check if we can connect to Firebase at all
                val testCollection = projectRepository.getAllProjects()
                
                // Test 2: Check each project for expenses
                testCollection.forEachIndexed { index, project ->
                    
                    try {
                        // Get all expenses for this project (not just pending)
                        val allExpenses = expenseRepository.getExpenseSummary(project.id)
                        
                        // Get direct pending expenses for this project using direct query
                        
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                // Test 3: Try direct query again
                val directResult = expenseRepository.getPendingExpensesDirectly()
                
                if (directResult.isEmpty()) {
                    _error.value = "No pending expenses found. Check if any expenses are submitted and have PENDING status."
                } else {
                    _pendingExpenses.value = directResult
                    
                    // Calculate approval summary
                    val totalAmount = directResult.sumOf { it.amount }
                    val recentSubmissions = directResult.sortedByDescending { 
                        it.submittedAt?.toDate()?.time ?: 0L 
                    }.take(5)
                    
                    _approvalSummary.value = ApprovalSummary(
                        totalPendingCount = directResult.size,
                        totalPendingAmount = totalAmount,
                        recentSubmissions = recentSubmissions
                    )
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "Firebase connection failed: ${e.message}"
            }
        }
    }
    
    private fun sendExpenseStatusNotification(expense: Expense, isApproved: Boolean, approverName: String) {
        viewModelScope.launch {
            try {
                Log.d("ApprovalViewModel", "🔄 Sending expense status notification for expense: ${expense.id}")
                
                // Use the new NotificationService to send expense status notification
                notificationService.sendExpenseStatusNotification(
                    expenseId = expense.id,
                    projectId = expense.projectId,
                    submittedByUserId = expense.userId,
                    isApproved = isApproved,
                    amount = expense.amount,
                    reviewerName = approverName,
                    comments = expense.reviewComments ?: ""
                ).onSuccess {
                    Log.d("ApprovalViewModel", "✅ Successfully sent expense ${if (isApproved) "approval" else "rejection"} notification")
                }.onFailure { error ->
                    Log.e("ApprovalViewModel", "❌ Failed to send expense status notification: ${error.message}")
                }
                
            } catch (e: Exception) {
                Log.e("ApprovalViewModel", "❌ Error sending expense status notification: ${e.message}")
            }
        }
    }
} 
