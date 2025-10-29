package com.deeksha.avr.ui.view.productionhead

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.model.TemporaryApprover
import com.deeksha.avr.model.User
import com.deeksha.avr.model.UserRole
import com.deeksha.avr.utils.FormatUtils
import com.deeksha.avr.viewmodel.TemporaryApproverViewModel
import com.deeksha.avr.viewmodel.AuthViewModel
import com.deeksha.avr.viewmodel.ProductionHeadViewModel
import com.deeksha.avr.repository.AuthRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelegationScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    temporaryApproverViewModel: TemporaryApproverViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    productionHeadViewModel: ProductionHeadViewModel = hiltViewModel()
) {
    val temporaryApprovers by temporaryApproverViewModel.temporaryApprovers.collectAsState()
    val isLoading by temporaryApproverViewModel.isLoading.collectAsState()
    val error by temporaryApproverViewModel.error.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    
    // Project data for filtering
    val editingProject by productionHeadViewModel.editingProject.collectAsState()
    
    // State for approver selection
    var showingApproverSelection by remember { mutableStateOf(false) }
    var availableApprovers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoadingApprovers by remember { mutableStateOf(false) }
    var approverError by remember { mutableStateOf<String?>(null) }
    
    // State for date editing
    var showingDatePicker by remember { mutableStateOf(false) }
    var selectedDateType by remember { mutableStateOf("") } // "start" or "end"
    var editingApprover by remember { mutableStateOf<TemporaryApprover?>(null) }
    
    // State for confirmation dialog
    var showingDeleteConfirmation by remember { mutableStateOf(false) }
    
    // State for approver selection with dates
    var selectedApproverForAssignment by remember { mutableStateOf<User?>(null) }
    var tempStartDate by remember { mutableStateOf<Date?>(null) }
    var tempEndDate by remember { mutableStateOf<Date?>(null) }
    var showDateSelectionDialog by remember { mutableStateOf(false) }
    var isAssigningApprover by remember { mutableStateOf(false) }
    
    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()
    
    // Get the current temporary approver (first one if exists)
    val currentApprover = temporaryApprovers.firstOrNull()
    
    // Load data when screen opens
    LaunchedEffect(projectId) {
        temporaryApproverViewModel.loadTemporaryApprovers(projectId)
        productionHeadViewModel.loadProjectForEdit(projectId)
    }
    
    // Reload temporary approvers when assignment completes
    LaunchedEffect(isAssigningApprover) {
        if (!isAssigningApprover) {
            // Assignment completed, reload data
            temporaryApproverViewModel.loadTemporaryApprovers(projectId)
        }
    }
    
    // Load approvers when showing selection dialog
    LaunchedEffect(showingApproverSelection, editingProject, currentApprover) {
        if (showingApproverSelection) {
            isLoadingApprovers = true
            approverError = null
            try {
                val authRepository = AuthRepository(FirebaseFirestore.getInstance())
                val allApprovers = authRepository.getUsersByRole(UserRole.APPROVER)
                
                // Filter out permanent approver and current temporary approver
                val filteredApprovers = allApprovers.filter { approver ->
                    val isPermanentApprover = editingProject?.managerId == approver.phone
                    val isCurrentTemporaryApprover = currentApprover?.approverPhone == approver.phone
                    
                    !isPermanentApprover && !isCurrentTemporaryApprover
                }
                
                availableApprovers = filteredApprovers
            } catch (e: Exception) {
                approverError = e.message ?: "Failed to load approvers"
            } finally {
                isLoadingApprovers = false
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Delegate Management",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFF007AFF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { /* Save functionality can be added here */ }
                    ) {
                        Text(
                            text = "Save",
                            color = Color(0xFF007AFF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8F8F8) // Light gray background
                )
            )
        },
        containerColor = Color(0xFFF8F8F5)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF1976D2))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading delegate information...",
                            color = Color.Gray
                        )
                    }
                }
            } else if (error != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Error: $error",
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Delegate Management Summary Card
                DelegateManagementSummaryCard(
                    currentApprover = currentApprover
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Delegate Information Card
                DelegateInformationCard(
                    currentApprover = currentApprover,
                    onStartDateClick = {
                        editingApprover = currentApprover
                        selectedDateType = "start"
                        showingDatePicker = true
                    },
                    onEndDateClick = {
                        editingApprover = currentApprover
                        selectedDateType = "end"
                        showingDatePicker = true
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action Buttons - No card wrapper, just buttons
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Change Temp Approver Button
                    Button(
                        onClick = {
                            showingApproverSelection = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1976D2)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = if (currentApprover != null) "Change Temp Approver" else "Add Temp Approver",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentApprover != null) "Change Temp Approver" else "Add Temp Approver",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    // Remove Temp Approver Button - only show if there's a current approver
                    if (currentApprover != null) {
                        Button(
                            onClick = {
                                showingDeleteConfirmation = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove Temp Approver",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Remove Temp Approver",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    // Refresh Details Button
                    OutlinedButton(
                        onClick = {
                            temporaryApproverViewModel.loadTemporaryApprovers(projectId)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF1976D2)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh Details",
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Refresh Details",
                            color = Color(0xFF1976D2),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        
        // Approver Selection Dialog
        if (showingApproverSelection) {
            ApproverSelectionDialog(
                approvers = availableApprovers,
                isLoading = isLoadingApprovers,
                error = approverError,
                onApproverSelected = { approver ->
                    selectedApproverForAssignment = approver
                    tempStartDate = Date() // Default to current date
                    tempEndDate = null // Default to no end date
                    showingApproverSelection = false
                    showDateSelectionDialog = true
                },
                onDismiss = { showingApproverSelection = false }
            )
        }
        
        // Date Picker Dialog
        if (showingDatePicker && editingApprover != null) {
            val currentApprover = editingApprover!!
            val currentDate = when (selectedDateType) {
                "start" -> currentApprover.startDate.toDate()
                "end" -> currentApprover.expiringDate?.toDate() ?: Date()
                else -> Date()
            }
            
            DatePickerDialog(
                onDismissRequest = { 
                    showingDatePicker = false
                    editingApprover = null
                },
                onDateSelected = { selectedDate ->
                    val timestamp = Timestamp(selectedDate)
                    val updatedApprover = when (selectedDateType) {
                        "start" -> currentApprover.copy(
                            startDate = timestamp,
                            updatedAt = Timestamp.now()
                        )
                        "end" -> currentApprover.copy(
                            expiringDate = timestamp,
                            updatedAt = Timestamp.now()
                        )
                        else -> currentApprover
                    }
                    
                    // Update the temporary approver
                    val currentUser = authState.user
                    val changedBy = currentUser?.name ?: "Unknown User"
                    
                    coroutineScope.launch {
                        try {
                            // Use specific update methods for better reliability
                            if (selectedDateType == "start") {
                                temporaryApproverViewModel.updateStartDate(
                                    projectId = projectId,
                                    tempApproverId = currentApprover.id,
                                    newStartDate = timestamp
                                )
                            } else if (selectedDateType == "end") {
                                temporaryApproverViewModel.updateExpiringDate(
                                    projectId = projectId,
                                    tempApproverId = currentApprover.id,
                                    newExpiringDate = timestamp.toDate()
                                )
                            }
                            
                            // Wait for update to complete
                            delay(500)
                            
                            // Refresh the data to ensure UI updates
                            temporaryApproverViewModel.loadTemporaryApprovers(projectId)
                            
                        } catch (e: Exception) {
                            println("Error updating date: ${e.message}")
                            // Fallback to general update method
                            try {
                                temporaryApproverViewModel.updateTemporaryApprover(
                                    projectId = projectId,
                                    updatedApprover = updatedApprover,
                                    originalApprover = currentApprover,
                                    changedBy = changedBy
                                )
                                delay(300)
                                temporaryApproverViewModel.loadTemporaryApprovers(projectId)
                            } catch (fallbackError: Exception) {
                                println("Fallback update also failed: ${fallbackError.message}")
                            }
                        }
                    }
                    
                    showingDatePicker = false
                    editingApprover = null
                },
                initialDate = currentDate
            )
        }
        
        // Date Selection Dialog for Temporary Approver Assignment
        if (showDateSelectionDialog && selectedApproverForAssignment != null) {
            TemporaryApproverDateSelectionDialog(
                approver = selectedApproverForAssignment!!,
                startDate = tempStartDate,
                endDate = tempEndDate,
                isAssigning = isAssigningApprover,
                onStartDateChanged = { tempStartDate = it },
                onEndDateChanged = { tempEndDate = it },
                onConfirm = {
                    isAssigningApprover = true
                    coroutineScope.launch {
                        try {
                            // Remove existing approver first if it exists
                            if (currentApprover != null) {
                                temporaryApproverViewModel.removeTemporaryApproverById(
                                    projectId = projectId, 
                                    documentId = currentApprover.id
                                )
                                
                                // Wait for deletion to complete
                                delay(1000)
                            }
                            
                            // Create new delegation with selected approver and dates
                            temporaryApproverViewModel.createTemporaryApprover(
                                projectId = projectId,
                                approverId = selectedApproverForAssignment!!.uid,
                                approverName = selectedApproverForAssignment!!.name,
                                approverPhone = selectedApproverForAssignment!!.phone,
                                startDate = Timestamp(tempStartDate ?: Date()),
                                expiringDate = tempEndDate?.let { Timestamp(it) }
                            )
                            
                            // Wait for creation to complete
                            delay(1000)
                            
                            // Refresh the data to ensure UI updates
                            temporaryApproverViewModel.loadTemporaryApprovers(projectId)
                            
                            // Wait for data to load
                            delay(500)
                            
                            // Close dialog and reset state
                            showDateSelectionDialog = false
                            selectedApproverForAssignment = null
                            tempStartDate = null
                            tempEndDate = null
                            isAssigningApprover = false
                            
                        } catch (e: Exception) {
                            // Handle any errors
                            println("Error assigning temp approver: ${e.message}")
                            isAssigningApprover = false
                        }
                    }
                },
                onDismiss = {
                    showDateSelectionDialog = false
                    selectedApproverForAssignment = null
                    tempStartDate = null
                    tempEndDate = null
                }
            )
        }
        
        // Delete Confirmation Dialog
        if (showingDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showingDeleteConfirmation = false },
                title = {
                    Text("Remove Temporary Approver")
                },
                text = {
                    Text("Are you sure you want to completely remove this temporary approver from the project? This action cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            currentApprover?.let { approver ->
                                coroutineScope.launch {
                                    try {
                                        temporaryApproverViewModel.removeTemporaryApproverById(
                                            projectId = projectId,
                                            documentId = approver.id
                                        )
                                        // Refresh the data to ensure UI updates
                                        temporaryApproverViewModel.loadTemporaryApprovers(projectId)
                                    } catch (e: Exception) {
                                        println("Error removing temp approver: ${e.message}")
                                    }
                                }
                            }
                            showingDeleteConfirmation = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD32F2F))
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showingDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun DelegateManagementSummaryCard(
    currentApprover: TemporaryApprover?
) {
    // Determine status - Active if accepted and currently active
    val isActiveStatus = currentApprover?.status == "ACCEPTED" && currentApprover.isActive
    
    // Format last updated date with time
    val lastUpdatedDate = currentApprover?.updatedAt ?: Timestamp.now()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(lastUpdatedDate.toDate())
    val formattedTime = timeFormat.format(lastUpdatedDate.toDate())
    val lastUpdatedText = "Last updated: $formattedDate at $formattedTime"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large circular blue icon with person icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Color(0xFF1976D2),
                            shape = RoundedCornerShape(28.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = "Delegate Management",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Delegate Management",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Manage temporary approver details",
                        fontSize = 14.sp,
                        color = Color(0xFF757575)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status pill - green for Active
                Box(
                    modifier = Modifier
                        .background(
                            if (isActiveStatus) Color(0xFF4CAF50) else Color(0xFFFF9800),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isActiveStatus) "Active" else "Pending",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Last updated text
                Text(
                    text = lastUpdatedText,
                    fontSize = 12.sp,
                    color = Color(0xFF757575)
                )
            }
        }
    }
}

@Composable
private fun DelegateInformationCard(
    currentApprover: TemporaryApprover?,
    onStartDateClick: () -> Unit = {},
    onEndDateClick: () -> Unit = {}
) {
    // Helper function to format date with time
    fun formatDateWithTime(timestamp: Timestamp?): String {
        if (timestamp == null) return "Ongoing"
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val date = timestamp.toDate()
        return "${dateFormat.format(date)} at ${timeFormat.format(date)}"
    }
    
    // Determine status
    val isActiveStatus = currentApprover?.status == "ACCEPTED" && currentApprover.isActive
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with circular blue icon
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            Color(0xFF1976D2),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Delegate Information",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Delegate Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (currentApprover != null) {
                // Name section
                InfoRow(
                    icon = Icons.Default.Person,
                    label = "NAME",
                    value = currentApprover.approverName
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Phone section
                InfoRow(
                    icon = Icons.Default.Phone,
                    label = "PHONE NUMBER",
                    value = currentApprover.approverPhone
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Divider line
                Divider(
                    color = Color(0xFFE0E0E0),
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Start date section - with Add icon overlay
                InfoRowWithArrow(
                    icon = Icons.Default.CalendarToday,
                    label = "START DATE & TIME",
                    value = formatDateWithTime(currentApprover.startDate),
                    onClick = onStartDateClick,
                    showAddIcon = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // End date section - with Remove icon overlay
                InfoRowWithArrow(
                    icon = Icons.Default.CalendarToday,
                    label = "END DATE & TIME",
                    value = if (currentApprover.expiringDate != null) {
                        formatDateWithTime(currentApprover.expiringDate)
                    } else {
                        "Ongoing"
                    },
                    onClick = onEndDateClick,
                    showRemoveIcon = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Current status section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Current Status",
                            tint = Color(0xFF1976D2),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Current Status",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    
                    // Status pill - green for Active
                    Box(
                        modifier = Modifier
                            .background(
                                if (isActiveStatus) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isActiveStatus) "Active" else "Pending",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // No delegate assigned
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No delegate assigned",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = Color(0xFF1976D2),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF757575),
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun InfoRowWithArrow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit = {},
    showAddIcon: Boolean = false,
    showRemoveIcon: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with optional Add/Remove overlay
            Box(
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.Center)
                )
                // Add icon overlay (top-right corner)
                if (showAddIcon) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add",
                        tint = Color(0xFF1976D2),
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                    )
                }
                // Remove icon overlay (bottom-right corner)
                if (showRemoveIcon) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Remove",
                        tint = Color(0xFF1976D2),
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = Color(0xFF757575),
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.Black
                )
            }
        }
        
        Icon(
            Icons.Default.ArrowForward,
            contentDescription = "Arrow",
            tint = Color(0xFF757575),
            modifier = Modifier.size(16.dp)
        )
    }
}


@Composable
private fun ApproverSelectionDialog(
    approvers: List<User>,
    isLoading: Boolean = false,
    error: String? = null,
    onApproverSelected: (User) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Select New Approver")
        },
        text = {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFF1976D2))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading approvers...",
                                color = Color.Gray
                            )
                        }
                    }
                }
                error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = Color.Red,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Error: $error",
                                color = Color.Red,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                approvers.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No approvers available",
                            color = Color.Gray
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 400.dp)
                    ) {
                        items(approvers) { approver ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onApproverSelected(approver) },
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = approver.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = approver.phone,
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    onDateSelected: (Date) -> Unit,
    initialDate: Date
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.time,
        yearRange = IntRange(2024, 2030)
    )
    
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Date(millis))
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            title = {
                Text(
                    text = "Select Date",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            showModeToggle = false,
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = Color(0xFF1976D2),
                todayDateBorderColor = Color(0xFF1976D2),
                dayContentColor = Color.Black,
                weekdayContentColor = Color.Black,
                yearContentColor = Color.Black
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemporaryApproverDateSelectionDialog(
    approver: User,
    startDate: Date?,
    endDate: Date?,
    isAssigning: Boolean = false,
    onStartDateChanged: (Date?) -> Unit,
    onEndDateChanged: (Date?) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    val startDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = startDate?.time ?: System.currentTimeMillis(),
        yearRange = IntRange(2024, 2030)
    )
    
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = endDate?.time ?: System.currentTimeMillis(),
        yearRange = IntRange(2024, 2030)
    )
    
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Set Assignment Dates")
        },
        text = {
            if (isAssigning) {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF1976D2))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Assigning temporary approver...",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Selected Approver Info
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Selected Approver",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = approver.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = approver.phone,
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Start Date Selection
                    OutlinedTextField(
                        value = startDate?.let { dateFormatter.format(it) } ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Start Date") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "Select Start Date",
                                modifier = Modifier.clickable { showStartDatePicker = true }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartDatePicker = true },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1976D2),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // End Date Selection
                    OutlinedTextField(
                        value = endDate?.let { dateFormatter.format(it) } ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("End Date (Optional)") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "Select End Date",
                                modifier = Modifier.clickable { showEndDatePicker = true }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEndDatePicker = true },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF1976D2),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Clear End Date Button
                    TextButton(
                        onClick = { onEndDateChanged(null) },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Clear End Date")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = startDate != null && !isAssigning
            ) {
                if (isAssigning) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color(0xFF1976D2)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Assigning...")
                    }
                } else {
                    Text("Assign")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
    
    // Start Date Picker
    if (showStartDatePicker) {
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDatePickerState.selectedDateMillis?.let { millis ->
                            onStartDateChanged(Date(millis))
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = startDatePickerState,
                modifier = Modifier.padding(16.dp),
                title = {
                    Text(
                        text = "Select Start Date",
                        modifier = Modifier.padding(16.dp)
                    )
                },
                showModeToggle = false
            )
        }
    }
    
    // End Date Picker
    if (showEndDatePicker) {
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        endDatePickerState.selectedDateMillis?.let { millis ->
                            onEndDateChanged(Date(millis))
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = endDatePickerState,
                modifier = Modifier.padding(16.dp),
                title = {
                    Text(
                        text = "Select End Date",
                        modifier = Modifier.padding(16.dp)
                    )
                },
                showModeToggle = false
            )
        }
    }
}
