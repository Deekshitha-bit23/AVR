package com.deeksha.avr.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.Timestamp

object FormatUtils {
    
    // Currency formatting - replaces all formatCurrency duplicates
    fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
    }
    
    // Simple currency formatting for exports
    fun formatCurrencySimple(amount: Double): String {
        return "₹${String.format("%.2f", amount)}"
    }
    
    // Date formatting utilities
    fun formatDate(timestamp: Timestamp?, pattern: String = "dd/MM/yyyy"): String {
        return timestamp?.let {
            SimpleDateFormat(pattern, Locale.getDefault()).format(it.toDate())
        } ?: "N/A"
    }
    
    fun formatDateShort(timestamp: Timestamp?): String {
        return formatDate(timestamp, "dd/MM")
    }
    
    fun formatDateLong(timestamp: Timestamp?): String {
        return formatDate(timestamp, "dd MMM yyyy")
    }
    
    fun formatDateTime(timestamp: Timestamp?): String {
        return formatDate(timestamp, "dd/MM/yyyy HH:mm")
    }
    
    // Current timestamp for file naming
    fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
    
    // Calculate days between dates
    fun calculateDaysLeft(endDate: Long): Long {
        val currentTime = System.currentTimeMillis()
        val diffInMillis = endDate - currentTime
        return diffInMillis / (1000 * 60 * 60 * 24)
    }
    
    // Project initials generator
    fun getProjectInitials(projectName: String): String {
        return projectName
            .split(" ")
            .take(2)
            .map { it.firstOrNull()?.uppercaseChar() ?: "" }
            .joinToString("")
            .ifEmpty { projectName.take(2).uppercase() }
    }
    
    // Format time ago for notifications
    fun formatTimeAgo(timestamp: Timestamp?): String {
        if (timestamp == null) return "Unknown"
        
        val now = System.currentTimeMillis()
        val timestampMillis = timestamp.toDate().time
        val diffInMillis = now - timestampMillis
        
        return when {
            diffInMillis < 60_000 -> "Just now"
            diffInMillis < 3_600_000 -> "${diffInMillis / 60_000}m ago"
            diffInMillis < 86_400_000 -> "${diffInMillis / 3_600_000}h ago"
            diffInMillis < 604_800_000 -> "${diffInMillis / 86_400_000}d ago"
            else -> formatDateShort(timestamp)
        }
    }
} 