# How to Test Different Roles - Complete Guide

## The Problem You're Experiencing

When you open the app, it **automatically logs you in** with the previous session:
- User: **Prajwal SS Reddy**
- Role: **PRODUCTION_HEAD**

This happens because Firebase Auth caches the login session. You're not actually logging in fresh - the app is restoring the old session.

## Solution: Logout Before Testing

### Method 1: Use the New Logout Button (EASIEST)

I've added a **"Logout Current Session"** button at the top-right of the login screen.

**Steps:**
1. Open the app
2. If you see the Production Head screen, go back or restart the app
3. You should briefly see the login screen
4. **Click "🚪 Logout Current Session"** button at the top right
5. Now you can test with different phone numbers

### Method 2: Clear App Data (CLEAN SLATE)

**On Android Device:**
1. Go to **Settings** → **Apps** → **AVR** (your app name)
2. Tap **Storage**
3. Tap **Clear Data** or **Clear Storage**
4. Launch the app again
5. Now test with your desired phone number

**Using ADB:**
```bash
adb shell pm clear com.deeksha.avr
```

### Method 3: Use the Menu Logout (When Logged In)

If you're already logged in as Production Head:
1. Tap the hamburger menu (three dots)
2. Select **"Sign Out"**
3. Now test with a different phone number

## Testing Steps for Each Role

### Testing USER Role (Phone: 6360090611)

1. **Logout** using one of the methods above
2. Open the app (should be on Login screen)
3. Enter phone: **6360090611**
4. Click **"Development Skip"** button
5. **Expected Result:**
   - Should navigate to **User Project Selection** screen
   - Should see expense-related features
   - Bottom nav should have different options (not Projects/User Management)

### Testing APPROVER Role

1. **Logout** first
2. Open the app
3. Enter the APPROVER phone number
4. Click **"Development Skip"**
5. **Expected Result:**
   - Should navigate to **Approver Project Selection** screen
   - Should see approval-related features

### Testing PRODUCTION_HEAD Role

1. **Logout** first
2. Open the app
3. Enter the PRODUCTION_HEAD phone number
4. Click **"Development Skip"**
5. **Expected Result:**
   - Should navigate to **Production Head Dashboard**
   - Should see **Projects** and **User Management** tabs at the bottom
   - Should see the new iOS-style UI

## What to Check in Logcat

After logging out and logging in fresh, check these logs:

### 1. Logout Logs
```
LoginScreen: 🔄 Logging out cached session
AuthViewModel: 🔄 Logging out user...
AuthViewModel: ❌ Setting unauthenticated state
```

### 2. Development Skip Logs
```
AuthViewModel: 🧹 Clearing previous authentication state
AuthViewModel: ✅ Firebase auth cleared
AuthViewModel: ✅ Found actual user in database
AuthViewModel:    Name: Manya
AuthViewModel:    Role from DB: USER
AuthViewModel:    Role value: USER
```

### 3. Navigation Logs
```
Navigation: 🎯 Received role: USER
Navigation: 🎯 Navigating to USER flow (ProjectSelection)
Navigation: 🚀 Route: project_selection
```

## Common Issues & Solutions

### Issue 1: Still Seeing Production Head Screen
**Cause:** Cached session not cleared properly

**Solution:**
1. Force stop the app
2. Use Method 2 (Clear App Data)
3. Launch app again

### Issue 2: No Logout Button Visible
**Cause:** Not on login screen, or button not rendered

**Solution:**
1. Force close the app completely
2. Relaunch the app
3. If you're on Production Head screen, use the hamburger menu → Sign Out

### Issue 3: Wrong User Data After Development Skip
**Cause:** Phone number in Firestore is incorrect

**Solution:**
1. Check Firebase Console
2. Go to Firestore → users collection
3. Find the document with phone "6360090611"
4. Verify:
   - `phoneNumber` field = "6360090611"
   - `role` field = "USER"
   - `name` field = "Manya"

## Verification Checklist

Before testing each role:
- [ ] Logout from previous session
- [ ] Confirm you're on Login screen
- [ ] Clear Logcat
- [ ] Set filter: `AppNavHost OR AuthViewModel OR AuthRepository`
- [ ] Enter correct phone number
- [ ] Click Development Skip
- [ ] Check logs for correct role detection
- [ ] Verify correct screen is shown

## Testing Matrix

| Phone Number | Expected Role | Expected Screen | Bottom Navigation |
|-------------|---------------|-----------------|-------------------|
| 6360090611 | USER | User Project Selection | (User options) |
| (Approver phone) | APPROVER | Approver Project Selection | (Approver options) |
| (Production Head phone) | PRODUCTION_HEAD | Production Head Dashboard | Projects / User Management |

## Quick Test Commands

### Clear session and test USER
```bash
# Terminal 1: Monitor logs
adb logcat -s AuthViewModel:V AppNavHost:V -c && adb logcat -s AuthViewModel:V AppNavHost:V

# Terminal 2: Clear app data and launch
adb shell pm clear com.deeksha.avr
adb shell am start -n com.deeksha.avr/.MainActivity
```

## Important Notes

1. **Always logout before testing a different role**
2. **Firebase Auth caches sessions** - this is normal behavior
3. **Development Skip only works for users in Firestore** - if the phone number doesn't exist, it creates a default USER
4. **The new UI changes ONLY affect Production Head** - USER and APPROVER flows are unchanged

## Summary

### The Core Issue
The app caches authentication. You need to **logout** before testing different roles.

### The Solution
Use the **"🚪 Logout Current Session"** button at the top-right of the login screen, or clear app data.

### Testing Flow
1. **Logout** → 2. **Clear Logcat** → 3. **Enter phone** → 4. **Development Skip** → 5. **Verify screen**

---

## Need Help?

If you're still seeing the wrong screen after logout:
1. Share the Logcat output after clicking "Logout Current Session"
2. Share the Logcat output after clicking "Development Skip"
3. Check Firebase Console for the user data


