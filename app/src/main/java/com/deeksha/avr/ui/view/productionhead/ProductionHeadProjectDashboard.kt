package com.deeksha.avr.ui.view.productionhead

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.ui.view.approver.ApproverProjectDashboardScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionHeadProjectDashboard(
    projectId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPendingApprovals: () -> Unit,
    onNavigateToAddExpense: (String) -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToProjectNotifications: (String) -> Unit = {},
    onNavigateToDepartmentDetail: (String, String) -> Unit = { _, _ -> }
) {
    // Directly embed the existing ApproverProjectDashboardScreen without any wrapper
    ApproverProjectDashboardScreen(
        projectId = projectId,
        onNavigateBack = onNavigateBack,
        onNavigateToPendingApprovals = { _ -> onNavigateToPendingApprovals() },
        onNavigateToAddExpense = { onNavigateToAddExpense(projectId) },
        onNavigateToReports = { _ -> onNavigateToReports() },
        onNavigateToProjectNotifications = onNavigateToProjectNotifications,
        onNavigateToDepartmentDetail = onNavigateToDepartmentDetail
    )
} 