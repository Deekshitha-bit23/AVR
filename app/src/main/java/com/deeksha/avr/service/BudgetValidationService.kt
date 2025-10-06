package com.deeksha.avr.service

import com.deeksha.avr.model.Expense
import com.deeksha.avr.model.Project
import com.deeksha.avr.repository.ExpenseRepository
import com.deeksha.avr.repository.ProjectRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetValidationService @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val projectRepository: ProjectRepository
) {
    
    data class BudgetValidationResult(
        val isValid: Boolean,
        val departmentBudget: Double,
        val currentSpent: Double,
        val newExpenseAmount: Double,
        val remainingBudget: Double,
        val wouldExceedBudget: Boolean,
        val warningMessage: String? = null
    )
    
    /**
     * Validates if a new expense would exceed the department's allocated budget
     */
    suspend fun validateExpenseAgainstBudget(
        projectId: String,
        department: String,
        newExpenseAmount: Double
    ): BudgetValidationResult {
        return try {
            // Get project details
            val project = projectRepository.getProjectById(projectId)
            if (project == null) {
                return BudgetValidationResult(
                    isValid = false,
                    departmentBudget = 0.0,
                    currentSpent = 0.0,
                    newExpenseAmount = newExpenseAmount,
                    remainingBudget = 0.0,
                    wouldExceedBudget = true,
                    warningMessage = "Project not found"
                )
            }
            
            // Get department budget allocation
            val departmentBudget = project.departmentBudgets[department] ?: 0.0
            
            if (departmentBudget <= 0) {
                return BudgetValidationResult(
                    isValid = false,
                    departmentBudget = departmentBudget,
                    currentSpent = 0.0,
                    newExpenseAmount = newExpenseAmount,
                    remainingBudget = 0.0,
                    wouldExceedBudget = true,
                    warningMessage = "No budget allocated for department: $department"
                )
            }
            
            // Get current spending for this department
            val currentSpent = getCurrentDepartmentSpending(projectId, department)
            
            // Calculate remaining budget
            val remainingBudget = departmentBudget - currentSpent
            
            // Check if new expense would exceed budget
            val wouldExceedBudget = newExpenseAmount > remainingBudget
            
            val warningMessage = if (wouldExceedBudget) {
                "Expense amount (₹${String.format("%.2f", newExpenseAmount)}) would exceed remaining budget for $department department. " +
                "Remaining budget: ₹${String.format("%.2f", remainingBudget)}"
            } else null
            
            BudgetValidationResult(
                isValid = !wouldExceedBudget,
                departmentBudget = departmentBudget,
                currentSpent = currentSpent,
                newExpenseAmount = newExpenseAmount,
                remainingBudget = remainingBudget,
                wouldExceedBudget = wouldExceedBudget,
                warningMessage = warningMessage
            )
            
        } catch (e: Exception) {
            android.util.Log.e("BudgetValidationService", "Error validating budget: ${e.message}", e)
            BudgetValidationResult(
                isValid = false,
                departmentBudget = 0.0,
                currentSpent = 0.0,
                newExpenseAmount = newExpenseAmount,
                remainingBudget = 0.0,
                wouldExceedBudget = true,
                warningMessage = "Error validating budget: ${e.message}"
            )
        }
    }
    
    /**
     * Gets the current total spending for a specific department in a project
     */
    private suspend fun getCurrentDepartmentSpending(projectId: String, department: String): Double {
        return try {
            val expenses = expenseRepository.getExpensesByProject(projectId).first()
            expenses
                .filter { it.department == department && it.status == com.deeksha.avr.model.ExpenseStatus.APPROVED }
                .sumOf { it.amount }
        } catch (e: Exception) {
            android.util.Log.e("BudgetValidationService", "Error getting department spending: ${e.message}", e)
            0.0
        }
    }
    
    /**
     * Gets budget summary for all departments in a project
     */
    suspend fun getProjectBudgetSummary(projectId: String): Map<String, DepartmentBudgetSummary> {
        return try {
            val project = projectRepository.getProjectById(projectId)
            if (project == null) return emptyMap()
            
            val expenses = expenseRepository.getExpensesByProject(projectId).first()
            val approvedExpenses = expenses.filter { it.status == com.deeksha.avr.model.ExpenseStatus.APPROVED }
            
            project.departmentBudgets.mapValues { (department, allocatedBudget) ->
                val spent = approvedExpenses
                    .filter { it.department == department }
                    .sumOf { it.amount }
                
                DepartmentBudgetSummary(
                    department = department,
                    allocatedBudget = allocatedBudget,
                    spent = spent,
                    remaining = allocatedBudget - spent,
                    percentage = if (allocatedBudget > 0) (spent / allocatedBudget) * 100 else 0.0
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("BudgetValidationService", "Error getting budget summary: ${e.message}", e)
            emptyMap()
        }
    }
    
    /**
     * Checks if project has any budget allocated
     */
    suspend fun hasProjectBudget(projectId: String): Boolean {
        return try {
            val project = projectRepository.getProjectById(projectId)
            project?.departmentBudgets?.isNotEmpty() == true
        } catch (e: Exception) {
            android.util.Log.e("BudgetValidationService", "Error checking project budget: ${e.message}", e)
            false
        }
    }
    
    data class DepartmentBudgetSummary(
        val department: String,
        val allocatedBudget: Double,
        val spent: Double,
        val remaining: Double,
        val percentage: Double
    )
}
