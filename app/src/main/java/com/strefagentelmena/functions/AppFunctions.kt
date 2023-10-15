package com.strefagentelmena.functions

import android.content.Context
import android.content.Intent
import android.net.Uri

val appFunctions = AppFunctions()

class AppFunctions {
    fun dialPhoneNumber(context: Context, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }
}
