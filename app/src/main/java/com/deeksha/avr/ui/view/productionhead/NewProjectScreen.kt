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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material.icons.filled.Add

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
    
    // Debug logging for state changes
    LaunchedEffect(selectedTeamMembers) {
        android.util.Log.d("NewProjectScreen", "Selected team members updated: ${selectedTeamMembers.size}")
        selectedTeamMembers.forEach { user ->
            android.util.Log.d("NewProjectScreen", "Team member: ${user.name} (${user.uid})")
        }
    }
    
    LaunchedEffect(selectedApprover) {
        android.util.Log.d("NewProjectScreen", "Selected approver updated: ${selectedApprover?.name ?: "null"}")
    }
    
    // Safe team member operations
    fun addTeamMember(user: User) {
        try {
            android.util.Log.d("NewProjectScreen", "Adding team member: ${user.name} (${user.uid})")
            
            // Validate user object
            if (user.uid.isNullOrEmpty()) {
                android.util.Log.e("NewProjectScreen", "User UID is null or empty")
                Toast.makeText(context, "Invalid user data", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Check if user is already selected
            val isAlreadySelected = selectedTeamMembers.any { selectedUser -> 
                selectedUser.uid == user.uid
            }
            
            if (!isAlreadySelected) {
                selectedTeamMembers = selectedTeamMembers + user
                android.util.Log.d("NewProjectScreen", "Successfully added team member. New count: ${selectedTeamMembers.size}")
            } else {
                android.util.Log.d("NewProjectScreen", "User already selected: ${user.name}")
                Toast.makeText(context, "${user.name} is already selected", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("NewProjectScreen", "Error adding team member: ${e.message}", e)
            Toast.makeText(context, "Error adding team member: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun removeTeamMember(user: User) {
        try {
            android.util.Log.d("NewProjectScreen", "Removing team member: ${user.name} (${user.uid})")
            selectedTeamMembers = selectedTeamMembers.filter { it.uid != user.uid }
            android.util.Log.d("NewProjectScreen", "Successfully removed team member. New count: ${selectedTeamMembers.size}")
        } catch (e: Exception) {
            android.util.Log.e("NewProjectScreen", "Error removing team member: ${e.message}", e)
            Toast.makeText(context, "Error removing team member: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Department Budgets
    var departmentName by remember { mutableStateOf("") }
    var departmentBudgetAmount by remember { mutableStateOf("") }
    
    // Categories
    var categoryName by remember { mutableStateOf("") }
    var projectCategories by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // No predefined departments - users can type any department name
    
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val availableApprovers by viewModel.availableApprovers.collectAsState()
    val availableUsers by viewModel.availableUsers.collectAsState()
    val departmentBudgets by viewModel.departmentBudgets.collectAsState()
    val totalAllocated by viewModel.totalAllocated.collectAsState()
    
    // Debug logging for available data
    LaunchedEffect(availableUsers) {
        android.util.Log.d("NewProjectScreen", "Available users updated: ${availableUsers.size}")
        availableUsers.forEach { user ->
            android.util.Log.d("NewProjectScreen", "Available user: ${user.name} (${user.uid})")
        }
    }
    
    LaunchedEffect(availableApprovers) {
        android.util.Log.d("NewProjectScreen", "Available approvers updated: ${availableApprovers.size}")
        availableApprovers.forEach { approver ->
            android.util.Log.d("NewProjectScreen", "Available approver: ${approver.name} (${approver.uid})")
        }
    }
    
    // Handle success
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            onProjectCreated()
            viewModel.clearSuccessMessage()
        }
    }
    
    
    // Date formatters
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    
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
                        
                        // Refresh button for users
                        IconButton(
                            onClick = { 
                                viewModel.refreshUsers()
                                Toast.makeText(context, "Refreshing users...", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh Users",
                                tint = Color(0xFF4285F4)
                            )
                        }
                        
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
                // PROJECT DETAILS Section - iOS White Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Create,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PROJECT DETAILS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8E8E93)
                            )
                        }
                
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                            label = { Text("Project Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                            minLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                        )
                    }
                }
                
                // TIMELINE Section - iOS White Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TIMELINE",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8E8E93)
                            )
                        }
                
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
                                    tint = Color(0xFF007AFF),
                            modifier = Modifier.clickable { showStartDatePicker = true }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStartDatePicker = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
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
                                    tint = Color(0xFF007AFF),
                            modifier = Modifier.clickable { showEndDatePicker = true }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEndDatePicker = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF007AFF),
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                )
                
                Text(
                    text = "Start date is mandatory. End date is optional.",
                    fontSize = 12.sp,
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                // TEAM ASSIGNMENT Section - iOS White Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                Text(
                                text = "TEAM ASSIGNMENT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8E8E93)
                            )
                        }
                        
                        // Project Manager (Approver)
                        Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                                text = "Project Manager (Approver)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                                color = Color.Black
                )
                            Spacer(modifier = Modifier.height(8.dp))
                
                            // Show selected approver if exists
                selectedApprover?.let { approver ->
                    Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                    shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                            .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF34A853),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = approver.name,
                                    fontWeight = FontWeight.Medium,
                                                color = Color.Black
                                )
                                Text(
                                                text = approver.phone,
                                    fontSize = 12.sp,
                                                color = Color(0xFF8E8E93)
                                )
                            }
                            IconButton(
                                onClick = { selectedApprover = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                                tint = Color(0xFF8E8E93),
                                                modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                                Spacer(modifier = Modifier.height(8.dp))
                }

                            // Search field (only show if no approver selected)
                if (selectedApprover == null) {
                            ExposedDropdownMenuBox(
                                expanded = showApproverSearch,
                                onExpandedChange = { 
                                    android.util.Log.d("ApproverSearch", "Dropdown expanded changed to: $it")
                                    showApproverSearch = it 
                                }
                            ) {
                                OutlinedTextField(
                                        value = approverSearchQuery,
                                        onValueChange = { 
                                            approverSearchQuery = it
                                            showApproverSearch = true  // Auto-open dropdown when typing
                                        },
                                        placeholder = { Text("Search name or phone number...") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Search, 
                                            contentDescription = "Search",
                                                tint = Color(0xFF007AFF),
                                                modifier = Modifier.clickable { 
                                                    showApproverSearch = !showApproverSearch 
                                                }
                                        )
                                    },
                                        trailingIcon = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (approverSearchQuery.isNotEmpty()) {
                                                    IconButton(
                                                        onClick = { 
                                                            approverSearchQuery = ""
                                                            showApproverSearch = false
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = "Clear",
                                                            tint = Color(0xFF8E8E93),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                Icon(
                                                    Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Dropdown",
                                                    tint = Color(0xFF007AFF),
                                                    modifier = Modifier.clickable { 
                                                        showApproverSearch = !showApproverSearch 
                                                    }
                                                )
                                            }
                                        },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                            .menuAnchor()
                                            .clickable { showApproverSearch = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF007AFF),
                                        unfocusedBorderColor = Color(0xFFE0E0E0)
                                    )
                                )
                                    
                                    val filteredApprovers = if (approverSearchQuery.isEmpty()) {
                                        availableApprovers.filter { it.isActive }
                                    } else {
                                        availableApprovers.filter { it.isActive }.filter { 
                                            it.name.contains(approverSearchQuery, ignoreCase = true) || 
                                            it.phone.contains(approverSearchQuery, ignoreCase = true)
                                        }
                                    }
                                
                                ExposedDropdownMenu(
                                    expanded = showApproverSearch,
                                    onDismissRequest = { 
                                        android.util.Log.d("ApproverSearch", "Dropdown dismissed")
                                        showApproverSearch = false 
                                            // Don't clear search query on dismiss - keep it for continued typing
                                    }
                                ) {
                                        android.util.Log.d("ApproverSearch", "Filtered approvers count: ${filteredApprovers.size}")
                                        if (filteredApprovers.isEmpty()) {
                                        DropdownMenuItem(
                                                text = { Text("No approvers found") },
                                            onClick = { }
                                        )
                                    } else {
                                            filteredApprovers.forEach { item ->
                                            android.util.Log.d("ApproverSearch", "Rendering approver: ${item.name} (${item.uid})")
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
                                                    try {
                                                        android.util.Log.d("ApproverSearch", "Approver clicked: ${item.name} (${item.uid})")
                                                        
                                                        // Validate user object
                                                        if (item.uid.isNullOrEmpty()) {
                                                            android.util.Log.e("ApproverSearch", "Approver UID is null or empty")
                                                            Toast.makeText(context, "Invalid approver data", Toast.LENGTH_SHORT).show()
                                                            return@DropdownMenuItem
                                                        }
                                                        
                                                            selectedApprover = item
                                                            showApproverSearch = false
                                                            approverSearchQuery = "" // Clear only on selection
                                                            android.util.Log.d("ApproverSearch", "Approver selected and dropdown closed")
                                                    } catch (e: Exception) {
                                                        android.util.Log.e("ApproverSearch", "Error selecting approver: ${e.message}", e)
                                                        Toast.makeText(context, "Error selecting approver: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Default.Person,
                                                        contentDescription = "User",
                                                            tint = Color(0xFF007AFF)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                }
                
                // Team Members
                        Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                                text = "Team Members (Users)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Show selected team members if any
                            if (selectedTeamMembers.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    selectedTeamMembers.forEach { member ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color(0xFF34A853),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                Text(
                                                        text = member.name,
                    fontWeight = FontWeight.Medium,
                                                        color = Color.Black
                )
                Text(
                                                        text = member.phone,
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF8E8E93)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { removeTeamMember(member) },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Remove",
                                                        tint = Color(0xFF8E8E93),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            // Search field (always visible for adding multiple)
                        ExposedDropdownMenuBox(
                            expanded = showTeamMemberSearch,
                            onExpandedChange = { 
                                android.util.Log.d("TeamMemberSearch", "Dropdown expanded changed to: $it")
                                showTeamMemberSearch = it 
                            }
                        ) {
                            OutlinedTextField(
                                    value = teamMemberSearchQuery,
                                    onValueChange = { 
                                        teamMemberSearchQuery = it
                                        showTeamMemberSearch = true  // Auto-open dropdown when typing
                                    },
                                    placeholder = { Text("Search name or phone number...") },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search, 
                                        contentDescription = "Search",
                                            tint = Color(0xFF007AFF),
                                            modifier = Modifier.clickable { 
                                                showTeamMemberSearch = !showTeamMemberSearch 
                                            }
                                    )
                                },
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (teamMemberSearchQuery.isNotEmpty()) {
                                                IconButton(
                                                    onClick = { 
                                                        teamMemberSearchQuery = ""
                                                        showTeamMemberSearch = false
                                                    },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Clear",
                                                        tint = Color(0xFF8E8E93),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = "Dropdown",
                                                tint = Color(0xFF007AFF),
                                                modifier = Modifier.clickable { 
                                                    showTeamMemberSearch = !showTeamMemberSearch 
                                                }
                                            )
                                        }
                                    },
                                modifier = Modifier
                                    .fillMaxWidth()
                                        .menuAnchor()
                                        .clickable { showTeamMemberSearch = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF007AFF),
                                    unfocusedBorderColor = Color(0xFFE0E0E0)
                                )
                            )
                                
                                val filteredUsers = if (teamMemberSearchQuery.isEmpty()) {
                                    availableUsers.filter { it.isActive }
                                } else {
                                    availableUsers.filter { it.isActive }.filter { 
                                        it.name.contains(teamMemberSearchQuery, ignoreCase = true) || 
                                        it.phone.contains(teamMemberSearchQuery, ignoreCase = true)
                                    }
                                }
                            
                            ExposedDropdownMenu(
                                expanded = showTeamMemberSearch,
                                onDismissRequest = { 
                                    android.util.Log.d("TeamMemberSearch", "Dropdown dismissed")
                                    showTeamMemberSearch = false 
                                        // Don't clear search query on dismiss - keep it for continued typing
                                }
                            ) {
                                    android.util.Log.d("TeamMemberSearch", "Filtered users count: ${filteredUsers.size}")
                                    if (filteredUsers.isEmpty()) {
                                    DropdownMenuItem(
                                            text = { Text("No users found") },
                                        onClick = { }
                                    )
                                } else {
                                        filteredUsers.forEach { item ->
                                        android.util.Log.d("TeamMemberSearch", "Rendering user: ${item.name} (${item.uid})")
                                        DropdownMenuItem(
                                            text = { 
                                                Column {
                                                    Text(
                                                        text = item.name.ifEmpty { "Unknown User" },
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = item.phone.ifEmpty { "No phone" },
                                                        fontSize = 12.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            },
                                            onClick = {
                                                android.util.Log.d("TeamMemberSearch", "User clicked: ${item.name} (${item.uid})")
                                                android.util.Log.d("TeamMemberSearch", "Current selected team members: ${selectedTeamMembers.size}")
                                                
                                                // Validate user object
                                                if (item.uid.isNullOrEmpty()) {
                                                    android.util.Log.e("TeamMemberSearch", "User UID is null or empty")
                                                    Toast.makeText(context, "Invalid user data", Toast.LENGTH_SHORT).show()
                                                    return@DropdownMenuItem
                                                }
                                                
                                                // Check if user is already selected
                                                val isAlreadySelected = selectedTeamMembers.any { selectedUser -> 
                                                    selectedUser.uid == item.uid
                                                }
                                                
                                                android.util.Log.d("TeamMemberSearch", "Is already selected: $isAlreadySelected")
                                                
                                                if (!isAlreadySelected) {
                                                    android.util.Log.d("TeamMemberSearch", "Adding user to team members")
                                                    addTeamMember(item)
                                                    android.util.Log.d("TeamMemberSearch", "Updated team members count: ${selectedTeamMembers.size}")
                                                } else {
                                                    android.util.Log.d("TeamMemberSearch", "User already selected, skipping")
                                                    Toast.makeText(context, "${item.name} is already selected", Toast.LENGTH_SHORT).show()
                                                }
                                                
                                                    showTeamMemberSearch = false
                                                    teamMemberSearchQuery = "" // Clear only on selection
                                                    android.util.Log.d("TeamMemberSearch", "Dropdown closed")
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = "User",
                                                        tint = Color(0xFF007AFF)
                                                )
                                            }
                                        )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // DEPARTMENTS Section - iOS White Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Column(
                    modifier = Modifier
                        .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Section Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Build,
                                contentDescription = null,
                                tint = Color(0xFF007AFF),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "DEPARTMENTS",
                                fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                                color = Color(0xFF8E8E93)
                            )
                        }
                        
                        // Department Budget Entry
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Department input field
                            OutlinedTextField(
                                value = departmentName,
                                onValueChange = { departmentName = it },
                                label = { Text("DEPARTMENT") },
                                placeholder = { Text("e.g., Marketing") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF007AFF),
                                    unfocusedBorderColor = Color(0xFFE0E0E0)
                                ),
                                singleLine = true
                            )
                            
                            // Budget display field (read-only)
                            OutlinedTextField(
                                value = departmentBudgetAmount,
                                onValueChange = { departmentBudgetAmount = it },
                                label = { Text("BUDGET") },
                                placeholder = { Text("₹0") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                prefix = { Text("₹") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF007AFF),
                                    unfocusedBorderColor = Color(0xFFE0E0E0)
                                )
                            )
                        }
                        
                        // Add Department Button
                        Button(
                            onClick = {
                                if (departmentName.isNotEmpty() && departmentBudgetAmount.isNotEmpty()) {
                                    val budget = departmentBudgetAmount.toDoubleOrNull() ?: 0.0
                                    if (budget > 0) {
                                        viewModel.addDepartmentBudget(departmentName, budget)
                                        departmentName = ""
                                        departmentBudgetAmount = ""
                                    } else {
                                        Toast.makeText(context, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Please enter both department name and budget", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF007AFF),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Add Department",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        // Department Budget List
                        if (departmentBudgets.isNotEmpty()) {
                            departmentBudgets.forEach { dept ->
                                DepartmentBudgetItem(
                                    department = dept,
                                    onRemove = { viewModel.removeDepartmentBudget(dept.departmentName) }
                                )
                            }
                        }
                        
                        // Total Budget Display
                        val totalBudgetValue = totalAllocated
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AttachMoney,
                                    contentDescription = null,
                                    tint = Color(0xFF34A853),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Total Budget:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )
                            }
                            Text(
                                text = "₹${String.format("%.2f", totalBudgetValue)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
                
                // Categories section removed as per user request
                
                // Create Project Button - iOS Style
                Button(
                    onClick = {
                        try {
                            if (isFormValid(projectName, startDate, selectedApprover, selectedTeamMembers, departmentBudgets)) {
                                android.util.Log.d("NewProjectScreen", "🚀 Creating project with:")
                                android.util.Log.d("NewProjectScreen", "  📋 Name: $projectName")
                                android.util.Log.d("NewProjectScreen", "  📅 Start Date: $startDate")
                                android.util.Log.d("NewProjectScreen", "  👤 Manager: ${selectedApprover?.name} (${selectedApprover?.uid})")
                                android.util.Log.d("NewProjectScreen", "  👥 Team Members: ${selectedTeamMembers.size}")
                                android.util.Log.d("NewProjectScreen", "  💰 Budget: $totalAllocated")
                                
                                viewModel.createProject(
                                    projectName = projectName,
                                    description = description,
                                    startDate = Timestamp(startDate!!),
                                    endDate = endDate?.let { Timestamp(it) },
                                    totalBudget = totalAllocated,
                                    managerId = selectedApprover!!.phone,
                                    teamMemberIds = selectedTeamMembers.map { it.phone },
                                    departmentBudgets = departmentBudgets,
                                    categories = projectCategories
                                )
                            } else {
                                val validationError = getFormValidationError(projectName, startDate, selectedApprover, selectedTeamMembers, departmentBudgets)
                                Toast.makeText(context, validationError, Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("NewProjectScreen", "❌ Error creating project: ${e.message}", e)
                            Toast.makeText(context, "Error creating project: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isLoading && isFormValid(projectName, startDate, selectedApprover, selectedTeamMembers, departmentBudgets),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),  // iOS button height
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF007AFF),  // iOS blue
                        disabledContainerColor = Color(0xFF8E8E93)  // iOS gray when disabled
                    ),
                    shape = RoundedCornerShape(12.dp)  // iOS rounded corners
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create Project",
                                fontSize = 17.sp,  // iOS button text size
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                        )
                        }
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
    
    val chipColor = chipColors[user.hashCode().let { if (it < 0) -it else it } % chipColors.size]
    
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
                text = user.name.ifEmpty { "Unknown User" },
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
fun CategoryItem(
    category: String,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.List,
                contentDescription = "Category",
                tint = Color(0xFF9C27B0),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = category,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242)
            )
            Spacer(modifier = Modifier.weight(1f))
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
            val remainingBudget = totalBudget - totalAllocated
            
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
                    text = formatter.format(remainingBudget),
                    fontWeight = FontWeight.Bold,
                    color = if (remainingBudget >= 0) Color(0xFF4CAF50) else Color(0xFFD32F2F)
                )
            }
            
            // Show warning if allocated exceeds budget
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
                        text = "₹${formatter.format(remainingBudget)} available for allocation",
                        color = Color(0xFF2196F3),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

fun isFormValid(
    projectName: String,
    startDate: Date?,
    selectedApprover: User?,
    selectedTeamMembers: List<User>,
    departmentBudgets: List<DepartmentBudget>
): Boolean {
    return projectName.isNotEmpty() &&
            startDate != null &&
            selectedApprover != null &&
            selectedTeamMembers.isNotEmpty() &&
            departmentBudgets.isNotEmpty()
}

fun getFormValidationError(
    projectName: String,
    startDate: Date?,
    selectedApprover: User?,
    selectedTeamMembers: List<User>,
    departmentBudgets: List<DepartmentBudget>
): String {
    return when {
        projectName.isEmpty() -> "Project name is required"
        startDate == null -> "Start date is required"
        selectedApprover == null -> "Please select a project manager (approver)"
        selectedTeamMembers.isEmpty() -> "Please select at least one team member"
        departmentBudgets.isEmpty() -> "Please add at least one department with budget"
        else -> "Please fill in all required fields"
    }
} 