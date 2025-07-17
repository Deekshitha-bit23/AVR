package com.deeksha.avr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.deeksha.avr.navigation.AppNavHost
import com.deeksha.avr.service.FCMService
import com.deeksha.avr.ui.theme.AvrTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize notification channel
        FCMService.createNotificationChannel(this)
        
        // Handle notification navigation
        handleNotificationNavigation()
        
        enableEdgeToEdge()
        setContent {
            AvrTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
    
    private fun handleNotificationNavigation() {
        // Handle navigation from notification tap
        intent?.let { intent ->
            val projectId = intent.getStringExtra("projectId")
            val expenseId = intent.getStringExtra("expenseId")
            val notificationType = intent.getStringExtra("notificationType")
            
            if (projectId != null) {
                // TODO: Navigate to appropriate screen based on notification type
                // This will be handled by the navigation system
                android.util.Log.d("MainActivity", "📱 Notification navigation: projectId=$projectId, expenseId=$expenseId, type=$notificationType")
            }
        }
    }
}