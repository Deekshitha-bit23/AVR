# Team Assignment Section Refactor Needed

## Current Status

I've started redesigning the Team Assignment section to match the iOS design, but the implementation has become too complex with nested Card structures and dropdown menus.

## Issues

1. **Complex Nested Structure:** The current implementation has Cards within Cards within Cards, making the code hard to maintain
2. **Inconsistent Styling:** Mix of iOS colors (#007AFF) and Material colors (#4285F4)
3. **Broken Compilation:** Missing closing braces causing build failures

## iOS Design Requirements

From the reference images:

### Team Assignment Section Should Have:
- **White Card** with iOS blue icon
- **Section Header:** "TEAM ASSIGNMENT" with person icon in iOS blue
- **Project Manager (Approver)**
  - Label: "Project Manager (Approver)"
  - Subtext: "X approvers available" in iOS gray
  - Simple search field: "Search name or phone number..."
  - iOS blue icons (search and dropdown)
- **Team Members (Users)**
  - Label: "Team Members (Users)"
  - Subtext: "X team members available" in iOS gray
  - Simple search field: "Search name or phone number..."
  - iOS blue icons

### Current Problems

1. The section starts properly with a white Card at line 347
2. But then nested Cards for search fields start at line 446
3. Team Members section is OUTSIDE the Card (starts at line 576)
4. This breaks the structure

## Recommended Fix

The entire Team Assignment section (including Project Manager AND Team Members) should be inside ONE white Card with:
1. Header with "TEAM ASSIGNMENT" label
2. Project Manager subsection
3. Separator
4. Team Members subsection

Currently Team Members (lines 576+) are OUTSIDE the white Card, breaking the iOS design.

## Files to Fix

- `app/src/main/java/com/deeksha/avr/ui/view/productionhead/NewProjectScreen.kt`
  - Lines 346-575: Team Assignment Card (partially done)
  - Lines 576-800: Team Members (needs to be inside the Card)
  - Missing closing braces around line 572-575

## Next Steps

1. Move Team Members section INSIDE the white Card
2. Remove duplicate nested Cards
3. Simplify search fields to single OutlinedTextField per subsection
4. Use consistent iOS colors (#007AFF)
5. Add proper closing braces

## Build Status
❌ **Build failed** - Missing closing braces and structural issues

