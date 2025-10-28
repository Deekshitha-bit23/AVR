# Menu Bottom Sheet Implementation

## Overview
Implemented a bottom sheet menu that appears when clicking the hamburger menu icon (three dots) in the Production Head Projects screen. The menu includes About, Settings, and Sign Out options, with Sign Out functionality that logs the user out and navigates to the login page.

## Features Implemented

### 1. Menu Trigger
- Added `showMainMenu` state variable
- Made hamburger menu icon clickable to show the bottom sheet

### 2. Bottom Sheet Modal
- Uses `ModalBottomSheet` from Material3
- Includes dismiss functionality (tap outside or X button)
- White background matching iOS design

### 3. Menu Items

#### Header
- Title: "Menu" in bold
- Subtitle: "Settings & Account"
- Close button (X icon) on the right

#### Menu Options
1. **About**
   - Icon: Information icon (i) with light blue background
   - Title: "About"
   - Subtitle: "App information & version"
   - Status: Placeholder (TODO: Navigate to About screen)

2. **Settings**
   - Icon: Settings/gear icon with light blue background
   - Title: "Settings"
   - Subtitle: "Preferences & configuration"
   - Status: Placeholder (TODO: Navigate to Settings screen)

3. **Sign Out**
   - Icon: Exit icon on red background
   - Title: "Sign Out" in red text
   - Subtitle: "Logout from your account" in red text
   - Background: Light red/pink tint
   - **Functionality: Fully implemented** - Logs out and navigates to login page

## Design Details

### Styling (Matching iOS Design)
- Rounded corners: 12dp for menu items
- Icon containers: 40dp circular with color backgrounds
- Spacing: 24dp between sections, 8dp between items
- Colors:
  - Icon backgrounds: Light blue (#E3F2FD) for About/Settings
  - Sign Out icon: Red background with white icon
  - Sign Out container: Light red tint (#FFEBEE)
  - Text colors: Black for titles, gray for subtitles, red for Sign Out

### Layout
```
[Header with Title, Subtitle, and Close Button]
[Spacing]
[About - with blue info icon]
[Spacing]
[Settings - with blue gear icon]
[Spacing]
[Sign Out - with red background and red text]
[Bottom spacing]
```

## Implementation Details

### Code Structure

#### 1. State Management
```kotlin
var showMainMenu by remember { mutableStateOf(false) }
```

#### 2. Menu Trigger
```kotlin
IconButton(onClick = { showMainMenu = true }) {
    Icon(Icons.Default.Menu, ...)
}
```

#### 3. Bottom Sheet
```kotlin
if (showMainMenu) {
    ModalBottomSheet(
        onDismissRequest = { showMainMenu = false },
        ...
    ) {
        MenuBottomSheet(
            onDismiss = { showMainMenu = false },
            onLogout = {
                showMainMenu = false
                onLogout()
            }
        )
    }
}
```

#### 4. MenuItem Composable
- Reusable component for each menu item
- Supports custom colors for Sign Out
- Includes icon, title, subtitle, and arrow indicator
- Clickable Surface wrapper

## Functionality

### Sign Out Feature
When the user clicks "Sign Out":
1. Bottom sheet closes
2. `onLogout()` callback is triggered
3. User is logged out from the app
4. Navigation returns to Login screen

### Future Enhancements
- **About Screen**: Show app version, developer info, etc.
- **Settings Screen**: Allow users to configure preferences

## Files Modified

### `app/src/main/java/com/deeksha/avr/ui/view/productionhead/ProductionHeadProjectsTab.kt`
- Added `showMainMenu` state
- Made menu button clickable
- Added ModalBottomSheet component
- Created MenuBottomSheet composable
- Created MenuItem composable
- Added ImageVector import

## Testing

### How to Test:
1. Navigate to Production Head projects screen
2. Click the hamburger menu icon (three dots) on the top right
3. Bottom sheet slides up from the bottom
4. Verify menu options are displayed:
   - About (blue info icon)
   - Settings (blue gear icon)
   - Sign Out (red background)
5. Click "Sign Out"
6. Verify:
   - Bottom sheet closes
   - User is logged out
   - App navigates to Login screen

### Expected Behavior:
- ✅ Menu appears when clicking hamburger icon
- ✅ Menu dismisses when clicking close (X)
- ✅ Menu dismisses when tapping outside
- ✅ Sign Out closes menu and logs out user
- ✅ About and Settings show placeholders (for future implementation)

## Screenshots
The implementation matches the iOS reference design:
- Same layout and spacing
- Same colors (light blue for info/settings, red for sign out)
- Same icon styles
- Same bottom sheet animation

## Notes
- The menu is only visible when `showMainMenu` is true
- Logout functionality is fully connected to the navigation system
- About and Settings are placeholders that can be implemented later
- Design matches the iOS reference screenshot exactly

