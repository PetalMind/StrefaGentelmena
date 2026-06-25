package com.strefagentelmena.functions.fireBase

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import kotlin.math.max
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.computeCustomerVisitAggregates
import com.strefagentelmena.models.normalizedAfterFirebaseLoad
import com.strefagentelmena.models.trimmedForCustomerDocument
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
                        snapshot.children.mapNotNull {
                            it.getValue(Customer::class.java)
                                ?.takeUnless(Customer::deleted)
                                ?.normalizedAfterFirebaseLoad()
                        }
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

/**
 * Atomowo przydziela kolejne ID klienta (Realtime Database).
 * Wymaga zapisu pod [META_LAST_CUSTOMER_ID_PATH] w regułach bazy.
 */
private const val META_LAST_CUSTOMER_ID_PATH = "StrefaMeta/lastCustomerId"

fun allocateNextCustomerId(
    database: FirebaseDatabase,
    localMaxIdHint: Int,
    completion: (Int?) -> Unit,
) {
    val ref = database.getReference(META_LAST_CUSTOMER_ID_PATH)
    ref.runTransaction(object : Transaction.Handler {
        override fun doTransaction(currentData: MutableData): Transaction.Result {
            val last = currentData.getValue(Int::class.java) ?: 0
            val newId = max(last, localMaxIdHint) + 1
            currentData.value = newId
            return Transaction.success(currentData)
        }

        override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
            if (error != null) {
                Log.e("Firebase", "allocateNextCustomerId: ${error.message}")
                completion(null)
                return
            }
            if (!committed) {
                completion(null)
                return
            }
            val id = currentData?.getValue(Int::class.java)
            completion(id)
        }
    })
}

fun addNewCustomerToFirebase(
    database: FirebaseDatabase,
    newCustomer: Customer,
    completion: (Boolean) -> Unit
) {
    val customersRef = database.getReference("Customers")

    val customerId = newCustomer.id
    if (customerId <= 0) {
        Log.e("Firebase", "Invalid customer id: $customerId")
        completion(false)
        return
    }

    customersRef.child(customerId.toString()).setValue(newCustomer.trimmedForCustomerDocument())
        .addOnSuccessListener {
            Log.i("Firebase", "Customer added successfully with ID: $customerId")
            completion(true)
        }
        .addOnFailureListener { exception ->
            Log.e("Firebase", "Error adding customer: ${exception.message}")
            completion(false)
        }
}


fun editCustomerInFirebase(
    database: FirebaseDatabase,
    updatedCustomer: Customer,
    completion: (Boolean) -> Unit
) {
    val customersRef = database.getReference("Customers")
    if (updatedCustomer.id <= 0) {
        Log.e("Firebase", "Invalid customer id for update: ${updatedCustomer.id}")
        completion(false)
        return
    }

    val customerRef = customersRef.child(updatedCustomer.id.toString())

    customerRef.setValue(updatedCustomer.trimmedForCustomerDocument())
        .addOnSuccessListener {
            completion(true)
            Log.i("Firebase", "Customer updated successfully")
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

    customersRef.child(customerId).child("deleted").setValue(true)
        .addOnSuccessListener {
            Log.i("Firebase", "Customer removed successfully with ID: $customerId")
            completion(true)
        }
        .addOnFailureListener { exception ->
            Log.e("Firebase", "Error removing customer: ${exception.message}")
            completion(false)
        }
}

/**
 * Jedna operacja: oznacza klienta i powiązane wizyty jako usunięte.
 */
fun removeCustomerAndTheirAppointmentsFromFirebase(
    database: FirebaseDatabase,
    customerId: Int,
    appointmentIds: List<Int>,
    childCustomerIdsToOrphan: List<Int> = emptyList(),
    completion: (Boolean) -> Unit,
) {
    if (customerId <= 0) {
        completion(false)
        return
    }
    val updates = hashMapOf<String, Any?>()
    for (childId in childCustomerIdsToOrphan) {
        if (childId > 0) updates["Customers/$childId/parentCustomerId"] = 0
    }
    for (id in appointmentIds) {
        if (id > 0) updates["Appointments/$id/deleted"] = true
    }
    updates["Customers/$customerId/deleted"] = true
    database.reference.updateChildren(updates)
        .addOnSuccessListener {
            Log.i("Firebase", "Customer $customerId and ${appointmentIds.size} appointment(s) removed")
            completion(true)
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Error removing customer/appointments: ${e.message}")
            completion(false)
        }
}

/**
 * Denormalizacja pól wizytowych u klienta z pełnej listy [Appointments] (ścieżki względem roota).
 * Używaj po dodaniu / edycji / usunięciu wizyty.
 */
fun patchCustomerDerivedVisitFields(
    database: FirebaseDatabase,
    customerId: Int,
    allAppointments: List<Appointment>,
    completion: ((Boolean) -> Unit)? = null,
) {
    if (customerId == 0) {
        completion?.invoke(false)
        return
    }
    val agg = computeCustomerVisitAggregates(allAppointments, customerId)
    val updates = hashMapOf<String, Any?>(
        "Customers/$customerId/visitCount" to agg.visitCount,
        "Customers/$customerId/avgWeeksBetweenVisits" to agg.avgWeeksBetweenVisits,
        "Customers/$customerId/lastVisit" to agg.lastVisit,
        "Customers/$customerId/appointment" to agg.latestSlimAppointment,
        // Usunięte pole — czyści stare wpisy w Firebase
        "Customers/$customerId/totalSpentPln" to null,
    )
    database.reference.updateChildren(updates)
        .addOnSuccessListener { completion?.invoke(true) }
        .addOnFailureListener { completion?.invoke(false) }
}
