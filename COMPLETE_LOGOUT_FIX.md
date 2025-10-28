# Complete Logout Fix - Testing Different Roles

## The Problem You're Experiencing

1. Login as **Production Head** → Works
2. Try to login as **USER** → Still shows **Production Head screen**
3. Even after clicking "Development Skip" → Wrong screen appears

**Root Cause:** Firebase Auth caches the login session. When you try to login as a different user, the app finds the cached Firebase session and uses it instead of your new login.

## The Complete Fix

I've added proper Firebase sign-out in the logout function. Now:

### What Changed

**Before:**
```kotlin
fun logout() {
    authRepository.signOut()  // Only cleared local state
    setUnauthenticatedState()
}
```

**After:**
```kotlin
fun logout() {
    firebaseAuth.signOut()  // ✅ Clears Firebase session!
    authRepository.signOut()
    setUnauthenticatedState()
}
```

## How to Test Different Roles (STEP BY STEP)

### Step 1: Force Logout First

**Option A: Using Logout Button**
1. Launch the app
2. You'll see this card at the top:
   ```
   ⚠️ Existing Session Found
   Prajwal SS Reddy • PRODUCTION_HEAD
   Phone: 9008826666
   [CONTINUE] [LOGOUT]
   ```
3. Click the **"LOGOUT"** button
4. Wait for confirmation in Logcat:
   ```
   AuthViewModel: ✅ Firebase Auth signout completed
   AuthViewModel: ✅ Logout complete - all state cleared
   ```

**Option B: Using Production Head Menu**
If you're already in Production Head:
1. Tap the hamburger menu (three dots)
2. Select "Sign Out"
3. Confirm

### Step 2: Verify Logout Success

Check Logcat for:
```
AuthViewModel: 🔄 Logging out user...
AuthViewModel: ✅ Firebase Auth signout completed
AuthViewModel: ✅ Logout complete - all state cleared
AuthViewModel: ❌ No authentication found
```

If you see these logs, logout was successful.

### Step 3: Now Login as USER

1. Enter phone: **6360090611**
2. Click "Send OTP"
3. Verify OTP
4. OR click "Development Skip"

**Expected Result:**
```
AuthViewModel: 🧹 Clearing previous authentication state
AuthViewModel: ✅ Firebase auth cleared
AuthViewModel: Role value: USER
Navigation: 🎯 Navigating to USER flow
```

You should see **USER screen** (not Production Head).

## Troubleshooting

### Issue: Still seeing Production Head after logout

**Solution:**
1. Force stop the app
2. Go to Settings → Apps → AVR → Clear Data
3. Launch app again
4. Now login fresh

### Issue: Logout button doesn't appear

**Cause:** Auto-navigation is kicking in before you see the login screen

**Solution:**
Clear app data as above, then the logout button should appear.

### Issue: Logout clicks but nothing happens

**Check Logcat for:**
```
AuthViewModel: 🔄 Logging out user...
AuthViewModel: ✅ Firebase Auth signout completed
```

If you see "Firebase Auth signout completed" but still auto-logins:
- It might be a timing issue
- Try **Clearing app data** instead

## Quick Test Sequence

### Test Scenario: Production Head → USER → APPROVER

1. **Login as Production Head**
   - Phone: 9008826666
   - Should see Production Head screen

2. **LOGOUT** (Tap menu → Sign Out)

3. **Verify Logout:**
   - Should see login screen
   - Should see "Existing Session Found" warning (if cached)
   - Click "LOGOUT" button

4. **Login as USER**
   - Phone: 6360090611
   - Should see USER screen

5. **LOGOUT** (logout functionality)

6. **Login as APPROVER**
   - Should see APPROVER screen

## Code Changes Made

### 1. AuthViewModel.kt
```kotlin
fun logout() {
    firebaseAuth.signOut()  // ✅ Now clears Firebase Auth cache!
    authRepository.signOut()
    setUnauthenticatedState()
}
```

### 2. AppNavHost.kt
```kotlin
// Prevents auto-navigation for cached sessions
var isCachedSessionRestoration = true

// Shows logout option instead of auto-navigating
if (isCachedSessionRestoration && !isFromOTP && !isDevelopmentSkip) {
    // Don't auto-navigate - let user see logout button
}
```

### 3. LoginScreen.kt
```kotlin
// Prominent warning card with logout button
if (authState.isAuthenticated) {
    // Shows cached session info
    // Has CONTINUE and LOGOUT buttons
}
```

## What to Share If Still Not Working

If you still face issues after trying the above:

### Logcat After Logout
```
adb logcat -s AuthViewModel -d | findstr "logout\|signout\|Logging out"
```

### Logcat After Fresh Login
```
adb logcat -s AuthViewModel AppNavHost -d | findstr "Role value\|Navigating\|Route:"
```

## Summary

✅ **Logout now properly clears Firebase Auth cache**
✅ **Prominent warning banner shows cached sessions**
✅ **Auto-navigation disabled for cached sessions**
✅ **Two options: CONTINUE or LOGOUT**

### Testing Checklist

- [ ] Click LOGOUT button on login screen
- [ ] Verify Logcat shows "Firebase Auth signout completed"
- [ ] Enter phone for desired role (e.g., 6360090611 for USER)
- [ ] Click Development Skip (or complete OTP)
- [ ] Check Logcat for "Role value: USER"
- [ ] Verify correct screen appears

**If logout doesn't work, use: Settings → Apps → AVR → Clear Data**

