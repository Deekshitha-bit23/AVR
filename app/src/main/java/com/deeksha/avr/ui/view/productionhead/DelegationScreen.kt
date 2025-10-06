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
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val temporaryApprovers by temporaryApproverViewModel.temporaryApprovers.collectAsState()
    val isLoading by temporaryApproverViewModel.isLoading.collectAsState()
    val error by temporaryApproverViewModel.error.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    
    // State for approver selection
    var showingApproverSelection by remember { mutableStateOf(false) }
    var availableApprovers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoadingApprovers by remember { mutableStateOf(false) }
    var approverError by remember { mutableStateOf<String?>(null) }
    
    // State for date editing
    var showingDatePicker by remember { mutableStateOf(false) }
    var selectedDateType by remember { mutableStateOf("") } // "start" or "end"
    var editingApprover by remember { mutableStateOf<TemporaryApprover?>(null) }
    
    // Coroutine scope for async operations
    val coroutineScope = rememberCoroutineScope()
    
    // Get the current temporary approver (first one if exists)
    val currentApprover = temporaryApprovers.firstOrNull()
    
    // Load data when screen opens
    LaunchedEffect(projectId) {
        temporaryApproverViewModel.loadTemporaryApprovers(projectId)
    }
    
    // Load approvers when showing selection dialog
    LaunchedEffect(showingApproverSelection) {
        if (showingApproverSelection) {
            isLoadingApprovers = true
            approverError = null
            try {
                val authRepository = AuthRepository(FirebaseFirestore.getInstance())
                val approvers = authRepository.getUsersByRole(UserRole.APPROVER)
                availableApprovers = approvers
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
                        color = Color.White
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text(
                            text = "Cancel",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { /* Save functionality can be added here */ }) {
                        Text(
                            text = "Save",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2) // Blue color
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
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
                
                // Action Buttons Card
                ActionButtonsCard(
                    onChangeTempApprover = {
                        showingApproverSelection = true
                    },
                    onRefreshDetails = {
                        temporaryApproverViewModel.loadTemporaryApprovers(projectId)
                    }
                )
            }
        }
        
        // Approver Selection Dialog
        if (showingApproverSelection) {
            ApproverSelectionDialog(
                approvers = availableApprovers,
                isLoading = isLoadingApprovers,
                error = approverError,
                onApproverSelected = { approver ->
                    coroutineScope.launch {
                        try {
                            // Remove existing approver first if it exists
                            if (currentApprover != null) {
                                temporaryApproverViewModel.removeTemporaryApproverById(
                                    projectId = projectId, 
                                    documentId = currentApprover.id
                                )
                                
                                // Wait for deletion to complete
                                delay(500)
                            }
                            
                            // Create new delegation with selected approver
                            temporaryApproverViewModel.createTemporaryApprover(
                                projectId = projectId,
                                approverId = approver.uid,
                                approverName = approver.name,
                                approverPhone = approver.phone,
                                startDate = Timestamp.now(),
                                expiringDate = null
                            )
                            
                            // Refresh the data to ensure UI updates
                            temporaryApproverViewModel.loadTemporaryApprovers(projectId)
                            
                        } catch (e: Exception) {
                            // Handle any errors
                            println("Error replacing temp approver: ${e.message}")
                        }
                    }
                    showingApproverSelection = false
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
    }
}

@Composable
private fun DelegateManagementSummaryCard(
    currentApprover: TemporaryApprover?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Person with clock icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            Color(0xFF1976D2),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.People,
                        contentDescription = "Delegate Management",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Delegate Management",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "Manage temporary approver details.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pending status pill
                Box(
                    modifier = Modifier
                        .background(
                            Color(0xFFFF9800),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Pending",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Last updated text
                Text(
                    text = "Last updated: ${FormatUtils.formatDate(Timestamp.now())}",
                    fontSize = 12.sp,
                    color = Color.Gray
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "Delegate Information",
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Delegate Information",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (currentApprover != null) {
                // Name section
                InfoRow(
                    icon = Icons.Default.Person,
                    label = "NAME",
                    value = currentApprover.approverName
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Phone section
                InfoRow(
                    icon = Icons.Default.Phone,
                    label = "PHONE NUMBER",
                    value = currentApprover.approverPhone
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Start date section
                InfoRowWithArrow(
                    icon = Icons.Default.CalendarToday,
                    label = "START DATE & TIME",
                    value = FormatUtils.formatDate(currentApprover.startDate),
                    onClick = onStartDateClick
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // End date section
                InfoRowWithArrow(
                    icon = Icons.Default.CalendarToday,
                    label = "END DATE & TIME",
                    value = if (currentApprover.expiringDate != null) {
                        FormatUtils.formatDate(currentApprover.expiringDate)
                    } else {
                        "Ongoing"
                    },
                    onClick = onEndDateClick
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
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
                    
                    // Pending status pill
                    Box(
                        modifier = Modifier
                            .background(
                                Color(0xFFFF9800),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Pending",
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = Color(0xFF1976D2),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
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
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color(0xFF1976D2),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
        
        Icon(
            Icons.Default.ArrowForward,
            contentDescription = "Arrow",
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun ActionButtonsCard(
    onChangeTempApprover: () -> Unit,
    onRefreshDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Change Temp Approver Button
            Button(
                onClick = onChangeTempApprover,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1976D2)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.People,
                    contentDescription = "Change Temp Approver",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Change Temp Approver",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Refresh Details Button
            OutlinedButton(
                onClick = onRefreshDetails,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF1976D2)
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp
                ),
                shape = RoundedCornerShape(8.dp)
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
