package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.CIDApp
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (FirebaseApp.getApps(this).isEmpty()) {
            try {
                FirebaseApp.initializeApp(this)
            } catch (e: Exception) {
                try {
                    val apiKey = try {
                        val key = BuildConfig.FIREBASE_API_KEY
                        if (key.isNotBlank() && !key.contains("MY_FIREBASE_API_KEY")) key else "AIzaSyCRpKQHYykQok7r9P3jn-PVxq9w-c8TE0w"
                    } catch (_: Throwable) {
                        "AIzaSyCRpKQHYykQok7r9P3jn-PVxq9w-c8TE0w"
                    }
                    val options = FirebaseOptions.Builder()
                        .setApplicationId("1:1070286926896:android:b8971f49615a995e")
                        .setApiKey(apiKey)
                        .setProjectId("cid-network-app")
                        .setGcmSenderId("1070286926896")
                        .build()
                    FirebaseApp.initializeApp(this, options)
                } catch (_: Exception) {
                    // Handled gracefully if already initialized or unavailable
                }
            }
        }

        setContent {
            CIDApp()
        }
    }
}


