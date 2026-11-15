package com.hyperkit.frontend

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hyperkit.frontend.theme.HyperKitTheme
import com.hyperkit.frontend.whiteboard.WhiteboardScreen
import com.hyperkit.integration.BoardService

/**
 * Frontend Shelf -> application shell.
 * Traditional terminology -> root composable / app scaffold.
 *
 * The whiteboard is the only destination today, so there is deliberately no
 * navigation library yet. When a second feature arrives, destinations are
 * added here.
 */
@Composable
fun HyperKitApp(boards: BoardService) {
    HyperKitTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            WhiteboardScreen(boards = boards)
        }
    }
}
