# User Role Navigation Fix

## Issue Description
User with phone number 6360090611 (role: USER) is seeing the Production Head screen instead of the regular User screen.

## Root Cause Analysis
The navigation logic correctly routes users based on their role:
- **USER** → `Screen.ProjectSelection.route` ("project_selection")
- **APPROVER** → `Screen.ApproverProjectSelection.route` ("approver_project_selection")
- **PRODUCTION_HEAD** → `Screen.ProductionHeadDashboard.route` ("production_head_dashboard")
- **ADMIN** → `Screen.AdminDashboard.route` ("admin_dashboard")

If a user is seeing the wrong screen, it means their role in Firestore is incorrectly set.

## Verification Steps

### 1. Check User Role in Firestore
Run this command to check the user's role in Firestore:
```bash
adb logcat -s AppNavHost | grep "Authenticated user detected"
```

Or check directly in Firestore Console:
1. Go to Firestore Database
2. Navigate to the "users" collection
3. Find document ID matching phone number "6360090611" (or with +91 prefix)
4. Check the "role" field value

### 2. Expected Role Values
The role field should be one of:
- `"USER"` - Regular end user
- `"APPROVER"` - Approver role
- `"ADMIN"` - Admin role
- `"PRODUCTION_HEAD"` - Production Head role

### 3. Fix the Role in Firestore
If the role is incorrect, update it in Firestore:
```
Collection: users
Document: [user's phone number or UID]
Field: role
Value: "USER"
```

## Code Changes Made

### Enhanced Logging
Added detailed logging to track role-based navigation:
```kotlin
android.util.Log.d("AppNavHost", "🎯 Auth state changed - isAuthenticated: ${authState.isAuthenticated}, user: ${user.name} (${user.phone}), role: ${user.role}")
android.util.Log.d("AppNavHost", "🎯 Processing role navigation for role: $role")
android.util.Log.d("AppNavHost", "🎯 Navigating USER to ProjectSelection route: ${Screen.ProjectSelection.route}")
```

## How to Fix the Issue

### Option 1: Update Role in Firestore (Recommended)
1. Open Firebase Console
2. Go to Firestore Database
3. Navigate to `users` collection
4. Find the user document (search by phoneNumber: "6360090611")
5. Update the `role` field to `"USER"`
6. Save the changes
7. Log out and log back in

### Option 2: Check Logcat Output
Run the app and check Logcat for the role detection:
```bash
adb logcat -s AppNavHost AuthRepository | grep -E "user|role"
```

The logs will show:
- User's name and phone
- Detected role
- Navigation route being used

## Testing
After fixing the role:
1. Log out of the app
2. Log in again with phone number 6360090611
3. Check the Logcat output to verify correct role is detected
4. Verify the correct screen is shown

## Expected Behavior

### USER Role:
- Should navigate to `Screen.ProjectSelection.route`
- Should show the regular user project selection screen
- Should have access to project selection, expense list, etc.

### PRODUCTION_HEAD Role:
- Should navigate to `Screen.ProductionHeadDashboard.route`
- Should show the Production Head screen with Projects and User Management tabs
- Should have access to user management, role management, etc.

## Additional Notes
- The navigation logic is correct and working as designed
- The issue is with the user's role data in Firestore
- All role checks use the same enum: `UserRole.USER`, `UserRole.APPROVER`, `UserRole.ADMIN`, `UserRole.PRODUCTION_HEAD`
- The role is stored as a string in Firestore but converted to enum in the app

## Need Logcat Output?
To help diagnose the issue, please share the Logcat output when logging in:

```bash
adb logcat -s AppNavHost AuthRepository -d > role_check.log
```

This will capture all navigation and authentication logs to help identify the issue.


