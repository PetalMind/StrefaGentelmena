package com.strefagentelmena.functions.fileFuctions

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import java.io.File
import java.io.FileReader
import java.io.FileWriter

val fileFunctionsSettings = SettingsFilesFunctions()

class SettingsFilesFunctions {
    fun loadSettingsFromFile(context: Context): ProfilePreferences {
        val file = File(context.filesDir, "preferences.json")

        val loadedPreferences: ProfilePreferences = if (file.exists()) {
            FileReader(file).use {
                val type = object : TypeToken<ProfilePreferences>() {}.type
                Gson().fromJson(it, type)
            }
        } else {
            ProfilePreferences()
        }

        return loadedPreferences
    }

    fun saveSettingsToFile(context: Context, preferences: ProfilePreferences) {
        val gson = Gson()
        val jsonString = gson.toJson(preferences)
        val file = File(context.filesDir, "preferences.json")

        FileWriter(file).use {
            it.write(jsonString)
        }
    }
}