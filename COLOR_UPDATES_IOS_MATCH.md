# Color Updates - iOS Style Matching

## Changes Made to ViewAllUsersScreen

### Role Badge Colors

#### Before (Solid colors with white text):
- **USER**: Solid green (#4CAF50) with white text
- **APPROVER**: Solid blue (#2196F3) with white text
- **PRODUCTION_HEAD**: Solid purple (#9C27B0) with white text
- **ADMIN**: Solid red (#D32F2F) with white text

#### After (Light backgrounds with darker text - iOS style):

| Role | Background Color | Text Color | Description |
|------|-----------------|------------|-------------|
| **User** | `#D4F4DD` | `#34A853` | Light green background with darker green text |
| **Approver** | `#D6EBFF` | `#1976D2` | Light blue background with darker blue text |
| **Production Head** | `#E1D4F4` | `#7B1FA2` | Light purple background with darker purple text |
| **Admin** | `#FFE0E0` | `#D32F2F` | Light red background with darker red text |

### Toggle Switch Colors (iOS Green)

**Before:**
- Default Material3 colors

**After:**
- **Checked state**: Green track (#34C759) with white thumb
- **Unchecked state**: Light gray track (#E5E5EA) with white thumb

### Phone Number Text Color

**Before:** `#888888`
**After:** `#8E8E93` (iOS standard gray)

## Visual Improvements

### 1. Softer, More Pastel Look
- Light, pastel backgrounds instead of solid colors
- Better contrast and readability
- Less aggressive color palette

### 2. iOS Color Palette
- Matches iOS design guidelines
- Uses system colors where appropriate
- Green toggle matches iOS toggle color

### 3. Better Text Contrast
- Darker text on light backgrounds
- Improved accessibility
- Easier to read at a glance

## Color Reference

### User (Green)
- Background: `Color(0xFFD4F4DD)` - Very light mint green
- Text: `Color(0xFF34A853)` - Google Green

### Approver (Blue)
- Background: `Color(0xFFD6EBFF)` - Very light sky blue
- Text: `Color(0xFF1976D2)` - Material Blue 700

### Production Head (Purple)
- Background: `Color(0xFFE1D4F4)` - Very light lavender
- Text: `Color(0xFF7B1FA2)` - Material Purple 700

### Admin (Red)
- Background: `Color(0xFFFFE0E0)` - Very light pink/red
- Text: `Color(0xFFD32F2F)` - Material Red 700

### Toggle Colors
- Active: `Color(0xFF34C759)` - iOS Green
- Inactive: `Color(0xFFE5E5EA)` - iOS Gray

## Comparison

### Before:
```kotlin
// Solid background
color = Color(0xFF4CAF50)  // USER
// White text
color = Color.White
```

### After:
```kotlin
// Light pastel background
color = Color(0xFFD4F4DD)  // USER - Light green
// Darker matching text
color = Color(0xFF34A853)  // USER - Dark green
```

## Testing

To see the changes:
1. Build and run the app
2. Navigate to Production Head → User Management → View All Users
3. Observe the new soft, pastel badge colors
4. Toggle switches now have iOS-style green color

## Files Modified
- `app/src/main/java/com/deeksha/avr/ui/view/productionhead/ViewAllUsersScreen.kt`

## Design Philosophy
The new colors follow iOS Human Interface Guidelines:
- Subtle, light backgrounds
- High contrast text
- System-standard colors for interactive elements
- Consistent with the overall iOS aesthetic


