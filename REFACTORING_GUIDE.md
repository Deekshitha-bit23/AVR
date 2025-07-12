# 🛠️ AVR Entertainment - Refactoring Guide

## Summary
This guide outlines the refactoring steps to eliminate duplicates and improve code organization while maintaining functionality.

## ✅ **Completed Actions**

### 1. **Utility Classes Created**
- ✅ `FormatUtils.kt` - Centralized formatting functions
- ✅ `CommonComponents.kt` - Reusable UI components  
- ✅ `Summary.kt` - Consolidated data classes
- ✅ Deleted 6 unused log files

## 🔄 **Implementation Steps**

### **Phase 1: Replace formatCurrency Functions**

Replace in these files (10+ locations):
```kotlin
// OLD (in each file)
private fun formatCurrency(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
}

// NEW (import and use)
import com.deeksha.avr.utils.FormatUtils
// Replace all formatCurrency(amount) calls with:
FormatUtils.formatCurrency(amount)
```

**Files to update:**
- `UserDashboardScreen.kt`
- `ApproverDashboardScreen.kt` 
- `ApproverProjectDashboardScreen.kt`
- `ApproverProjectSelectionScreen.kt`
- `OverallReportsScreen.kt`
- `ReportsScreen.kt`
- `ProjectSelectionScreen.kt`
- `ReviewExpenseScreen.kt` (use `formatCurrencySimple`)
- `ExportRepository.kt` (use `formatCurrencySimple`)

### **Phase 2: Replace Date Formatting**

Replace SimpleDateFormat calls:
```kotlin
// OLD
SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it.toDate())

// NEW  
FormatUtils.formatDate(it)
FormatUtils.formatDateShort(it) // for "dd/MM"
FormatUtils.formatDateLong(it)  // for "dd MMM yyyy"
```

**Files to update:**
- `TrackSubmissionsScreen.kt`
- `OverallReportsScreen.kt`
- `ReviewExpenseScreen.kt`
- `ReportsScreen.kt`
- `PendingApprovalsScreen.kt`
- `UserDashboardScreen.kt`
- `ExportRepository.kt`

### **Phase 3: Update Data Classes**

Replace duplicate data classes with consolidated versions:

#### **In ViewModels:**
```kotlin
// OLD (remove these)
data class UserExpenseSummary(...)
data class ProjectExpenseSummary(...)
data class StatusCounts(...)
data class ExpenseFormData(...)

// NEW (import and use)
import com.deeksha.avr.model.ExpenseSummary
import com.deeksha.avr.model.StatusCounts
import com.deeksha.avr.model.ExpenseFormData
```

**Files to update:**
- `ExpenseViewModel.kt` - Remove duplicate data classes
- `ApprovalViewModel.kt` - Use `ApprovalSummary` from Summary.kt
- `ApproverProjectViewModel.kt` - Use consolidated classes

#### **In Model Files:**
```kotlin
// In Expense.kt - Remove these duplicates:
// - ExpenseFormData (use Summary.kt version)
// - ExpenseStatusCounts (use StatusCounts from Summary.kt)
```

### **Phase 4: Replace UI Components**

Use common components from `CommonComponents.kt`:

#### **Status Cards:**
```kotlin
// OLD (various implementations)
StatusCard(...) 
ExpenseSummaryCard(...)

// NEW (unified)
StatusSummaryCard(
    title = "Approved",
    amount = summary.totalApproved, // for expense cards
    count = summary.approvedCount,
    icon = Icons.Default.CheckCircle,
    color = Color(0xFF4CAF50)
)

// For filter cards (no amount):
StatusSummaryCard(
    title = "Pending", 
    count = statusCounts.pending,
    icon = Icons.Default.Warning,
    color = Color(0xFFFF9800),
    isSelected = selectedFilter == ExpenseStatus.PENDING,
    onClick = { /* filter logic */ }
)
```

#### **Project Cards:**
```kotlin
// OLD (multiple implementations)
ProjectCard(project, onClick)

// NEW (unified with options)
ProjectCard(
    project = project,
    onProjectClick = { onProjectSelected(project.id) },
    showBudget = true, // optional
    showEndDate = true // optional
)
```

#### **Category Cards:**
```kotlin
// OLD 
CategoryItem(categoryName, amount)

// NEW
CategoryAmountCard(categoryName, amount)
```

### **Phase 5: Refactor Repository Mapping**

In `ExpenseRepository.kt`, create a single expense mapping function:

```kotlin
private fun mapDocumentToExpense(doc: DocumentSnapshot, projectId: String): Expense? {
    return try {
        val expenseData = doc.data ?: return null
        Expense(
            id = doc.id,
            projectId = projectId,
            userId = expenseData["userId"] as? String ?: "",
            userName = expenseData["userName"] as? String ?: "",
            date = expenseData["date"] as? Timestamp,
            amount = (expenseData["amount"] as? Number)?.toDouble() ?: 0.0,
            department = expenseData["department"] as? String ?: "",
            category = expenseData["category"] as? String ?: "",
            description = expenseData["description"] as? String ?: "",
            modeOfPayment = expenseData["modeOfPayment"] as? String ?: "",
            tds = (expenseData["tds"] as? Number)?.toDouble() ?: 0.0,
            gst = (expenseData["gst"] as? Number)?.toDouble() ?: 0.0,
            netAmount = (expenseData["netAmount"] as? Number)?.toDouble() ?: 0.0,
            attachmentUrl = expenseData["attachmentUrl"] as? String ?: "",
            attachmentFileName = expenseData["attachmentFileName"] as? String ?: "",
            status = when (expenseData["status"] as? String) {
                "APPROVED" -> ExpenseStatus.APPROVED
                "REJECTED" -> ExpenseStatus.REJECTED
                "DRAFT" -> ExpenseStatus.DRAFT
                else -> ExpenseStatus.PENDING
            },
            submittedAt = expenseData["submittedAt"] as? Timestamp,
            reviewedAt = expenseData["reviewedAt"] as? Timestamp,
            reviewedBy = expenseData["reviewedBy"] as? String ?: "",
            reviewComments = expenseData["reviewComments"] as? String ?: "",
            receiptNumber = expenseData["receiptNumber"] as? String ?: ""
        )
    } catch (e: Exception) {
        Log.e("ExpenseRepository", "Error mapping expense: ${e.message}")
        null
    }
}
```

Then replace all 4+ duplicate mapping blocks with:
```kotlin
val expenses = snapshot.documents.mapNotNull { doc ->
    mapDocumentToExpense(doc, projectId)
}
```

## 📊 **Benefits After Refactoring**

### **Code Reduction:**
- ❌ Remove ~150 lines of duplicate `formatCurrency` functions
- ❌ Remove ~50 lines of duplicate date formatting
- ❌ Remove ~100 lines of duplicate data classes
- ❌ Remove ~200 lines of duplicate UI components
- ❌ Remove ~300 lines of duplicate repository mapping

**Total: ~800 lines of code eliminated**

### **Maintainability:**
- ✅ Single source of truth for formatting
- ✅ Consistent UI components
- ✅ Centralized data models
- ✅ Easier to update currency format across app
- ✅ Reduced risk of inconsistencies

### **Performance:**
- ✅ Smaller APK size
- ✅ Faster compilation
- ✅ Better code reuse

## ⚠️ **Testing Strategy**

After each phase:
1. **Compile:** `./gradlew assembleDebug`
2. **Test key flows:**
   - User expense submission
   - Approver workflow
   - Reports generation
   - Export functionality
3. **Verify UI consistency:**
   - All cards render correctly
   - Currency formatting is consistent
   - Date formats are consistent

## 🎯 **Files That Should NOT Be Changed**

Keep these files as they serve different purposes:
- `ReportsViewModel.kt` vs `OverallReportsViewModel.kt` (different data sources)
- `ProjectSelectionScreen.kt` vs `ApproverProjectSelectionScreen.kt` (different user roles)
- All screen-specific logic and navigation

## 📝 **Implementation Order**

1. ✅ **Phase 1:** FormatUtils (DONE) 
2. ✅ **Phase 2:** CommonComponents (DONE)
3. ✅ **Phase 3:** Summary models (DONE)
4. ✅ **Phase 4:** Delete log files (DONE)
5. 🔄 **Phase 5:** Update imports and references
6. 🔄 **Phase 6:** Test and verify
7. 🔄 **Phase 7:** Repository refactoring

This refactoring will significantly improve code maintainability while preserving all existing functionality. 