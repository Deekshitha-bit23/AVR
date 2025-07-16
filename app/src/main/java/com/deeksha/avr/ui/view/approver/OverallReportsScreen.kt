package com.deeksha.avr.ui.view.approver

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.ui.common.OverallReportsContent
import com.deeksha.avr.viewmodel.OverallReportsViewModel
import androidx.compose.ui.graphics.Color

@Composable
fun OverallReportsScreen(
    onNavigateBack: () -> Unit,
    overallReportsViewModel: OverallReportsViewModel = hiltViewModel()
) {
    OverallReportsContent(
        title = "Overall Reports",
        primaryColor = Color(0xFF8B5FBF),
        onNavigateBack = onNavigateBack,
        overallReportsViewModel = overallReportsViewModel,
        onExportPDF = {
            overallReportsViewModel.exportToPDF(
                onSuccess = { /* Handle success */ },
                onError = { /* Handle error */ }
            )
        },
        onExportCSV = {
            overallReportsViewModel.exportToCSV(
                onSuccess = { /* Handle success */ },
                onError = { /* Handle error */ }
            )
        }
    )
} 