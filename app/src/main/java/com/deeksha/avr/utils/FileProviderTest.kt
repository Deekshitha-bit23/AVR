package com.deeksha.avr.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object FileProviderTest {
    
    fun testFileProvider(context: Context): String {
        return try {
            // Test cache directory
            val cacheFile = File(context.cacheDir, "test_cache.txt")
            cacheFile.createNewFile()
            
            android.util.Log.d("FileProviderTest", "Testing cache file: ${cacheFile.absolutePath}")
            android.util.Log.d("FileProviderTest", "Cache directory: ${context.cacheDir.absolutePath}")
            android.util.Log.d("FileProviderTest", "File exists: ${cacheFile.exists()}")
            android.util.Log.d("FileProviderTest", "File can read: ${cacheFile.canRead()}")
            
            val cacheUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )
            
            android.util.Log.d("FileProviderTest", "Cache test successful: $cacheUri")
            android.util.Log.d("FileProviderTest", "URI scheme: ${cacheUri.scheme}")
            android.util.Log.d("FileProviderTest", "URI path: ${cacheUri.path}")
            
            cacheFile.delete()
            
            "Cache directory: SUCCESS - $cacheUri"
        } catch (e: Exception) {
            android.util.Log.e("FileProviderTest", "Cache test failed: ${e.message}", e)
            android.util.Log.e("FileProviderTest", "Exception type: ${e.javaClass.simpleName}")
            android.util.Log.e("FileProviderTest", "Stack trace: ${e.stackTrace.joinToString("\n")}")
            "Cache directory: FAILED - ${e.message}"
        }
    }
    
    fun testExternalFiles(context: Context): String {
        return try {
            val externalFilesDir = context.getExternalFilesDir(null)
            if (externalFilesDir != null) {
                val externalFile = File(externalFilesDir, "test_external.txt")
                externalFile.createNewFile()
                
                val externalUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    externalFile
                )
                
                android.util.Log.d("FileProviderTest", "External files test successful: $externalUri")
                externalFile.delete()
                
                "External files: SUCCESS - $externalUri"
            } else {
                "External files: NOT AVAILABLE"
            }
        } catch (e: Exception) {
            android.util.Log.e("FileProviderTest", "External files test failed: ${e.message}", e)
            "External files: FAILED - ${e.message}"
        }
    }
}