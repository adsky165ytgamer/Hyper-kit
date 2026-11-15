package com.hyperkit.frontend.whiteboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperkit.integration.BoardSummary
import java.text.DateFormat
import java.util.Date

/**
 * Frontend Shelf -> Whiteboard feature -> board menu.
 * Traditional terminology -> Presentation layer, modal bottom sheet.
 *
 * Every action is a callback; the screen decides what it does.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardMenuSheet(
    showGrid: Boolean,
    onDismiss: () -> Unit,
    onNewBoard: () -> Unit,
    onOpenBoards: () -> Unit,
    onInsertImage: () -> Unit,
    onExportPng: () -> Unit,
    onExportSvg: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onToggleGrid: (Boolean) -> Unit,
    onClearCanvas: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            MenuHeader("Board")
            MenuItem("New board", onNewBoard)
            MenuItem("My boards", onOpenBoards)
            MenuItem("Import board file (JSON)", onImportJson)
            HorizontalDivider()
            MenuHeader("Insert")
            MenuItem("Image from device", onInsertImage)
            HorizontalDivider()
            MenuHeader("Export")
            MenuItem("PNG image", onExportPng)
            MenuItem("SVG vector", onExportSvg)
            MenuItem("Board file (JSON)", onExportJson)
            HorizontalDivider()
            MenuHeader("View")
            ListItem(
                headlineContent = { Text("Show grid") },
                trailingContent = { Switch(checked = showGrid, onCheckedChange = onToggleGrid) },
            )
            MenuItem("Clear canvas", onClearCanvas)
        }
    }
}

@Composable
private fun MenuHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun MenuItem(text: String, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(text) }, modifier = Modifier.clickable(onClick = onClick))
}

/**
 * Frontend Shelf -> Whiteboard feature -> saved boards list.
 * Traditional terminology -> Presentation layer, modal bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardListSheet(
    boards: List<BoardSummary>?,
    currentId: String?,
    onDismiss: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<BoardSummary?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState()) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()) {
            MenuHeader("My boards")
            when {
                boards == null -> Text("Loading…", Modifier.padding(16.dp))
                boards.isEmpty() -> Text(
                    "No saved boards yet. Boards save automatically as you draw.",
                    Modifier.padding(16.dp),
                )
                else -> boards.forEach { b ->
                    ListItem(
                        headlineContent = { Text(b.handle.title) },
                        supportingContent = {
                            Text(
                                DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                                    .format(Date(b.updatedAt)) +
                                    if (b.handle.id == currentId) " · open now" else "",
                            )
                        },
                        trailingContent = {
                            IconButton(onClick = { pendingDelete = b }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete board")
                            }
                        },
                        modifier = Modifier.clickable { onOpen(b.handle.id) },
                    )
                }
            }
        }
    }
    pendingDelete?.let { b ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete this board?") },
            text = { Text("“${b.handle.title}” will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = { onDelete(b.handle.id); pendingDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}
