package com.strefagentelmena.functions

import android.content.Context
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.strefagentelmena.enums.AppState
import com.strefagentelmena.models.Appointment
import java.io.File
import java.io.FileReader

val filesFunctions = FilesFunctions()

class FilesFunctions {
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

}
