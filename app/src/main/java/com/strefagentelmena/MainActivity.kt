package com.strefagentelmena

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import com.google.firebase.FirebaseApp
import com.strefagentelmena.navigation.navigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        setContent {
            navigation.AppNavigation()
        }
    }
}
