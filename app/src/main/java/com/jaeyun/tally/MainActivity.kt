package com.jaeyun.tally

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jaeyun.tally.ui.TallyApp
import com.jaeyun.tally.ui.theme.TallyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TallyTheme {
                TallyApp()
            }
        }
    }
}
