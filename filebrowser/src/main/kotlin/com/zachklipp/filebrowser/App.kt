package com.zachklipp.filebrowser

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

@Composable
fun App(
    rootPath: Path,
    fileSystem: FileSystem,
    navState: TreeBrowserState = remember { TreeBrowserState() }
) {
    MaterialTheme {
        Surface {
            FileSystemBrowser(rootPath, fileSystem, state = navState)
        }
    }
}

@Preview
@Composable
private fun AppPreview() {
    App(
        rootPath = "/".toPath(),
        fileSystem = FakeFileSystem(),
    )
}