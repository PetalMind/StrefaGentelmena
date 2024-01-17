package com.strefagentelmena.functions.fireBase

import android.util.Log
import androidx.core.net.toUri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ListResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val storageFireBase = StorageFireBase()

class StorageFireBase {
    fun uploadBackupJson(filePath: String) {
        val storageRef = FirebaseStorage.getInstance().reference
        val file = File(filePath)
        val sdf = SimpleDateFormat("dd.MM.yy, HH:mm", Locale.getDefault())
        val currentDateandTime = sdf.format(Date())
        val filename = "backup-$currentDateandTime.json"

        val uploadTask =
            storageRef.child("Kopia_StrefaGentlemana/${filename}").putFile(file.toUri())

        uploadTask.addOnFailureListener { exception ->
            // Handle unsuccessful uploads
            Log.e("StorageFireBase", exception.toString())
        }.addOnSuccessListener { taskSnapshot ->
            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
            Log.e("StorageFireBase", "Upload successful")
        }
    }

    suspend fun listFilesInDirectory(directoryPath: String): MutableList<String> = coroutineScope {
        val storageRef = FirebaseStorage.getInstance().reference
        val directoryRef = storageRef.child(directoryPath)

        val deferred = CompletableDeferred<MutableList<String>>()

        directoryRef.listAll()
            .addOnSuccessListener { listResult: ListResult ->
                val filesList = mutableListOf<String>()
                for (item in listResult.items) {
                    filesList.add(item.name)
                }
                deferred.complete(filesList)
            }
            .addOnFailureListener { exception ->
                deferred.completeExceptionally(exception)
            }
        deferred.await()
    }

    suspend fun getFileFromFirebase(fileName: String): File? = coroutineScope {
        val storageRef = FirebaseStorage.getInstance().reference
        val fileRef = storageRef.child("Kopia_StrefaGentlemana/$fileName")

        val localFile = File.createTempFile("temp", null)

        try {
            val taskSnapshot = fileRef.getFile(localFile).await()

            if (taskSnapshot.error != null) {
                throw Exception("Failed to download file.", taskSnapshot.error)
            }


            return@coroutineScope localFile
        } catch (e: Exception) {
            println("Error downloading file: ${e.message}")
            return@coroutineScope null
        }
    }

}