package com.strefagentelmena.functions.fileFuctions

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.strefagentelmena.models.Customer
import java.io.File
import java.io.FileReader
import java.io.FileWriter

val fileFunctionsClients = ClientsFilesFuctions()
class ClientsFilesFuctions {
    fun loadCustomersFromFile(context: Context): List<Customer> {
        val file = File(context.filesDir, "customers.json")

        val loadedCustomers: List<Customer> = if (file.exists()) {
            FileReader(file).use {
                val type = object : TypeToken<List<Customer>>() {}.type
                Gson().fromJson(it, type)
            }
        } else {
            emptyList()
        }

        return loadedCustomers
    }
    fun saveCustomersToFile(context: Context, customers: List<Customer>) {
        val gson = Gson()
        val jsonString = gson.toJson(customers)
        val file = File(context.filesDir, "customers.json")

        FileWriter(file).use {
            it.write(jsonString)
        }
    }
}