# Expense Chat Notification Testing Guide

## Purpose
This guide helps you test and debug expense chat notifications to ensure they work bidirectionally between users and approvers.

## Testing Prerequisites

### 1. Clear Logcat Filters
In Android Studio Logcat, use these filters to see all relevant logs:
```
ChatViewModel|ChatRepository|NotificationRepository|UserExpenseChatScreen|ApproverExpenseChatScreen
```

### 2. Test Users Required
- **User Role**: Someone who can create expenses
- **Approver Role**: Someone who can approve expenses (APPROVER, PRODUCTION_HEAD, or ADMIN)

## Test Scenarios

### Test 1: User Sends First Expense Chat Message

#### Steps:
1. **As USER**: Create an expense and submit it
2. **As USER**: Navigate to the expense and open the chat
3. **As USER**: Send a message like "Please review this expense"
4. **As APPROVER**: Check notification panel

#### Expected Behavior:
- ✅ Approver receives in-app notification about the message
- ✅ Notification title: "New message about pending expense from [User Name]"
- ✅ Notification has `actionRequired: true`

#### Logs to Check:
Look for these in order:
```
1. ChatViewModel: 🚀 SEND MESSAGE CALLED
2. ChatViewModel: 🚀 Is Expense Chat: true
3. ChatRepository: 🚀 SEND MESSAGE START
4. ChatRepository: 📋 NEW EXPENSE CHAT CREATION (if first message)
   OR
   ChatRepository: 📋 EXPENSE CHAT MEMBER UPDATE (if chat exists)
5. ChatRepository: 📋 ✅ Added approver to members
6. ChatRepository: 💬 NOTIFICATION PREPARATION
7. ChatRepository: 💬 Other members count: [should be > 0]
8. ChatRepository: 📧 CREATING NOTIFICATION
9. ChatRepository: ✅ NOTIFICATION CREATED SUCCESSFULLY
10. ChatViewModel: ✅ Message sent successfully
```

#### If Notifications Don't Show:
Look for these warning logs:
```
⚠️ NO NOTIFICATIONS SENT
⚠️ No other members to notify
⚠️ ❌ CRITICAL: Expense chat has no recipients
```

---

### Test 2: Approver Responds to Expense Chat

#### Steps:
1. **As APPROVER**: Open the expense from pending approvals
2. **As APPROVER**: Navigate to the chat
3. **As APPROVER**: Send a response like "I will review this today"
4. **As USER**: Check notification panel

#### Expected Behavior:
- ✅ User receives in-app notification about the approver's response
- ✅ Notification title: "New message about pending expense from [Approver Name]"
- ✅ Notification has `actionRequired: true`

#### Logs to Check:
Same sequence as Test 1, but now:
- Sender should be the approver
- Recipient should be the user (expense submitter)

```
ChatRepository: 📋 Expense submitter: [USER_PHONE_NUMBER]
ChatRepository: 📋 ✅ Added expense submitter to members
ChatRepository: 💬 Will notify: [USER_PHONE_NUMBER]
```

---

### Test 3: Multiple Message Exchange

#### Steps:
1. User sends message
2. Approver responds
3. User sends another message
4. Approver responds again

#### Expected Behavior:
- ✅ Each party receives notifications for messages from the other
- ✅ No notifications received for own messages
- ✅ All messages appear in chat history

---

### Test 4: After Restart/Days Later

#### Steps:
1. Close and restart the app
2. Wait (or simulate time passage)
3. User sends another message
4. Verify approver gets notification
5. Approver responds
6. Verify user gets notification

#### Expected Behavior:
- ✅ Notifications still work after restart
- ✅ Chat members are automatically updated on each message

---

## Debugging with Logs

### What to Share

When sharing logs, please filter by these tags and share the complete output:
```
ChatViewModel
ChatRepository
NotificationRepository
```

### Critical Log Sections

#### 1. **Chat Member Update Logs**
```
📋 ========== EXPENSE CHAT MEMBER UPDATE ==========
📋 Current chat members: [...]
📋 ✅ Added expense submitter to members: xxx
📋 ✅ Added approver to members: xxx
📋 ✅ Updated chat members: X -> Y members
```
**What it means**: Shows who is being added to the chat

#### 2. **Notification Preparation Logs**
```
💬 ========== NOTIFICATION PREPARATION ==========
💬 Is Expense Chat: true
💬 All members count: X
💬 Other members count: Y
💬 📋 Will notify: [list of user IDs]
```
**What it means**: Shows who will receive notifications

#### 3. **Notification Creation Logs**
```
📧 ========== CREATING NOTIFICATION ==========
📧 Notification Details:
   - recipientId: 'xxx'
   - title: 'New message about pending expense from xxx'
✅ NOTIFICATION CREATED SUCCESSFULLY
```
**What it means**: Confirms notification was saved to Firestore

#### 4. **Error Logs**
```
⚠️ NO NOTIFICATIONS SENT
⚠️ No other members to notify
❌ CRITICAL: Expense chat has no recipients
```
**What it means**: Something is wrong with chat members

---

## Common Issues and Solutions

### Issue 1: No Notifications Created
**Symptom**: Log shows "NO NOTIFICATIONS SENT"

**Cause**: Chat has no members or sender is the only member

**Solution**:
1. Check if expense exists in database
2. Check if project has approvers
3. Check user IDs match between chat and users

**Logs to Share**:
```
📋 Current chat members: [...]
📋 Expense submitter: xxx
📋 Project manager: xxx
💬 All members: [...]
💬 SenderId: xxx
💬 Other members: [...]
```

### Issue 2: Notifications Created But Not Appearing
**Symptom**: Log shows "NOTIFICATION CREATED SUCCESSFULLY" but notifications don't appear in UI

**Cause**: 
- RecipientId mismatch
- Notification query not matching
- User ID format issues

**Solution**:
1. Check recipientId in notification matches logged-in user's ID
2. Verify notification query in NotificationViewModel
3. Check if user is logged in with correct credentials

**Logs to Share**:
```
📧 Notification Details:
   - recipientId: 'xxx'
NotificationViewModel: Loading notifications for user: xxx
NotificationRepository: Found X notifications for user: xxx
```

### Issue 3: Only One Direction Works
**Symptom**: User gets notifications from approver, but approver doesn't get notifications from user (or vice versa)

**Cause**: Member list not being updated properly

**Solution**:
1. Check if expense submitter is being added
2. Check if approvers are being added
3. Verify member update logs

**Logs to Share**:
```
📋 EXPENSE CHAT MEMBER UPDATE
📋 ✅ Added expense submitter to members: xxx
📋 ✅ Added approver to members: xxx
📋 Final members list: [...]
```

---

## Quick Test Command

To quickly test, run this in terminal connected to your device:
```bash
adb logcat -s ChatViewModel:D ChatRepository:D NotificationRepository:D
```

This will show only the relevant logs.

---

## Expected Full Log Flow

Here's what a successful message send should look like:

```
[ChatViewModel] 🚀 ========== SEND MESSAGE CALLED ==========
[ChatViewModel] 🚀 Chat ID: expense_approval_xxx
[ChatViewModel] 🚀 Is Expense Chat: true

[ChatRepository] 🚀 ========== SEND MESSAGE START ==========
[ChatRepository] 📋 ========== EXPENSE CHAT MEMBER UPDATE ==========
[ChatRepository] 📋 Current chat members: [user, approver] (count: 2)
[ChatRepository] 📋 ✓ Sender already in members: user
[ChatRepository] 📋 Found expense: xxx
[ChatRepository] 📋 Expense submitter: user
[ChatRepository] 📋 ✓ Expense submitter already in members: user
[ChatRepository] 📋 Project manager: approver
[ChatRepository] 📋 ✓ Project manager already in members: approver
[ChatRepository] 📋 ✓ No member updates needed (all members already present)

[ChatRepository] 💬 ========== NOTIFICATION PREPARATION ==========
[ChatRepository] 💬 Is Expense Chat: true
[ChatRepository] 💬 All members count: 2
[ChatRepository] 💬 Other members count: 1
[ChatRepository] 💬 📋 Will notify: approver

[ChatRepository] 📧 ========== CREATING NOTIFICATION ==========
[ChatRepository] 📧 Notification Details:
   - recipientId: 'approver'
   - title: 'New message about pending expense from user'
   - actionRequired: true

[NotificationRepository] ✅ Successfully saved notification to Firestore!

[ChatRepository] ✅ ========== NOTIFICATION CREATED SUCCESSFULLY ==========

[ChatViewModel] ✅ Message sent successfully
```

---

## Next Steps After Testing

1. **Run Test 1**: User sends message
2. **Capture full logs** (from ChatViewModel to NOTIFICATION CREATED)
3. **Share logs** with me
4. **Check if notification appears** in approver's notification panel
5. **Run Test 2**: Approver responds
6. **Capture logs** again
7. **Share logs** with me

This will help me identify exactly where the issue is occurring.

