# Temporary Approver Notification Implementation

## Overview
Implemented a complete notification system for temporary approver assignments. When a Production Head assigns a temporary approver to a project, the temporary approver receives a notification with accept/reject options. Based on their response, they either get access to the project or are completely removed.

## Implementation Details

### 1. Notification Service Enhancement
**File:** `app/src/main/java/com/deeksha/avr/service/NotificationService.kt`

Added a new method `sendTemporaryApproverAssignmentNotification()` that:
- Fetches project details from Firestore
- Formats start and expiry dates for better readability
- Creates a notification with:
  - `recipientId`: Phone number of the temporary approver
  - `recipientRole`: "APPROVER"
  - `type`: `NotificationType.TEMPORARY_APPROVER_ASSIGNMENT`
  - `actionRequired`: `true` (enables accept/reject buttons in UI)
  - `relatedId`: Approver ID (used for action handling)
  - Clear message explaining the assignment with dates

### 2. Repository Update
**File:** `app/src/main/java/com/deeksha/avr/repository/TemporaryApproverRepository.kt`

Updated `createTemporaryApprover()` method to:
- Fetch production head details from the project document
- Create temporary approver record with status "PENDING"
- Automatically send notification with accept/reject actions
- Log all operations for debugging

### 3. Notification UI
**File:** `app/src/main/java/com/deeksha/avr/ui/view/approver/ApproverNotificationScreen.kt`

The UI already had support for accept/reject buttons (lines 336-395):
- Shows **Accept** (green) and **Reject** (red) buttons for temporary approver assignment notifications
- Buttons only appear when:
  - Notification type is `TEMPORARY_APPROVER_ASSIGNMENT`
  - `actionRequired` is `true`
  - Notification is unread
- Clicking either button marks the notification as read

### 4. ViewModel Methods
**File:** `app/src/main/java/com/deeksha/avr/viewmodel/TemporaryApproverViewModel.kt`

Already contains the required methods (lines 219-294):

#### Accept Assignment
```kotlin
fun acceptTemporaryApproverAssignment(
    projectId: String,
    approverId: String,
    responseMessage: String = ""
)
```
- Calls repository method to update status to "ACCEPTED"
- Updates `isAccepted` to `true`
- Records `acceptedAt` timestamp
- Sends notification to production head about acceptance
- Cleans up any other pending assignments

#### Reject Assignment
```kotlin
fun rejectTemporaryApproverAssignment(
    projectId: String,
    approverId: String,
    responseMessage: String = ""
)
```
- Calls repository method to update status to "REJECTED"
- Updates `isAccepted` to `false` and `isActive` to `false`
- Records `rejectedAt` timestamp
- Removes temporary approver phone from project document
- Sends notification to production head about rejection

## Data Flow

### Assignment Flow
```
1. Production Head → DelegationScreen
2. Selects approver and dates
3. Calls createTemporaryApprover()
4. Repository creates record with status "PENDING"
5. NotificationService sends notification with accept/reject actions
6. Temporary approver receives notification
```

### Accept Flow
```
1. Temporary Approver clicks "Accept"
2. UI calls acceptTemporaryApproverAssignment()
3. Repository updates:
   - status → "ACCEPTED"
   - isAccepted → true
   - acceptedAt → current timestamp
4. Cleanup: Remove any other pending assignments
5. Send notification to production head
6. Temporary approver gets access to project
```

### Reject Flow
```
1. Temporary Approver clicks "Reject"
2. UI calls rejectTemporaryApproverAssignment()
3. Repository updates:
   - status → "REJECTED"
   - isAccepted → false
   - isActive → false
   - rejectedAt → current timestamp
4. Remove temporaryApproverPhone from project
5. Send notification to production head
6. Temporary approver is completely removed
```

## Database Structure

### Temporary Approver Document
```
projects/{projectId}/temporaryApprovers/{docId}
{
  id: String
  projectId: String
  approverId: String
  approverName: String
  approverPhone: String
  startDate: Timestamp
  expiringDate: Timestamp?
  isActive: Boolean
  assignedBy: String
  assignedByName: String
  createdAt: Timestamp
  updatedAt: Timestamp
  status: String  // "PENDING", "ACCEPTED", "REJECTED"
  isAccepted: Boolean?
  acceptedAt: Timestamp?
  rejectedAt: Timestamp?
  responseMessage: String
}
```

### Project Document
```
projects/{projectId}
{
  ...
  temporaryApproverPhone: String  // Set when assigned, removed when rejected
  ...
}
```

### Notification Document
```
notifications/{notificationId}
{
  recipientId: String  // Phone number of temporary approver
  recipientRole: String  // "APPROVER"
  title: String
  message: String
  type: NotificationType.TEMPORARY_APPROVER_ASSIGNMENT
  projectId: String
  projectName: String
  relatedId: String  // Approver ID
  isRead: Boolean
  createdAt: Timestamp
  actionRequired: Boolean  // true for accept/reject buttons
  navigationTarget: String
}
```

## Key Features

### 1. Accept/Reject Options
- Clear UI with green "Accept" and red "Reject" buttons
- Buttons only visible for unread temporary approver assignment notifications
- Immediate feedback with success/error messages

### 2. Access Control
- **Accept**: Temporary approver gets full access to the project
- **Reject**: Temporary approver is completely removed and cannot access the project

### 3. Notifications to Production Head
- Production head receives notification when assignment is accepted
- Production head receives notification when assignment is rejected
- Includes approver name and response message

### 4. Audit Trail
- All actions are logged with timestamps
- Status changes are tracked (PENDING → ACCEPTED/REJECTED)
- Response messages can be stored for reference

### 5. Cleanup
- Pending assignments are automatically cleaned up when one is accepted
- Rejected assignments are marked inactive and removed from project
- Expired assignments are automatically deactivated

## Testing Checklist

### Manual Testing Steps

1. **Assignment Creation**
   - ✓ Login as Production Head
   - ✓ Navigate to project delegation screen
   - ✓ Select a temporary approver and set dates
   - ✓ Confirm assignment creation
   - ✓ Verify notification is sent

2. **Accept Flow**
   - ✓ Login as the assigned temporary approver
   - ✓ Open notifications screen
   - ✓ Verify notification appears with accept/reject buttons
   - ✓ Click "Accept" button
   - ✓ Verify notification is marked as read
   - ✓ Verify access to project is granted
   - ✓ Verify production head receives acceptance notification

3. **Reject Flow**
   - ✓ Login as the assigned temporary approver
   - ✓ Open notifications screen
   - ✓ Click "Reject" button
   - ✓ Verify notification is marked as read
   - ✓ Verify access to project is denied
   - ✓ Verify approver is removed from project
   - ✓ Verify production head receives rejection notification

4. **Edge Cases**
   - ✓ Multiple pending assignments (only one should remain after accept)
   - ✓ Expired assignments (should be automatically deactivated)
   - ✓ Network failures (should show error messages)
   - ✓ Invalid project/approver IDs (should fail gracefully)

## Files Modified

1. **NotificationService.kt** - Added `sendTemporaryApproverAssignmentNotification()` method
2. **TemporaryApproverRepository.kt** - Updated `createTemporaryApprover()` to send notification
3. **TemporaryApproverViewModel.kt** - Already had accept/reject methods (verified)
4. **ApproverNotificationScreen.kt** - Already had UI for accept/reject buttons (verified)

## Usage Example

### Production Head assigns temporary approver:
```kotlin
temporaryApproverViewModel.createTemporaryApprover(
    projectId = "projectId",
    approverId = "approverId",
    approverName = "John Doe",
    approverPhone = "9876543210",
    startDate = Timestamp(startDate),
    expiringDate = Timestamp(endDate)
)
```

### Temporary approver accepts:
```kotlin
temporaryApproverViewModel.acceptTemporaryApproverAssignment(
    projectId = "projectId",
    approverId = "approverId"
)
```

### Temporary approver rejects:
```kotlin
temporaryApproverViewModel.rejectTemporaryApproverAssignment(
    projectId = "projectId",
    approverId = "approverId"
)
```

## Benefits

1. **Clear Communication**: Temporary approvers know exactly what they're being assigned to
2. **User Control**: Approvers can accept or reject assignments
3. **Automatic Access Management**: Access is granted/revoked based on response
4. **Audit Trail**: All actions are tracked with timestamps and status changes
5. **Production Head Visibility**: Production head is notified of all responses
6. **Clean Database**: Rejected and expired assignments are properly cleaned up

## Future Enhancements

1. Add response message field for approvers to explain rejection
2. Add reminder notifications for pending assignments
3. Add bulk accept/reject functionality
4. Add assignment expiry warnings
5. Add assignment transfer functionality


