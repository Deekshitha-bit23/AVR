package com.deeksha.avr.navigation

import androidx.lifecycle.viewmodel.compose.viewModel
import AddExpenseViewModel
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import kotlinx.coroutines.tasks.await
import com.deeksha.avr.model.Project
import com.deeksha.avr.model.UserRole
import com.deeksha.avr.repository.ExpenseRepository
import com.deeksha.avr.ui.view.admin.AdminDashboardScreen
import com.deeksha.avr.ui.view.admin.ManageProjectsScreen
import com.deeksha.avr.ui.view.admin.ManageUsersScreen
import com.deeksha.avr.ui.view.admin.ReportsScreen
import com.deeksha.avr.ui.view.approver.ApproverDashboardScreen
import com.deeksha.avr.ui.view.approver.ApproverProjectDashboardScreen
import com.deeksha.avr.ui.view.approver.ApproverProjectSelectionScreen
import com.deeksha.avr.ui.view.approver.ApproverNotificationScreen
import com.deeksha.avr.ui.view.approver.CategoryDetailScreen
import com.deeksha.avr.ui.view.approver.PendingApprovalsScreen
import com.deeksha.avr.ui.view.approver.ReviewExpenseScreen
import com.deeksha.avr.ui.view.approver.ReportsScreen as ApproverReportsScreen
import com.deeksha.avr.ui.view.approver.OverallReportsScreen
import com.deeksha.avr.ui.view.auth.LoginScreen
import com.deeksha.avr.ui.view.auth.OtpVerificationScreen
import com.deeksha.avr.ui.view.auth.AccessRestrictedScreen
import com.deeksha.avr.ui.view.common.ProjectSelectionScreen
import com.deeksha.avr.ui.view.common.NotificationListScreen
import com.deeksha.avr.ui.view.common.ProjectNotificationScreen
import com.deeksha.avr.ui.view.user.AddExpenseScreen
import com.deeksha.avr.ui.view.user.ExpenseListScreen
import com.deeksha.avr.ui.view.user.TrackSubmissionsScreen
import com.deeksha.avr.ui.view.user.UserDashboardScreen
import com.deeksha.avr.ui.view.productionhead.ProductionHeadProjectSelectionScreen
import com.deeksha.avr.ui.view.productionhead.CreateUserScreen
import com.deeksha.avr.ui.view.productionhead.NewProjectScreen
import com.deeksha.avr.ui.view.productionhead.EditProjectScreen
import com.deeksha.avr.ui.view.productionhead.ProductionHeadDashboard
import com.deeksha.avr.ui.view.productionhead.ProductionHeadPendingApprovals
import com.deeksha.avr.ui.view.productionhead.ProductionHeadProjectDashboard
import com.deeksha.avr.ui.view.productionhead.ProductionHeadReports
import com.deeksha.avr.ui.view.productionhead.ProductionHeadOverallReports
import com.deeksha.avr.ui.view.productionhead.ProductionHeadCategoryDetail
import com.deeksha.avr.viewmodel.AuthViewModel
import com.deeksha.avr.viewmodel.ProjectViewModel
import com.deeksha.avr.ui.view.approver.DepartmentDetailScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val authState by authViewModel.authState.collectAsState()
    
    // Auto-navigate authenticated users (for both existing sessions and OTP-based authentication)
    LaunchedEffect(authState.isAuthenticated, authState.user) {
        if (authState.isAuthenticated && authState.user != null) {
            val user = authState.user!!
            val currentRoute = navController.currentDestination?.route
            
            android.util.Log.d("AppNavHost", "🎯 Auth state changed - isAuthenticated: ${authState.isAuthenticated}, user: ${user.name}, role: ${user.role}")
            android.util.Log.d("AppNavHost", "🎯 Current route: $currentRoute")
            
            // Only navigate if we're on the login screen or OTP verification screen
            if (currentRoute == Screen.Login.route || currentRoute?.startsWith("otp_verification/") == true) {
                android.util.Log.d("AppNavHost", "🎯 Authenticated user detected - User: ${user.name}, Role: ${user.role}")
                
                // Add a small delay to ensure authentication state is fully synchronized
                kotlinx.coroutines.delay(200)
                
                when (user.role) {
                    UserRole.USER -> {
                        android.util.Log.d("AppNavHost", "🎯 Navigating to USER flow")
                        navController.navigate(Screen.ProjectSelection.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                    UserRole.APPROVER -> {
                        android.util.Log.d("AppNavHost", "🎯 Navigating to APPROVER flow")
                        navController.navigate(Screen.ApproverProjectSelection.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                    UserRole.ADMIN -> {
                        android.util.Log.d("AppNavHost", "🎯 Navigating to ADMIN flow")
                        navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                    UserRole.PRODUCTION_HEAD -> {
                        android.util.Log.d("AppNavHost", "🎯 Navigating to PRODUCTION_HEAD flow")
                        navController.navigate(Screen.ProductionHeadProjectSelection.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth Flow
        composable(Screen.Login.route) {
            val isAccessRestricted by authViewModel.isAccessRestricted.collectAsState()
            val restrictedPhoneNumber by authViewModel.restrictedPhoneNumber.collectAsState()
            
            // Navigate to access restricted screen if access is denied
            LaunchedEffect(isAccessRestricted) {
                if (isAccessRestricted && restrictedPhoneNumber != null) {
                    android.util.Log.d("Navigation", "🚫 Access restricted, navigating to restriction screen")
                    navController.navigate("${Screen.AccessRestricted.route}/$restrictedPhoneNumber")
                }
            }
            
            // Navigation will be handled by the global LaunchedEffect above
            // No need for duplicate navigation logic here
            
            LoginScreen(
                onNavigateToOtp = { phoneNumber ->
                    navController.navigate("otp_verification/${phoneNumber}")
                },
                onSkipForDevelopment = {
                    // Development skip - navigation handled by direct callback
                    android.util.Log.d("Navigation", "🔍 Development skip navigation handled by direct callback")
                },
                onNavigateToRole = { role ->
                    android.util.Log.d("Navigation", "🎯 Direct navigation callback received")
                    android.util.Log.d("Navigation", "🎯 Received role: $role")
                    android.util.Log.d("Navigation", "🎯 Role type: ${role.javaClass.simpleName}")
                    when (role) {
                        UserRole.USER -> {
                            android.util.Log.d("Navigation", "🎯 Navigating to USER flow (ProjectSelection)")
                            android.util.Log.d("Navigation", "🚀 Route: ${Screen.ProjectSelection.route}")
                            navController.navigate(Screen.ProjectSelection.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                        UserRole.APPROVER -> {
                            android.util.Log.d("Navigation", "🎯 Navigating to APPROVER flow (ApproverProjectSelection)")
                            android.util.Log.d("Navigation", "🚀 Route: ${Screen.ApproverProjectSelection.route}")
                            navController.navigate(Screen.ApproverProjectSelection.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                        UserRole.ADMIN -> {
                            android.util.Log.d("Navigation", "🎯 Navigating to ADMIN flow (AdminDashboard)")
                            android.util.Log.d("Navigation", "🚀 Route: ${Screen.AdminDashboard.route}")
                            navController.navigate(Screen.AdminDashboard.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                        UserRole.PRODUCTION_HEAD -> {
                            android.util.Log.d("Navigation", "🎯 Navigating to PRODUCTION_HEAD flow (ProductionHeadProjectSelection)")
                            android.util.Log.d("Navigation", "🚀 Route: ${Screen.ProductionHeadProjectSelection.route}")
                            navController.navigate(Screen.ProductionHeadProjectSelection.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    }
                }
            )
        }
        
        composable(
            route = "${Screen.AccessRestricted.route}/{phoneNumber}",
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            val authViewModel: AuthViewModel = hiltViewModel()
            
            AccessRestrictedScreen(
                phoneNumber = phoneNumber,
                onNavigateBack = {
                    authViewModel.clearAccessRestriction()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = "otp_verification/{phoneNumber}",
            arguments = listOf(navArgument("phoneNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val phoneNumber = backStackEntry.arguments?.getString("phoneNumber") ?: ""
            val authViewModel: AuthViewModel = hiltViewModel(
                viewModelStoreOwner = remember(backStackEntry) { navController.getBackStackEntry(Screen.Login.route) }
            )
            val authState by authViewModel.authState.collectAsState()
            
            OtpVerificationScreen(
                phoneNumber = phoneNumber,
                authViewModel = authViewModel,
                onVerificationSuccess = {
                    // Enhanced navigation logic with proper user data loading
                    android.util.Log.d("Navigation", "🔍 OTP verified. Current user: ${authState.user?.name}, Role: ${authState.user?.role}")
                    
                    // Ensure we have valid user data before navigation
                    val currentUser = authState.user
                    if (currentUser != null && authState.isAuthenticated) {
                        android.util.Log.d("Navigation", "🎯 User data loaded. Routing to ${currentUser.role} flow")
                        
                        when (currentUser.role) {
                            UserRole.USER -> {
                                android.util.Log.d("Navigation", "🎯 Routing USER to Project Selection")
                                navController.navigate(Screen.ProjectSelection.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                            UserRole.APPROVER -> {
                                android.util.Log.d("Navigation", "🎯 Routing APPROVER to Approver Project Selection")
                                // Add a small delay to ensure user data is fully synchronized
                                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                                    kotlinx.coroutines.delay(500) // 500ms delay
                                    android.util.Log.d("Navigation", "🎯 Delayed navigation to Approver Project Selection")
                                    navController.navigate(Screen.ApproverProjectSelection.route) {
                                        popUpTo(Screen.Login.route) { inclusive = true }
                                    }
                                }
                            }
                            UserRole.ADMIN -> {
                                android.util.Log.d("Navigation", "🎯 Routing ADMIN to Admin Dashboard")
                                navController.navigate(Screen.AdminDashboard.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                            UserRole.PRODUCTION_HEAD -> {
                                android.util.Log.d("Navigation", "🎯 Routing PRODUCTION_HEAD to Project Selection")
                                navController.navigate(Screen.ProductionHeadProjectSelection.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }
                        }
                    } else {
                        android.util.Log.w("Navigation", "⚠️ User data not loaded, navigation failed")
                        // Stay on OTP screen or return to login
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Project Selection
        composable(Screen.ProjectSelection.route) {
            ProjectSelectionScreen(
                onProjectSelected = { projectId ->
                    navController.navigate(Screen.ExpenseList.createRoute(projectId))
                },
                onNotificationClick = { userId ->
                    navController.navigate(Screen.NotificationList.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }
        
        // Notification Screen
        composable(Screen.NotificationList.route) {
            NotificationListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProject = { projectId ->
                            navController.navigate(Screen.ExpenseList.createRoute(projectId))
                },
                onNavigateToExpense = { projectId, expenseId ->
                    // Navigate to expense detail or list
                    navController.navigate(Screen.ExpenseList.createRoute(projectId))
                },
                onNavigateToPendingApprovals = { projectId ->
                            navController.navigate(Screen.ProjectPendingApprovals.createRoute(projectId))
                        },
                authViewModel = authViewModel
            )
        }
        
        // Project-specific Notification Screen
        composable(
            route = Screen.ProjectNotifications.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val projectViewModel: ProjectViewModel = hiltViewModel()
            
            // Ensure projects are loaded
            LaunchedEffect(Unit) {
                projectViewModel.loadProjects()
            }
            
            // Get the project from the viewmodel
            val projects by projectViewModel.projects.collectAsState()
            val isLoading by projectViewModel.isLoading.collectAsState()
            val selectedProject = projects.find { it.id == projectId }
            
            when {
                isLoading -> {
                    // Show loading screen
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
                                text = "Loading project...",
                                color = Color.Gray
                            )
                        }
                    }
                }
                selectedProject != null -> {
                    ProjectNotificationScreen(
                        projectId = projectId,
                        projectName = selectedProject.name,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToProject = { projectId ->
                                    navController.navigate(Screen.ExpenseList.createRoute(projectId))
                        },
                        onNavigateToExpense = { projectId, expenseId ->
                            navController.navigate(Screen.ExpenseList.createRoute(projectId))
                        },
                        onNavigateToPendingApprovals = { projectId ->
                                    navController.navigate(Screen.ProjectPendingApprovals.createRoute(projectId))
                                },
                        authViewModel = authViewModel
                    )
                }
                else -> {
                    // Project not found - show error and back button
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "❌ Project Not Found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The selected project could not be loaded.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4285F4)
                            )
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
        
        // Expense Flow
        composable(
            route = Screen.ExpenseList.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val projectViewModel: ProjectViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()
            val authState by authViewModel.authState.collectAsState()
            
            // Ensure projects are loaded
            LaunchedEffect(Unit) {
                projectViewModel.loadProjects()
            }
            
            // Get the project from the viewmodel
            val projects by projectViewModel.projects.collectAsState()
            val isLoading by projectViewModel.isLoading.collectAsState()
            val selectedProject = projects.find { it.id == projectId }
            
            when {
                isLoading -> {
                    // Show loading screen
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
                                text = "Loading project...",
                                color = Color.Gray
                            )
                        }
                    }
                }
                selectedProject != null -> {
                    ExpenseListScreen(
                        project = selectedProject,
                        onNavigateBack = { navController.popBackStack() },
                        onAddExpense = {
                            navController.navigate(Screen.AddExpense.createRoute(projectId))
                        },
                        onTrackSubmissions = {
                            navController.navigate(Screen.TrackSubmissions.createRoute(projectId))
                        },
                        onNavigateToNotifications = { projectId ->
                            navController.navigate(Screen.ProjectNotifications.createRoute(projectId))
                        }
                    )
                }
                else -> {
                    // Project not found - show error and back button
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "❌ Project Not Found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The selected project could not be loaded.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4285F4)
                            )
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
        
        composable(
            route = Screen.AddExpense.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val projectViewModel: ProjectViewModel = hiltViewModel()
            val authViewModel: AuthViewModel = hiltViewModel()
            val authState by authViewModel.authState.collectAsState()
            
            // Ensure projects are loaded
            LaunchedEffect(Unit) {
                projectViewModel.loadProjects()
            }
            
            // Get the project from the viewmodel
            val projects by projectViewModel.projects.collectAsState()
            val isLoading by projectViewModel.isLoading.collectAsState()
            val selectedProject = projects.find { it.id == projectId }
            
            when {
                isLoading -> {
                    // Show loading screen
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
                                text = "Loading project...",
                                color = Color.Gray
                            )
                        }
                    }
                }


                selectedProject != null -> {
                    val currentUser = FirebaseAuth.getInstance().currentUser

                    if (currentUser != null && currentUser.phoneNumber != null) {
                        val phoneNumber = currentUser.phoneNumber!!.removePrefix("+91")
                        val firestore = FirebaseFirestore.getInstance()

                        // Compose states to store user data
                        var userName by remember { mutableStateOf<String?>(null) }
                        var userPhone by remember { mutableStateOf<String?>(null) }

                        // One-time effect to fetch user data
                        LaunchedEffect(phoneNumber) {
                            firestore.collection("users")
                                .document(phoneNumber)
                                .get()
                                .addOnSuccessListener { documentSnapshot ->
                                    if (documentSnapshot.exists()) {
                                        userName = documentSnapshot.getString("name") ?: ""
                                        userPhone = documentSnapshot.getString("phoneNumber") ?: ""
                                        Log.d("Firestore", "✅ Name: $userName, Phone: $userPhone")
                                    } else {
                                        Log.d("Firestore", "❌ No user found with phone number: $phoneNumber")
                                    }
                                }
                                .addOnFailureListener { exception ->
                                    Log.e("Firestore", "Error fetching user document", exception)
                                }
                        }

                        // Only show AddExpenseScreen when userName is loaded
                        if (userName != null && userPhone != null) {
                            AddExpenseScreen(
                                project = selectedProject,
                                userId = userPhone!!,   // Phone number as userId
                                userName = userName!!,
                                onNavigateBack = { navController.popBackStack() },
                                onExpenseAdded = { navController.popBackStack() }
                            )
                        }
                    } else {
                        Log.e("Auth", "Current user is null or phone number is not available")
                    }

                }
                else -> {
                    // Project not found - show error and back button
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "❌ Project Not Found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The selected project could not be loaded.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4285F4)
                            )
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }
        
        composable(
            route = Screen.TrackSubmissions.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val projectViewModel: ProjectViewModel = hiltViewModel()
            
            // Ensure projects are loaded
            LaunchedEffect(Unit) {
                projectViewModel.loadProjects()
            }
            
            // Get the project from the viewmodel
            val projects by projectViewModel.projects.collectAsState()
            val isLoading by projectViewModel.isLoading.collectAsState()
            val selectedProject = projects.find { it.id == projectId }
            
            when {
                isLoading -> {
                    // Show loading screen
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
                                text = "Loading project...",
                                color = Color.Gray
                            )
                        }
                    }
                }
                selectedProject != null -> {
                    TrackSubmissionsScreen(
                        project = selectedProject,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                else -> {
                    // Project not found - show error and back button
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "❌ Project Not Found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "The selected project could not be loaded.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { navController.popBackStack() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4285F4)
                            )
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            }
        }

        // User Dashboard (Alternative entry point)
        composable(Screen.UserDashboard.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            UserDashboardScreen(
                onNavigateToProjectSelection = {
                    navController.navigate(Screen.ProjectSelection.route)
                },
                onNavigateToAddExpense = {
                    navController.navigate(Screen.ProjectSelection.route)
                },
                onNavigateToExpenseList = {
                    navController.navigate(Screen.ProjectSelection.route)
                },
                onNavigateToTrackSubmissions = {
                    navController.navigate(Screen.ProjectSelection.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // Approver Flow
        composable(Screen.ApproverDashboard.route) {
            ApproverDashboardScreen(
                onNavigateToPendingApprovals = {
                    navController.navigate(Screen.PendingApprovals.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        // New Approver Project Flow
        composable(Screen.ApproverProjectSelection.route) {
            ApproverProjectSelectionScreen(
                onProjectSelected = { projectId ->
                    navController.navigate(Screen.ApproverProjectDashboard.createRoute(projectId))
                },
                onNavigateToOverallReports = {
                    navController.navigate(Screen.OverallReports.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.ApproverNotificationScreen.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }
        
        composable(Screen.ApproverNotificationScreen.route) {
            ApproverNotificationScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToApproverProjectDashboard = { projectId ->
                    navController.navigate(Screen.ApproverProjectDashboard.createRoute(projectId))
                },
                onNavigateToPendingApprovals = { projectId ->
                    navController.navigate(Screen.ProjectPendingApprovals.createRoute(projectId))
                },
                authViewModel = authViewModel
            )
        }
        
        composable(
            route = Screen.ApproverProjectDashboard.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ApproverProjectDashboardScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPendingApprovals = { projectId ->
                    navController.navigate(Screen.ProjectPendingApprovals.createRoute(projectId))
                },
                onNavigateToAddExpense = {
                    navController.navigate(Screen.AddExpense.createRoute(projectId))
                },
                onNavigateToReports = { projectId ->
                    navController.navigate(Screen.ApproverReports.createRoute(projectId))
                },
                onNavigateToDepartmentDetail = { projectId, departmentName ->
                    navController.navigate(Screen.DepartmentDetail.createRoute(projectId, departmentName))
                },
                onNavigateToProjectNotifications = { projectId ->
                    navController.navigate(Screen.ProjectNotifications.createRoute(projectId))
                }
            )
        }
        
        composable(
            route = Screen.CategoryDetail.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            CategoryDetailScreen(
                projectId = projectId,
                categoryName = categoryName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.DepartmentDetail.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("departmentName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val departmentName = backStackEntry.arguments?.getString("departmentName") ?: ""
            DepartmentDetailScreen(
                projectId = projectId,
                departmentName = departmentName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.ApproverReports.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ApproverReportsScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.OverallReports.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            OverallReportsScreen(
                onNavigateBack = { navController.popBackStack() },
                authViewModel = authViewModel
            )
        }
        
        composable(Screen.PendingApprovals.route) {
            PendingApprovalsScreen(
                onNavigateBack = { navController.popBackStack() },
                onReviewExpense = { expenseId ->
                    navController.navigate("review_expense/$expenseId")
                }
            )
        }
        
        composable(
            route = Screen.ProjectPendingApprovals.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            PendingApprovalsScreen(
                projectId = projectId, // Pass the projectId for project-specific approvals
                onNavigateBack = { navController.popBackStack() },
                onReviewExpense = { expenseId ->
                    navController.navigate("review_expense/$expenseId")
                }
            )
        }
        
        composable(
            route = "review_expense/{expenseId}",
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
            ReviewExpenseScreen(
                expenseId = expenseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Admin Flow
        composable(Screen.AdminDashboard.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            AdminDashboardScreen(
                onNavigateToManageUsers = {
                    navController.navigate(Screen.ManageUsers.route)
                },
                onNavigateToManageProjects = {
                    navController.navigate(Screen.ManageProjects.route)
                },
                onNavigateToReports = {
                    navController.navigate(Screen.Reports.route)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.ManageUsers.route) {
            ManageUsersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ManageProjects.route) {
            ManageProjectsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.Reports.route) {
            ReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Production Head Flow - Complete with all Approver functionality
        composable(Screen.ProductionHeadProjectSelection.route) {
            ProductionHeadProjectSelectionScreen(
                onProjectSelected = { projectId ->
                    navController.navigate(Screen.ProductionHeadProjectDashboard.createRoute(projectId))
                },
                onCreateUser = {
                    navController.navigate(Screen.CreateUser.route)
                },
                onNewProject = {
                    navController.navigate(Screen.NewProject.route)
                },
                onEditProject = { projectId ->
                    navController.navigate(Screen.EditProject.createRoute(projectId))
                },
                onNavigateToDashboard = {
                    navController.navigate(Screen.ProductionHeadDashboard.route)
                },
                onNavigateToOverallReports = {
                    navController.navigate(Screen.ProductionHeadOverallReports.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.NotificationList.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                authViewModel = authViewModel
            )
        }
        
        composable(
            route = Screen.EditProject.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            EditProjectScreen(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ProductionHeadDashboard.route) {
            ProductionHeadDashboard(
                onNavigateToProjectSelection = {
                    navController.navigate(Screen.ProductionHeadProjectSelection.route)
                },
                onNavigateToPendingApprovals = {
                    navController.navigate(Screen.ProductionHeadPendingApprovals.route)
                },
                onNavigateToOverallReports = {
                    navController.navigate(Screen.ProductionHeadOverallReports.route)
                },
                onNavigateToCreateUser = {
                    navController.navigate(Screen.CreateUser.route)
                },
                onNavigateToNewProject = {
                    navController.navigate(Screen.NewProject.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = Screen.ProductionHeadProjectDashboard.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ProductionHeadProjectDashboard(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPendingApprovals = {
                    navController.navigate(Screen.ProductionHeadProjectPendingApprovals.createRoute(projectId))
                },
                onNavigateToAddExpense = { projectId ->
                    navController.navigate(Screen.AddExpense.createRoute(projectId))
                },
                onNavigateToReports = { 
                    navController.navigate(Screen.ProductionHeadReports.createRoute(projectId))
                },
                onNavigateToProjectNotifications = { projectId ->
                    navController.navigate(Screen.ProjectNotifications.createRoute(projectId))
                },
                onNavigateToDepartmentDetail = { projectId, departmentName ->
                    navController.navigate(Screen.DepartmentDetail.createRoute(projectId, departmentName))
                }
            )
        }
        
        composable(Screen.ProductionHeadPendingApprovals.route) {
            ProductionHeadPendingApprovals(
                projectId = null, // Overall pending approvals
                onNavigateBack = { navController.popBackStack() },
                onReviewExpense = { expenseId ->
                    navController.navigate(Screen.ProductionHeadReviewExpense.createRoute(expenseId))
                }
            )
        }
        
        composable(
            route = Screen.ProductionHeadProjectPendingApprovals.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ProductionHeadPendingApprovals(
                projectId = projectId, // Project-specific pending approvals
                onNavigateBack = { navController.popBackStack() },
                onReviewExpense = { expenseId ->
                    navController.navigate(Screen.ProductionHeadReviewExpense.createRoute(expenseId))
                }
            )
        }
        
        composable(
            route = Screen.ProductionHeadReviewExpense.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.StringType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getString("expenseId") ?: ""
            ReviewExpenseScreen(
                expenseId = expenseId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.ProductionHeadReports.route,
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            ProductionHeadReports(
                projectId = projectId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = Screen.ProductionHeadCategoryDetail.route,
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("categoryName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
            val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
            ProductionHeadCategoryDetail(
                projectId = projectId,
                categoryName = categoryName,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.ProductionHeadOverallReports.route) {
            ProductionHeadOverallReports(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(Screen.CreateUser.route) {
            CreateUserScreen(
                onNavigateBack = { navController.popBackStack() },
                onUserCreated = { navController.popBackStack() }
            )
        }
        
        composable(Screen.NewProject.route) {
            NewProjectScreen(
                onNavigateBack = { navController.popBackStack() },
                onProjectCreated = { navController.popBackStack() }
            )
        }
    }
}
