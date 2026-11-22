package com.example.checklistapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.checklistapp.ui.AppNavHost
import com.example.checklistapp.ui.theme.ChecklistTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChecklistTheme {
                AppNavHost()
            }
        }
    }
}
