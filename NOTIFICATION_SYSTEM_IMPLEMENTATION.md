# Role-Based Notification System Implementation

## Overview

This document describes the comprehensive notification system implemented for the AVR (Approval and Verification Request) app, which provides role-based, device-specific notifications for all user flows.

## Architecture

### 1. **Enhanced User Model**
- **Device Information**: FCM token, device ID, model, OS version, app version, online status
- **Notification Preferences**: Granular control over different notification types
- **Role-Based Access**: USER, APPROVER, PRODUCTION_HEAD, ADMIN

### 2. **Service Layer**
- **NotificationService**: Handles in-app notifications and coordinates with device notifications
- **DeviceNotificationService**: Manages FCM tokens and device-specific notifications
- **FCMService**: Handles incoming FCM messages and local notification display

### 3. **Repository Layer**
- **AuthRepository**: Enhanced with device info management and role-based user queries
- **NotificationRepository**: Manages in-app notifications in Firestore

## Notification Flows

### **User Flow (Expense Submission)**
1. **User submits expense** → **NotificationService.sendExpenseSubmissionNotification()**
2. **Targets**: All APPROVER and PRODUCTION_HEAD users assigned to the project
3. **Notification Types**:
   - **In-App**: Stored in Firestore, visible in notification tabs
   - **Device**: FCM push notification with clickable navigation
4. **Navigation**: Taps navigate to pending approvals for the specific project

### **Approver/Production Head Flow (Expense Review)**
1. **Approver reviews expense** → **NotificationService.sendExpenseStatusNotification()**
2. **Targets**: The USER who submitted the expense
3. **Notification Types**:
   - **In-App**: Stored in Firestore, visible in user's notification tab
   - **Device**: FCM push notification with clickable navigation
4. **Navigation**: Taps navigate to expense list for the specific project

## Key Features

### **1. Role-Based Filtering**
```kotlin
private fun shouldSendNotificationToUser(user: User, notificationType: NotificationType): Boolean {
    return when (notificationType) {
        NotificationType.EXPENSE_SUBMITTED -> {
            (user.role == UserRole.APPROVER || user.role == UserRole.PRODUCTION_HEAD) &&
            user.notificationPreferences.expenseSubmitted
        }
        NotificationType.EXPENSE_APPROVED -> {
            user.role == UserRole.USER &&
            user.notificationPreferences.expenseApproved
        }
        // ... other types
    }
}
```

### **2. Device Information Collection**
```kotlin
suspend fun collectDeviceInfo(context: Context): DeviceInfo {
    val fcmToken = FirebaseMessaging.getInstance().token.await()
    return DeviceInfo(
        fcmToken = fcmToken,
        deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID),
        deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
        osVersion = "Android ${Build.VERSION.RELEASE}",
        // ... other fields
    )
}
```

### **3. Notification Preferences**
Users can control:
- **Expense Submitted**: Notifications when expenses are submitted
- **Expense Approved**: Notifications when expenses are approved
- **Expense Rejected**: Notifications when expenses are rejected
- **Project Assignment**: Notifications for project assignments
- **Pending Approvals**: Notifications for pending approvals
- **Push Notifications**: Enable/disable device notifications
- **In-App Notifications**: Enable/disable in-app notifications

### **4. FCM Integration**
- **Token Management**: Automatic FCM token collection and storage
- **Role-Based Targeting**: Notifications sent only to relevant users
- **Clickable Navigation**: Notifications navigate to specific screens
- **Fallback Handling**: Graceful handling when FCM tokens are unavailable

## Implementation Details

### **Authentication Enhancement**
After successful authentication:
1. **Device Info Collection**: Automatically collects device information
2. **FCM Token Update**: Updates user's FCM token in Firebase
3. **Role Detection**: Determines user role for notification targeting
4. **Preference Loading**: Loads user's notification preferences

### **Notification Sending Process**
1. **Create Notification Object**: With recipient, type, and navigation target
2. **Role-Based Filtering**: Check if user should receive this notification type
3. **Preference Check**: Verify user has enabled this notification type
4. **In-App Storage**: Store notification in Firestore for in-app display
5. **Device Notification**: Send FCM notification if user has FCM token
6. **Navigation Setup**: Configure clickable navigation to specific screens

### **Error Handling**
- **Missing FCM Token**: Graceful fallback to in-app notifications only
- **User Not Found**: Logged and handled without breaking the flow
- **Network Issues**: Retry mechanisms and offline handling
- **Invalid Roles**: Fallback to global role-based notifications

## Testing the System

### **1. Test Expense Submission**
```bash
# Submit an expense from a USER account
# Expected: APPROVER and PRODUCTION_HEAD users receive notifications
# Check logs for: "✅ Sent device notification to approver: [userId]"
```

### **2. Test Expense Approval**
```bash
# Approve an expense from an APPROVER account
# Expected: The USER who submitted receives approval notification
# Check logs for: "✅ Successfully sent device expense status notification"
```

### **3. Test Device Information**
```bash
# Login with any user account
# Check logs for: "📱 Device info collected" and FCM token availability
```

## Configuration

### **Firebase Setup**
1. **FCM Configuration**: Ensure `google-services.json` is properly configured
2. **Firestore Rules**: Allow read/write access to notifications collection
3. **User Documents**: Must include device info and notification preferences

### **Project Configuration**
1. **Approver IDs**: Set `approverIds` array in project documents
2. **Production Head IDs**: Set `productionHeadIds` array in project documents
3. **Manager ID**: Set `managerId` for fallback approver assignment

## Troubleshooting

### **Common Issues**

1. **No Notifications Received**
   - Check if user has FCM token: `user.deviceInfo.fcmToken.isNotEmpty()`
   - Verify notification preferences are enabled
   - Check if user role matches notification target

2. **FCM Token Issues**
   - Ensure Google Play Services is available
   - Check Firebase project configuration
   - Verify `google-services.json` is up to date

3. **Role-Based Issues**
   - Verify user role in Firestore matches expected role
   - Check project's `approverIds` and `productionHeadIds` arrays
   - Ensure user is assigned to the project

### **Debug Logs**
The system provides comprehensive logging:
- `NotificationService`: Main notification logic
- `DeviceNotificationService`: Device-specific notifications
- `FCMService`: FCM message handling
- `AuthRepository`: User and device info management

## Future Enhancements

1. **Backend FCM API**: Implement server-side FCM sending for better reliability
2. **Notification History**: Add notification history and management
3. **Custom Sounds**: Role-specific notification sounds
4. **Batch Notifications**: Group multiple notifications for better UX
5. **Offline Support**: Queue notifications for offline users

## Conclusion

This notification system provides a robust, role-based, device-specific notification experience that ensures users receive relevant notifications based on their role and preferences, with proper navigation to the appropriate screens when notifications are tapped. 