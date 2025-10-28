package com.deeksha.avr.ui.view.productionhead

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionHeadMainScreen(
    onNavigateToProject: (String) -> Unit,
    onNavigateToCreateUser: () -> Unit,
    onNavigateToViewAllUsers: () -> Unit,
    onNavigateToRoleManagement: () -> Unit,
    onNavigateToNewProject: () -> Unit,
    onNavigateToEditProject: (String) -> Unit,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    // Use rememberSaveable to persist tab state across navigation
    var selectedTab by rememberSaveable { mutableStateOf(0) } // Default to Projects tab (0)
    
    Scaffold(
        floatingActionButton = {
            // Show FAB only on Projects tab
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = onNavigateToNewProject,
                    containerColor = Color(0xFF007AFF),  // iOS blue
                    contentColor = Color.White,
                    shape = androidx.compose.foundation.shape.CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Project",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        floatingActionButtonPosition = androidx.compose.material3.FabPosition.End,
        bottomBar = {
            NavigationBar(
                containerColor = Color.White
            ) {
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Projects",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Projects",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1976D2),
                        selectedTextColor = Color(0xFF1976D2),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = "User Management",
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "User Management",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF1976D2),
                        selectedTextColor = Color(0xFF1976D2),
                        unselectedIconColor = Color.Gray,
                        unselectedTextColor = Color.Gray
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> ProductionHeadProjectsTab(
                    onNavigateToProject = onNavigateToProject,
                    onNavigateToNewProject = onNavigateToNewProject,
                    onNavigateToEditProject = onNavigateToEditProject,
                    onLogout = onLogout
                )
                1 -> ProductionHeadUserManagementTab(
                    onNavigateToCreateUser = onNavigateToCreateUser,
                    onNavigateToViewAllUsers = onNavigateToViewAllUsers,
                    onNavigateToRoleManagement = onNavigateToRoleManagement,
                    onLogout = onLogout
                )
            }
        }
    }
}

