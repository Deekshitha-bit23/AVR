package com.deeksha.avr.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.InputStream

object ImageUriHelper {
    
    /**
     * Creates a proper FileProvider URI for camera images
     */
    fun createCameraImageUri(context: Context, fileName: String): Uri? {
        return try {
            // Use cache directory as primary method (more reliable for FileProvider)
            val tempFile = File(context.cacheDir, fileName)
            
            android.util.Log.d("ImageUriHelper", "Creating camera image URI for file: ${tempFile.absolutePath}")
            android.util.Log.d("ImageUriHelper", "Cache directory: ${context.cacheDir.absolutePath}")
            android.util.Log.d("ImageUriHelper", "Cache directory exists: ${context.cacheDir.exists()}")
            android.util.Log.d("ImageUriHelper", "Cache directory can write: ${context.cacheDir.canWrite()}")
            
            // Ensure the file is created
            if (!tempFile.exists()) {
                val created = tempFile.createNewFile()
                android.util.Log.d("ImageUriHelper", "File created: $created")
            }
            
            android.util.Log.d("ImageUriHelper", "File exists: ${tempFile.exists()}")
            android.util.Log.d("ImageUriHelper", "File can read: ${tempFile.canRead()}")
            android.util.Log.d("ImageUriHelper", "File can write: ${tempFile.canWrite()}")
            
            // Create FileProvider URI
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            
            android.util.Log.d("ImageUriHelper", "FileProvider URI created: $uri")
            android.util.Log.d("ImageUriHelper", "URI scheme: ${uri.scheme}")
            android.util.Log.d("ImageUriHelper", "URI path: ${uri.path}")
            
            uri
        } catch (e: Exception) {
            android.util.Log.e("ImageUriHelper", "Error creating camera image URI: ${e.message}", e)
            android.util.Log.e("ImageUriHelper", "Exception type: ${e.javaClass.simpleName}")
            android.util.Log.e("ImageUriHelper", "Stack trace: ${e.stackTrace.joinToString("\n")}")
            null
        }
    }
    
    /**
     * Tests if a URI is accessible for reading
     */
    fun isUriAccessible(context: Context, uri: Uri): Boolean {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val accessible = inputStream != null
            inputStream?.close()
            accessible
        } catch (e: Exception) {
            android.util.Log.e("ImageUriHelper", "Error testing URI accessibility: ${e.message}", e)
            false
        }
    }
    
    /**
     * Gets a readable file from URI if possible
     */
    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            when (uri.scheme) {
                "file" -> File(uri.path ?: "")
                "content" -> {
                    // For content URIs, we can't get the file directly
                    // but we can test if it's accessible
                    if (isUriAccessible(context, uri)) {
                        // Create a temporary file and copy the content
                        val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            tempFile.outputStream().use { outputStream ->
                                inputStream.copyTo(outputStream)
                            }
                        }
                        tempFile
                    } else {
                        null
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageUriHelper", "Error getting file from URI: ${e.message}", e)
            null
        }
    }
}
