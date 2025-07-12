package com.deeksha.avr.model

import com.google.firebase.Timestamp

data class ReportData(
    val totalSpent: Double = 0.0,
    val totalBudget: Double = 0.0,
    val budgetUsagePercentage: Double = 0.0,
    val expensesByCategory: Map<String, Double> = emptyMap(),
    val expensesByDepartment: Map<String, Double> = emptyMap(),
    val detailedExpenses: List<DetailedExpense> = emptyList(),
    val timeRange: String = "This Year",
    val selectedDepartment: String = "All Departments"
)

data class DetailedExpense(
    val id: String = "",
    val date: Timestamp? = null,
    val invoice: String = "",
    val by: String = "",
    val amount: Double = 0.0,
    val department: String = "",
    val modeOfPayment: String = ""
)

data class CategoryData(
    val category: String,
    val amount: Double,
    val color: Long
)

data class CategoryBudgetData(
    val category: String,
    val spent: Double,
    val budgetAllocated: Double,
    val color: Long
)

data class DepartmentFilter(
    val name: String,
    val value: String
)

data class TimeRangeFilter(
    val displayName: String,
    val value: String
)

data class ExportData(
    val totalSpent: Double,
    val timeRange: String,
    val department: String,
    val categoryBreakdown: Map<String, Double>,
    val detailedExpenses: List<DetailedExpense>,
    val generatedAt: Timestamp
)

// Filter options
object FilterOptions {
    val timeRanges = listOf(
        TimeRangeFilter("This Month", "this_month"),
        TimeRangeFilter("This Year", "this_year"),
        TimeRangeFilter("Last 6 Months", "last_6_months"),
        TimeRangeFilter("Last 12 Months", "last_12_months"),
        TimeRangeFilter("All Time", "all_time")
    )
    
    val departments = listOf(
        DepartmentFilter("All Departments", "all"),
        DepartmentFilter("Production", "Production"),
        DepartmentFilter("Direction", "Direction"),
        DepartmentFilter("Cinematography", "Cinematography"),
        DepartmentFilter("Art Department", "Art Department"),
        DepartmentFilter("Costumes", "Costumes"),
        DepartmentFilter("Makeup", "Makeup"),
        DepartmentFilter("Sound", "Sound"),
        DepartmentFilter("Editing", "Editing"),
        DepartmentFilter("VFX", "VFX"),
        DepartmentFilter("Administration", "Administration")
    )
    
    val categoryColors = mapOf(
        "Wages & Crew Payments" to 0xFF4285F4,
        "Equipment Rental" to 0xFF34A853,
        "Catering & Food" to 0xFFFBBC05,
        "Transportation" to 0xFFEA4335,
        "Costumes & Makeup" to 0xFF9C27B0,
        "Post Production" to 0xFF607D8B,
        "Marketing & Promotion" to 0xFF3F51B5,
        "Other" to 0xFFFF9800,
        "Production" to 0xFF4CAF50,
        "Direction" to 0xFF2196F3,
        "Cinematography" to 0xFFFF5722,
        "Art Department" to 0xFF795548,
        "Sound" to 0xFF009688,
        "Editing" to 0xFFE91E63,
        "VFX" to 0xFF673AB7,
        "Administration" to 0xFF795548
    )
} 