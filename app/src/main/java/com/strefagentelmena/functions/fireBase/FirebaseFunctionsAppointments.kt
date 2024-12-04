package com.strefagentelmena.functions.fireBase

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.strefagentelmena.models.appoimentsModel.Appointment
import kotlinx.coroutines.tasks.await

class FirebaseFunctionsAppointments {
    fun addNewAppointmentToFirebase(
        firebaseDatabase: FirebaseDatabase,
        newAppointment: Appointment,
        completion: (Boolean) -> Unit
    ) {
        val appointmentsRef = firebaseDatabase.getReference("Appointments")

        // Używamy wartości 'id' z obiektu newAppointment, aby zapisać wizytę w Firebase.
        val appointmentId = newAppointment.id.toString() // Możesz tutaj użyć id jako String

        // Sprawdzamy, czy id jest prawidłowe (czy nie jest 0 lub inne niepożądane wartości)
        if (appointmentId.isNotEmpty()) {
            // Zapisujemy appointment w Firebase z Twoim własnym id
            appointmentsRef.child(appointmentId).setValue(newAppointment)
                .addOnSuccessListener {
                    Log.e("Firebase", "Appointment added successfully with ID: $appointmentId")
                    completion(true) // Wizyta została dodana
                }
                .addOnFailureListener { exception ->
                    Log.e("Firebase", "Error adding appointment: ${exception.message}")
                    completion(false) // Wystąpił błąd
                }
        } else {
            Log.e("Firebase", "Appointment ID is invalid or empty.")
            completion(false) // Błąd z ID
        }
    }



    fun editAppointmentInFirebase(
        firebaseDatabase: FirebaseDatabase,
        updatedAppointment: Appointment,
        completion: (Boolean) -> Unit
    ) {
        val appointmentsRef = firebaseDatabase.getReference("Appointments")
        val appointmentRef =
            appointmentsRef.child(updatedAppointment.id.toString()) // Aktualizujemy konkretną wizytę przez jej ID

        appointmentRef.setValue(updatedAppointment)
            .addOnSuccessListener {
                Log.e("Firebase", "Appointment updated successfully")
                completion(true)
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error updating appointment: ${exception.message}")
                completion(false)
            }
    }

    // Zapisz listę appointment w Firebase
    fun saveAppointmentsToFirebase(
        firebaseDatabase: FirebaseDatabase,
        appointments: List<Appointment?>,
        completion: (Boolean) -> Unit
    ) {
        val appointmentsRef = firebaseDatabase.getReference("Appointments")

        appointments.forEach { appointment ->
            val appointmentId = appointment?.id ?: return@forEach
            val appointmentRef = appointmentsRef.child(appointmentId.toString())

            // Przekazanie appointment do Firebase
            appointmentRef.setValue(appointment)
                .addOnSuccessListener {
                    Log.e("Firebase", "Appointment saved successfully with ID: $appointmentId")
                    completion(true) // Success
                }
                .addOnFailureListener { exception ->
                    Log.e("Firebase", "Error saving appointment: ${exception.message}")
                    completion(false) // Failure
                }
        }
    }

    // Załaduj wszystkie appointment z Firebase
    suspend fun loadAppointmentsFromFirebase(firebaseDatabase: FirebaseDatabase): List<Appointment> {
        val appointmentsRef = firebaseDatabase.getReference("Appointments")

        return try {
            val dataSnapshot = appointmentsRef.get().await() // Czekamy na wynik zapytania
            val appointments = mutableListOf<Appointment>()

            for (snapshot in dataSnapshot.children) {
                val appointment = snapshot.getValue(Appointment::class.java)
                appointment?.let { appointments.add(it) }
            }

            appointments // Zwracamy załadowaną listę
        } catch (e: Exception) {
            Log.e("Firebase", "Error loading appointments: ${e.message}")
            emptyList() // W przypadku błędu zwracamy pustą listę
        }
    }

    // Usuń appointment z Firebase
    fun deleteAppointmentFromFirebase(
        firebaseDatabase: FirebaseDatabase,
        appointmentId: Int,
        completion: (Boolean) -> Unit
    ) {
        val appointmentRef =
            firebaseDatabase.getReference("Appointments").child(appointmentId.toString())

        appointmentRef.removeValue()
            .addOnSuccessListener {
                Log.e("Firebase", "Appointment deleted successfully with ID: $appointmentId")
                completion(true) // Success
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error deleting appointment: ${exception.message}")
                completion(false) // Failure
            }
    }
}
