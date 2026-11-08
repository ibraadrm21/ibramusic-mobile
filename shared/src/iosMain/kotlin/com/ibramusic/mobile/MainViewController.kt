package com.ibramusic.mobile

import androidx.compose.ui.window.ComposeUIViewController
import com.ibramusic.mobile.ui.IbraMusicApp
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController = ComposeUIViewController {
    IbraMusicApp()
}

