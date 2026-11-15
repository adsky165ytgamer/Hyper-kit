package com.hyperkit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.hyperkit.backend.whiteboard.BoardRepository
import com.hyperkit.frontend.HyperKitApp
import com.hyperkit.integration.BoardService
import java.io.File

/**
 * Android application entry point.
 * Wires the three Shelves together by hand (no dependency-injection framework):
 * Backend Shelf (BoardRepository) -> Integration + API Shelf (BoardService) -> Frontend Shelf (HyperKitApp).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val boards = BoardService(BoardRepository(File(filesDir, "boards")))
        setContent { HyperKitApp(boards) }
    }
}
