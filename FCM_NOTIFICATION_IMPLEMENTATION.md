# FCM Notification System Implementation

## Overview

This document describes the implementation of Firebase Cloud Messaging (FCM) notifications in the AVR Expense Tracker app. The system uses stored FCM tokens from the users collection to send targeted push notifications for expense submissions and approvals.

## 🔧 Architecture

### 1. **FCM Token Storage**
- FCM tokens are stored in the `users` collection under each user's document
- Token is stored in the `deviceInfo.fcmToken` field
- Tokens are automatically updated when the app starts or when new tokens are generated

### 2. **Notification Flow**

#### **Expense Submission Flow:**
1. User submits an expense
2. System identifies project approvers and production heads
3. FCM notifications are sent to all relevant managers/approvers
4. In-app notifications are also created in Firestore

#### **Expense Approval/Rejection Flow:**
1. Manager approves/rejects an expense
2. FCM notification is sent to the user who submitted the expense
3. In-app notification is created in Firestore

## 📱 Implementation Details

### 1. **FCM Token Management**

#### **Token Collection:**
```kotlin
// In FCMService.kt
override fun onNewToken(token: String) {
    super.onNewToken(token)
    
    // Update FCM token in background
    CoroutineScope(Dispatchers.IO).launch {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Find user in Firestore and update FCM token
            val firestore = FirebaseFirestore.getInstance()
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("phone", currentUser.phoneNumber)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val document = querySnapshot.documents.first()
                firestore.collection("users")
                    .document(document.id)
                    .update("deviceInfo.fcmToken", token)
                    .await()
            }
        }
    }
}
```

#### **Token Retrieval:**
```kotlin
// In FCMNotificationService.kt
suspend fun sendNotificationToUser(userId: String, title: String, message: String): Result<Unit> {
    val user = authRepository.getUserById(userId)
    val fcmToken = user?.deviceInfo?.fcmToken
    
    if (fcmToken?.isNotEmpty() == true) {
        sendFCMNotification(fcmToken, title, message, projectId, expenseId, notificationType)
    }
}
```

### 2. **Notification Service Integration**

#### **Expense Submission Notifications:**
```kotlin
// In NotificationService.kt
suspend fun sendExpenseSubmissionNotification(...): Result<Unit> {
    // Get project approvers and production heads
    val approverIds = project.approverIds
    val productionHeadIds = project.productionHeadIds
    
    // Send FCM notifications
    fcmNotificationService.sendNotificationToUsers(
        userIds = approverIds,
        title = "New Expense Submitted",
        message = "New expense of ₹$amount submitted by $submittedBy in ${project.name}",
        projectId = projectId,
        expenseId = expenseId,
        notificationType = "EXPENSE_SUBMITTED"
    )
}
```

#### **Expense Status Notifications:**
```kotlin
// In NotificationService.kt
suspend fun sendExpenseStatusNotification(...): Result<Unit> {
    // Send FCM notification to expense submitter
    fcmNotificationService.sendNotificationToUser(
        userId = submittedByUserId,
        title = if (isApproved) "✅ Expense Approved" else "❌ Expense Rejected",
        message = notificationMessage,
        projectId = projectId,
        expenseId = expenseId,
        notificationType = if (isApproved) "EXPENSE_APPROVED" else "EXPENSE_REJECTED"
    )
}
```

### 3. **FCM Message Structure**

#### **Notification Payload:**
```json
{
  "to": "FCM_TOKEN_HERE",
  "notification": {
    "title": "New Expense Submitted",
    "body": "New expense of ₹500.00 submitted by John Doe in Project Alpha",
    "sound": "default",
    "priority": "high"
  },
  "data": {
    "title": "New Expense Submitted",
    "message": "New expense of ₹500.00 submitted by John Doe in Project Alpha",
    "projectId": "project_123",
    "expenseId": "expense_456",
    "type": "EXPENSE_SUBMITTED",
    "click_action": "FLUTTER_NOTIFICATION_CLICK"
  },
  "priority": "high"
}
```

## 🚀 Usage Examples

### 1. **Sending Notification to Specific User**
```kotlin
fcmNotificationService.sendNotificationToUser(
    userId = "user_123",
    title = "Expense Approved",
    message = "Your expense has been approved by Manager",
    projectId = "project_456",
    expenseId = "expense_789",
    notificationType = "EXPENSE_APPROVED"
)
```

### 2. **Sending Notification to Multiple Users**
```kotlin
fcmNotificationService.sendNotificationToUsers(
    userIds = listOf("user_1", "user_2", "user_3"),
    title = "New Expense Submitted",
    message = "New expense requires your approval",
    projectId = "project_123",
    expenseId = "expense_456",
    notificationType = "EXPENSE_SUBMITTED"
)
```

### 3. **Sending Notification to Users by Role**
```kotlin
fcmNotificationService.sendNotificationToUsersByRole(
    role = UserRole.APPROVER,
    title = "Pending Approvals",
    message = "You have 5 expenses awaiting approval",
    notificationType = "PENDING_APPROVAL"
)
```

## 🔧 Configuration

### 1. **FCM Server Key**
Replace `YOUR_FCM_SERVER_KEY` in `FCMNotificationService.kt` with your actual FCM server key from Firebase Console.

### 2. **Dependency Injection**
The FCM notification services are automatically injected via Hilt:
```kotlin
@Provides
@Singleton
fun provideFCMNotificationService(authRepository: AuthRepository): FCMNotificationService {
    return FCMNotificationService(authRepository)
}
```

## 📊 Database Schema

### **Users Collection:**
```json
{
  "uid": "user_123",
  "name": "John Doe",
  "phone": "9876543210",
  "role": "USER",
  "deviceInfo": {
    "fcmToken": "FCM_TOKEN_HERE",
    "deviceId": "DEVICE_ID",
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

## 🎯 Notification Types

### **Supported Notification Types:**
- `EXPENSE_SUBMITTED` - When user submits an expense
- `EXPENSE_APPROVED` - When manager approves an expense
- `EXPENSE_REJECTED` - When manager rejects an expense
- `PROJECT_ASSIGNMENT` - When user is assigned to a project
- `PENDING_APPROVAL` - When there are pending approvals

## 🔍 Troubleshooting

### **Common Issues:**

1. **FCM Token Not Found:**
   - Check if user has logged in recently
   - Verify FCM token is stored in Firestore
   - Check device internet connection

2. **Notifications Not Received:**
   - Verify FCM server key is correct
   - Check notification permissions on device
   - Ensure app is not in battery optimization mode

3. **Token Update Failures:**
   - Check Firebase Auth connection
   - Verify user exists in Firestore
   - Check network connectivity

## 📈 Performance Considerations

### **Optimizations:**
- FCM tokens are cached locally
- Notifications are sent in batches for multiple users
- Failed notifications are logged but don't block the main flow
- Network requests are made on background threads

### **Rate Limiting:**
- FCM has rate limits (1000 messages per second per project)
- Implement exponential backoff for failed requests
- Monitor FCM quota usage in Firebase Console

## 🔐 Security

### **Best Practices:**
- FCM server key should be kept secure
- Validate user permissions before sending notifications
- Sanitize notification content to prevent injection attacks
- Use HTTPS for all FCM requests

## 📝 Testing

### **Test Scenarios:**
1. **Expense Submission:** Verify approvers receive notifications
2. **Expense Approval:** Verify submitter receives notification
3. **Token Updates:** Verify FCM token is updated on app restart
4. **Offline Behavior:** Verify notifications are queued when offline
5. **Multiple Devices:** Verify notifications work across devices

### **Test Commands:**
```bash
# Test FCM token update
adb shell am start -n com.deeksha.avr/.MainActivity

# Check FCM logs
adb logcat | grep FCMNotificationService

# Test notification delivery
adb shell am broadcast -a com.google.firebase.MESSAGING_EVENT
```

## 🚀 Deployment

### **Production Checklist:**
- [ ] Replace FCM server key with production key
- [ ] Test notifications on real devices
- [ ] Verify notification permissions
- [ ] Monitor FCM quota usage
- [ ] Set up error monitoring
- [ ] Test offline scenarios
- [ ] Verify notification content and formatting

## 📚 Additional Resources

- [Firebase Cloud Messaging Documentation](https://firebase.google.com/docs/cloud-messaging)
- [FCM HTTP v1 API Reference](https://firebase.google.com/docs/reference/fcm/rest/v1/projects.messages)
- [Android Notification Best Practices](https://developer.android.com/guide/topics/ui/notifiers/notifications)
- [Firebase Console](https://console.firebase.google.com/)

---

**Note:** This implementation provides a robust FCM notification system that integrates seamlessly with the existing expense tracking functionality while maintaining clean separation of concerns and proper error handling. 