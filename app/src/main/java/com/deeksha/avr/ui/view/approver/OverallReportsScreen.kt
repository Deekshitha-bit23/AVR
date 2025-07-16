package com.deeksha.avr.ui.view.approver

import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.ui.common.OverallReportsContent
import com.deeksha.avr.viewmodel.OverallReportsViewModel
import androidx.compose.ui.graphics.Color
import android.widget.Toast

@Composable
fun OverallReportsScreen(
    onNavigateBack: () -> Unit,
    overallReportsViewModel: OverallReportsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    OverallReportsContent(
        title = "Overall Reports",
        primaryColor = Color(0xFF8B5FBF),
        onNavigateBack = onNavigateBack,
        overallReportsViewModel = overallReportsViewModel,
        onExportPDF = {
            overallReportsViewModel.exportToPDF(
                onSuccess = { intent ->
                    if (intent != null) {
                        try {
                            context.startActivity(intent)
                            Toast.makeText(context, "PDF exported successfully!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to open PDF: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to create PDF export", Toast.LENGTH_LONG).show()
                    }
                },
                onError = { errorMessage ->
                    Toast.makeText(context, "Export failed: $errorMessage", Toast.LENGTH_LONG).show()
                }
            )
        },
        onExportCSV = {
            overallReportsViewModel.exportToCSV(
                onSuccess = { intent ->
                    if (intent != null) {
                        try {
                            context.startActivity(intent)
                            Toast.makeText(context, "CSV exported successfully!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to open CSV: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to create CSV export", Toast.LENGTH_LONG).show()
                    }
                },
                onError = { errorMessage ->
                    Toast.makeText(context, "Export failed: $errorMessage", Toast.LENGTH_LONG).show()
                }
            )
        }
    )
} 