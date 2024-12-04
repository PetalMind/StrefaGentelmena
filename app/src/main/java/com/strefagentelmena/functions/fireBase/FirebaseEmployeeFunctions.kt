package com.strefagentelmena.functions.fireBase

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.strefagentelmena.models.settngsModel.Employee
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseEmployeeFunctions {

    // Dodaj nowego pracownika do Firebase
    fun addEmployeeToFirebase(
        firebaseDatabase: FirebaseDatabase,
        employee: Employee,
        completion: (Boolean) -> Unit
    ) {
        val employeesRef = firebaseDatabase.getReference("Employees")

        // Używamy employee.id jako klucza w bazie
        val newEmployeeRef = employeesRef.child(employee.id.toString())

        newEmployeeRef.setValue(employee)
            .addOnSuccessListener {
                Log.i("Firebase", "Employee added successfully")
                completion(true)
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error adding employee: ${exception.message}")
                completion(false)
            }
    }


    // Edytuj istniejącego pracownika w Firebase
    fun editEmployeeInFirebase(
        firebaseDatabase: FirebaseDatabase,
        updatedEmployee: Employee,
        completion: (Boolean) -> Unit
    ) {
        if (updatedEmployee.id == null) {
            Log.e("Firebase", "Employee ID is null. Cannot update employee.")
            completion(false)
            return
        }

        val employeesRef = firebaseDatabase.getReference("Employees")
        val employeeRef = employeesRef.child(updatedEmployee.id.toString())

        employeeRef.setValue(updatedEmployee)
            .addOnSuccessListener {
                Log.i("Firebase", "Employee updated successfully")
                completion(true)
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error updating employee: ${exception.message}")
                completion(false)
            }
    }

    // Usuń pracownika z Firebase
    fun deleteEmployeeFromFirebase(
        firebaseDatabase: FirebaseDatabase,
        employeeId: Int,
        completion: (Boolean) -> Unit
    ) {
        val employeesRef = firebaseDatabase.getReference("Employees").child(employeeId.toString())

        employeesRef.removeValue()
            .addOnSuccessListener {
                Log.i("Firebase", "Employee deleted successfully")
                completion(true)
            }
            .addOnFailureListener { exception ->
                Log.e("Firebase", "Error deleting employee: ${exception.message}")
                completion(false)
            }
    }

    // Odczytaj listę pracowników z Firebase
    suspend fun loadEmployeesFromFirebase(firebaseDatabase: FirebaseDatabase): MutableList<Employee> {
        val employeesRef = firebaseDatabase.getReference("Employees")
        return suspendCancellableCoroutine { continuation ->
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val employeesList = try {
                        snapshot.children.mapNotNull { it.getValue(Employee::class.java) }.toMutableList()
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                        return
                    }
                    continuation.resume(employeesList)
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }
            }

            employeesRef.addListenerForSingleValueEvent(listener)

            continuation.invokeOnCancellation {
                employeesRef.removeEventListener(listener)
            }
        }
    }


}
