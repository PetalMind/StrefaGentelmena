package com.strefagentelmena.functions.fireBase

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.strefagentelmena.models.Customer
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun getAllCustomersFromFirebase(database: FirebaseDatabase): List<Customer> {
    val customersRef = database.getReference("Customers")
    return suspendCancellableCoroutine { continuation ->
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val customersList =
                        snapshot.children.mapNotNull { it.getValue(Customer::class.java) }
                    continuation.resume(customersList) // Zwraca listę klientów
                } catch (e: Exception) {
                    continuation.resumeWithException(e) // Obsługa błędów
                }
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resumeWithException(error.toException())
            }
        }

        // Dodanie listenera
        customersRef.addListenerForSingleValueEvent(listener)

        // Anulowanie w przypadku anulowania coroutine
        continuation.invokeOnCancellation {
            customersRef.removeEventListener(listener)
        }
    }
}

fun addNewCustomerToFirebase(
    database: FirebaseDatabase,
    newCustomer: Customer,
    completion: (Boolean) -> Unit
) {
    val customersRef = database.getReference("Customers")

    // Upewnij się, że klient ma ID
    val customerId = newCustomer.id

    // Używamy własnego ID jako klucza
    customersRef.child(customerId.toString()).setValue(newCustomer)
        .addOnSuccessListener {
            Log.e("Firebase", "Customer added successfully with ID: $customerId")
            completion(true) // Dodano klienta pomyślnie
        }
        .addOnFailureListener { exception ->
            Log.e("Firebase", "Error adding customer: ${exception.message}")
            completion(false) // Błąd dodawania klienta
        }
}


fun editCustomerInFirebase(
    database: FirebaseDatabase,
    updatedCustomer: Customer,
    completion: (Boolean) -> Unit
) {
    val customersRef = database.getReference("Customers")
    val customerRef =
        customersRef.child(updatedCustomer.id.toString()) // Get the reference to the specific customer

    customerRef.setValue(updatedCustomer)
        .addOnSuccessListener {
            completion(true) // Customer updated sucfcessfully
            Log.e("Firebase", "Customer updated successfully")
        }
        .addOnFailureListener { exception ->
            Log.e("Firebase", "Error updating customer: ${exception.message}")
            completion(false) // Error updating customer
        }
}

fun removeCustomerFromFirebase(
    database: FirebaseDatabase,
    customerId: String,
    completion: (Boolean) -> Unit
) {
    if (customerId.isEmpty()) {
        Log.e("Firebase", "Customer ID is empty. Cannot remove customer.")
        return completion(false)
    }

    val customersRef = database.getReference("Customers")

    // Usuwanie danych klienta
    customersRef.child(customerId).removeValue()
        .addOnSuccessListener {
            Log.i("Firebase", "Customer removed successfully with ID: $customerId")
            completion(true)
        }
        .addOnFailureListener { exception ->
            Log.e("Firebase", "Error removing customer: ${exception.message}")
            completion(false)
        }
}
