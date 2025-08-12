# Project Change Notification System

## Overview
This document describes the notification system implemented for the Production Head module that automatically sends notifications to approvers and users when project changes occur.

## Features Implemented

### 1. New Notification Types
- **PROJECT_CHANGED**: Sent when project details are modified
- **PROJECT_ASSIGNMENT**: Sent when users are assigned to projects (existing)

### 2. Notification Triggers

#### Project Creation
- Notifications sent to:
  - Project Manager (approver)
  - Team Members
  - All assigned approvers
- Message: "You have been assigned as [Role] to the newly created project: [ProjectName]"

#### Project Updates
- Notifications sent when any of these fields change:
  - Project name
  - Description
  - Budget
  - Start/End dates
  - Project manager
  - Team members
  - Department budgets
  - Categories
- Recipients:
  - Project Manager
  - All approvers
  - All team members
- Message: "Project '[ProjectName]' has been updated by [ChangedBy]. [ChangeDescription]"

#### Project Status Changes
- Notifications sent when project status changes to:
  - ACTIVE
  - PAUSED
  - COMPLETED
  - CANCELLED
- Recipients: Same as project updates
- Message: Status-specific messages like "Project has been activated"

#### Budget Changes (Special Handling)
- Additional notifications specifically for budget modifications
- Sent to all approvers and project manager (if approver)
- Message: "Budget changed from ₹[OldAmount] to ₹[NewAmount]"

### 3. Implementation Details

#### Files Modified
1. **Notification.kt** - Added PROJECT_CHANGED notification type
2. **NotificationRepository.kt** - Added methods:
   - `createProjectChangeNotification()`
   - `createNewProjectNotification()`
3. **ProductionHeadViewModel.kt** - Added notification logic:
   - `sendProjectChangeNotifications()`
   - `sendNewProjectNotifications()`
   - `sendProjectStatusChangeNotifications()`
   - `sendBudgetChangeNotifications()`
   - `updateProjectStatus()`
4. **ProjectRepository.kt** - Added `updateProjectStatus()` method

#### Notification Content
- **Title**: Descriptive titles like "Project Updated", "New Project Created"
- **Message**: Detailed change descriptions with context
- **Action Required**: Set to false for changes, true for new assignments
- **Navigation Target**: Direct links to relevant project dashboards

### 4. User Experience

#### For Approvers
- Receive notifications for all project changes
- Special emphasis on budget modifications
- Direct navigation to project dashboards
- Real-time updates via Firebase

#### For Team Members
- Notified of project changes affecting their work
- Updated project information
- Access to modified project details

#### For Production Heads
- Can track all notifications sent
- Comprehensive change logging
- Audit trail of modifications

### 5. Technical Implementation

#### Change Detection
- Compares original vs. updated project data
- Identifies specific field changes
- Generates human-readable change descriptions
- Skips notifications if no significant changes detected

#### Notification Delivery
- Uses Firebase Cloud Firestore
- Real-time updates via snapshot listeners
- Automatic recipient role detection
- Error handling and logging

#### Performance Considerations
- Batch notification creation
- Efficient user role lookups
- Minimal database queries
- Comprehensive logging for debugging

### 6. Usage Examples

#### Creating a New Project
```kotlin
// Automatically sends notifications to all assigned users
createProject(projectName, description, startDate, endDate, budget, managerId, teamMembers, deptBudgets)
```

#### Updating Project Details
```kotlin
// Automatically detects changes and sends notifications
updateProject(projectId, projectName, description, startDate, endDate, budget, managerId, teamMembers, deptBudgets)
```

#### Changing Project Status
```kotlin
// Sends status-specific notifications
updateProjectStatus(projectId, "COMPLETED")
```

### 7. Benefits

1. **Transparency**: All stakeholders are informed of project changes
2. **Accountability**: Clear tracking of who made what changes
3. **Efficiency**: Automatic notifications reduce manual communication
4. **Compliance**: Audit trail for project modifications
5. **User Experience**: Direct navigation to relevant project information

### 8. Future Enhancements

- Email notifications for critical changes
- Push notifications for mobile users
- Notification preferences and filtering
- Change approval workflows
- Integration with external project management tools

## Conclusion
The notification system provides comprehensive coverage of project changes, ensuring all stakeholders are informed in real-time. The implementation is robust, efficient, and provides a solid foundation for future enhancements.


