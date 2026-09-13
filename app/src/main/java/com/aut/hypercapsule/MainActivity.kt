package com.aut.hypercapsule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.aut.hypercapsule.ui.AppPreferences
import com.aut.hypercapsule.ui.HyperCapsuleApp
import com.aut.hypercapsule.ui.theme.HyperCapsuleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preferences = remember { AppPreferences(applicationContext) }
            HyperCapsuleTheme(dynamicColorEnabled = preferences.dynamicColorEnabled) {
                HyperCapsuleApp(preferences = preferences)
            }
        }
    }
}
