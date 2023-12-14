package com.strefagentelmena.functions.fileFuctions

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.strefagentelmena.models.AppoimentsModel.Appointment
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.settngsModel.Backup
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.prefs.Preferences
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

val backupFilesFunctions = BackupFilesFunctions()

class BackupFilesFunctions {
    //crete backup file to store data
    fun createBackupFile(context: Context) {
        val file = File(context.filesDir, "backup.json")

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
        val gson = Gson()
        val jsonString = gson.toJson(backupFile)

        // Write JSON to the file
        val fileWriter = FileWriter(file)
        fileWriter.write(jsonString)
        fileWriter.close()
    }

    fun readBackupFile(context: Context) {
        val file = File(context.filesDir, "backup.json")
        val fileReader = FileReader(file)
        val gson = Gson()

        val backupType = object : TypeToken<Backup>() {}.type
        val backup = gson.fromJson<Backup>(fileReader, backupType)

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

        if (!preferences.backupPreferences.isBackupCreated) {
            fileFunctionsSettings.saveSettingsToFile(context, preferences)
        }
    }

    fun unzipFile(zipFilePath: String, destinationDir: String) {
        val zipFile = ZipFile(zipFilePath)

        zipFile.entries().asSequence().forEach { entry ->
            val outputFilePath = "$destinationDir/${entry.name}"
            val outputFile = File(outputFilePath)

            // If the entry is a directory, create it
            if (entry.isDirectory) {
                outputFile.mkdirs()
            } else {
                // If the entry is a file, write it to the output directory
                zipFile.getInputStream(entry).use { input ->
                    outputFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }

    fun zipFiles(files: File) {
        val buffer = ByteArray(1024)

        // Get the current date and format it as "dd.mm.yyyy"
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        // Create a ZipOutputStream for the zip file
        val zipOutputStream = ZipOutputStream(FileOutputStream("backup_$currentDate.zip"))

        files.listFiles()?.forEach { file ->
            val fileInputStream = FileInputStream(file)
            val zipEntry = ZipEntry(file.name)
            zipOutputStream.putNextEntry(zipEntry)

            var len: Int
            while (fileInputStream.read(buffer).also { len = it } > 0) {
                zipOutputStream.write(buffer, 0, len)
            }

            fileInputStream.close()
        }

        zipOutputStream.close()
    }
}