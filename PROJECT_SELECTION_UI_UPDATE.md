# Project Selection UI Update for User Role

## Implementation Date
October 16, 2025

## Overview
Updated the project selection screen UI for user role to match the requested design while keeping the chat functionality from the original UI.

## Changes Made

### 1. Created New Project Selection Screen
- Created a new screen component `NewProjectSelectionScreen.kt` specifically for user role
- Updated the navigation to conditionally show this UI only for users with `UserRole.USER`
- Other roles (approvers, production heads) continue to see the original UI

### 2. Updated Project Card Design
- Changed from horizontal card layout to vertical card layout
- Added more detailed project information:
  - Project name and code at the top
  - Budget information with currency symbol
  - Date range with calendar icon
  - Days left indicator
  - Team members count

### 3. UI Elements Retained from Original Design
- Kept the chat icon from the first image design
- Maintained the same header with app name and "Select Project" text
- Preserved notification badge and refresh functionality

### 4. UI Elements Removed as Requested
- Removed edit icon for projects
- Removed active status indicator
- Simplified the project card design

### 5. Visual Improvements
- Used rounded corners for cards (16dp radius)
- Added proper spacing between elements
- Used appropriate typography hierarchy
- Added visual indicators for dates and budget

## Implementation Details

### Role-Based UI Selection
```kotlin
// In AppNavHost.kt
composable(Screen.ProjectSelection.route) {
    // Check if user is a regular user, then show the new UI
    val authState = authViewModel.authState.collectAsState().value
    val isRegularUser = authState.user?.role == com.deeksha.avr.model.UserRole.USER
    
    if (isRegularUser) {
        // New UI for regular users
        NewProjectSelectionScreen(...)
    } else {
        // Original UI for other roles
        ProjectSelectionScreen(...)
    }
}
```

### New Project Card Design
```kotlin
@Composable
fun NewProjectCard(
    project: Project,
    onProjectClick: () -> Unit,
    onChatClick: () -> Unit = {},
    projectNotifications: List<Notification> = emptyList()
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onProjectClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Project name and code with chat icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Project Initial Circle
                    Box(...)
                    
                    Text(
                        text = project.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }
                
                // Chat icon from first image
                IconButton(
                    onClick = onChatClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "Chat",
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Project description (if available)
            
            // Budget information
            
            // Date range with days left
            
            // Team members count
        }
    }
}
```

## Testing
The implementation has been successfully tested and built. The UI now properly displays the new design for users while maintaining the original design for other roles.

## Files Modified
1. `app/src/main/java/com/deeksha/avr/ui/view/common/NewProjectSelectionScreen.kt` (New file)
2. `app/src/main/java/com/deeksha/avr/navigation/AppNavHost.kt` (Updated)

## Notes
- The original UI is still available and used for non-user roles
- All functionality from the original UI has been preserved
- The chat icon functionality works the same as before
- The UI is responsive and adapts to different screen sizes
