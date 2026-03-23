package com.strefagentelmena.functions.fireBase

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.strefagentelmena.models.Customer
import com.strefagentelmena.models.appoimentsModel.Appointment
import com.strefagentelmena.models.firestore.SCHEMA_VERSION
import com.strefagentelmena.models.firestore.appointmentToFirestoreMap
import com.strefagentelmena.models.firestore.customerToFirestoreMap
import com.strefagentelmena.models.firestore.employeeToFirestoreMap
import com.strefagentelmena.models.firestore.profilePreferencesToFirestoreMap
import com.strefagentelmena.models.normalizedAfterFirebaseLoad
import com.strefagentelmena.models.settngsModel.Employee
import com.strefagentelmena.models.settngsModel.ProfilePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Migracja **Firebase Realtime Database** → **Cloud Firestore** z **przekształceniem modeli**
 * (płaskie dokumenty, pola pod zapytania, brak rekurencji klient ↔ wizyta).
 *
 * Znane węzły RTDB:
 * - `Customers` → `customers/{id}` — zobacz [com.strefagentelmena.models.firestore.customerToFirestoreMap]
 * - `Appointments` → `appointments/{id}` — [com.strefagentelmena.models.firestore.appointmentToFirestoreMap]
 * - `Employees` → `employees/{id}` — [com.strefagentelmena.models.firestore.employeeToFirestoreMap]
 * - `ProfilePreferences` → `settings/profilePreferences`
 *
 * Inne klucze z korzenia → `rtdb_mirror/{klucz}` (surowy `payload` + `rootKey`).
 *
 * Każdy zmigrowany dokument ma `schemaVersion` = [SCHEMA_VERSION] oraz `migratedAt` (timestamp serwera).
 */
object FirebaseRealtimeToFirestoreMigration {

    private const val TAG = "RtdbToFirestore"
    private const val BATCH_LIMIT = 450

    data class MigrationResult(
        val success: Boolean,
        val documentWrites: Int,
        val topLevelKeys: List<String>,
        val skippedInvalid: Int,
        val errorMessage: String? = null,
    )

    suspend fun migrateAll(
        rtdb: FirebaseDatabase = FirebaseDatabase.getInstance(),
        firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    ): MigrationResult = withContext(Dispatchers.IO) {
        try {
            val root = rtdb.reference.get().await()
            val topLevelKeys = root.children.mapNotNull { it.key }.toList()
            val operations = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()
            var skipped = 0

            for (node in root.children) {
                when (node.key) {
                    "Customers" -> {
                        for (child in node.children) {
                            val c = child.getValue(Customer::class.java)?.normalizedAfterFirebaseLoad()
                            if (c == null) {
                                skipped++
                                continue
                            }
                            val docId = sanitizeDocumentId(child.key ?: c.id.toString())
                            operations += firestore.collection("customers").document(docId) to
                                customerToFirestoreMap(c)
                        }
                    }
                    "Appointments" -> {
                        for (child in node.children) {
                            val a = child.getValue(Appointment::class.java)?.normalizedAfterFirebaseLoad()
                            if (a == null) {
                                skipped++
                                continue
                            }
                            val docId = sanitizeDocumentId(child.key ?: a.id.toString())
                            operations += firestore.collection("appointments").document(docId) to
                                appointmentToFirestoreMap(a)
                        }
                    }
                    "Employees" -> {
                        for (child in node.children) {
                            val e = child.getValue(Employee::class.java)
                            if (e == null) {
                                skipped++
                                continue
                            }
                            val rawId = child.key ?: e.id?.toString()
                            if (rawId == null) {
                                skipped++
                                continue
                            }
                            val docId = sanitizeDocumentId(rawId)
                            operations += firestore.collection("employees").document(docId) to
                                employeeToFirestoreMap(e)
                        }
                    }
                    "ProfilePreferences" -> {
                        val p = node.getValue(ProfilePreferences::class.java)
                        if (p != null) {
                            operations += firestore.collection("settings")
                                .document("profilePreferences") to profilePreferencesToFirestoreMap(p)
                        }
                    }
                    else -> {
                        val key = node.key ?: continue
                        val payload = dataSnapshotToFirestoreValue(node)
                        if (payload != null) {
                            val wrapped = mutableMapOf<String, Any>(
                                "rootKey" to key,
                                "schemaVersion" to SCHEMA_VERSION,
                                "migratedAt" to FieldValue.serverTimestamp(),
                                "payload" to payload,
                            )
                            operations += firestore.collection("rtdb_mirror")
                                .document(sanitizeDocumentId(key)) to wrapped
                        }
                    }
                }
            }

            var written = 0
            for (chunk in operations.chunked(BATCH_LIMIT)) {
                val batch = firestore.batch()
                for ((ref, data) in chunk) {
                    batch.set(ref, data)
                }
                batch.commit().await()
                written += chunk.size
            }

            Log.i(
                TAG,
                "Migracja zakończona: $written dokumentów, pominięte wpisy: $skipped, węzły: $topLevelKeys",
            )
            MigrationResult(
                success = true,
                documentWrites = written,
                topLevelKeys = topLevelKeys,
                skippedInvalid = skipped,
                errorMessage = null,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Migracja nie powiodła się: ${e.message}", e)
            MigrationResult(
                success = false,
                documentWrites = 0,
                topLevelKeys = emptyList(),
                skippedInvalid = 0,
                errorMessage = e.message,
            )
        }
    }

    private fun sanitizeDocumentId(raw: String): String {
        var s = raw.replace("/", "_").replace(".", "_").trim()
        if (s.isEmpty()) s = "_empty"
        if (s.length > 700) s = s.take(700)
        return s
    }

    private fun dataSnapshotToFirestoreValue(snapshot: DataSnapshot): Any? {
        val children = snapshot.children.toList()
        if (children.isEmpty()) {
            return leafToFirestore(snapshot.value)
        }
        val map = linkedMapOf<String, Any>()
        for (c in children) {
            val k = c.key ?: continue
            val v = dataSnapshotToFirestoreValue(c) ?: continue
            map[k] = v
        }
        return map
    }

    private fun leafToFirestore(value: Any?): Any? {
        if (value == null) return null
        return when (value) {
            is Boolean, is String -> value
            is Int -> value.toLong()
            is Long -> value
            is Float -> value.toDouble()
            is Double -> value
            is Number -> {
                val d = value.toDouble()
                if (d.isFinite() && d % 1.0 == 0.0) value.toLong() else d
            }
            is Map<*, *> -> {
                val out = linkedMapOf<String, Any>()
                for ((k, v) in value) {
                    val key = k?.toString() ?: continue
                    val converted = when (v) {
                        null -> continue
                        is Map<*, *> -> leafToFirestore(v)
                        is List<*> -> listToFirestore(v)
                        else -> leafToFirestore(v)
                    } ?: continue
                    out[key] = converted
                }
                out
            }
            is List<*> -> listToFirestore(value)
            else -> value.toString()
        }
    }

    private fun listToFirestore(list: List<*>): List<Any> {
        return list.mapNotNull { item ->
            when (item) {
                null -> null
                is Map<*, *> -> leafToFirestore(item)
                is List<*> -> listToFirestore(item)
                else -> leafToFirestore(item)
            }
        }
    }
}
