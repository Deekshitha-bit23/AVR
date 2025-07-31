package com.deeksha.avr.model

import com.google.firebase.firestore.PropertyName

data class User(
    @PropertyName("uid") val uid: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("email") val email: String = "",
    @PropertyName("phone") val phone: String = "",
    @PropertyName("role") val role: UserRole = UserRole.USER,
    @PropertyName("createdAt") val createdAt: Long = System.currentTimeMillis(),
    @PropertyName("isActive") val isActive: Boolean = true,
    @PropertyName("assignedProjects") val assignedProjects: List<String> = emptyList(),
    @PropertyName("deviceInfo") val deviceInfo: DeviceInfo = DeviceInfo(),
    @PropertyName("notificationPreferences") val notificationPreferences: NotificationPreferences = NotificationPreferences()
)

enum class UserRole {
    USER,
    APPROVER, 
    ADMIN,
    PRODUCTION_HEAD
} 