package com.deeksha.avr.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.Timestamp

data class Project(
    @PropertyName("id") val id: String = "",
    @PropertyName("name") val name: String = "",
    @PropertyName("description") val description: String = "",
    @PropertyName("budget") val budget: Double = 0.0,
    @PropertyName("spent") val spent: Double = 0.0,
    @PropertyName("startDate") val startDate: Timestamp? = null,
    @PropertyName("endDate") val endDate: Timestamp? = null,
    @PropertyName("status") val status: String = "ACTIVE",
    @PropertyName("managerId") val managerId: String = "",
    @PropertyName("approverIds") val approverIds: List<String> = emptyList(),
    @PropertyName("productionHeadIds") val productionHeadIds: List<String> = emptyList(),
    @PropertyName("teamMembers") val teamMembers: List<String> = emptyList(),
    @PropertyName("createdAt") val createdAt: Timestamp? = null,
    @PropertyName("updatedAt") val updatedAt: Timestamp? = null,
    @PropertyName("code") val code: String = "", // Project code like "MO", "DO", etc.
    @PropertyName("departmentBudgets") val departmentBudgets: Map<String, Double> = emptyMap(),
    @PropertyName("categories") val categories: List<String> = emptyList(),
    @PropertyName("temporaryApproverPhone") val temporaryApproverPhone: String = ""
)

data class DepartmentBudget(
    val departmentName: String = "",
    val allocatedBudget: Double = 0.0
) 