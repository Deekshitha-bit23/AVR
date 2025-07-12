package com.deeksha.avr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.deeksha.avr.navigation.AppNavHost
import com.deeksha.avr.ui.theme.AvrTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AvrTheme {
                val navController = rememberNavController()
                AppNavHost(navController = navController)
            }
        }
    }
}