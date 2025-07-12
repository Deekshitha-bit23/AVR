package com.deeksha.avr.model

import com.google.firebase.firestore.PropertyName
import com.google.firebase.Timestamp

data class Expense(
    @PropertyName("id") val id: String = "",
    @PropertyName("projectId") val projectId: String = "",
    @PropertyName("userId") val userId: String = "",
    @PropertyName("userName") val userName: String = "",
    @PropertyName("date") val date: Timestamp? = null,
    @PropertyName("amount") val amount: Double = 0.0,
    @PropertyName("department") val department: String = "",
    @PropertyName("category") val category: String = "",
    @PropertyName("description") val description: String = "",
    @PropertyName("modeOfPayment") val modeOfPayment: String = "", // "cash", "upi", "check"
    @PropertyName("tds") val tds: Double = 0.0,
    @PropertyName("gst") val gst: Double = 0.0,
    @PropertyName("netAmount") val netAmount: Double = 0.0,
    @PropertyName("attachmentUrl") val attachmentUrl: String = "",
    @PropertyName("attachmentFileName") val attachmentFileName: String = "",
    @PropertyName("status") val status: ExpenseStatus = ExpenseStatus.PENDING,
    @PropertyName("submittedAt") val submittedAt: Timestamp? = null,
    @PropertyName("reviewedAt") val reviewedAt: Timestamp? = null,
    @PropertyName("reviewedBy") val reviewedBy: String = "",
    @PropertyName("reviewComments") val reviewComments: String = "",
    @PropertyName("receiptNumber") val receiptNumber: String = ""
)

enum class ExpenseStatus {
    PENDING,
    APPROVED,
    REJECTED,
    DRAFT
}

// Note: Summary data classes moved to com.deeksha.avr.model.Summary
// Import from Summary.kt: ExpenseSummary, ExpenseFormData, StatusCounts 