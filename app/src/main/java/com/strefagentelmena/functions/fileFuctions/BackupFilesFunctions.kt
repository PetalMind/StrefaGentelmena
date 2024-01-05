package com.strefagentelmena.functions.fileFuctions

import android.content.Context
import android.os.Environment
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import com.strefagentelmena.models.appoimentsModel.Appointment
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.time.LocalTime
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.google.gson.JsonDeserializationContext
import com.strefagentelmena.models.settngsModel.Backup

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

            if (!preferences.backupPreferences.isBackupCreated) {
                fileFunctionsSettings.saveSettingsToFile(context, preferences)
            }
        }

        return file.exists()
    }
}
