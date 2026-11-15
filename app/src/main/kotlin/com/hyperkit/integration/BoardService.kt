package com.hyperkit.integration

import com.hyperkit.backend.whiteboard.BoardDocument
import com.hyperkit.backend.whiteboard.BoardRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Identity of a board as the Frontend Shelf sees it. */
data class BoardHandle(val id: String, val title: String, val createdAt: Long)

data class BoardSummary(val handle: BoardHandle, val updatedAt: Long)

/** A board read from storage: its identity plus the DrawBox JSON to load into the canvas. */
data class LoadedBoard(val handle: BoardHandle, val drawBoxJson: String)

/**
 * Integration + API Shelf -> the one door the Frontend Shelf uses to reach board storage.
 * Traditional terminology -> Application service between Presentation and Data layers.
 *
 * Today it forwards to the local [BoardRepository]. A future cloud-sync or
 * remote API plugs in here without the screen changing.
 */
class BoardService(private val repository: BoardRepository) {

    /** Creates a handle only. Nothing is written until the first [save]. */
    fun newBoard(now: Long = System.currentTimeMillis()): BoardHandle {
        val title = "Board " + SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(now))
        return BoardHandle(id = now.toString(), title = title, createdAt = now)
    }

    suspend fun save(handle: BoardHandle, drawBoxJson: String) {
        repository.save(
            BoardDocument(
                id = handle.id,
                title = handle.title,
                createdAt = handle.createdAt,
                updatedAt = System.currentTimeMillis(),
                engine = BoardDocument.ENGINE_DRAWBOX,
                engineVersion = DRAWBOX_VERSION,
                enginePayload = drawBoxJson,
            ),
        )
    }

    suspend fun open(id: String): LoadedBoard? {
        val doc = repository.load(id) ?: return null
        if (doc.engine != BoardDocument.ENGINE_DRAWBOX) return null
        repository.setLastOpened(id)
        return doc.toLoaded()
    }

    /** The board that was open last time, or null on first launch. */
    suspend fun openLast(): LoadedBoard? = repository.lastOpenedId()?.let { open(it) }

    suspend fun list(): List<BoardSummary> =
        repository.list().map { BoardSummary(it.toHandle(), it.updatedAt) }

    suspend fun delete(id: String) = repository.delete(id)

    /**
     * Imports a file chosen by the user as a NEW board. Accepts both a HyperKit
     * document and plain DrawBox JSON (e.g. exported from the DrawBox sample).
     * Returns null when the text is neither.
     */
    suspend fun importFile(text: String): LoadedBoard? {
        val payload = if (BoardDocument.isEnvelope(text)) {
            BoardDocument.fromJson(text)?.takeIf { it.engine == BoardDocument.ENGINE_DRAWBOX }?.enginePayload
        } else {
            text
        } ?: return null
        if (!isValidDrawBoxJson(payload)) return null
        val handle = newBoard()
        save(handle, payload)
        return LoadedBoard(handle, payload)
    }

    private fun BoardDocument.toHandle() = BoardHandle(id, title, createdAt)
    private fun BoardDocument.toLoaded() = LoadedBoard(toHandle(), enginePayload)
}
