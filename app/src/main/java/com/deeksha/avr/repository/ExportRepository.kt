package com.deeksha.avr.repository

import android.content.Context
import android.content.Intent
import android.os.Environment
import androidx.core.content.FileProvider
import com.deeksha.avr.model.ExportData
import com.deeksha.avr.model.DetailedExpense
import com.deeksha.avr.utils.FormatUtils
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportRepository @Inject constructor(
    private val context: Context
) {
    
    suspend fun exportToPDF(exportData: ExportData): Result<File> {
        return try {
            val fileName = "expense_report_${getCurrentTimestamp()}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            val writer = PdfWriter(file)
            val pdf = PdfDocument(writer)
            val document = Document(pdf)
            
            // Title
            document.add(
                Paragraph("Expense Report")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(20f)
                    .setBold()
            )
            
            // Report Info
            document.add(
                Paragraph("Generated on: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(12f)
            )
            
            document.add(
                Paragraph("Time Range: ${exportData.timeRange}")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(12f)
            )
            
            document.add(
                Paragraph("Department: ${exportData.department}")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(12f)
            )
            
            // Total Amount
            document.add(
                Paragraph("Total Amount: ${FormatUtils.formatCurrencySimple(exportData.totalSpent)}")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(16f)
                    .setBold()
            )
            
            // Category Breakdown
            if (exportData.categoryBreakdown.isNotEmpty()) {
                document.add(Paragraph("Category Breakdown").setBold().setFontSize(14f))
                val categoryTable = Table(2)
                categoryTable.addCell("Category")
                categoryTable.addCell("Amount")
                
                exportData.categoryBreakdown.forEach { (category, amount) ->
                    categoryTable.addCell(category)
                    categoryTable.addCell(FormatUtils.formatCurrencySimple(amount))
                }
                
                document.add(categoryTable)
            }
            
            // Detailed Expenses
            if (exportData.detailedExpenses.isNotEmpty()) {
                document.add(Paragraph("Detailed Expenses").setBold().setFontSize(14f))
                val expenseTable = Table(5)
                expenseTable.addCell("Date")
                expenseTable.addCell("Invoice")
                expenseTable.addCell("By")
                expenseTable.addCell("Amount")
                expenseTable.addCell("Department")
                
                exportData.detailedExpenses.forEach { expense ->
                    expenseTable.addCell(expense.date?.toDate()?.let { 
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it) 
                    } ?: "N/A")
                    expenseTable.addCell(expense.invoice)
                    expenseTable.addCell(expense.by)
                    expenseTable.addCell(FormatUtils.formatCurrencySimple(expense.amount))
                    expenseTable.addCell(expense.department)
                }
                
                document.add(expenseTable)
            }
            
            document.close()
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun exportToCSV(exportData: ExportData): Result<File> {
        return try {
            val fileName = "expense_report_${getCurrentTimestamp()}.csv"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            
            val writer = BufferedWriter(FileWriter(file))
            
            // Header information
            writer.write("Expense Report\n")
            writer.write("Generated on: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}\n")
            writer.write("Time Range: ${exportData.timeRange}\n")
            writer.write("Department: ${exportData.department}\n")
            writer.write("Total Amount: ${FormatUtils.formatCurrencySimple(exportData.totalSpent)}\n")
            writer.write("\n")
            
            // Category Breakdown
            if (exportData.categoryBreakdown.isNotEmpty()) {
                writer.write("Category Breakdown\n")
                writer.write("Category,Amount\n")
                
                exportData.categoryBreakdown.forEach { (category, amount) ->
                    writer.write("$category,${FormatUtils.formatCurrencySimple(amount)}\n")
                }
                writer.write("\n")
            }
            
            // Detailed Expenses
            if (exportData.detailedExpenses.isNotEmpty()) {
                writer.write("Detailed Expenses\n")
                writer.write("Date,Invoice,By,Amount,Department,Mode of Payment\n")
                
                exportData.detailedExpenses.forEach { expense ->
                    val dateStr = expense.date?.toDate()?.let { 
                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it) 
                    } ?: "N/A"
                    writer.write("$dateStr,${expense.invoice},${expense.by},${FormatUtils.formatCurrencySimple(expense.amount)},${expense.department},${expense.modeOfPayment}\n")
                }
            }
            
            writer.close()
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun getCurrentTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    }
    
    fun shareFile(file: File, mimeType: String): Intent? {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Expense Report")
                putExtra(Intent.EXTRA_TEXT, "Please find the attached expense report.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            Intent.createChooser(shareIntent, "Share Report")
        } catch (e: Exception) {
            null
        }
    }
} 