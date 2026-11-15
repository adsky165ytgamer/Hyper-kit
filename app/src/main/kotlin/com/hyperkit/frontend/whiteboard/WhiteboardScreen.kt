@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, FlowPreview::class)

package com.hyperkit.frontend.whiteboard

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.hyperkit.integration.BoardHandle
import com.hyperkit.integration.BoardService
import com.hyperkit.integration.BoardSummary
import com.hyperkit.integration.LoadedBoard
import com.hyperkit.integration.loadDrawBoxJson
import com.hyperkit.integration.rememberFileExchange
import com.hyperkit.integration.toDrawBoxJson
import io.ak1.drawbox.DrawBox
import io.ak1.drawbox.domain.model.Element
import io.ak1.drawbox.domain.model.Event
import io.ak1.drawbox.domain.model.Intent
import io.ak1.drawbox.domain.model.Mode
import io.ak1.drawbox.domain.model.ShapeType
import io.ak1.drawbox.domain.model.State
import io.ak1.drawbox.presentation.viewmodel.rememberDrawBoxController
import io.ak1.drawbox.text.FontRegistry
import io.ak1.drawbox.text.InlineTextEditor
import io.ak1.drawbox.ui.context.ContextBar
import io.ak1.drawbox.ui.controls.ControlsBar
import io.ak1.drawbox.ui.controls.ControlsBarIntent
import io.ak1.drawbox.ui.controls.ControlsBarState
import io.ak1.drawbox.ui.controls.defaultControlsBarItems
import io.ak1.drawbox.ui.model.ContextBarIntent
import io.ak1.drawbox.ui.model.ContextBarSlots
import io.ak1.drawbox.ui.model.ContextBarState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

// A classroom whiteboard starts as white paper with dark ink.
private val BoardPaper = Color.White
private val BoardInk = Color(0xFF1B1B1F)
private const val AUTOSAVE_DELAY_MS = 500L

private val HandleSaver = listSaver<BoardHandle?, Any>(
    save = { h -> if (h == null) emptyList() else listOf(h.id, h.title, h.createdAt) },
    restore = { l -> if (l.isEmpty()) null else BoardHandle(l[0] as String, l[1] as String, l[2] as Long) },
)

/**
 * Filename: WhiteboardScreen.kt
 *
 * Frontend Shelf -> Whiteboard feature.
 * Traditional terminology -> Presentation layer, Whiteboard screen.
 *
 * Displays the DrawBox canvas and toolbars, connects them to the DrawBox
 * controller, and talks to board storage ONLY through [BoardService]
 * (Integration + API Shelf).
 *
 * The toolbar wiring (ContextBar / ControlsBar / inline text editing) follows
 * the DrawBox sample app's HomeScreen (Apache-2.0, see ups/drawbox/LICENSE),
 * trimmed to what HyperKit uses.
 */
@Composable
fun WhiteboardScreen(boards: BoardService) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val files = rememberFileExchange()
    val textMeasurer = rememberTextMeasurer()
    fun toast(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    val controller = rememberDrawBoxController(
        initialState = State(strokeColor = BoardInk, bgColor = BoardPaper),
    )
    val state by controller.state.collectAsState()
    val canUndo by controller.canUndo.collectAsState()
    val canRedo by controller.canRedo.collectAsState()

    // ---- board identity / persistence state ---------------------------------
    var handle by rememberSaveable(stateSaver = HandleSaver) { mutableStateOf<BoardHandle?>(null) }
    var ready by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }
    var showGrid by rememberSaveable { mutableStateOf(true) }
    var menuOpen by remember { mutableStateOf(false) }
    var boardsOpen by remember { mutableStateOf(false) }
    var boardList by remember { mutableStateOf<List<BoardSummary>?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val canvasCenter = Offset(canvasSize.width / 2f, canvasSize.height / 2f)

    // ---- inline text editing state (as in the DrawBox sample) ---------------
    var editingTextId by remember { mutableStateOf<String?>(null) }
    var editDraft by remember { mutableStateOf("") }
    var editOriginal by remember { mutableStateOf("") }

    fun loadIntoCanvas(json: String) {
        controller.loadDrawBoxJson(json)
        // DrawBox's import keeps the ink colour of a freshly reset state (red); restore ours.
        controller.setColor(BoardInk)
        editingTextId = null
    }

    // Save the open board right now if it has unsaved changes.
    suspend fun flush() {
        val h = handle ?: return
        if (dirty) {
            boards.save(h, controller.toDrawBoxJson())
            dirty = false
        }
    }

    fun switchTo(next: LoadedBoard?) {
        scope.launch {
            ready = false
            flush()
            if (next != null) {
                loadIntoCanvas(next.drawBoxJson)
                handle = next.handle
            } else {
                controller.reset()
                controller.setColor(BoardInk)
                controller.setBgColor(BoardPaper)
                editingTextId = null
                handle = boards.newBoard()
            }
            dirty = false
            ready = true
        }
    }

    // Startup: reopen the last board (or the one restored after rotation / process death).
    LaunchedEffect(Unit) {
        val restored = handle
        val loaded = if (restored != null) boards.open(restored.id) else boards.openLast()
        if (loaded != null && controller.state.value.elements.isEmpty()) loadIntoCanvas(loaded.drawBoxJson)
        if (restored == null) handle = loaded?.handle ?: boards.newBoard()
        ready = true
    }

    // Autosave: shortly after the drawing stops changing. Pan/zoom/selection do not trigger a save.
    LaunchedEffect(ready, handle) {
        val h = handle
        if (!ready || h == null) return@LaunchedEffect
        controller.state
            .map { it.elements to it.bgColor }
            .distinctUntilChanged()
            .drop(1) // the value present when collection starts is what was just loaded
            .onEach { dirty = true }
            .debounce(AUTOSAVE_DELAY_MS)
            .collect {
                boards.save(h, controller.toDrawBoxJson())
                dirty = false
            }
    }

    // Events from the UPS: exports, errors, "edit this text".
    LaunchedEffect(controller) {
        controller.events.collect { event ->
            when (event) {
                is Event.PngExported -> files.savePng(event.bytes)
                is Event.SvgExported -> files.saveSvg(event.svg)
                is Event.JsonExported -> files.saveJson(event.json)
                is Event.Error -> toast(event.message)
                is Event.TextEditRequested -> {
                    (state.elements.firstOrNull { it.id == event.id } as? Element.Text)?.let {
                        editingTextId = it.id
                        editDraft = it.text
                        editOriginal = it.text
                    }
                }
                else -> {}
            }
        }
    }

    // TEXT mode: DrawBox inserts an empty Text element on tap; open the editor for it.
    LaunchedEffect(state.elements) {
        if (editingTextId != null) return@LaunchedEffect
        val last = state.elements.lastOrNull()
        if (last is Element.Text && last.text.isEmpty()) {
            editingTextId = last.id
            editDraft = ""
            editOriginal = ""
        }
    }
    fun commitTextEdit() {
        val id = editingTextId ?: return
        if (editDraft.isEmpty()) controller.onIntent(Intent.DeleteElement(id)) else controller.updateText(id, editDraft)
        editingTextId = null
    }
    fun cancelTextEdit() {
        val id = editingTextId ?: return
        if (editOriginal.isEmpty()) controller.onIntent(Intent.DeleteElement(id))
        editingTextId = null
    }

    // ---- derived selection / tool flags (same logic as the DrawBox sample) ---
    val selectedDrawables = state.elements.filter { it.id in state.selectedIds }
    val hasSelection = selectedDrawables.isNotEmpty()
    val selectedRoundable = selectedDrawables.filter {
        it is Element.Shape && (it.shapeType == ShapeType.RECTANGLE || it.shapeType == ShapeType.TRIANGLE)
    }
    val isShapeMode = state.mode == Mode.PEN || state.mode == Mode.RECTANGLE || state.mode == Mode.CIRCLE ||
        state.mode == Mode.TRIANGLE || state.mode == Mode.ARROW || state.mode == Mode.LINE
    val isTextMode = state.mode == Mode.TEXT
    val selectedHasStrokeable = selectedDrawables.any { it is Element.Shape || it is Element.Path }
    val currentRadius = if (selectedRoundable.isNotEmpty()) (selectedRoundable.first() as Element.Shape).cornerRadius
    else state.currentItemCornerRadius
    val currentStrokeStyle = when (val first = selectedDrawables.firstOrNull()) {
        is Element.Shape -> first.strokeStyle
        is Element.Path -> first.strokeStyle
        else -> state.currentItemStrokeStyle
    }
    val currentShapeColor = when (val first = selectedDrawables.firstOrNull()) {
        is Element.Shape -> first.strokeColor
        is Element.Path -> first.strokeColor
        is Element.Text -> first.color
        else -> state.strokeColor
    }
    val currentStrokeWidth = when (val first = selectedDrawables.firstOrNull()) {
        is Element.Shape -> first.strokeWidth
        is Element.Path -> first.strokeWidth
        else -> state.strokeWidth
    }
    val selectedShapes = selectedDrawables.filterIsInstance<Element.Shape>()
    val currentFillColor = selectedShapes.firstOrNull()?.fillColor
    val currentStrokeEnabled = selectedShapes.firstOrNull()?.strokeEnabled ?: true
    val hasFillableSelection = selectedShapes.any {
        it.shapeType == ShapeType.RECTANGLE || it.shapeType == ShapeType.CIRCLE || it.shapeType == ShapeType.TRIANGLE
    }
    val isFillableToolMode = state.mode == Mode.RECTANGLE || state.mode == Mode.CIRCLE || state.mode == Mode.TRIANGLE
    val showFillTarget = hasFillableSelection || (!hasSelection && isFillableToolMode)
    val toolOrSelectionStrokeEnabled = if (hasFillableSelection) currentStrokeEnabled else state.currentItemStrokeEnabled
    val toolOrSelectionFillColor = if (hasFillableSelection) currentFillColor else state.currentItemFillColor

    val selectedTexts = selectedDrawables.filterIsInstance<Element.Text>()
    val singleSelectedText = selectedTexts.singleOrNull()?.takeIf { selectedDrawables.size == 1 }
    val firstSelectedText = selectedTexts.firstOrNull()
    val currentFontSize = firstSelectedText?.fontSize ?: state.currentItemFontSize
    val currentTextAlignment = firstSelectedText?.alignment ?: state.currentItemTextAlignment
    val currentFontFamilyKey = firstSelectedText?.fontFamilyKey ?: state.currentItemFontFamilyKey
    val fontSizeMixed = selectedTexts.size > 1 && selectedTexts.distinctBy { it.fontSize }.size > 1
    val textAlignmentMixed = selectedTexts.size > 1 && selectedTexts.distinctBy { it.alignment }.size > 1
    val fontFamilyMixed = selectedTexts.size > 1 && selectedTexts.distinctBy { it.fontFamilyKey }.size > 1

    // Fade floating chrome while a finger is on the canvas.
    var isGesturing by remember { mutableStateOf(false) }
    val chromeAlpha by animateFloatAsState(
        targetValue = if (isGesturing) 0.35f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "chromeAlpha",
    )

    Box(Modifier.fillMaxSize()) {
        // ===== Whiteboard UPS: the DrawBox canvas =====
        DrawBox(
            state = state,
            onIntent = controller::onIntent,
            modifier = Modifier.fillMaxSize().clipToBounds()
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            isGesturing = event.changes.any { it.pressed }
                        }
                    }
                },
            showGrid = showGrid,
            hiddenElementIds = editingTextId?.let { setOf(it) } ?: emptySet(),
        )

        // Tap outside the text editor commits it. Composed above the canvas, below the toolbars.
        if (editingTextId != null) {
            Box(
                Modifier.fillMaxSize().pointerInput(editingTextId) {
                    detectTapGestures(onTap = { commitTextEdit() })
                },
            )
        }

        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            val commonBarState = ContextBarState(
                strokeColor = currentShapeColor,
                strokeEnabled = currentStrokeEnabled,
                strokeStyle = currentStrokeStyle,
                strokeWidth = currentStrokeWidth,
                fillColor = currentFillColor,
                cornerRadius = currentRadius,
                fontSize = currentFontSize,
                textAlignment = currentTextAlignment,
                fontFamilyKey = currentFontFamilyKey,
                fontFamilyKeys = FontRegistry.keys(),
                fontSizeMixed = fontSizeMixed,
                textAlignmentMixed = textAlignmentMixed,
                fontFamilyMixed = fontFamilyMixed,
            )

            // Tool options for the NEXT stroke/insert (only when nothing is selected).
            if (!hasSelection && (isShapeMode || isTextMode)) {
                ContextBar(
                    state = commonBarState,
                    slots = ContextBarSlots(
                        showStroke = false,
                        strokeToggleable = false,
                        showShapeStroke = isShapeMode,
                        showFill = false,
                        showCornerRadius = state.mode == Mode.RECTANGLE || state.mode == Mode.TRIANGLE,
                        showText = isTextMode,
                        showEditText = false,
                        showSelectionActions = false,
                    ),
                    onIntent = { intent ->
                        when (intent) {
                            is ContextBarIntent.SetStrokeColor -> controller.setColor(intent.color)
                            is ContextBarIntent.SetStrokeStyle -> controller.setStrokeStyle(intent.style)
                            is ContextBarIntent.SetStrokeWidth -> controller.setStrokeWidth(intent.width)
                            is ContextBarIntent.SetCornerRadius -> controller.setCornerRadius(intent.radius)
                            is ContextBarIntent.SetFontSize -> controller.setFontSize(intent.size)
                            is ContextBarIntent.SetTextAlignment -> controller.setTextAlignment(intent.alignment)
                            is ContextBarIntent.SetFontFamily -> controller.setFontFamily(intent.key)
                            else -> {}
                        }
                    },
                    fontFamilyResolver = { key -> FontRegistry.resolve(key) },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 76.dp).alpha(chromeAlpha),
                )
            }

            // Edit / act on the current selection.
            if (hasSelection) {
                ContextBar(
                    state = commonBarState,
                    slots = ContextBarSlots(
                        showStroke = false,
                        strokeToggleable = false,
                        showShapeStroke = selectedHasStrokeable,
                        showFill = false,
                        showCornerRadius = selectedRoundable.isNotEmpty(),
                        showText = selectedTexts.isNotEmpty(),
                        showEditText = singleSelectedText != null,
                        showSelectionActions = true,
                    ),
                    onIntent = { intent ->
                        when (intent) {
                            is ContextBarIntent.SetStrokeColor -> controller.setSelectionColor(intent.color)
                            is ContextBarIntent.SetStrokeEnabled -> controller.setSelectionStrokeEnabled(intent.enabled)
                            is ContextBarIntent.SetStrokeStyle -> controller.setSelectionStrokeStyle(intent.style)
                            is ContextBarIntent.SetStrokeWidth -> controller.setSelectionStrokeWidth(intent.width)
                            is ContextBarIntent.SetFillColor -> controller.setSelectionFillColor(intent.color)
                            is ContextBarIntent.SetCornerRadius -> controller.setSelectionCornerRadius(intent.radius)
                            is ContextBarIntent.SetFontSize -> controller.setSelectionFontSize(intent.size)
                            is ContextBarIntent.SetTextAlignment -> controller.setSelectionTextAlignment(intent.alignment)
                            is ContextBarIntent.SetFontFamily -> controller.setSelectionFontFamily(intent.key)
                            ContextBarIntent.EditText -> singleSelectedText?.let { target ->
                                editingTextId = target.id
                                editDraft = target.text
                                editOriginal = target.text
                            }
                            ContextBarIntent.BringToFront -> controller.bringSelectionToFront()
                            ContextBarIntent.SendToBack -> controller.sendSelectionToBack()
                            ContextBarIntent.Delete -> controller.deleteSelected()
                            ContextBarIntent.ClearSelection -> controller.clearSelection()
                        }
                    },
                    fontFamilyResolver = { key -> FontRegistry.resolve(key) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 72.dp, end = 12.dp).alpha(chromeAlpha),
                )
            }

            WhiteboardTopBar(
                title = handle?.title.orEmpty(),
                scalePercent = state.viewport.scalePercent,
                onOpenBoards = {
                    boardsOpen = true
                    boardList = null
                    scope.launch { boardList = boards.list() }
                },
                onZoomIn = { controller.zoomBy(1.25f, canvasCenter) },
                onZoomOut = { controller.zoomBy(0.8f, canvasCenter) },
                onZoomReset = { controller.resetCamera() },
                onOpenMenu = { menuOpen = true },
                modifier = Modifier.align(Alignment.TopCenter).alpha(chromeAlpha),
            )

            // Main tool bar (modes, undo/redo, colour).
            ControlsBar(
                items = defaultControlsBarItems(
                    state = ControlsBarState(
                        currentMode = state.mode,
                        canUndo = canUndo,
                        canRedo = canRedo,
                        strokeColor = currentShapeColor,
                        showFillTarget = showFillTarget,
                        strokeEnabled = toolOrSelectionStrokeEnabled,
                        fillColor = toolOrSelectionFillColor,
                    ),
                    dispatch = { intent ->
                        when (intent) {
                            ControlsBarIntent.Undo -> controller.undo()
                            ControlsBarIntent.Redo -> controller.redo()
                            is ControlsBarIntent.SelectMode -> controller.setMode(intent.mode)
                            is ControlsBarIntent.SetStrokeColor ->
                                if (hasSelection) controller.setSelectionColor(intent.color) else controller.setColor(intent.color)
                            is ControlsBarIntent.SetStrokeEnabled ->
                                if (hasSelection) controller.setSelectionStrokeEnabled(intent.enabled)
                                else controller.setStrokeEnabled(intent.enabled)
                            is ControlsBarIntent.SetFillColor ->
                                if (hasSelection) controller.setSelectionFillColor(intent.color) else controller.setFillColor(intent.color)
                        }
                    },
                ),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp).alpha(chromeAlpha),
            )
        }

        // ===== Board menu =====
        if (menuOpen) {
            BoardMenuSheet(
                showGrid = showGrid,
                onDismiss = { menuOpen = false },
                onNewBoard = { menuOpen = false; switchTo(null) },
                onOpenBoards = {
                    menuOpen = false
                    boardsOpen = true
                    boardList = null
                    scope.launch { boardList = boards.list() }
                },
                onInsertImage = {
                    menuOpen = false
                    files.loadImage { bytes, size ->
                        controller.insertImage(bytes, size, state.viewport.screenToWorld(canvasCenter))
                    }
                },
                onExportPng = { menuOpen = false; controller.exportPng(scale = 2f, background = state.bgColor, textMeasurer = textMeasurer) },
                onExportSvg = { menuOpen = false; controller.exportSvg() },
                onExportJson = { menuOpen = false; controller.exportJson() },
                onImportJson = {
                    menuOpen = false
                    files.loadJson { text ->
                        scope.launch {
                            val imported = boards.importFile(text)
                            if (imported != null) switchTo(imported) else toast("Not a valid board file")
                        }
                    }
                },
                onToggleGrid = { showGrid = it },
                onClearCanvas = {
                    menuOpen = false
                    controller.reset()
                    controller.setColor(BoardInk)
                    controller.setBgColor(BoardPaper)
                },
            )
        }

        // ===== My boards =====
        if (boardsOpen) {
            BoardListSheet(
                boards = boardList,
                currentId = handle?.id,
                onDismiss = { boardsOpen = false },
                onOpen = { id ->
                    boardsOpen = false
                    if (id != handle?.id) {
                        scope.launch {
                            val loaded = boards.open(id)
                            if (loaded != null) switchTo(loaded) else toast("Could not open that board")
                        }
                    }
                },
                onDelete = { id ->
                    scope.launch {
                        boards.delete(id)
                        boardList = boards.list()
                        if (id == handle?.id) {
                            dirty = false // do not resurrect the deleted board
                            boardsOpen = false
                            switchTo(null)
                        }
                    }
                },
            )
        }

        // ===== Inline text editor: composed last so it sits above the toolbars =====
        val editingId = editingTextId
        if (editingId != null) {
            val target = state.elements.firstOrNull { it.id == editingId } as? Element.Text
            if (target == null) {
                editingTextId = null
            } else {
                Box(
                    Modifier.fillMaxSize().onPreviewKeyEvent { e ->
                        if (e.type == KeyEventType.KeyDown && e.key == Key.Escape) {
                            cancelTextEdit()
                            true
                        } else {
                            false
                        }
                    },
                ) {
                    InlineTextEditor(
                        element = target,
                        viewport = state.viewport,
                        draft = editDraft,
                        onDraftChange = { editDraft = it },
                    )
                }
            }
        }
    }
}
