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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    
    // State for editing
    var editingApprover by remember { mutableStateOf<TemporaryApprover?>(null) }
    var showingDatePicker by remember { mutableStateOf(false) }
    var selectedDateType by remember { mutableStateOf("") } // "start" or "end"
    var showingApproverSelection by remember { mutableStateOf(false) }
    var selectedApprover by remember { mutableStateOf<User?>(null) }
    
    // State for dynamic approvers
    var availableApprovers by remember { mutableStateOf<List<User>>(emptyList()) }
    var isLoadingApprovers by remember { mutableStateOf(false) }
    var approverError by remember { mutableStateOf<String?>(null) }
    
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
                        text = "Approval Delegation Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: Add settings */ }) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF8B4513) // Dark brown color
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
            // Header Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Delegation",
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Approval Delegation Settings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF4285F4))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading delegations...",
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
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(temporaryApprovers) { tempApprover ->
                        TemporaryApproverCard(
                            temporaryApprover = if (editingApprover?.id == tempApprover.id) editingApprover!! else tempApprover,
                            isEditing = editingApprover?.id == tempApprover.id,
                            onEdit = { 
                                editingApprover = if (editingApprover?.id == tempApprover.id) null else tempApprover
                            },
                            onRemove = { 
                                temporaryApproverViewModel.removeTemporaryApproverById(projectId, tempApprover.id)
                            },
                            onSaveEdit = { updatedApprover ->
                                // Save updated delegation with the original document ID
                                val approverToUpdate = updatedApprover.copy(id = tempApprover.id)
                                temporaryApproverViewModel.updateTemporaryApprover(projectId, approverToUpdate)
                                editingApprover = null
                            },
                            onCancelEdit = {
                                editingApprover = null
                            },
                            onStartDateClick = {
                                selectedDateType = "start"
                                showingDatePicker = true
                            },
                            onEndDateClick = {
                                selectedDateType = "end"
                                showingDatePicker = true
                            }
                        )
                    }
                    
                    // Add new delegation card
                    item {
                        AddDelegationCard(
                            onClick = { 
                                showingApproverSelection = true
                            }
                        )
                    }
                }
            }
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
                onDismissRequest = { showingDatePicker = false },
                onDateSelected = { selectedDate ->
                    val timestamp = Timestamp(selectedDate)
                    val updatedApprover = when (selectedDateType) {
                        "start" -> currentApprover.copy(startDate = timestamp)
                        "end" -> currentApprover.copy(expiringDate = timestamp)
                        else -> currentApprover
                    }
                    editingApprover = updatedApprover
                    showingDatePicker = false
                },
                initialDate = currentDate
            )
        }
        
        // Approver Selection Dialog
        if (showingApproverSelection) {
            ApproverSelectionDialog(
                approvers = availableApprovers,
                isLoading = isLoadingApprovers,
                error = approverError,
                onApproverSelected = { approver ->
                    selectedApprover = approver
                    showingApproverSelection = false
                    // Create new delegation with selected approver
                    temporaryApproverViewModel.createTemporaryApprover(
                        projectId = projectId,
                        approverId = approver.uid,
                        approverName = approver.name,
                        approverPhone = approver.phone,
                        startDate = Timestamp.now(),
                        expiringDate = null
                    )
                },
                onDismiss = { showingApproverSelection = false }
            )
        }
    }
}

@Composable
private fun TemporaryApproverCard(
    temporaryApprover: TemporaryApprover,
    isEditing: Boolean = false,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onSaveEdit: (TemporaryApprover) -> Unit = {},
    onCancelEdit: () -> Unit = {},
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
            // Header with folder icon and department/category
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Department",
                    tint = Color(0xFF4285F4),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Temporary Approver: ${temporaryApprover.approverName}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Assigned To section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Assigned To:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = temporaryApprover.approverName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Phone number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Phone:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = temporaryApprover.approverPhone,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Date range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Start date
                Column {
                    Text(
                        text = "From:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEditing) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = if (isEditing) Modifier.clickable { onStartDateClick() } else Modifier
                    ) {
                        Text(
                            text = FormatUtils.formatDate(temporaryApprover.startDate),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                
                // End date
                Column {
                    Text(
                        text = "To:",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isEditing) Color(0xFFE3F2FD) else Color(0xFFF5F5F5)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = if (isEditing) Modifier.clickable { onEndDateClick() } else Modifier
                    ) {
                        Text(
                            text = if (temporaryApprover.expiringDate != null) {
                                FormatUtils.formatDate(temporaryApprover.expiringDate)
                            } else {
                                "Ongoing"
                            },
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isEditing) {
                    // Save button
                    Button(
                        onClick = { onSaveEdit(temporaryApprover) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Save",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    
                    // Cancel button
                    Button(
                        onClick = onCancelEdit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF757575)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cancel",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    // Edit button
                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF8B5FBF)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Edit",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    
                    // Remove button
                    Button(
                        onClick = onRemove,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE91E63)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Remove",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddDelegationCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF4285F4),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Add New Delegation",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4285F4)
                )
            }
            
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4285F4)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Assign",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
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
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text("Select Date")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                DatePicker(
                    state = datePickerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    colors = DatePickerDefaults.colors(
                        selectedDayContainerColor = Color(0xFF8B4513),
                        todayDateBorderColor = Color(0xFF8B4513),
                        dayContentColor = Color.Black,
                        weekdayContentColor = Color.Black,
                        yearContentColor = Color.Black
                    )
                )
            }
        },
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
    )
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
            Text("Select Approver")
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
                            CircularProgressIndicator(color = Color(0xFF4285F4))
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
