# Temporary Approver Access Control Implementation

## Overview
Implemented access control for temporary approver assignments to ensure that approvers can only access projects after accepting the assignment. Pending assignments are hidden from the project selection screen, and rejections require a reason that is sent to the production head.

## Problem Statement
Previously, when a production head assigned a temporary approver to a project:
- The project appeared in the approver's project list immediately (even if pending)
- No access control based on assignment status (PENDING vs ACCEPTED)
- Rejection didn't require a reason
- Production head wasn't informed about the rejection reason

## Solution Implemented

### 1. Access Control - Filter Pending Assignments
**File:** `app/src/main/java/com/deeksha/avr/repository/ProjectRepository.kt`

#### Changes Made:
- Updated `getTemporaryApproverProjects()` method to check assignment status in the `temporaryApprovers` subcollection
- Only projects with **ACCEPTED** status are returned to the UI
- **PENDING** and **REJECTED** assignments are filtered out

#### Implementation Details:
```kotlin
// Filter projects where user has temporaryApproverPhone match
val temporaryProjectsWithPhone = allProjects.filter { ... }

// Check status in temporaryApprovers subcollection
launch {
    val acceptedTemporaryProjects = mutableListOf<Project>()
    
    for (project in temporaryProjectsWithPhone) {
        // Query for ACCEPTED assignments only
        val tempApproversSnapshot = firestore
            .collection("projects")
            .document(project.id)
            .collection("temporaryApprovers")
            .whereEqualTo("approverPhone", userId)
            .whereEqualTo("isActive", true)
            .whereEqualTo("status", "ACCEPTED")
            .limit(1)
            .get()
            .await()
        
        if (!tempApproversSnapshot.isEmpty) {
            acceptedTemporaryProjects.add(project)
        }
    }
    
    trySend(acceptedTemporaryProjects)
}
```

### 2. Rejection Reason Dialog
**File:** `app/src/main/java/com/deeksha/avr/ui/view/approver/ApproverNotificationScreen.kt`

#### New UI Components:
1. **State Variables:**
   ```kotlin
   var showRejectionDialog by remember { mutableStateOf(false) }
   var rejectionReason by remember { mutableStateOf("") }
   var pendingRejectionProjectId by remember { mutableStateOf<String?>(null) }
   var pendingRejectionApproverId by remember { mutableStateOf<String?>(null) }
   ```

2. **Rejection Dialog:**
   - Shows when user clicks "Reject" button
   - Contains a multiline text field for rejection reason
   - Validates that reason is not empty before allowing rejection
   - Sends reason to production head

#### Dialog Features:
- **Title:** "Reason for Rejection"
- **Input:** Multiline text field (3-5 lines)
- **Validation:** Reject button disabled until reason is entered
- **Actions:**
  - **Reject:** Submits rejection with reason (red button)
  - **Cancel:** Closes dialog without action

### 3. Rejection Flow Enhancement
**File:** `app/src/main/java/com/deeksha/avr/repository/TemporaryApproverRepository.kt`

#### Updated Notification Logic:
```kotlin
val message = if (isAccepted) {
    "$approverName has accepted the temporary approver assignment for project '$projectName'."
} else {
    if (responseMessage.isNotEmpty()) {
        "$approverName has rejected the temporary approver assignment for project '$projectName'.\n\nReason: $responseMessage"
    } else {
        "$approverName has rejected the temporary approver assignment for project '$projectName'."
    }
}
```

## Data Flow

### Scenario 1: Assignment Creation
```
1. Production Head assigns temporary approver
2. Status set to "PENDING"
3. Notification sent to approver with accept/reject buttons
4. Project DOES NOT appear in approver's project list (PENDING)
```

### Scenario 2: Accept Assignment
```
1. Approver clicks "Accept" in notification
2. Status updated to "ACCEPTED"
3. Project NOW appears in approver's project list
4. Production head receives acceptance notification
5. Approver can now access the project
```

### Scenario 3: Reject Assignment  
```
1. Approver clicks "Reject" in notification
2. Rejection reason dialog appears
3. Approver enters reason and clicks "Reject"
4. Status updated to "REJECTED"
5. isActive set to false
6. temporaryApproverPhone removed from project
7. Production head receives rejection notification WITH REASON
8. Project remains inaccessible to approver
```

## Database Structure

### TemporaryApprover Document
```
projects/{projectId}/temporaryApprovers/{docId}
{
  approverPhone: String
  isActive: Boolean
  status: String  // "PENDING", "ACCEPTED", "REJECTED"
  responseMessage: String  // Rejection reason
  isAccepted: Boolean?
  acceptedAt: Timestamp?
  rejectedAt: Timestamp?
  ...
}
```

### Project Document
```
projects/{projectId}
{
  temporaryApproverPhone: String  // Only set for ACCEPTED assignments
  ...
}
```

## User Experience

### For Temporary Approver:

**Before Accepting:**
- ❌ Project NOT visible in project selection screen
- ✅ Notification visible with accept/reject buttons
- ✅ Can view assignment details in notification

**After Accepting:**
- ✅ Project appears in project selection screen
- ✅ Full access to project dashboard
- ✅ Can view expenses, approve/reject, etc.

**After Rejecting:**
- ❌ Project remains hidden
- ✅ Rejection reason dialog appears
- ✅ Must provide reason for rejection
- ✅ Notification marked as read after rejection

### For Production Head:

**After Assignment:**
- ✅ Sees "Pending" status in delegation management
- ✅ Can track assignment status

**After Acceptance:**
- ✅ Receives notification: "{Name} has accepted..."
- ✅ Can see approver in project's temporary approver list

**After Rejection:**
- ✅ Receives detailed notification with reason
- ✅ Format: "{Name} has rejected...\n\nReason: {rejection_reason}"
- ✅ Can reassign to different approver

## Testing Checklist

### Access Control Testing
- [ ] ✅ PENDING assignment does NOT show in project list
- [ ] ✅ ACCEPTED assignment DOES show in project list
- [ ] ✅ REJECTED assignment does NOT show in project list
- [ ] ✅ Expired assignments do NOT show in project list

### Rejection Reason Testing
- [ ] ✅ Reject button shows rejection reason dialog
- [ ] ✅ Dialog prevents rejection without reason
- [ ] ✅ Rejection reason is sent to production head
- [ ] ✅ Rejection reason appears in notification

### Integration Testing
- [ ] ✅ Accept → Project appears → Full access granted
- [ ] ✅ Reject → Dialog → Reason → Notification → No access
- [ ] ✅ Multiple assignments handled correctly
- [ ] ✅ Notification badge updates correctly

## Edge Cases Handled

1. **Empty Rejection Reason:**
   - Reject button disabled until reason is entered
   - Validation prevents empty submissions

2. **Multiple Pending Assignments:**
   - Each assignment checked individually
   - Only ACCEPTED ones shown in project list

3. **Assignment State Changes:**
   - Real-time updates via Firestore listeners
   - Project list refreshes automatically

4. **Network Failures:**
   - Proper error handling in async operations
   - Logs for debugging

## Security Considerations

1. **Access Control:**
   - Server-side validation in Firestore rules should match
   - Status must be "ACCEPTED" to grant access

2. **Data Integrity:**
   - Status changes tracked with timestamps
   - Audit trail maintained with acceptedAt/rejectedAt

3. **User Intent:**
   - Explicit reason required for rejection
   - Prevents accidental rejections

## Files Modified

1. **ProjectRepository.kt**
   - Updated `getTemporaryApproverProjects()` to filter by status
   - Added coroutine to check status in subcollection

2. **ApproverNotificationScreen.kt**
   - Added rejection reason dialog
   - Updated reject button to show dialog
   - Added state management for dialog

3. **TemporaryApproverRepository.kt**
   - Updated notification message to include rejection reason
   - Enhanced notification formatting

## Usage Example

### For Approver:
```kotlin
// When rejecting an assignment
1. Click "Reject" button
2. Dialog appears
3. Enter: "I am currently assigned to another project and cannot take on additional responsibilities."
4. Click "Reject"
5. Notification sent to production head with reason
```

### For Production Head:
```kotlin
// Notification received:
Title: "Temporary Approver Assignment Rejected"
Message: "John Doe has rejected the temporary approver assignment for project 'Web Project'.

Reason: I am currently assigned to another project and cannot take on additional responsibilities."
```

## Benefits

1. **Clear Access Control:**
   - Approvers only see projects they've accepted
   - Prevents confusion about accessible projects

2. **Better Communication:**
   - Production heads know why assignments were rejected
   - Can make informed decisions about reassignment

3. **Improved UX:**
   - Clean project list (no pending items)
   - Explicit acceptance required for access

4. **Audit Trail:**
   - All rejections have reasons
   - Timestamps for all status changes

5. **Data Integrity:**
   - Status-based filtering ensures accuracy
   - Real-time updates prevent stale data

## Future Enhancements

1. **Rejection Categories:**
   - Predefined rejection reasons (dropdown)
   - Custom reason as optional additional field

2. **Reassignment Workflow:**
   - Quick reassign from rejection notification
   - Suggest alternative approvers

3. **Assignment History:**
   - View all past assignments (accepted/rejected)
   - Analytics on rejection reasons

4. **Reminder System:**
   - Remind approvers of pending assignments
   - Auto-expire after X days

5. **Bulk Operations:**
   - Accept/reject multiple assignments
   - Batch reason entry for similar rejections



