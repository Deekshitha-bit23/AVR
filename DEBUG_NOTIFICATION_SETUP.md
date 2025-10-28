# Debug Notification Setup Guide

## 🔍 Quick Debug Commands

### 1. Check Your Firebase Data Structure

**In Firebase Console → Firestore Database:**

#### Check AVR 2 Project:
```
Collection: projects
Document: [AVR 2 project ID]
Required Fields:
✅ name: "AVR 2"
✅ approverIds: ["approver_uid_here"]  ← CRITICAL!
✅ productionHeadIds: ["production_head_uid_here"]
✅ managerId: "manager_uid_here"
```

#### Check Your Users:
```
Collection: users
Documents:
1. User Phone:
   ✅ role: "USER"
   ✅ uid: "user_uid"
   ✅ deviceInfo: { fcmToken: "token", deviceId: "id", ... }

2. Approver Phone:
   ✅ role: "APPROVER"
   ✅ uid: "approver_uid" 
   ✅ deviceInfo: { fcmToken: "token", deviceId: "id", ... }

3. Production Head Phone:
   ✅ role: "PRODUCTION_HEAD"
   ✅ uid: "production_head_uid"
   ✅ deviceInfo: { fcmToken: "token", deviceId: "id", ... }
```

### 2. Test Notification Flow

#### Step 1: Login with User Phone
```
1. Enter user phone number
2. Click "Development Login (Skip OTP)"
3. Should navigate to Project Selection
4. Select AVR 2 project
5. Navigate to Add Expense
```

#### Step 2: Submit Expense
```
1. Fill expense form
2. Click Submit
3. Check logcat for:
   ✅ "Sending expense submission notifications"
   ✅ "Project approverIds: [approver_uid]"
   ✅ "Successfully sent expense submission notifications"
```

#### Step 3: Check Approver Device
```
1. Login with approver phone
2. Check notification icon (bell) for new notification
3. Click notification
4. Should navigate to AVR 2 project pending approvals
```

### 3. Common Issues & Fixes

#### Issue: "No approvers found for project"
**Fix:**
```javascript
// In Firebase Console, update AVR 2 project:
approverIds: ["your_approver_uid_here"]
```

#### Issue: "User not found or not approver"
**Fix:**
```javascript
// In Firebase Console, check approver user:
role: "APPROVER"  // Must be exactly this
uid: "correct_uid_here"
```

#### Issue: "No FCM token for user"
**Fix:**
```javascript
// In Firebase Console, check user deviceInfo:
deviceInfo: {
  fcmToken: "token_here",  // Should not be empty
  deviceId: "device_id",
  isOnline: true
}
```

### 4. Logcat Filter Commands

**Filter for notification logs:**
```
adb logcat | grep -E "(NotificationService|ExpenseViewModel|DeviceNotificationService)"
```

**Filter for authentication logs:**
```
adb logcat | grep -E "(AuthViewModel|AuthRepository)"
```

**Filter for all app logs:**
```
adb logcat | grep "com.deeksha.avr"
```

### 5. Expected Log Output

**When expense is submitted successfully:**
```
ExpenseViewModel: 🔄 Sending expense submission notifications for expense: expense_id
ExpenseViewModel: 📋 Project ID: project_id
ExpenseViewModel: 📋 Expense details: User Name - ₹1000 - Category
NotificationService: 🔄 Sending expense submission notification for project: project_id
NotificationService: 📋 Project: AVR 2
NotificationService: 📋 Project approverIds: [approver_uid]
NotificationService: ✅ Added approver: Approver Name (approver_uid)
NotificationService: ✅ Sent in-app notification to approver approver_uid
DeviceNotificationService: 🔄 Sending device notification to user: approver_uid
DeviceNotificationService: ✅ Device notification sent successfully to: Approver Name
ExpenseViewModel: ✅ Successfully sent expense submission notifications
```

### 6. Manual Test Commands

**Test notification creation manually:**
```kotlin
// In your app, you can test by calling:
notificationService.sendExpenseSubmissionNotification(
    projectId = "your_avr2_project_id",
    expenseId = "test_expense_id",
    submittedBy = "Test User",
    amount = 1000.0,
    category = "Test Category"
)
```

**Test device notification manually:**
```kotlin
// Test device notification for specific user:
deviceNotificationService.sendDeviceNotification(
    userId = "approver_uid",
    notification = Notification(
        recipientId = "approver_uid",
        title = "Test Notification",
        message = "This is a test notification",
        type = NotificationType.EXPENSE_SUBMITTED,
        projectId = "avr2_project_id"
    )
)
```

## 🎯 Success Criteria

**✅ Notification System Working When:**
1. Expense submission triggers notification logs
2. Approver receives in-app notification
3. Approver receives device push notification (if FCM configured)
4. Clicking notification navigates to correct project
5. Notification appears in notification list with correct details

**❌ System Not Working When:**
1. No notification logs appear after expense submission
2. Approver doesn't receive notifications
3. Notifications appear but don't navigate correctly
4. Wrong users receive notifications 