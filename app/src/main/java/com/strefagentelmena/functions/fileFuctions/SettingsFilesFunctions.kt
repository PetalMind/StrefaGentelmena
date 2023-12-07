package com.strefagentelmena.functions.fileFuctions

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.SettngsModel.Preferences
import java.io.File
import java.io.FileReader
import java.io.FileWriter

val fileFunctionsSettings = SettingsFilesFunctions()

class SettingsFilesFunctions {
    fun loadSettingsFromFile(context: Context): Preferences {
        val file = File(context.filesDir, "preferences.json")

        val loadedPreferences: Preferences = if (file.exists()) {
            FileReader(file).use {
                val type = object : TypeToken<Preferences>() {}.type
                Gson().fromJson(it, type)
            }
        } else {
            Preferences()
        }

        return loadedPreferences
    }

    fun saveSettingsToFile(context: Context, preferences: Preferences) {
        val gson = Gson()
        val jsonString = gson.toJson(preferences)
        val file = File(context.filesDir, "preferences.json")

        FileWriter(file).use {
            it.write(jsonString)
        }
    }
}