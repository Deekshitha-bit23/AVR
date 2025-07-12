package com.deeksha.avr.ui.view.productionhead

import androidx.compose.runtime.Composable
import com.deeksha.avr.ui.view.approver.CategoryDetailScreen

@Composable
fun ProductionHeadCategoryDetail(
    projectId: String,
    categoryName: String,
    onNavigateBack: () -> Unit
) {
    // Reuse the existing CategoryDetailScreen from approver flow
    // The functionality is identical for both roles
    CategoryDetailScreen(
        projectId = projectId,
        categoryName = categoryName,
        onNavigateBack = onNavigateBack
    )
} 