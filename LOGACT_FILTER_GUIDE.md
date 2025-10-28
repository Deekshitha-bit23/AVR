# Logcat Filter Guide - Debugging Role Navigation Issue

## The Problem
When logging in as USER role (phone: 6360090611) using Development Skip:
- ❌ NOT asking for OTP (bypassing verification)
- ❌ Showing Production Head screen instead of User screen
- ❌ Should route to `project_selection` but routing to `production_head_dashboard`

## Correct Logcat Filter Settings

### Method 1: Filter by Tag Names (RECOMMENDED)
In Android Studio Logcat search box, enter:
```
AuthRepository OR AppNavHost OR AuthViewModel OR Navigation
```

**What this captures:**
- `AuthRepository`: Role detection from Firestore
- `AppNavHost`: Navigation routing decisions
- `AuthViewModel`: Authentication state changes
- `Navigation`: Direct navigation callbacks

### Method 2: Filter by Package Name
Enter:
```
package:com.deeksha.avr
```

This captures ALL logs from your app.

### Method 3: Clear All Logs First
1. Click the **trash can icon** (🗑️) to clear current logs
2. Set filter: `AuthRepository OR AppNavHost OR AuthViewModel`
3. Reproduce the issue

## Steps to Reproduce & Capture Logs

### Step 1: Prepare
1. **Close your app completely**
2. Open Android Studio Logcat
3. Set filter to: `AuthRepository OR AppNavHost OR AuthViewModel OR Navigation`
4. Click trash icon to clear old logs

### Step 2: Reproduce Issue
1. Launch the app
2. You should be on Login screen
3. Enter phone number: **6360090611**
4. Click **"Development Skip"** button
5. Observe what happens

### Step 3: Capture Logs
1. In Logcat, **select all logs** (Ctrl+A / Cmd+A)
2. **Copy** (Ctrl+C / Cmd+C)
3. **Save to a file** or paste here

## What Logs to Look For

### Expected Logs (if working correctly):

```
AuthRepository: 🔍 Querying Firestore for phone: '6360090611'
AuthRepository: 📊 Query completed. Found 1 documents
AuthRepository: 🔍 Raw role from database: 'USER'
AuthRepository: ✅ Detected USER role
AuthViewModel: ✅ Found actual user in database
AuthViewModel:    Role from DB: USER
AuthViewModel:    Role value: USER
AuthViewModel: ✅ Development skip successful: Manya  (USER)
AuthViewModel: 🚀 Calling navigation callback with role: USER
Navigation: 🎯 Direct navigation callback received
Navigation: 🎯 Received role: USER
Navigation: 🎯 Navigating to USER flow (ProjectSelection)
Navigation: 🚀 Route: project_selection
```

### If You See These Logs Instead:

```
AuthViewModel:    Role value: PRODUCTION_HEAD  ← ❌ WRONG!
AppNavHost: 🎯 Navigating PRODUCTION_HEAD to ProductionHeadDashboard route  ← ❌ WRONG!
```

## Troubleshooting

### Issue: No logs appear after filtering
**Solution:** 
1. Make sure app is running
2. Try filter: `package:com.deeksha.avr`
3. Check if "Verbose" level is selected in Logcat

### Issue: Too many logs
**Solution:** Use multiple filters:
```
tag:AuthRepository | tag:AppNavHost | tag:AuthViewModel
```

### Issue: Logs still showing system processes
**Solution:**
1. Deselect "Show only selected application" if checked
2. Use: `package:com.deeksha.avr` filter

## What to Share
After reproducing the issue, share:

1. **First 10-15 log lines** after clicking Development Skip
2. **Any line containing:**
   - "Role from DB"
   - "Role value"
   - "Navigating to" / "Navigating PRODUCTION_HEAD"
   - "Route:"

## Quick Check Command
If you have ADB access via terminal:

```bash
adb logcat -s AuthRepository:V AppNavHost:V AuthViewModel:V Navigation:V | head -50
```

This will show the first 50 relevant log lines.

---

## Summary: What to do RIGHT NOW

1. **Clear Logcat** (trash icon)
2. **Set filter:** `AuthRepository OR AppNavHost OR AuthViewModel OR Navigation`
3. **Enter phone:** 6360090611
4. **Click Development Skip**
5. **Copy ALL logs** and share them
6. **Look for lines with "Role value"** - this tells us what role is being detected

The key question: **What role is being detected in the logs?**
- If it says "USER" → Navigation bug
- If it says "PRODUCTION_HEAD" → Database or role parsing bug


