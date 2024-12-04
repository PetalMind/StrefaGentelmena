package com.strefagentelmena.functions.fireBase

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseProfilePreferences {

    fun saveProfilePreferencesToFirebase(
        firebaseDatabase: FirebaseDatabase,
        profilePreferences: ProfilePreferences,
        completion: (Boolean) -> Unit
    ) {
        val profileRef = firebaseDatabase.getReference("ProfilePreferences")

        profileRef.setValue(profilePreferences)
            .addOnSuccessListener {
                Log.i("Firebase", "ProfilePreferences saved successfully")
                completion(true) // Zapis zakończony sukcesem
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error saving ProfilePreferences: ${exception.message}")
                completion(false) // Wystąpił błąd
            }
    }

    // Funkcja do odczytu ProfilePreferences z Firebase
    suspend fun loadProfilePreferencesFromFirebase(firebaseDatabase: FirebaseDatabase): ProfilePreferences {
        val profileRef = firebaseDatabase.getReference("ProfilePreferences")

        return suspendCancellableCoroutine { continuation ->
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        val profilePreferences = snapshot.getValue(ProfilePreferences::class.java)
                        if (profilePreferences != null) {
                            continuation.resume(profilePreferences) // Zwracamy dane ProfilePreferences
                        } else {
                            continuation.resumeWithException(Exception("ProfilePreferences is null"))
                        }
                    } catch (e: Exception) {
                        continuation.resumeWithException(e) // Obsługuje wyjątki
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException()) // Obsługuje anulowanie
                }
            }

            // Dodanie listenera
            profileRef.addListenerForSingleValueEvent(listener)

            // Anulowanie w przypadku anulowania coroutine
            continuation.invokeOnCancellation {
                profileRef.removeEventListener(listener)
            }
        }
    }

}
