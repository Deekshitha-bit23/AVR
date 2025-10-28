package com.deeksha.avr.ui.view.productionhead

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.model.User
import com.deeksha.avr.model.UserRole
import com.deeksha.avr.viewmodel.ProductionHeadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewAllUsersScreen(
    onNavigateBack: () -> Unit,
    viewModel: ProductionHeadViewModel = hiltViewModel()
) {
    val availableUsers by viewModel.availableUsers.collectAsState()
    val availableApprovers by viewModel.availableApprovers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    // Load users when screen appears
    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }
    
    // Combine and sort all users
    val allUsers = (availableUsers + availableApprovers).sortedBy { it.name }
    val userCount = allUsers.size
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All Users",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onNavigateBack) {
                            Text(
                                text = "Done",
                                fontSize = 17.sp,
                                color = Color(0xFF1976D2)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back"
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
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = Color(0xFF1976D2))
                        Text(
                            text = "Loading users...",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Red
                        )
                        Text(
                            text = "Error Loading Users",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = error ?: "Unknown error",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { viewModel.loadUsers() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1976D2)
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            allUsers.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = "No Users Found",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = "No users available",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    // User count
                    Text(
                        text = "$userCount user${if (userCount != 1) "s" else ""}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    
                    // Users list
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(allUsers) { user ->
                            UserCard(
                                user = user,
                                onToggleActive = { isActive ->
                                    viewModel.updateUserActiveStatus(user.phone, isActive)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    user: User,
    onToggleActive: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // User name
                Text(
                    text = user.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // Phone number
                Text(
                    text = user.phone,
                    fontSize = 14.sp,
                    color = Color(0xFF8E8E93)  // iOS gray text color
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                // Role badge
                Surface(
                    color = when (user.role) {
                        UserRole.USER -> Color(0xFFD4F4DD)  // Light green background
                        UserRole.APPROVER -> Color(0xFFD6EBFF)  // Light blue background
                        UserRole.PRODUCTION_HEAD -> Color(0xFFE1D4F4)  // Light purple background
                        UserRole.ADMIN -> Color(0xFFFFE0E0)  // Light red background
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = when (user.role) {
                            UserRole.USER -> "User"
                            UserRole.APPROVER -> "Approver"
                            UserRole.PRODUCTION_HEAD -> "Production Head"
                            UserRole.ADMIN -> "Admin"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = when (user.role) {
                            UserRole.USER -> Color(0xFF34A853)  // Darker green text
                            UserRole.APPROVER -> Color(0xFF1976D2)  // Darker blue text
                            UserRole.PRODUCTION_HEAD -> Color(0xFF7B1FA2)  // Darker purple text
                            UserRole.ADMIN -> Color(0xFFD32F2F)  // Darker red text
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Toggle switch
            Switch(
                checked = user.isActive,
                onCheckedChange = { checked ->
                    onToggleActive(checked)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF34C759),  // iOS green toggle
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color(0xFFE5E5EA)  // iOS gray toggle
                )
            )
        }
    }
}
