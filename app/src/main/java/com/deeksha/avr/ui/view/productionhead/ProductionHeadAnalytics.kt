package com.deeksha.avr.ui.view.productionhead

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.deeksha.avr.viewmodel.ReportsViewModel
import com.deeksha.avr.utils.FormatUtils
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionHeadAnalytics(
    projectId: String,
    onNavigateBack: () -> Unit,
    reportsViewModel: ReportsViewModel = hiltViewModel()
) {
    val reportData by reportsViewModel.reportData.collectAsState()
    val isLoading by reportsViewModel.isLoading.collectAsState()
    val error by reportsViewModel.error.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Forecast", "Variance", "Trends")
    
    // Load reports when screen starts
    LaunchedEffect(projectId) {
        reportsViewModel.loadReportsForProject(projectId)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Top Bar
        TopAppBar(
            title = {
                Text(
                    text = "PREDICTIVE ANALYSIS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF8B5FBF)
            )
        )
        
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF8B5FBF))
                }
            }
            error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Error Loading Analytics",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                        Text(
                            text = error ?: "Unknown error",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = { 
                                reportsViewModel.clearError()
                                reportsViewModel.loadReportsForProject(projectId) 
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B5FBF)
                            )
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Tab Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            TabButton(
                                text = tab,
                                isSelected = selectedTab == index,
                                onClick = { selectedTab = index },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Content based on selected tab
                    when (selectedTab) {
                        0 -> ForecastContent(reportData = reportData, projectId = projectId)
                        1 -> VarianceContent(reportData = reportData)
                        2 -> TrendsContent(reportData = reportData)
                    }
                }
            }
        }
    }
}

@Composable
private fun TabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF4285F4) else Color(0xFFE0E0E0)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ForecastContent(
    reportData: com.deeksha.avr.model.ReportData,
    projectId: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Forecast Report",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B5FBF),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Predict total project cost over remaining timeline for this project",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            // Generate dynamic forecast data based on real project data
            val forecastData = generateDynamicForecastData(reportData)
            
            // Enhanced Line Chart with better visualization
            EnhancedLineChart(
                months = forecastData.months,
                budgetData = forecastData.budgetData,
                actualData = forecastData.actualData,
                forecastData = forecastData.forecastData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("Budget", Color(0xFF4285F4))
                LegendItem("Actual", Color(0xFF9C27B0))
                LegendItem("Forecast", Color(0xFF4CAF50))
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Dynamic Forecast Summary
            DynamicForecastSummary(
                totalBudget = reportData.totalBudget,
                totalSpent = reportData.totalSpent,
                forecastTotal = forecastData.forecastTotal,
                remainingBudget = reportData.totalBudget - reportData.totalSpent,
                forecastVariance = forecastData.forecastTotal - reportData.totalBudget,
                budgetUsagePercentage = reportData.budgetUsagePercentage
            )
        }
    }
}

@Composable
private fun EnhancedLineChart(
    months: List<String>,
    budgetData: List<Double>,
    actualData: List<Double>,
    forecastData: List<Double>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Calculate max value for scaling
        val maxValue = maxOf(
            budgetData.maxOrNull() ?: 0.0,
            actualData.maxOrNull() ?: 0.0,
            forecastData.maxOrNull() ?: 0.0
        )
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Y-axis labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cost",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${(maxValue / 1000).toInt()}k",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
            
            // Mobile-style Line Chart
            MobileLineChart(
                months = months,
                budgetData = budgetData,
                actualData = actualData,
                forecastData = forecastData,
                maxValue = maxValue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

@Composable
private fun MobileLineChart(
    months: List<String>,
    budgetData: List<Double>,
    actualData: List<Double>,
    forecastData: List<Double>,
    maxValue: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val padding = 20f
            val chartWidth = canvasWidth - (padding * 2)
            val chartHeight = canvasHeight - (padding * 2)
            
            // Draw grid lines
            val gridColor = Color(0xFFE5E7EB)
            val strokeWidth = 1.dp.toPx()
            
            // Horizontal grid lines
            for (i in 0..4) {
                val y = padding + (chartHeight / 4) * i
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(padding + chartWidth, y),
                    strokeWidth = strokeWidth
                )
            }
            
            // Vertical grid lines
            for (i in 0 until months.size) {
                val x = padding + (chartWidth / (months.size - 1)) * i
                drawLine(
                    color = gridColor,
                    start = Offset(x, padding),
                    end = Offset(x, padding + chartHeight),
                    strokeWidth = strokeWidth
                )
            }
            
            // Convert data to points
            fun getPoint(index: Int, value: Double): Offset {
                val x = padding + (chartWidth / (months.size - 1)) * index
                val y = padding + chartHeight - (value / maxValue * chartHeight)
                return Offset(x.toFloat(), y.toFloat())
            }
            
            // Draw lines with markers
            val budgetPoints = budgetData.mapIndexed { index, value -> getPoint(index, value) }
            val actualPoints = actualData.mapIndexed { index, value -> getPoint(index, value) }
            val forecastPoints = forecastData.mapIndexed { index, value -> getPoint(index, value) }
            
            // Draw budget line (blue)
            for (i in 0 until budgetPoints.size - 1) {
                drawLine(
                    color = Color(0xFF2563EB),
                    start = budgetPoints[i],
                    end = budgetPoints[i + 1],
                    strokeWidth = 3.dp.toPx()
                )
            }
            
            // Draw actual line (purple)
            for (i in 0 until actualPoints.size - 1) {
                drawLine(
                    color = Color(0xFF7C3AED),
                    start = actualPoints[i],
                    end = actualPoints[i + 1],
                    strokeWidth = 3.dp.toPx()
                )
            }
            
            // Draw forecast line (green)
            for (i in 0 until forecastPoints.size - 1) {
                drawLine(
                    color = Color(0xFF059669),
                    start = forecastPoints[i],
                    end = forecastPoints[i + 1],
                    strokeWidth = 3.dp.toPx()
                )
            }
            
            // Draw markers (circles)
            val markerRadius = 4.dp.toPx()
            
            // Budget markers
            budgetPoints.forEach { point ->
                drawCircle(
                    color = Color(0xFF2563EB),
                    radius = markerRadius,
                    center = point
                )
                // White center
                drawCircle(
                    color = Color.White,
                    radius = markerRadius * 0.6f,
                    center = point
                )
            }
            
            // Actual markers
            actualPoints.forEach { point ->
                drawCircle(
                    color = Color(0xFF7C3AED),
                    radius = markerRadius,
                    center = point
                )
                // White center
                drawCircle(
                    color = Color.White,
                    radius = markerRadius * 0.6f,
                    center = point
                )
            }
            
            // Forecast markers
            forecastPoints.forEach { point ->
                drawCircle(
                    color = Color(0xFF059669),
                    radius = markerRadius,
                    center = point
                )
                // White center
                drawCircle(
                    color = Color.White,
                    radius = markerRadius * 0.6f,
                    center = point
                )
            }
        }
        
        // Month labels at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            months.forEach { month ->
                Text(
                    text = month,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun DynamicForecastSummary(
    totalBudget: Double,
    totalSpent: Double,
    forecastTotal: Double,
    remainingBudget: Double,
    forecastVariance: Double,
    budgetUsagePercentage: Double
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Forecast Summary",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5FBF),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // First row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total Budget",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(totalBudget),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total Spent",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(totalSpent),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9C27B0)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Second row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Forecast Total",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(forecastTotal),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Variance",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(forecastVariance),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (forecastVariance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Additional metrics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Budget Usage",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${String.format("%.1f", budgetUsagePercentage)}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (budgetUsagePercentage <= 80) Color(0xFF4CAF50) else if (budgetUsagePercentage <= 100) Color(0xFFFF9800) else Color(0xFFF44336)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Remaining",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(remainingBudget),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (remainingBudget >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
private fun VarianceContent(reportData: com.deeksha.avr.model.ReportData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Variance Analysis",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B5FBF),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Compare forecast vs. budget (gain/loss)",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            // Generate variance data for clustered bar chart
            val varianceData = generateVarianceData(reportData)
            
            // Clustered Bar Chart
            ClusteredBarChart(
                months = varianceData.months,
                budgetData = varianceData.budgetData,
                actualData = varianceData.actualData,
                forecastData = varianceData.forecastData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem("Budget", Color(0xFF4285F4))
                LegendItem("Actual", Color(0xFF9C27B0))
                LegendItem("Forecast", Color(0xFF4CAF50))
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Variance Summary
            VarianceSummary(
                totalBudget = reportData.totalBudget,
                totalSpent = reportData.totalSpent,
                forecastTotal = varianceData.forecastTotal,
                budgetVariance = reportData.totalBudget - reportData.totalSpent,
                forecastVariance = varianceData.forecastTotal - reportData.totalBudget,
                budgetUsagePercentage = reportData.budgetUsagePercentage
            )
        }
    }
}

@Composable
private fun TrendsContent(reportData: com.deeksha.avr.model.ReportData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Trends Analysis",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8B5FBF),
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            // Generate trends data for pie chart
            val trendsData = generateTrendsData(reportData)
            
            // Pie Chart
            PieChart(
                data = trendsData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                trendsData.forEach { item ->
                    LegendItem(item.label, item.color)
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Trends Summary
            TrendsSummary(
                totalBudget = reportData.totalBudget,
                totalSpent = reportData.totalSpent,
                budgetUsagePercentage = reportData.budgetUsagePercentage,
                trendsData = trendsData,
                reportData = reportData
            )
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
private fun VarianceMetric(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Black
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun TrendIndicator(
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Black
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
private fun ClusteredBarChart(
    months: List<String>,
    budgetData: List<Double>,
    actualData: List<Double>,
    forecastData: List<Double>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Calculate max value for scaling
        val maxValue = maxOf(
            budgetData.maxOrNull() ?: 0.0,
            actualData.maxOrNull() ?: 0.0,
            forecastData.maxOrNull() ?: 0.0
        )
        
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Y-axis labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cost",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${(maxValue / 1000).toInt()}k",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
            
            // Mobile-style Bar Chart
            MobileBarChart(
                months = months,
                budgetData = budgetData,
                actualData = actualData,
                forecastData = forecastData,
                maxValue = maxValue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}

@Composable
private fun MobileBarChart(
    months: List<String>,
    budgetData: List<Double>,
    actualData: List<Double>,
    forecastData: List<Double>,
    maxValue: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val padding = 20f
            val chartWidth = canvasWidth - (padding * 2)
            val chartHeight = canvasHeight - (padding * 2)
            
            // Draw grid lines
            val gridColor = Color(0xFFE5E7EB)
            val strokeWidth = 1.dp.toPx()
            
            // Horizontal grid lines
            for (i in 0..4) {
                val y = padding + (chartHeight / 4) * i
                drawLine(
                    color = gridColor,
                    start = Offset(padding, y),
                    end = Offset(padding + chartWidth, y),
                    strokeWidth = strokeWidth
                )
            }
            
            // Calculate bar dimensions
            val barWidth = (chartWidth / months.size) * 0.6f // 60% of available space
            val barSpacing = (chartWidth / months.size) * 0.4f / 4f // 40% for spacing, divided by 4 (3 bars + 3 gaps)
            val clusterWidth = barWidth * 0.8f // 80% of bar width for each bar
            val barGap = (barWidth - clusterWidth) / 2f
            
            // Draw bars for each month
            months.forEachIndexed { index, _ ->
                val x = padding + (chartWidth / months.size) * index + (chartWidth / months.size - barWidth) / 2f
                
                // Budget bar (blue) - left
                val budgetHeight = (budgetData[index] / maxValue * chartHeight).toFloat()
                val budgetY = padding + chartHeight - budgetHeight
                drawRect(
                    color = Color(0xFF2563EB),
                    topLeft = Offset(x + barGap, budgetY),
                    size = androidx.compose.ui.geometry.Size(clusterWidth, budgetHeight)
                )
                
                // Actual bar (purple) - center
                val actualHeight = (actualData[index] / maxValue * chartHeight).toFloat()
                val actualY = padding + chartHeight - actualHeight
                drawRect(
                    color = Color(0xFF7C3AED),
                    topLeft = Offset(x + barGap + clusterWidth + barGap, actualY),
                    size = androidx.compose.ui.geometry.Size(clusterWidth, actualHeight)
                )
                
                // Forecast bar (green) - right
                val forecastHeight = (forecastData[index] / maxValue * chartHeight).toFloat()
                val forecastY = padding + chartHeight - forecastHeight
                drawRect(
                    color = Color(0xFF059669),
                    topLeft = Offset(x + barGap + (clusterWidth + barGap) * 2, forecastY),
                    size = androidx.compose.ui.geometry.Size(clusterWidth, forecastHeight)
                )
            }
        }
        
        // Month labels at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            months.forEach { month ->
                Text(
                    text = month,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun VarianceSummary(
    totalBudget: Double,
    totalSpent: Double,
    forecastTotal: Double,
    budgetVariance: Double,
    forecastVariance: Double,
    budgetUsagePercentage: Double
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Variance Summary",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5FBF),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // First row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Budget Variance",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(budgetVariance),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (budgetVariance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Forecast Variance",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(forecastVariance),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (forecastVariance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Second row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Budget vs Actual",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${String.format("%.1f", budgetUsagePercentage)}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (budgetUsagePercentage <= 100) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Forecast vs Budget",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                val forecastVsBudgetPercentage = if (totalBudget > 0) (forecastTotal / totalBudget) * 100 else 0.0
                Text(
                    text = "${String.format("%.1f", forecastVsBudgetPercentage)}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (forecastVsBudgetPercentage <= 100) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Third row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total Budget",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(totalBudget),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4285F4)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Forecast Total",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(forecastTotal),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
private fun PieChart(
    data: List<PieChartItem>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Mobile-style Pie Chart
        MobilePieChart(
            data = data,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        )
    }
}

@Composable
private fun MobilePieChart(
    data: List<PieChartItem>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color(0xFFF8F9FA), RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = minOf(size.width, size.height) / 2f - 20f
            
            var startAngle = -90f
            
            data.forEach { item ->
                val sweepAngle = ((item.percentage / 100f) * 360f).toFloat()
                
                // Draw pie slice
                drawArc(
                    color = item.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)
                )
                
                startAngle += sweepAngle
            }
        }
        
        // Add percentage labels on the pie chart
        data.forEachIndexed { index, item ->
            val angle = -90f + (data.take(index).sumOf { it.percentage } * 3.6f).toFloat() + (item.percentage * 1.8f).toFloat()
            val textRadius = 60f
            val textX = 100f + textRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
            val textY = 100f + textRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
            
            Text(
                text = "${item.percentage.toInt()}%",
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(
                        x = (textX - 100f).dp,
                        y = (textY - 100f).dp
                    )
            )
        }
    }
}

@Composable
private fun TrendsSummary(
    totalBudget: Double,
    totalSpent: Double,
    budgetUsagePercentage: Double,
    trendsData: List<PieChartItem>,
    reportData: com.deeksha.avr.model.ReportData
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Category Analysis",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5FBF),
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // Show categories that exceed limits (dynamic threshold based on data)
        val threshold = if (trendsData.size > 3) 25.0 else 30.0 // Lower threshold for more categories
        val exceedingCategories = trendsData.filter { it.percentage > threshold }
        
        if (exceedingCategories.isNotEmpty()) {
            Text(
                text = "Categories Exceeding Limits:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF44336),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            exceedingCategories.forEach { category ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = category.label,
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                    Text(
                        text = "${String.format("%.1f", category.percentage)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF44336)
                    )
                }
            }
        } else {
            Text(
                text = "All categories within acceptable limits",
                fontSize = 12.sp,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold
            )
        }
        
        // Show top spending departments (instead of categories)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Top Spending Departments:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B5FBF),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Generate department data for top spending areas
        val departmentData = generateDepartmentTrendsData(reportData)
        departmentData.take(3).forEach { department ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = department.label,
                    fontSize = 12.sp,
                    color = Color.Black
                )
                Text(
                    text = "${String.format("%.1f", department.percentage)}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B5FBF)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Overall spending trend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total Spent",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FormatUtils.formatCurrency(totalSpent),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9C27B0)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Budget Usage",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = "${String.format("%.1f", budgetUsagePercentage)}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (budgetUsagePercentage <= 80) Color(0xFF4CAF50) else if (budgetUsagePercentage <= 100) Color(0xFFFF9800) else Color(0xFFF44336)
                )
            }
        }
    }
}

// Data class for pie chart items
private data class PieChartItem(
    val label: String,
    val percentage: Double,
    val color: Color
)

// Data class for forecast data
private data class ForecastData(
    val months: List<String>,
    val budgetData: List<Double>,
    val actualData: List<Double>,
    val forecastData: List<Double>,
    val forecastTotal: Double
)

// Data class for variance data
private data class VarianceData(
    val months: List<String>,
    val budgetData: List<Double>,
    val actualData: List<Double>,
    val forecastData: List<Double>,
    val forecastTotal: Double
)

// Generate dynamic forecast data based on real project data
private fun generateDynamicForecastData(reportData: com.deeksha.avr.model.ReportData): ForecastData {
    // Get current date and generate months from current month onwards
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    val currentYear = calendar.get(Calendar.YEAR)
    
    // Generate 6 months starting from current month
    val months = (0..5).map { monthOffset ->
        val targetMonth = (currentMonth + monthOffset) % 12
        val monthNames = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        monthNames[targetMonth]
    }
    
    // Calculate dynamic parameters based on real data
    val totalBudget = reportData.totalBudget
    val totalSpent = reportData.totalSpent
    val budgetUsagePercentage = reportData.budgetUsagePercentage
    
    // Calculate project duration (assuming 6 months from current date)
    val projectDurationMonths = 6.0
    val monthlyAverage = if (totalSpent > 0) totalSpent / projectDurationMonths else totalBudget / projectDurationMonths
    
    // Generate budget data (linear distribution based on project timeline)
    val budgetData = months.mapIndexed { index, _ ->
        totalBudget * (index + 1) / months.size
    }
    
    // Generate actual data based on real spending patterns
    // Since project starts now, all data is projected based on current spending rate
    val actualData = months.mapIndexed { index, _ ->
        if (index == 0) {
            // Current month - use actual spent amount
            totalSpent
        } else {
            // Future months - project based on current spending rate
            totalSpent + (monthlyAverage * index)
        }
    }
    
    // Generate forecast data based on spending velocity and remaining budget
    val remainingBudget = totalBudget - totalSpent
    val remainingMonths = months.size - 1.0 // Exclude current month
    val forecastMonthlySpend = if (remainingMonths > 0) remainingBudget / remainingMonths else 0.0
    
    val forecastData = months.mapIndexed { index, _ ->
        if (index == 0) {
            // Current month - use actual data
            actualData[index]
        } else {
            // Future months - forecast based on remaining budget
            val projectedSpend = totalSpent + (forecastMonthlySpend * index)
            min(projectedSpend, totalBudget) // Cap at total budget
        }
    }
    
    val forecastTotal = forecastData.last()
    
    return ForecastData(
        months = months,
        budgetData = budgetData,
        actualData = actualData,
        forecastData = forecastData,
        forecastTotal = forecastTotal
    )
}

// Generate dynamic variance data based on real project data
private fun generateVarianceData(reportData: com.deeksha.avr.model.ReportData): VarianceData {
    // Get current date and generate months from current month onwards
    val calendar = Calendar.getInstance()
    val currentMonth = calendar.get(Calendar.MONTH)
    
    // Generate 5 months starting from current month
    val months = (0..4).map { monthOffset ->
        val targetMonth = (currentMonth + monthOffset) % 12
        val monthNames = listOf(
            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        )
        monthNames[targetMonth]
    }
    
    // Calculate dynamic parameters based on real data
    val totalBudget = reportData.totalBudget
    val totalSpent = reportData.totalSpent
    val budgetUsagePercentage = reportData.budgetUsagePercentage
    
    // Calculate spending velocity based on actual data
    val historicalMonths = 5.0
    val monthlyAverage = if (totalSpent > 0) totalSpent / historicalMonths else totalBudget / 12.0
    
    // Generate budget data (linear distribution based on project timeline)
    val budgetData = months.mapIndexed { index, _ ->
        totalBudget * (index + 1) / months.size
    }
    
    // Generate actual data based on real spending patterns
    val actualData = months.mapIndexed { index, _ ->
        if (index < 3) {
            // Historical data (first 3 months) - distribute actual spending
            totalSpent * (index + 1) / 3.0
        } else {
            // Projected actual data based on current spending rate
            totalSpent + (monthlyAverage * (index - 2))
        }
    }
    
    // Generate forecast data based on spending velocity and remaining budget
    val remainingBudget = totalBudget - totalSpent
    val remainingMonths = months.size - 3.0
    val forecastMonthlySpend = if (remainingMonths > 0) remainingBudget / remainingMonths else 0.0
    
    val forecastData = months.mapIndexed { index, _ ->
        if (index < 3) {
            // Historical actual data
            actualData[index]
        } else {
            // Forecast based on remaining budget and spending velocity
            val projectedSpend = totalSpent + (forecastMonthlySpend * (index - 2))
            min(projectedSpend, totalBudget) // Cap at total budget
        }
    }
    
    val forecastTotal = forecastData.last()
    
    return VarianceData(
        months = months,
        budgetData = budgetData,
        actualData = actualData,
        forecastData = forecastData,
        forecastTotal = forecastTotal
    )
}

// Generate dynamic trends data based on real project data
private fun generateTrendsData(reportData: com.deeksha.avr.model.ReportData): List<PieChartItem> {
    val totalSpent = reportData.totalSpent
    val expensesByCategory = reportData.expensesByCategory
    val expensesByDepartment = reportData.expensesByDepartment
    
    // If we have category data, use it; otherwise fall back to department data
    val dataSource = if (expensesByCategory.isNotEmpty()) {
        expensesByCategory
    } else if (expensesByDepartment.isNotEmpty()) {
        expensesByDepartment
    } else {
        // Fallback to default categories if no data
        mapOf(
            "Travel" to (totalSpent * 0.45),
            "Meals" to (totalSpent * 0.30),
            "Misc" to (totalSpent * 0.25)
        )
    }
    
    // Convert to pie chart items with dynamic colors
    val pieChartItems = dataSource.map { (name, amount) ->
        val percentage = if (totalSpent > 0) (amount / totalSpent) * 100 else 0.0
        val color = getDynamicColor(name, dataSource.keys.indexOf(name))
        
        PieChartItem(
            label = name,
            percentage = percentage,
            color = color
        )
    }.sortedByDescending { it.percentage }
    
    // If we have more than 5 categories, group smaller ones into "Others"
    return if (pieChartItems.size > 5) {
        val topCategories = pieChartItems.take(4)
        val others = pieChartItems.drop(4)
        val othersTotal = others.sumOf { it.percentage }
        
        topCategories + PieChartItem(
            label = "Others",
            percentage = othersTotal,
            color = Color(0xFF9E9E9E) // Gray for others
        )
    } else {
        pieChartItems
    }
}

// Generate department trends data for top spending areas
private fun generateDepartmentTrendsData(reportData: com.deeksha.avr.model.ReportData): List<PieChartItem> {
    val totalSpent = reportData.totalSpent
    val expensesByDepartment = reportData.expensesByDepartment
    
    // If we have department data, use it; otherwise fall back to default departments
    val departmentData = if (expensesByDepartment.isNotEmpty()) {
        expensesByDepartment
    } else {
        // Fallback to default departments if no data
        mapOf(
            "Production" to (totalSpent * 0.45),
            "Marketing" to (totalSpent * 0.28),
            "Operations" to (totalSpent * 0.15),
            "Finance" to (totalSpent * 0.08),
            "HR" to (totalSpent * 0.04)
        )
    }
    
    // Convert to pie chart items with dynamic colors and percentages
    val pieChartItems = departmentData.map { (name, amount) ->
        val percentage = if (totalSpent > 0) (amount / totalSpent) * 100 else 0.0
        val color = getDynamicColor(name, departmentData.keys.indexOf(name))
        
        PieChartItem(
            label = name,
            percentage = percentage,
            color = color
        )
    }.sortedByDescending { it.percentage }
    
    return pieChartItems
}

// Generate dynamic colors for categories/departments
private fun getDynamicColor(name: String, index: Int): Color {
    val colorPalette = listOf(
        Color(0xFF4285F4), // Blue
        Color(0xFF9C27B0), // Purple
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFFF44336), // Red
        Color(0xFF2196F3), // Light Blue
        Color(0xFF795548), // Brown
        Color(0xFF607D8B), // Blue Grey
        Color(0xFF3F51B5), // Indigo
        Color(0xFF009688)  // Teal
    )
    
    return colorPalette[index % colorPalette.size]
}