package com.strefagentelmena.functions.fileFuctions

import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import com.strefagentelmena.functions.fireBase.storageFireBase
import com.strefagentelmena.models.settngsModel.Backup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val backupFilesFunctions = BackupFilesFunctions()

class BackupFilesFunctions {
    fun createBackupFile(context: Context): Boolean {
        val filename = "backup.json"
        val directory = Environment.getExternalStorageDirectory()
        val file = File(directory, filename)

        // Load data from other files
        val customersList = fileFunctionsClients.loadCustomersFromFile(context)
        val appoimentsList = filesFunctionsAppoiments.loadAppointmentFromFile(context)
        val preferences = fileFunctionsSettings.loadSettingsFromFile(context)

        // Create Backup instance
        val backupFile = Backup(
            customersCopy = customersList,
            appoimentsCopy = appoimentsList,
            profileCopy = preferences
        )

        // Serialize the Backup instance to JSON


        val jsonString = ownGson.toJson(backupFile)
        Log.e("jsonString", jsonString)
        // Write JSON to the file
        val fileWriter = FileWriter(file)
        fileWriter.write(jsonString)

        fileWriter.close()

        return true
    }

    fun createAndUploadBackupFile(context: Context): Boolean {
        val sdf = SimpleDateFormat("dd.MM.yy, HH:mm", Locale.getDefault())
        val currentDateandTime = sdf.format(Date())
        val filename = "backup.json"
        val directory = Environment.getExternalStorageDirectory()
        val file = File(directory, filename)

        // Load data from other files
        val customersList = fileFunctionsClients.loadCustomersFromFile(context)
        val appoimentsList = filesFunctionsAppoiments.loadAppointmentFromFile(context)
        val preferences = fileFunctionsSettings.loadSettingsFromFile(context)

        // Create Backup instance
        val backupFile = Backup(
            customersCopy = customersList,
            appoimentsCopy = appoimentsList,
            profileCopy = preferences
        )

        // Serialize the Backup instance to JSON
        val jsonString = ownGson.toJson(backupFile)
        Log.e("jsonString", jsonString)
        // Write JSON to the file
        val fileWriter = FileWriter(file)
        fileWriter.write(jsonString)

        fileWriter.close()

        // Upload the backup file to Firebase Storage
        if (preferences.backupSettings.hasOnlineCopy) {
            storageFireBase.uploadBackupJson(file.absolutePath)
        }

        return true
    }



    fun readBackupFile(context: Context): Boolean {
        val directory = Environment.getExternalStorageDirectory()
        val file = File(directory, "backup.json")

        if (file.exists()) {
            val fileReader = FileReader(file)

            val backupType = object : TypeToken<Backup>() {}.type
            val backup = ownGson.fromJson<Backup>(fileReader, backupType)

            fileReader.close()

            // Extract data from the Backup instance
            val preferences = backup.profileCopy
            val customersList = backup.customersCopy
            val appointmentsList = backup.appoimentsCopy

            //save no empty lists
            if (customersList.isNotEmpty()) {
                fileFunctionsClients.saveCustomersToFile(context, customersList)
            }
            if (appointmentsList.isNotEmpty()) {
                filesFunctionsAppoiments.saveAppointmentToFile(context, appointmentsList)
            }

            if (!preferences.backupSettings.isBackupCreated) {
                fileFunctionsSettings.saveSettingsToFile(context, preferences)
            }

        }

        return file.exists()
    }
    fun readBackupFileFromFile(context: Context, file: File): Boolean {
        var success = false
        if (file.exists()) {
            val fileReader = FileReader(file)

            val backupType = object : TypeToken<Backup>() {}.type
            val backup = ownGson.fromJson<Backup>(fileReader, backupType)

            fileReader.close()

            // Extract data from the Backup instance
            val preferences = backup.profileCopy
            val customersList = backup.customersCopy
            val appointmentsList = backup.appoimentsCopy

            //save no empty lists
            if (customersList.isNotEmpty()) {
                fileFunctionsClients.saveCustomersToFile(context, customersList)
            }
            if (appointmentsList.isNotEmpty()) {
                filesFunctionsAppoiments.saveAppointmentToFile(context, appointmentsList)
            }

            if (!preferences.backupSettings.isBackupCreated) {
                fileFunctionsSettings.saveSettingsToFile(context, preferences)
            }
            success = true
        }
        return success
    }

    suspend fun fetchAndReadFileFromFirebase(fileName: String, context: Context): Boolean {
        var success = false
        val localFile = storageFireBase.getFileFromFirebase(fileName)
        if (localFile != null) {
            success = readBackupFileFromFile(context, localFile)
        }
        return success
    }
}
