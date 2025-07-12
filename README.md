# AVR - Expense Management System

## 📊 Track Recent Submissions - Complete Implementation

The Track Recent Submissions feature provides users with **real-time monitoring** of their expense submissions with automatic status updates from pending to approved/rejected.

### 🔄 Complete Workflow

#### 1. **Expense Submission**
- User submits expense through `AddExpenseScreen`
- Expense is created with `ExpenseStatus.PENDING` 
- Stored in Firebase: `projects/{projectId}/expenses/{expenseId}`
- Real-time listeners immediately detect the new submission

#### 2. **Real-time Status Tracking**
- `TrackSubmissionsScreen` uses Firebase Firestore listeners
- Automatically updates when expense status changes
- No manual refresh needed - everything is live

#### 3. **Approver Workflow**
- Approvers see pending expenses in `PendingApprovalsScreen`
- Can approve/reject individual expenses in `ReviewExpenseScreen`
- Bulk approve/reject multiple expenses
- Status updates are instant via `updateExpenseStatus()`

#### 4. **Status Change Notifications**
- User receives immediate visual notifications
- Different colors and icons for each status:
  - ✅ **Approved**: Green with checkmark
  - ⏳ **Pending**: Orange with clock
  - ❌ **Rejected**: Red with X
  - 📝 **Draft**: Gray with edit icon

### 🎯 Key Features

#### **Real-time Updates**
```kotlin
// Firebase listener automatically updates UI
expenseRepository.getUserExpensesForProject(projectId, userId)
    .onEach { expenseList ->
        // Status changes detected automatically
        // UI updates immediately
    }
```

#### **Status Filtering**
- Filter by Approved, Pending, or Rejected
- Live count updates for each status
- "Show All" button to clear filters

#### **Visual Feedback**
- Enhanced status badges with emojis
- Color-coded status cards
- Time elapsed indicators for pending expenses
- Pull-to-refresh functionality

#### **Notification System**
- Toast notifications for status changes
- Dismissible notification cards
- Different colors based on approval/rejection

### 🔧 Technical Implementation

#### **Firebase Structure**
```
projects/
  {projectId}/
    expenses/
      {expenseId}/
        - status: "PENDING" | "APPROVED" | "REJECTED"
        - submittedAt: Timestamp
        - reviewedAt: Timestamp
        - reviewedBy: String
        - reviewComments: String
        - (other expense fields)
```

#### **Real-time Listeners**
```kotlin
// ExpenseRepository.kt
fun getUserExpensesForProject(projectId: String, userId: String): Flow<List<Expense>> = 
    callbackFlow {
        val listener = firestore.collection("projects")
            .document(projectId)
            .collection("expenses")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                // Real-time updates
                trySend(expenses)
            }
        awaitClose { listener.remove() }
    }
```

#### **Status Update Process**
```kotlin
// ApprovalViewModel.kt
fun approveExpense(expense: Expense, reviewerName: String, comments: String) {
    expenseRepository.updateExpenseStatus(
        projectId = expense.projectId,
        expenseId = expense.id,
        status = ExpenseStatus.APPROVED,
        reviewedBy = reviewerName,
        reviewComments = comments,
        reviewedAt = Timestamp.now()
    )
    // Real-time listeners automatically update user's UI
}
```

### 📱 User Experience

1. **Submit Expense** → Status shows "⏳ Pending"
2. **Approver Reviews** → Status changes to "✅ Approved" or "❌ Rejected"
3. **User Gets Notified** → Immediate visual notification
4. **Status Updates** → Real-time without refresh

### 🚀 Enhanced Features Added

- **Better Status Notifications**: Color-coded with dismiss option
- **Improved Status Badges**: Emojis and better visual design
- **Enhanced Filtering**: Clearer "Show All" button
- **Live Indicators**: Shows "Live" status for real-time updates
- **Time Tracking**: Shows time elapsed for pending expenses

The system ensures users always have up-to-date information about their expense submissions and can track the complete approval process in real-time. 