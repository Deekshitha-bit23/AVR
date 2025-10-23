# Temporary Approver Acceptance/Rejection Implementation

## Summary

This implementation adds a dialog flow that appears when an approver has a pending temporary approver assignment. The user must accept or reject the role before accessing projects.

## Files to Modify

### 1. TemporaryApproverViewModel.kt
Add function to get pending assignments for current user:

```kotlin
/**
 * Get pending temporary approver assignments for a user by phone number
 */
fun getPendingAssignmentsForUser(userPhone: String) {
    viewModelScope.launch {
        try {
            _isLoading.value = true
            _error.value = null
            
            Log.d(TAG, "🔄 Getting pending assignments for user: $userPhone")
            
            val result = temporaryApproverRepository.getPendingAssignmentsForUser(userPhone)
            
            if (result.isSuccess) {
                _temporaryApprovers.value = result.getOrNull() ?: emptyList()
                Log.d(TAG, "✅ Found ${_temporaryApprovers.value.size} pending assignments")
            } else {
                val exception = result.exceptionOrNull()
                _error.value = "Failed to get pending assignments: ${exception?.message}"
                Log.e(TAG, "❌ Failed to get pending assignments", exception)
            }
            
        } catch (e: Exception) {
            _error.value = "Error getting pending assignments: ${e.message}"
            Log.e(TAG, "❌ Exception while getting pending assignments", e)
        } finally {
            _isLoading.value = false
        }
    }
}
```

### 2. TemporaryApproverRepository.kt
Add function to fetch pending assignments:

```kotlin
/**
 * Get pending temporary approver assignments for a user by phone number
 */
suspend fun getPendingAssignmentsForUser(userPhone: String): Result<List<TemporaryApprover>> {
    return try {
        Log.d(TAG, "🔄 Getting pending assignments for user: $userPhone")
        
        // Query all projects to find pending temporary approver assignments
        val projectsSnapshot = firestore.collection(COLLECTION_PROJECTS)
            .get()
            .await()
        
        val pendingAssignments = mutableListOf<TemporaryApprover>()
        
        for (projectDoc in projectsSnapshot.documents) {
            val projectId = projectDoc.id
            
            // Query temporary approvers for this project
            val tempApproversSnapshot = projectDoc.reference
                .collection(SUBCOLLECTION_TEMP_APPROVERS)
                .whereEqualTo("approverPhone", userPhone)
                .whereEqualTo("status", "PENDING")
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            for (tempApproverDoc in tempApproversSnapshot.documents) {
                val tempApprover = tempApproverDoc.toObject(TemporaryApprover::class.java)
                if (tempApprover != null) {
                    pendingAssignments.add(tempApprover.copy(id = tempApproverDoc.id))
                }
            }
        }
        
        Log.d(TAG, "✅ Found ${pendingAssignments.size} pending assignments")
        Result.success(pendingAssignments)
        
    } catch (e: Exception) {
        Log.e(TAG, "❌ Failed to get pending assignments", e)
        Result.failure(e)
    }
}
```

### 3. ApproverProjectSelectionScreen.kt
Add dialogs for accept/reject flow at the beginning of the screen composable:

```kotlin
// State for temporary approver assignment dialog
var pendingAssignment by remember { mutableStateOf<TemporaryApprover?>(null) }
var showAcceptRejectDialog by remember { mutableStateOf(false) }
var showRejectReasonDialog by remember { mutableStateOf(false) }
var rejectionReason by remember { mutableStateOf("") }

// Check for pending assignments when user data loads
LaunchedEffect(currentUser) {
    if (currentUser != null && currentUser.phone.isNotEmpty()) {
        temporaryApproverViewModel.getPendingAssignmentsForUser(currentUser.phone)
    }
}

// Show dialog when pending assignment is found
LaunchedEffect(temporaryApprovers) {
    if (temporaryApprovers.isNotEmpty() && !showAcceptRejectDialog) {
        pendingAssignment = temporaryApprovers.first()
        showAcceptRejectDialog = true
    }
}

// Accept/Reject Dialog
if (showAcceptRejectDialog && pendingAssignment != null) {
    AlertDialog(
        onDismissRequest = { /* Don't allow dismiss without action */ },
        icon = {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Temporary Approver Role",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "You have been assigned as a temporary approver for a project. Please accept or reject this role to continue.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Project: ${pendingAssignment?.projectId}", fontSize = 12.sp)
                        Text("Assigned by: ${pendingAssignment?.assignedByName}", fontSize = 12.sp)
                        pendingAssignment?.expiringDate?.let {
                            Text("Valid until: ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(it.toDate())}", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    showAcceptRejectDialog = false
                    pendingAssignment?.let { assignment ->
                        temporaryApproverViewModel.acceptTemporaryApproverAssignment(
                            projectId = assignment.projectId,
                            approverId = currentUser?.phone ?: "",
                            responseMessage = "Accepted"
                        )
                    }
                    pendingAssignment = null
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("Accept Role")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    showAcceptRejectDialog = false
                    showRejectReasonDialog = true
                }
            ) {
                Text("Reject Role", color = Color.Red)
            }
        }
    )
}

// Rejection Reason Dialog
if (showRejectReasonDialog && pendingAssignment != null) {
    AlertDialog(
        onDismissRequest = { /* Don't allow dismiss without action */ },
        icon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color.Red,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = "Reject Temporary Approver Role",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    text = "Please provide a reason for rejecting this temporary approver role. This will help us understand your decision.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = rejectionReason,
                    onValueChange = { rejectionReason = it },
                    label = { Text("Reason for Rejection") },
                    placeholder = { Text("Enter your reason...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    showRejectReasonDialog = false
                    pendingAssignment?.let { assignment ->
                        temporaryApproverViewModel.rejectTemporaryApproverAssignment(
                            projectId = assignment.projectId,
                            approverId = currentUser?.phone ?: "",
                            responseMessage = rejectionReason
                        )
                    }
                    pendingAssignment = null
                    rejectionReason = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                enabled = rejectionReason.isNotBlank()
            ) {
                Text("Confirm Rejection")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    showRejectReasonDialog = false
                    showAcceptRejectDialog = true
                    rejectionReason = ""
                }
            ) {
                Text("Cancel")
            }
        }
    )
}
```

## Implementation Steps

1. Add `getPendingAssignmentsForUser()` to TemporaryApproverRepository
2. Add `getPendingAssignmentsForUser()` to TemporaryApproverViewModel  
3. Add TemporaryApproverViewModel to ApproverProjectSelectionScreen
4. Add dialog states and LaunchedEffects
5. Add Accept/Reject dialog composables
6. Add Rejection Reason dialog composable

## User Flow

1. User logs in with phone number that has a pending assignment
2. Opens project selection screen
3. Dialog appears automatically asking to accept/reject
4. If Accept → Assignment marked as ACCEPTED, user can access project
5. If Reject → Reason dialog appears
6. User enters reason and confirms
7. Assignment marked as REJECTED, removed from system

## Testing

- Assign a temporary approver to a project
- Login as that approver
- Check that dialog appears
- Test accept flow
- Test reject flow with reason
- Verify Firestore updates correctly



