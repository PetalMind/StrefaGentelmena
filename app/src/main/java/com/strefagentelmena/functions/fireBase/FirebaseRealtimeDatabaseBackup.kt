package com.strefagentelmena.functions.fireBase

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Pełny eksport **Realtime Database** do pliku JSON w **Firebase Storage**.
 *
 * Bucket produkcyjny: [STORAGE_BUCKET] (zgodny z `storage_bucket` w `google-services.json`).
 *
 * **Reguły Storage** muszą zezwalać na zapis pod prefiksem `backups/` (np. tylko dla zalogowanego użytkownika).
 * Przykład: `match /backups/{file=**} { allow write: if request.auth != null; }`
 */
object FirebaseRealtimeDatabaseBackup {

    const val STORAGE_BUCKET = "gs://strefagentlemena.appspot.com"

    private const val BACKUP_FOLDER = "backups"

    data class BackupResult(
        val success: Boolean,
        /** Ścieżka względna w bucketcie, np. `backups/rtdb_backup_2025-03-20_12-30-00.json`. */
        val storagePath: String? = null,
        val errorMessage: String? = null,
    )

    suspend fun exportFullDatabaseJsonToStorage(
        database: FirebaseDatabase = FirebaseDatabase.getInstance(),
        storage: FirebaseStorage = FirebaseStorage.getInstance(STORAGE_BUCKET),
    ): BackupResult = withContext(Dispatchers.IO) {
        try {
            val snapshot = database.reference.get().await()
            val tree = snapshot.value
            val gson = GsonBuilder().setPrettyPrinting().create()
            val json = when (tree) {
                null -> "{}"
                else -> gson.toJson(tree)
            }
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Europe/Warsaw")
            }.format(Date())
            val fileName = "rtdb_backup_$timestamp.json"
            val path = "$BACKUP_FOLDER/$fileName"
            val ref = storage.reference.child(path)
            val metadata = StorageMetadata.Builder()
                .setContentType("application/json; charset=utf-8")
                .build()
            ref.putBytes(json.toByteArray(Charsets.UTF_8), metadata).await()
            BackupResult(success = true, storagePath = path, errorMessage = null)
        } catch (e: Exception) {
            BackupResult(success = false, storagePath = null, errorMessage = e.message)
        }
    }
}
