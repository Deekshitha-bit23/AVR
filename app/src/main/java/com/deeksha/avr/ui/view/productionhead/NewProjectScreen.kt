package com.deeksha.avr.ui.view.productionhead

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.model.DepartmentBudget
import com.deeksha.avr.model.User
import com.deeksha.avr.viewmodel.ProductionHeadViewModel
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(
    onNavigateBack: () -> Unit,
    onProjectCreated: () -> Unit,
    viewModel: ProductionHeadViewModel = hiltViewModel()
) {
    var projectName by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf<Date?>(null) }
    var endDate by remember { mutableStateOf<Date?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    // Date picker states
    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()
    
    // Toast context
    val context = LocalContext.current
    
    // Team Assignment
    var selectedApprover by remember { mutableStateOf<User?>(null) }
    var selectedTeamMembers by remember { mutableStateOf<List<User>>(emptyList()) }
    var showApproverSearch by remember { mutableStateOf(false) }
    var showTeamMemberSearch by remember { mutableStateOf(false) }
    var approverSearchQuery by remember { mutableStateOf("") }
    var teamMemberSearchQuery by remember { mutableStateOf("") }
    
    // Department Budgets
    var totalBudget by remember { mutableStateOf("") }
    var departmentName by remember { mutableStateOf("") }
    var departmentBudgetAmount by remember { mutableStateOf("") }
    var showDepartmentDropdown by remember { mutableStateOf(false) }
    
    // Predefined departments
    val predefinedDepartments = listOf(
        "Costumes", "Camera", "Lighting", "Sound", "Art", "Other", 
        "Direction", "Production", "Post-Production", "Marketing", "Finance"
    )
    
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val availableApprovers by viewModel.availableApprovers.collectAsState()
    val availableUsers by viewModel.availableUsers.collectAsState()
    val departmentBudgets by viewModel.departmentBudgets.collectAsState()
    val totalAllocated by viewModel.totalAllocated.collectAsState()
    
    // Handle success
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            onProjectCreated()
            viewModel.clearSuccessMessage()
        }
    }
    
    // Update total budget in viewmodel
    LaunchedEffect(totalBudget) {
        val budget = totalBudget.toDoubleOrNull() ?: 0.0
        viewModel.updateTotalBudget(budget)
    }
    
    // Date formatters
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    // Budget validation
    fun validateBudgetAllocation(newBudget: Double): Boolean {
        val totalBudgetValue = totalBudget.toDoubleOrNull() ?: 0.0
        val newTotalAllocated = totalAllocated + newBudget
        
        if (newTotalAllocated > totalBudgetValue) {
            Toast.makeText(
                context,
                "⚠️ Budget allocation (₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(newTotalAllocated)}) exceeds total budget (₹${NumberFormat.getNumberInstance(Locale("en", "IN")).format(totalBudgetValue)})",
                Toast.LENGTH_LONG
            ).show()
            return false
        }
        return true
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        item {
            // Top App Bar
            Surface(
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                    
                    Text(
                        text = "New Project",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    
                    TextButton(
                        onClick = { /* Handle Cancel */ },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF4285F4)
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
        
        item {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // PROJECT DETAILS Section
                SectionHeader(
                    icon = Icons.Default.Create,
                    title = "PROJECT DETAILS"
                )
                
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(8.dp),
                    minLines = 3
                )
                
                // TIMELINE Section
                SectionHeader(
                    icon = Icons.Default.DateRange,
                    title = "TIMELINE"
                )
                
                // Start Date
                OutlinedTextField(
                    value = startDate?.let { dateFormatter.format(it) } ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Start Date *") },
                    trailingIcon = {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Select Date",
                            modifier = Modifier.clickable { showStartDatePicker = true }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clickable { showStartDatePicker = true },
                    shape = RoundedCornerShape(8.dp)
                )
                
                // End Date
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
                        .padding(bottom = 8.dp)
                        .clickable { showEndDatePicker = true },
                    shape = RoundedCornerShape(8.dp)
                )
                
                Text(
                    text = "Start date is mandatory. End date is optional.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                // TEAM ASSIGNMENT Section
                SectionHeader(
                    icon = Icons.Default.Person,
                    title = "TEAM ASSIGNMENT"
                )
                
                // Project Manager (Approver)
                Text(
                    text = "Project Manager (Approver) *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4285F4),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Selected Approver Display
                selectedApprover?.let { approver ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Approver",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = approver.name,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "+91${approver.phone}",
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E7D32).copy(alpha = 0.7f)
                                )
                            }
                            IconButton(
                                onClick = { selectedApprover = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                                // Approver Search (only show if no approver selected)
                if (selectedApprover == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = showApproverSearch,
                                onExpandedChange = { showApproverSearch = it }
                            ) {
                                OutlinedTextField(
                                    value = "",
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("Search Approver by name or phone") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Search, 
                                            contentDescription = "Search",
                                            tint = Color(0xFF4285F4)
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Dropdown",
                                            tint = Color(0xFF4285F4)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF4285F4),
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    )
                                )
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${availableApprovers.size} approvers available",
                                        fontSize = 12.sp,
                                        color = Color(0xFF4285F4),
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    Text(
                                        text = "Select Approver",
                                        fontSize = 14.sp,
                                        color = Color(0xFF757575),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                ExposedDropdownMenu(
                                    expanded = showApproverSearch,
                                    onDismissRequest = { showApproverSearch = false }
                                ) {
                                    availableApprovers.forEach { item ->
                                        DropdownMenuItem(
                                            text = { 
                                                Column {
                                                    Text(
                                                        text = item.name,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = item.phone,
                                                        fontSize = 12.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedApprover = item
                                                showApproverSearch = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = "User",
                                                    tint = Color(0xFF4285F4)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Team Members
                Text(
                    text = "Team Members (Users) *",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF4285F4),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Selected Team Members Display
                if (selectedTeamMembers.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        items(selectedTeamMembers) { user ->
                            TeamMemberChip(
                                user = user,
                                onRemove = { selectedTeamMembers = selectedTeamMembers - user }
                            )
                        }
                    }
                }

                // Team Member Search
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = showTeamMemberSearch,
                            onExpandedChange = { showTeamMemberSearch = it }
                        ) {
                            OutlinedTextField(
                                value = "",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Search Team Member by name or phone") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search, 
                                        contentDescription = "Search",
                                        tint = Color(0xFF4285F4)
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Dropdown",
                                        tint = Color(0xFF4285F4)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF4285F4),
                                    unfocusedBorderColor = Color(0xFFE0E0E0)
                                )
                            )
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${availableUsers.size} team members available",
                                    fontSize = 12.sp,
                                    color = Color(0xFF4285F4),
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Text(
                                    text = "Select Team Members",
                                    fontSize = 14.sp,
                                    color = Color(0xFF757575),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            ExposedDropdownMenu(
                                expanded = showTeamMemberSearch,
                                onDismissRequest = { showTeamMemberSearch = false }
                            ) {
                                availableUsers.forEach { item ->
                                    DropdownMenuItem(
                                        text = { 
                                            Column {
                                                Text(
                                                    text = item.name,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = item.phone,
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        },
                                        onClick = {
                                            // Add item to existing selectedItems if not already present
                                            if (!selectedTeamMembers.contains(item)) {
                                                selectedTeamMembers = selectedTeamMembers + item
                                            }
                                            showTeamMemberSearch = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Person,
                                                contentDescription = "User",
                                                tint = Color(0xFF4285F4)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // DEPARTMENTS Section
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(
                    icon = Icons.Default.Build,
                    title = "DEPARTMENTS"
                )
                
                // Total Budget
                OutlinedTextField(
                    value = totalBudget,
                    onValueChange = { totalBudget = it },
                    label = { Text("Total Budget *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(8.dp),
                    prefix = { Text("₹") }
                )
                
                // Department Budget Entry
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Department Dropdown with manual input
                    ExposedDropdownMenuBox(
                        expanded = showDepartmentDropdown,
                        onExpandedChange = { showDepartmentDropdown = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = departmentName,
                            onValueChange = { departmentName = it },
                            label = { Text("Select Department") },
                            placeholder = { Text("Choose or type...") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDepartmentDropdown)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4285F4),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            ),
                            singleLine = true
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showDepartmentDropdown,
                            onDismissRequest = { showDepartmentDropdown = false }
                        ) {
                            predefinedDepartments.forEach { department ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = department,
                                            fontWeight = FontWeight.Medium
                                        ) 
                                    },
                                    onClick = {
                                        departmentName = department
                                        showDepartmentDropdown = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Build,
                                            contentDescription = "Department",
                                            tint = Color(0xFF4285F4),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                    
                    OutlinedTextField(
                        value = departmentBudgetAmount,
                        onValueChange = { departmentBudgetAmount = it },
                        label = { Text("Budget") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        prefix = { Text("₹") }
                    )
                    
                    IconButton(
                        onClick = {
                            if (departmentName.isNotEmpty() && departmentBudgetAmount.isNotEmpty()) {
                                val budget = departmentBudgetAmount.toDoubleOrNull() ?: 0.0
                                if (validateBudgetAllocation(budget)) {
                                    viewModel.addDepartmentBudget(departmentName, budget)
                                    departmentName = ""
                                    departmentBudgetAmount = ""
                                }
                            }
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                Color(0xFF4285F4),
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Department",
                            tint = Color.White
                        )
                    }
                }
                
                // Department Budget List
                if (departmentBudgets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    departmentBudgets.forEach { dept ->
                        DepartmentBudgetItem(
                            department = dept,
                            onRemove = { viewModel.removeDepartmentBudget(dept.departmentName) }
                        )
                    }
                }
                
                // Budget Summary
                Spacer(modifier = Modifier.height(16.dp))
                BudgetSummaryCard(
                    totalBudget = totalBudget.toDoubleOrNull() ?: 0.0,
                    totalAllocated = totalAllocated
                )
                
                // Create Project Button
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        if (isFormValid(projectName, startDate, selectedApprover, selectedTeamMembers)) {
                            viewModel.createProject(
                                projectName = projectName,
                                description = description,
                                startDate = Timestamp(startDate!!),
                                endDate = endDate?.let { Timestamp(it) },
                                totalBudget = totalBudget.toDoubleOrNull() ?: 0.0,
                                managerId = selectedApprover!!.uid,
                                teamMemberIds = selectedTeamMembers.map { it.uid },
                                departmentBudgets = departmentBudgets
                            )
                        }
                    },
                    enabled = !isLoading && isFormValid(projectName, startDate, selectedApprover, selectedTeamMembers),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4285F4)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Text(
                            text = "Create Project",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Error Message
                error?.let { errorMessage ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = Color(0xFFD32F2F)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Start Date Picker
    if (showStartDatePicker) {
        DatePickerDialog(
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
        DatePickerDialog(
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

@Composable
fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = Color(0xFF4285F4),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4285F4)
        )
    }
}

@Composable
fun TeamMemberChip(
    user: User,
    onRemove: () -> Unit
) {
    val chipColors = listOf(
        Color(0xFFFFF3E0), // Light Orange
        Color(0xFFF3E5F5), // Light Purple
        Color(0xFFE8F5E8), // Light Green
        Color(0xFFE3F2FD), // Light Blue
        Color(0xFFFFF8E1), // Light Yellow
        Color(0xFFFCE4EC)  // Light Pink
    )
    
    val chipColor = chipColors[user.hashCode() % chipColors.size]
    
    Card(
        modifier = Modifier.padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = chipColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = user.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = Color(0xFF757575),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit,
    datePickerState: DatePickerState,
    title: String
) {
    DatePickerDialog(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchableDropdown(
    label: String,
    items: List<User>,
    selectedItem: User?,
    onItemSelected: (User?) -> Unit,
    itemText: (User) -> String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    availableCount: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = onExpandedChange
            ) {
                OutlinedTextField(
                    value = selectedItem?.let { itemText(it) } ?: "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(label) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = "Search",
                            tint = Color(0xFF4285F4)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown",
                            tint = Color(0xFF4285F4)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4285F4),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = availableCount,
                        fontSize = 12.sp,
                        color = Color(0xFF4285F4),
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Selected item is now displayed separately above
                }
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) }
                ) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = item.phone,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            },
                            onClick = {
                                onItemSelected(item)
                                onExpandedChange(false)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "User",
                                    tint = Color(0xFF4285F4)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectDropdown(
    label: String,
    items: List<User>,
    selectedItems: List<User>,
    onItemsSelected: (List<User>) -> Unit,
    itemText: (User) -> String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    availableCount: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = onExpandedChange
            ) {
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(label) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search, 
                            contentDescription = "Search",
                            tint = Color(0xFF4285F4)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Dropdown",
                            tint = Color(0xFF4285F4)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4285F4),
                        unfocusedBorderColor = Color(0xFFE0E0E0)
                    )
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = availableCount,
                        fontSize = 12.sp,
                        color = Color(0xFF4285F4),
                        fontWeight = FontWeight.Medium
                    )
                    
                    // Selected items are now displayed separately above
                }
                
                // Note: Selected items are now displayed separately above the dropdown
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { onExpandedChange(false) }
                ) {
                    items.forEach { item ->
                        DropdownMenuItem(
                            text = { 
                                Column {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = item.phone,
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }
                            },
                            onClick = {
                                // Add item to existing selectedItems if not already present
                                if (!selectedItems.contains(item)) {
                                    onItemsSelected(selectedItems + item)
                                }
                                onExpandedChange(false)
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "User",
                                    tint = Color(0xFF4285F4)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DepartmentBudgetItem(
    department: DepartmentBudget,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = department.departmentName,
                    fontWeight = FontWeight.Medium
                )
                val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                Text(
                    text = formatter.format(department.allocatedBudget),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = Color(0xFFD32F2F)
                )
            }
        }
    }
}

@Composable
fun BudgetSummaryCard(
    totalBudget: Double,
    totalAllocated: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Budget Summary",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4285F4),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Allocated:")
                Text(
                    text = formatter.format(totalAllocated),
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Budget:")
                Text(
                    text = formatter.format(totalBudget),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

fun isFormValid(
    projectName: String,
    startDate: Date?,
    selectedApprover: User?,
    selectedTeamMembers: List<User>
): Boolean {
    return projectName.isNotEmpty() &&
            startDate != null &&
            selectedApprover != null &&
            selectedTeamMembers.isNotEmpty()
} 