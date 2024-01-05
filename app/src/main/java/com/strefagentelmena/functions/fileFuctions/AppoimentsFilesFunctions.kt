package com.strefagentelmena.functions.fileFuctions

import android.content.Context
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

val ownGson: Gson = GsonBuilder()
    .registerTypeAdapter(LocalDate::class.java, LocalDateAdapter())
    .registerTypeAdapter(LocalTime::class.java, LocalTimeAdapter())
    .create()

val filesFunctionsAppoiments = FilesFunctionsAppoiments()

class FilesFunctionsAppoiments {
    fun loadAppointmentFromFile(context: Context): List<Appointment> {
        val file = File(context.filesDir, "appointment.json")

        return if (file.exists()) {
            FileReader(file).use {
                val type = object : TypeToken<List<Appointment>>() {}.type
                val loadedAppointments: List<Appointment> = ownGson.fromJson(it, type)

                loadedAppointments
            }
        } else {
            emptyList()
        }
    }

    fun saveAppointmentToFile(context: Context, appointments: List<Appointment?>) {

        val jsonString = ownGson.toJson(appointments)
        val file = File(context.filesDir, "appointment.json")

        FileWriter(file).use {
            it.write(jsonString)
        }
    }

}
class LocalDateAdapter : JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
    override fun serialize(src: LocalDate, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): LocalDate {
        return LocalDate.parse(json.asString, DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    }
}

class LocalTimeAdapter : JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
    override fun serialize(
        src: LocalTime?,
        typeOfSrc: Type?,
        context: JsonSerializationContext?
    ): JsonElement {
        return JsonPrimitive(src?.format(DateTimeFormatter.ofPattern("HH:mm")))
    }

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): LocalTime {
        return LocalTime.parse(json?.asString, DateTimeFormatter.ofPattern("HH:mm"))
    }
}