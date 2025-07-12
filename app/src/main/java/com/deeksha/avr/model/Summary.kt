package com.deeksha.avr.model

import com.google.firebase.Timestamp

// Consolidated expense summary for all use cases
data class ExpenseSummary(
    val totalExpenses: Double = 0.0,
    val totalApproved: Double = 0.0,
    val totalPending: Double = 0.0,
    val totalRejected: Double = 0.0,
    val approvedCount: Int = 0,
    val pendingCount: Int = 0,
    val rejectedCount: Int = 0,
    val expensesByCategory: Map<String, Double> = emptyMap(),
    val expensesByDepartment: Map<String, Double> = emptyMap(),
    val expensesByProject: Map<String, Double> = emptyMap(),
    val recentExpenses: List<Expense> = emptyList()
)

// Status counts for tracking different expense states
data class StatusCounts(
    val approved: Int = 0,
    val pending: Int = 0,
    val rejected: Int = 0,
    val total: Int = 0
)

// Form data for expense creation/editing
data class ExpenseFormData(
    val date: String = "",
    val amount: String = "",
    val department: String = "",
    val category: String = "",
    val description: String = "",
    val modeOfPayment: String = "cash",
    val attachmentUri: String = "",
    val tds: String = "",
    val gst: String = "",
    val receiptNumber: String = ""
)

// Project budget and category breakdown
data class CategoryBudget(
    val category: String,
    val budgetAllocated: Double,
    val spent: Double,
    val remaining: Double,
    val percentage: Double
)

// Project budget summary with detailed breakdown
data class ProjectBudgetSummary(
    val project: Project? = null,
    val totalBudget: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalRemaining: Double = 0.0,
    val spentPercentage: Double = 0.0,
    val categoryBreakdown: List<CategoryBudget> = emptyList(),
    val recentExpenses: List<Expense> = emptyList(),
    val pendingApprovalsCount: Int = 0,
    val approvedExpensesCount: Int = 0
)

// Approval summary for approvers
data class ApprovalSummary(
    val totalPendingCount: Int = 0,
    val totalPendingAmount: Double = 0.0,
    val recentSubmissions: List<Expense> = emptyList()
)

// Expense notification summary for status tracking
data class ExpenseNotificationSummary(
    val approvedCount: Int = 0,
    val rejectedCount: Int = 0,
    val submittedCount: Int = 0,
    val totalUnread: Int = 0,
    val total: Int = 0,
    val hasUpdates: Boolean = false
) 