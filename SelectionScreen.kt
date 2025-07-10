package com.google.mediapipe.examples.llminference

import androidx.compose.runtime.Composable

@Composable
internal fun SelectionRoute(
    onModelSelected: () -> Unit = {},
) {
    // Immediately proceed to loading screen since only one model is used
    onModelSelected()
}
