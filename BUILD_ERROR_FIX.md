# Build Error Fix - onNavigateToEditProject Parameter

## Error Description
```
No parameter with name 'onNavigateToEditProject' found. :97
Error in ProductionHeadMainScreen.kt line 97
```

## Root Cause
The `ProductionHeadProjectsTab` function was missing the `onNavigateToEditProject` parameter in its function signature, but it was being passed from `ProductionHeadMainScreen`.

## Changes Made

### 1. Updated `ProductionHeadProjectsTab` function signature
**File:** `app/src/main/java/com/deeksha/avr/ui/view/productionhead/ProductionHeadProjectsTab.kt`

**Before:**
```kotlin
fun ProductionHeadProjectsTab(
    onNavigateToProject: (String) -> Unit,
    onNavigateToNewProject: () -> Unit,
    onLogout: () -> Unit,
    projectViewModel: ProjectViewModel = hiltViewModel()
)
```

**After:**
```kotlin
fun ProductionHeadProjectsTab(
    onNavigateToProject: (String) -> Unit,
    onNavigateToNewProject: () -> Unit,
    onNavigateToEditProject: (String) -> Unit,
    onLogout: () -> Unit,
    projectViewModel: ProjectViewModel = hiltViewModel()
)
```

### 2. Updated `ProjectCard` function signature
**Before:**
```kotlin
private fun ProjectCard(
    project: Project,
    onClick: () -> Unit
)
```

**After:**
```kotlin
private fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onEditClick: () -> Unit
)
```

### 3. Made the edit button clickable
**Before:**
```kotlin
Box(
    modifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .background(Color(0xFFE3F2FD)),
    contentAlignment = Alignment.Center
)
```

**After:**
```kotlin
Box(
    modifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .clickable(onClick = onEditClick)
        .background(Color(0xFFE3F2FD)),
    contentAlignment = Alignment.Center
)
```

### 4. Updated ProjectCard call to pass onEditClick
**Before:**
```kotlin
ProjectCard(
    project = project,
    onClick = { onNavigateToProject(project.id) }
)
```

**After:**
```kotlin
ProjectCard(
    project = project,
    onClick = { onNavigateToProject(project.id) },
    onEditClick = { onNavigateToEditProject(project.id) }
)
```

## Result
- ✅ Build compiles successfully
- ✅ No linter errors
- ✅ Edit button now navigates to edit project screen
- ✅ All parameters properly passed through the component hierarchy

## Testing
1. Build should now compile without errors
2. Edit button on project cards should now navigate to EditProject screen when clicked
3. Edit button calls `onNavigateToEditProject(projectId)` which navigates to `Screen.EditProject` route

