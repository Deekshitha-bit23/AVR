# Expense Chat Notification Implementation

## Overview
Implemented in-app notifications for approvers when users send messages in pending expense approval chats.

## Implementation Date
October 13, 2025

## Problem Statement
When a user sends a message in a pending expense approval chat, the specific approver(s) assigned to that project need to be notified in the notification tab. No FCM push notifications required - only in-app notifications.

## Solution Architecture

### Key Changes Made

#### 1. Enhanced `ChatRepository.sendMessage()` Function
**File**: `app/src/main/java/com/deeksha/avr/repository/ChatRepository.kt`

**What was added:**
- **Expense Chat Detection**: Automatically detects expense approval chats by checking if `chatId` starts with `"expense_approval_"`
- **Dynamic Approver Addition**: When a message is sent in an expense chat:
  - Fetches all approvers for the project (regular approvers, production heads, temporary approvers)
  - Automatically adds them as chat members if not already present
  - Initializes unread counts for new members
- **Enhanced Notifications**: Creates notifications with:
  - Custom title: "New message about pending expense from {sender name}"
  - `actionRequired` flag set to `true` for better visibility
  - Proper navigation target to the chat

#### 2. New Helper Function: `getApproversForExpenseChat()`
**Location**: `ChatRepository.kt` (private function)

**What it does:**
- Queries the project document from Firestore
- Collects all types of approvers:
  - Regular approvers from `approverIds` field
  - Production heads from `productionHeadIds` field
  - Temporary approver from `temporaryApproverPhone` field
- Returns a combined list of all approver identifiers
- Includes comprehensive logging for debugging

### How It Works

```
User sends message in expense chat
         ↓
ChatRepository.sendMessage() detects "expense_approval_" prefix
         ↓
getApproversForExpenseChat() fetches all project approvers
         ↓
Approvers are added to chat members (if not already present)
         ↓
Message is saved to Firestore
         ↓
Notifications are created for all chat members (except sender)
         ↓
Approvers see notification in their notification tab
```

### Chat ID Format
- **Expense Approval Chats**: `expense_approval_{expenseId}`
- Example: `expense_approval_abc123xyz`

### Notification Details

**For Expense Chats:**
- **Title**: "New message about pending expense from {sender name}"
- **Message**: The actual chat message content
- **Type**: `CHAT_MESSAGE`
- **Action Required**: `true`
- **Navigation Target**: `"chat/{projectId}/{chatId}/{senderName}"`

**For Regular Chats:**
- **Title**: "New message from {sender name}"
- **Action Required**: `false`
- Everything else remains the same

## Features

### 1. Automatic Approver Discovery
The system automatically finds all relevant approvers:
- **Regular Approvers**: From project's `approverIds` list
- **Production Heads**: From project's `productionHeadIds` list
- **Temporary Approvers**: From project's `temporaryApproverPhone` field

### 2. Dynamic Member Management
- Approvers are automatically added to the chat when first message is sent
- No manual chat creation required
- Handles cases where approvers change mid-conversation

### 3. In-App Notifications Only
- Notifications appear in the notification tab immediately
- No FCM push notifications (as per requirements)
- Existing FCM infrastructure untouched

### 4. Backward Compatible
- Regular project chats continue to work as before
- Only expense approval chats get the enhanced behavior
- No breaking changes to existing functionality

## Technical Details

### Files Modified
1. **ChatRepository.kt**
   - Modified `sendMessage()` function (lines 248-433)
   - Added `getApproversForExpenseChat()` function (lines 195-236)
   - Enhanced notification title logic (lines 435-454)

### Database Structure

**Chat Document** (`projects/{projectId}/chats/{chatId}`):
```javascript
{
  members: ["user1Phone", "approver1Phone", "approver2Phone", "prodHeadPhone"],
  lastMessage: "string",
  lastMessageTime: Timestamp,
  lastMessageSenderId: "string",
  unreadCount: {
    "user1Phone": 0,
    "approver1Phone": 2,
    "approver2Phone": 1,
    "prodHeadPhone": 3
  },
  createdAt: Timestamp,
  updatedAt: Timestamp
}
```

**Notification Document** (`notifications/{notificationId}`):
```javascript
{
  recipientId: "approverPhone",
  recipientRole: "APPROVER",
  title: "New message about pending expense from John Doe",
  message: "Can you please review this expense?",
  type: "CHAT_MESSAGE",
  projectId: "projectId123",
  projectName: "Web Project",
  relatedId: "expense_approval_expenseId123",
  isRead: false,
  actionRequired: true,
  navigationTarget: "chat/projectId123/expense_approval_expenseId123/John Doe",
  createdAt: Timestamp
}
```

## User Flow Examples

### Scenario 1: User Sends Message to Pending Expense
1. User opens "Track Recent Submissions"
2. Sees pending expense with orange "Pending" badge
3. Clicks chat icon to open expense chat
4. Sends message: "Please review this urgent expense"
5. **Result**: 
   - All approvers (regular + production heads + temporary) are added to chat
   - All approvers receive notification in their notification tab
   - Notification title: "New message about pending expense from {User Name}"

### Scenario 2: Approver Responds
1. Approver sees notification in notification tab
2. Clicks notification → navigates to expense chat
3. Responds to user's message
4. **Result**:
   - User receives notification
   - Other approvers also see the message and notification

### Scenario 3: Multiple Approvers
1. Project has:
   - 2 regular approvers
   - 1 production head
   - 1 temporary approver
2. User sends message in expense chat
3. **Result**:
   - All 4 approvers receive notifications
   - All 4 are added as chat members
   - Any of them can respond

## Testing Checklist

- [x] Detect expense approval chats correctly
- [x] Fetch all types of approvers (regular, production head, temporary)
- [x] Add approvers to chat members automatically
- [x] Create notifications for all approvers
- [x] Notifications appear in approver's notification tab
- [x] Custom notification title for expense chats
- [x] Navigation from notification to chat works
- [x] No linter errors
- [x] Backward compatible with regular chats

## Logging

The implementation includes comprehensive logging:
- `📋` - Expense chat operations
- `✅` - Successful operations
- `❌` - Errors
- `🔍` - Debug information

### Example Logs:
```
ChatRepository: 📋 Detected expense approval chat: expense_approval_abc123
ChatRepository: 📋 Getting approvers for project: projectId123
ChatRepository: 📋 Added regular approver: 9876543210
ChatRepository: 📋 Added production head: 9876543211
ChatRepository: 📋 Added temporary approver: 9876543212
ChatRepository: 📋 Total approvers found: 3
ChatRepository: 📋 Current chat members: [9999999999]
ChatRepository: 📋 Adding approver to chat: 9876543210
ChatRepository: 📋 Updated chat members to: [9999999999, 9876543210, 9876543211, 9876543212]
ChatRepository: Created notification for receiver: 9876543210 (role: APPROVER)
```

## Benefits

1. **Improved Communication**: Approvers are instantly notified when users need their attention
2. **Better Visibility**: Special notification title makes expense-related messages stand out
3. **Automatic Management**: No manual setup required - approvers are added automatically
4. **Flexible**: Supports all types of approvers (regular, production heads, temporary)
5. **Non-Intrusive**: In-app only, no push notifications to disturb users
6. **Scalable**: Works with any number of approvers per project

## Future Enhancements (Not Implemented)
- FCM push notifications (if needed later)
- Read receipts for expense chats
- Typing indicators
- Message reactions
- Expense status updates in chat

## Notes
- Uses phone numbers as user identifiers (consistent with existing system)
- Notifications are stored in Firestore `notifications` collection
- Existing notification viewing infrastructure works out of the box
- No changes needed to UI components

