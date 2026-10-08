package com.joshrpr.knifetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.joshrpr.knifetracker.ui.KnifeTrackerNavHost
import com.joshrpr.knifetracker.ui.KnifeTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KnifeTrackerTheme {
                KnifeTrackerNavHost()
            }
        }
    }
}
