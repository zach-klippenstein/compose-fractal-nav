package com.zachklipp.galaxyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.zachklipp.galaxyapp.ui.theme.FractalnavTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FractalnavTheme {
                // A surface container using the 'background' color from the theme
                App()
            }
        }
    }
}
