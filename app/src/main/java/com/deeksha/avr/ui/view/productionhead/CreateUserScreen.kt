package com.deeksha.avr.ui.view.productionhead

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.deeksha.avr.model.UserRole
import com.deeksha.avr.viewmodel.ProductionHeadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUserScreen(
    onNavigateBack: () -> Unit,
    onUserCreated: () -> Unit,
    viewModel: ProductionHeadViewModel = hiltViewModel()
) {
    var phoneNumber by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.USER) }
    var showRoleDropdown by remember { mutableStateOf(false) }
    
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    // Handle success message
    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            onUserCreated()
            viewModel.clearSuccessMessage()
        }
    }
    
    // Handle error
    LaunchedEffect(error) {
        if (error != null) {
            // You can show a snackbar here if needed
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Create User",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (phoneNumber.isNotEmpty() && fullName.isNotEmpty()) {
                                viewModel.createUser(phoneNumber, fullName, selectedRole)
                            }
                        },
                        enabled = !isLoading && phoneNumber.isNotEmpty() && fullName.isNotEmpty()
                    ) {
                        Text(
                            text = "Create",
                            color = if (phoneNumber.isNotEmpty() && fullName.isNotEmpty()) {
                                Color(0xFF4285F4)
                            } else {
                                Color.Gray
                            },
                            fontWeight = FontWeight.Medium
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Section Header
            Text(
                text = "USER INFORMATION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Phone Number Field
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Phone",
                        tint = Color(0xFF4285F4)
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4285F4),
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                ),
                singleLine = true
            )
            
            // Full Name Field
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Person",
                        tint = Color(0xFF4285F4)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4285F4),
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                ),
                singleLine = true
            )
            
            // Role Selection
            Text(
                text = "Role",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            ExposedDropdownMenuBox(
                expanded = showRoleDropdown,
                onExpandedChange = { showRoleDropdown = !showRoleDropdown }
            ) {
                OutlinedTextField(
                    value = when (selectedRole) {
                        UserRole.USER -> "User"
                        UserRole.APPROVER -> "Approver"
                        UserRole.ADMIN -> "Admin"
                        UserRole.PRODUCTION_HEAD -> "Production Head"
                    },
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Select Role") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Role",
                            tint = Color(0xFF4285F4)
                        )
                    },
                    trailingIcon = {
                        Icon(
                            if (showRoleDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowDropDown,
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
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                    )
                )
                
                ExposedDropdownMenu(
                    expanded = showRoleDropdown,
                    onDismissRequest = { showRoleDropdown = false },
                    modifier = Modifier.background(Color.White)
                ) {
                    RoleOption(
                        role = UserRole.USER,
                        title = "User",
                        description = "Can submit expenses and view project details",
                        isSelected = selectedRole == UserRole.USER,
                        onClick = {
                            selectedRole = UserRole.USER
                            showRoleDropdown = false
                        }
                    )
                    
                    RoleOption(
                        role = UserRole.APPROVER,
                        title = "Approver",
                        description = "Can approve/reject expenses and manage budgets",
                        isSelected = selectedRole == UserRole.APPROVER,
                        onClick = {
                            selectedRole = UserRole.APPROVER
                            showRoleDropdown = false
                        }
                    )
                }
            }
            
            // Error Message
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
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
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            color = Color(0xFFD32F2F),
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            // Loading Indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF4285F4),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RoleOption(
    role: UserRole,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        },
        onClick = onClick,
        leadingIcon = {
            Icon(
                Icons.Default.Person,
                contentDescription = title,
                tint = if (isSelected) Color(0xFF4285F4) else Color.Gray
            )
        },
        modifier = Modifier.background(
            if (isSelected) Color(0xFFE3F2FD) else Color.Transparent
        )
    )
} 