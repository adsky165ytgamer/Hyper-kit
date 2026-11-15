package com.hyperkit.integration

import io.ak1.drawbox.domain.model.DrawingSerializer
import io.ak1.drawbox.domain.model.PayLoad
import io.ak1.drawbox.presentation.viewmodel.DrawBoxController

/**
 * Integration + API Shelf -> the narrow bridge to the Whiteboard UPS (DrawBox).
 * Traditional terminology -> Adapter / anti-corruption layer.
 *
 * Only this file and the whiteboard screen import DrawBox types. Everything
 * the Backend Shelf receives is a plain JSON string.
 */

/** Version of the DrawBox sources under ups/drawbox (DrawBox/gradle.properties: VERSION_NAME). */
const val DRAWBOX_VERSION = "2.1.0"

/** Current board as DrawBox JSON (background colour + elements). */
fun DrawBoxController.toDrawBoxJson(): String {
    val s = state.value
    return DrawingSerializer.serialize(PayLoad(bgColor = s.bgColor, elements = s.elements))
}

/** Replaces the current board with [json]. DrawBox emits an Error event if it cannot parse it. */
fun DrawBoxController.loadDrawBoxJson(json: String) = importPath(json)

/** True when [json] is parseable DrawBox JSON. */
fun isValidDrawBoxJson(json: String): Boolean = try {
    DrawingSerializer.deserialize(json)
    true
} catch (_: Exception) {
    false
}
