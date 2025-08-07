package com.deeksha.avr.ui.view.productionhead

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.model.DepartmentBudget
import com.deeksha.avr.model.Project
import com.deeksha.avr.model.User
import com.deeksha.avr.model.UserRole
import com.deeksha.avr.viewmodel.ProductionHeadViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProjectScreen(
    projectId: String,
    onNavigateBack: () -> Unit,
    viewModel: ProductionHeadViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val editingProject by viewModel.editingProject.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    
    val availableUsers by viewModel.availableUsers.collectAsState()
    val availableApprovers by viewModel.availableApprovers.collectAsState()
    val departmentBudgets by viewModel.departmentBudgets.collectAsState()
    val totalBudget by viewModel.totalBudget.collectAsState()
    val totalAllocated by viewModel.totalAllocated.collectAsState()
    
    // Form state
    var projectName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedManagerId by remember { mutableStateOf("") }
    var selectedTeamMembers by remember { mutableStateOf(setOf<String>()) }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var newDepartmentName by remember { mutableStateOf("") }
    var newDepartmentBudget by remember { mutableStateOf("") }
    
    // Date picker states
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()
    
    // Date formatter
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    
    // Load project for editing when screen opens
    LaunchedEffect(projectId) {
        viewModel.loadProjectForEdit(projectId)
    }
    
    // Update form when project is loaded
    LaunchedEffect(editingProject) {
        editingProject?.let { project ->
            projectName = project.name
            description = project.description
            selectedManagerId = project.managerId
            selectedTeamMembers = project.teamMembers.toSet()
            
            project.startDate?.let { start ->
                startDate = start.toDate()
            }
            
            project.endDate?.let { end ->
                endDate = end.toDate()
            }
        }
    }
    
    // Handle success message
    LaunchedEffect(successMessage) {
        successMessage?.let {
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Edit Project",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4285F4)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF4285F4)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4285F4))
            }
        } else if (editingProject != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Error message
                error?.let { errorMsg ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMsg,
                                color = Color(0xFFD32F2F),
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                
                // Project Name
                item {
                    OutlinedTextField(
                        value = projectName,
                        onValueChange = { projectName = it },
                        label = { Text("Project Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
                
                // Description
                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
                
                // Start Date
                item {
                    OutlinedTextField(
                        value = startDate?.let { dateFormatter.format(it) } ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Start Date") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "Select Date",
                                modifier = Modifier.clickable { showStartDatePicker = true }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartDatePicker = true },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
                
                // End Date
                item {
                    OutlinedTextField(
                        value = endDate?.let { dateFormatter.format(it) } ?: "",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("End Date (Optional)") },
                        trailingIcon = {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "Select Date",
                                modifier = Modifier.clickable { showEndDatePicker = true }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEndDatePicker = true },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
                
                // Total Budget
                item {
                    OutlinedTextField(
                        value = totalBudget.toString(),
                        onValueChange = { 
                            val budget = it.toDoubleOrNull() ?: 0.0
                            viewModel.updateTotalBudget(budget)
                        },
                        label = { Text("Total Budget") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF4285F4),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
                
                // Remaining Budget Indicator
                item {
                    val remainingBudget = totalBudget - totalAllocated
                    if (totalBudget > 0) {
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
                                    contentDescription = "Info",
                                    tint = if (remainingBudget >= 0) Color(0xFF4CAF50) else Color(0xFFD32F2F),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (remainingBudget >= 0) "Remaining for allocation:" else "Over-allocated:",
                                    fontSize = 12.sp,
                                    color = if (remainingBudget >= 0) Color(0xFF4CAF50) else Color(0xFFD32F2F),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "₹${String.format("%.2f", remainingBudget)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (remainingBudget >= 0) Color(0xFF4CAF50) else Color(0xFFD32F2F)
                            )
                        }
                    }
                }
                
                // Manager Selection
                item {
                    Text(
                        text = "Project Manager",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
                
                item {
                    availableApprovers.forEach { approver ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedManagerId == approver.phone,
                                onClick = { selectedManagerId = approver.phone }
                            )
                            Text(
                                text = "${approver.name} (${approver.phone})",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                
                // Team Members Selection
                item {
                    Text(
                        text = "Team Members",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
                
                item {
                    availableUsers.forEach { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedTeamMembers.contains(user.phone),
                                onCheckedChange = { checked ->
                                    selectedTeamMembers = if (checked) {
                                        selectedTeamMembers + user.phone
                                    } else {
                                        selectedTeamMembers - user.phone
                                    }
                                }
                            )
                            Text(
                                text = "${user.name} (${user.phone})",
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                
                // Department Budgets
                item {
                    Text(
                        text = "Department Budgets",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Black
                    )
                }
                
                // Add new department budget
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            OutlinedTextField(
                                value = newDepartmentName,
                                onValueChange = { newDepartmentName = it },
                                label = { Text("Department Name") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4285F4),
                                    unfocusedBorderColor = Color.Gray
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = newDepartmentBudget,
                                onValueChange = { newDepartmentBudget = it },
                                label = { Text("Budget Amount") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4285F4),
                                    unfocusedBorderColor = Color.Gray
                                )
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val remainingBudget = totalBudget - totalAllocated
                            Button(
                                onClick = {
                                    if (newDepartmentName.isNotBlank() && newDepartmentBudget.isNotBlank()) {
                                        viewModel.addDepartmentBudget(
                                            newDepartmentName,
                                            newDepartmentBudget.toDoubleOrNull() ?: 0.0
                                        )
                                        newDepartmentName = ""
                                        newDepartmentBudget = ""
                                    }
                                },
                                enabled = remainingBudget > 0,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (remainingBudget > 0) Color(0xFF4285F4) else Color(0xFFE0E0E0)
                                )
                            ) {
                                Text(
                                    text = if (remainingBudget > 0) "Add Department Budget" else "No Budget Remaining",
                                    color = if (remainingBudget > 0) Color.White else Color(0xFF9E9E9E)
                                )
                            }
                        }
                    }
                }
                
                // Display existing department budgets
                items(departmentBudgets) { deptBudget ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = deptBudget.departmentName,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "₹${deptBudget.allocatedBudget}",
                                    color = Color.Gray
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    viewModel.removeDepartmentBudget(deptBudget.departmentName)
                                }
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    tint = Color(0xFFD32F2F)
                                )
                            }
                        }
                    }
                }
                
                // Budget Summary
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Budget Summary",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4285F4)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val remainingBudget = totalBudget - totalAllocated
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Allocated:")
                                Text(
                                    text = "₹$totalAllocated",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Budget:")
                                Text(
                                    text = "₹$totalBudget",
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Remaining Budget:",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "₹$remainingBudget",
                                    fontWeight = FontWeight.Bold,
                                    color = if (remainingBudget >= 0) Color(0xFF4CAF50) else Color(0xFFD32F2F)
                                )
                            }
                            
                            if (totalAllocated > totalBudget) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Allocated amount exceeds total budget!",
                                        color = Color(0xFFD32F2F),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else if (remainingBudget > 0) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "Info",
                                        tint = Color(0xFF2196F3),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "₹$remainingBudget available for allocation",
                                        color = Color(0xFF2196F3),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Update Project Button
                item {
                    Button(
                        onClick = {
                            if (projectName.isNotBlank() && selectedManagerId.isNotBlank() && selectedTeamMembers.isNotEmpty()) {
                                val startTimestamp = startDate?.let { Timestamp(it) } ?: Timestamp.now()
                                val endTimestamp = endDate?.let { Timestamp(it) }
                                
                                viewModel.updateProject(
                                    projectId = projectId,
                                    projectName = projectName,
                                    description = description,
                                    startDate = startTimestamp,
                                    endDate = endTimestamp,
                                    totalBudget = totalBudget,
                                    managerId = selectedManagerId,
                                    teamMemberIds = selectedTeamMembers.toList(),
                                    departmentBudgets = departmentBudgets
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                        enabled = projectName.isNotBlank() && selectedManagerId.isNotBlank() && selectedTeamMembers.isNotEmpty()
                    ) {
                        Text("Update Project")
                    }
                }
                
                // Bottom spacing
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        } else {
            // Project not found
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Project not found")
            }
        }
    }
    
    // Start Date Picker
    if (showStartDatePicker) {
        CustomDatePickerDialog(
            onDateSelected = { selectedDate ->
                startDate = selectedDate
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false },
            datePickerState = startDatePickerState,
            title = "Select Start Date"
        )
    }
    
    // End Date Picker
    if (showEndDatePicker) {
        CustomDatePickerDialog(
            onDateSelected = { selectedDate ->
                endDate = selectedDate
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false },
            datePickerState = endDatePickerState,
            title = "Select End Date"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit,
    datePickerState: DatePickerState,
    title: String
) {
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
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
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            modifier = Modifier.padding(16.dp),
            title = {
                Text(
                    text = title,
                    modifier = Modifier.padding(16.dp)
                )
            },
            showModeToggle = false
        )
    }
} 