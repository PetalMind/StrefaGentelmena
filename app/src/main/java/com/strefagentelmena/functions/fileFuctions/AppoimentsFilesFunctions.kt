package com.strefagentelmena.functions.fileFuctions

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.strefagentelmena.models.AppoimentsModel.Appointment
import java.io.File
import java.io.FileReader
import java.io.FileWriter

val filesFunctionsAppoiments = FilesFunctionsAppoiments()

class FilesFunctionsAppoiments {
    fun loadAppointmentFromFile(
        context: Context,
    ): List<Appointment> {
        val file = File(context.filesDir, "appointment.json")

        return if (file.exists()) {
            FileReader(file).use {
                val type = object : TypeToken<List<Appointment>>() {}.type
                val loadedAppointments: List<Appointment> = Gson().fromJson(it, type)

                loadedAppointments
            }
        } else {
            emptyList()
        }
    }

    fun saveAppointmentToFile(context: Context, appointments: List<Appointment?>) {
        val gson = Gson()
        val jsonString = gson.toJson(appointments)
        val file = File(context.filesDir, "appointment.json")

        FileWriter(file).use {
            it.write(jsonString)
        }
    }

}
