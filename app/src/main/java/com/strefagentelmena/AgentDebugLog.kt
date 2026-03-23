package com.strefagentelmena

import android.util.Log
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Debug NDJSON: ingest (emulator → host) + mirror do pliku workspace (jeśli ścieżka istnieje na urządzeniu) + Logcat.
 */
object AgentDebugLog {
    private const val TAG = "DBG198D6F"
    private const val SESSION = "1a8e51"
    private const val INGEST =
        "http://10.0.2.2:7466/ingest/802aac94-4e02-412a-b39c-dd12e9c52242"
    private const val WORKSPACE_LOG =
        "/Users/dominik/Github/StrefaGentelmena/.cursor/debug-1a8e51.log"

    // #region agent log
    fun log(hypothesisId: String, location: String, message: String, data: Map<String, String?>) {
        try {
            val jo = JSONObject()
            jo.put("sessionId", SESSION)
            jo.put("hypothesisId", hypothesisId)
            jo.put("location", location)
            jo.put("message", message)
            jo.put("timestamp", System.currentTimeMillis())
            val dataObj = JSONObject()
            data.forEach { (k, v) -> dataObj.put(k, v ?: "null") }
            jo.put("data", dataObj)
            val line = jo.toString()
            Log.i(TAG, line)
            try {
                File(WORKSPACE_LOG).appendText(line + "\n")
            } catch (_: Throwable) { /* urządzenie — brak ścieżki hosta */ }
            Thread {
                try {
                    val url = URL(INGEST)
                    (url.openConnection() as HttpURLConnection).run {
                        requestMethod = "POST"
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("X-Debug-Session-Id", SESSION)
                        doOutput = true
                        outputStream.use { os -> os.write(line.toByteArray(Charsets.UTF_8)) }
                        disconnect()
                    }
                } catch (_: Exception) { /* brak sieci / fizyczne urządzenie */ }
            }.start()
        } catch (e: Exception) {
            Log.e(TAG, "log failed", e)
        }
    }
    // #endregion
}
