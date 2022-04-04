package com.zachklipp.galaxyapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.zachklipp.fractalnav.FractalNavState
import com.zachklipp.galaxyapp.ui.theme.FractalnavTheme

class MainActivity : ComponentActivity() {

    private var navState: FractalNavState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        val navState = navState ?: run {
            (lastCustomNonConfigurationInstance as? FractalNavState) ?: FractalNavState()
        }.also { navState = it }

        setContent {
            FractalnavTheme {
                // A surface container using the 'background' color from the theme
                App(navState)
            }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onRetainCustomNonConfigurationInstance(): Any? = navState
}
