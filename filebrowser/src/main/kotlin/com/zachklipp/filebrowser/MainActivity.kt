package com.zachklipp.filebrowser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.zachklipp.filebrowser.ui.theme.FileBrowserTheme
import okio.FileSystem
import okio.Path.Companion.toOkioPath

class MainActivity : ComponentActivity() {

    private var navState: TreeBrowserState? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        @Suppress("DEPRECATION")
        val navState = navState ?: run {
            (lastCustomNonConfigurationInstance as? TreeBrowserState) ?: TreeBrowserState()
        }.also { navState = it }

        val root = this.dataDir.toOkioPath()

        setContent {
            FileBrowserTheme {
                // A surface container using the 'background' color from the theme
                App(root, FileSystem.SYSTEM, navState)
            }
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onRetainCustomNonConfigurationInstance(): Any? = navState
}
