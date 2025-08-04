# Project-Based Notification Filtering Implementation

## Overview

This document describes the implementation of project-based notification filtering that ensures users with `UserRole.USER` only receive notifications for projects specifically allotted to them. This addresses the requirement: *"user role should get only team member assignment of that particular user and the approvals and rejection notification of that project alloted to him"*.

## Key Features

### 1. Project Assignment Validation
- **USER Role**: Only receives notifications for projects in their `assignedProjects` list
- **APPROVER Role**: Receives notifications for projects where they are listed in `approverIds` or as `managerId`
- **PRODUCTION_HEAD Role**: Receives notifications for projects where they are listed in `productionHeadIds`
- **ADMIN Role**: Receives notifications for all projects (full access)

### 2. Notification Types with Project Filtering

#### Expense Approval/Rejection Notifications
- **Target**: User who submitted the expense
- **Filtering**: Only sends notification if the user is assigned to the project
- **Implementation**: `NotificationService.sendExpenseStatusNotification()`

#### Project Assignment Notifications
- **Target**: Specific user being assigned to a project
- **Filtering**: Only sends to the assigned user
- **Implementation**: `NotificationService.sendProjectAssignmentNotification()`

#### Expense Submission Notifications
- **Target**: Project approvers and production heads
- **Filtering**: Only sends to users assigned to the specific project
- **Implementation**: `NotificationService.sendExpenseSubmissionNotification()`

#### Pending Approval Notifications
- **Target**: Production heads assigned to the project
- **Filtering**: Only sends to production heads assigned to the specific project
- **Implementation**: `NotificationService.sendPendingApprovalNotification()`

## Implementation Details

### 1. Project Assignment Check Function

```kotlin
private fun isUserAssignedToProject(user: User, projectId: String): Boolean {
    return when (user.role) {
        UserRole.USER -> {
            // For USER role, check if the project is in their assignedProjects list
            user.assignedProjects.contains(projectId)
        }
        UserRole.APPROVER -> {
            // For APPROVER role, check if they are in the project's approverIds
            val project = projectRepository.getProjectById(projectId)
            project?.approverIds?.contains(user.uid) == true || project?.managerId == user.uid
        }
        UserRole.PRODUCTION_HEAD -> {
            // For PRODUCTION_HEAD role, check if they are in the project's productionHeadIds
            val project = projectRepository.getProjectById(projectId)
            project?.productionHeadIds?.contains(user.uid) == true
        }
        UserRole.ADMIN -> {
            // ADMIN can see all projects
            true
        }
    }
}
```

### 2. Enhanced FCM Notification Service

The `FCMNotificationService` now includes:

- **Project assignment validation** in `sendNotificationToUser()`
- **New method** `sendNotificationToUsersByRoleWithProjectFilter()` for role-based notifications with project filtering
- **Enhanced logging** to track which users are filtered out and why

### 3. Database Schema Requirements

#### User Collection
```json
{
  "uid": "user123",
  "name": "John Doe",
  "role": "USER",
  "assignedProjects": ["project1", "project2", "project3"],
  "deviceInfo": {
    "fcmToken": "fcm_token_here",
    "deviceId": "device_id",
    "deviceModel": "Samsung Galaxy S21",
    "osVersion": "Android 12",
    "appVersion": "1.0.0",
    "lastLoginAt": 1640995200000,
    "isOnline": true
  },
  "notificationPreferences": {
    "pushNotifications": true,
    "expenseSubmitted": true,
    "expenseApproved": true,
    "expenseRejected": true,
    "projectAssignment": true,
    "pendingApprovals": true
  }
}
```

#### Project Collection
```json
{
  "id": "project1",
  "name": "Mobile App Development",
  "managerId": "manager123",
  "approverIds": ["approver1", "approver2"],
  "productionHeadIds": ["prodhead1"],
  "teamMembers": ["user1", "user2", "user3"],
  "status": "ACTIVE"
}
```

## Usage Examples

### 1. Sending Expense Status Notification

```kotlin
// This will only send notification if the user is assigned to the project
notificationService.sendExpenseStatusNotification(
    expenseId = "expense123",
    projectId = "project1",
    submittedByUserId = "user123",
    isApproved = true,
    amount = 1500.0,
    reviewerName = "Manager Name"
)
```

### 2. Sending Project Assignment Notification

```kotlin
// This will only send to the specific user being assigned
notificationService.sendProjectAssignmentNotification(
    projectId = "project1",
    userId = "user123",
    assignedRole = "Team Member"
)
```

### 3. Sending FCM Notifications with Project Filtering

```kotlin
// This will only send to USER role users who are assigned to the project
fcmNotificationService.sendNotificationToUsersByRoleWithProjectFilter(
    role = UserRole.USER,
    title = "New Project Update",
    message = "Project has been updated",
    projectId = "project1",
    notificationType = "PROJECT_UPDATE"
)
```

## Security Benefits

### 1. Data Privacy
- Users only receive notifications for projects they have access to
- Prevents information leakage to unauthorized users
- Maintains project confidentiality

### 2. Access Control
- Role-based notification filtering
- Project-specific access validation
- Prevents unauthorized notification delivery

### 3. Audit Trail
- Comprehensive logging of notification attempts
- Tracking of filtered notifications
- Debugging information for troubleshooting

## Logging and Monitoring

### 1. Notification Filtering Logs
```
⚠️ User John Doe (user123) is not assigned to project project1
⚠️ Skipping notification to prevent unauthorized access
```

### 2. FCM Token Validation Logs
```
⚠️ User user123 has no FCM token
⚠️ User John Doe is not assigned to project project1
```

### 3. Success Logs
```
✅ User is assigned to project Mobile App Development
✅ FCM notification sent to expense submitter
✅ Sent project assignment notification: notification_id
```

## Configuration

### 1. User Project Assignment
Users must have their `assignedProjects` field properly populated in Firestore:

```kotlin
// Example: Assigning a user to projects
val user = User(
    uid = "user123",
    name = "John Doe",
    role = UserRole.USER,
    assignedProjects = listOf("project1", "project2")
)
```

### 2. Project Configuration
Projects must have proper role assignments:

```kotlin
// Example: Configuring project roles
val project = Project(
    id = "project1",
    name = "Mobile App Development",
    managerId = "manager123",
    approverIds = listOf("approver1", "approver2"),
    productionHeadIds = listOf("prodhead1"),
    teamMembers = listOf("user1", "user2", "user3")
)
```

## Testing

### 1. Unit Tests
- Test `isUserAssignedToProject()` function with different user roles
- Test notification filtering logic
- Test FCM token validation

### 2. Integration Tests
- Test end-to-end notification flow
- Test project assignment validation
- Test role-based filtering

### 3. Manual Testing
- Verify notifications are only sent to assigned users
- Verify unauthorized users don't receive notifications
- Verify logging provides adequate debugging information

## Troubleshooting

### 1. Common Issues

#### Users Not Receiving Notifications
- Check if user is assigned to the project
- Verify FCM token is present and valid
- Check notification preferences are enabled

#### Unauthorized Notifications
- Verify project assignment logic
- Check user role and project configuration
- Review notification filtering logs

### 2. Debug Steps
1. Check user's `assignedProjects` list
2. Verify project's role assignments
3. Review notification service logs
4. Check FCM token availability
5. Validate notification preferences

## Future Enhancements

### 1. Advanced Filtering
- Department-based filtering
- Category-based filtering
- Time-based filtering

### 2. Notification Preferences
- Granular notification settings per project
- Custom notification schedules
- Notification priority levels

### 3. Analytics
- Notification delivery tracking
- User engagement metrics
- Project-specific notification statistics

## Conclusion

The project-based notification filtering implementation ensures that:

1. **USER role users** only receive notifications for projects they are assigned to
2. **Security is maintained** by preventing unauthorized access to project information
3. **Scalability is achieved** through efficient filtering and validation
4. **Debugging is facilitated** through comprehensive logging
5. **Flexibility is provided** for future enhancements

This implementation addresses the specific requirement for user role notifications to be strictly limited to their assigned projects while maintaining the existing functionality for other user roles. 