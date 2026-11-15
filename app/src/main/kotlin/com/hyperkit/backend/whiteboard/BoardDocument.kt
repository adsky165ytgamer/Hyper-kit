package com.hyperkit.backend.whiteboard

import org.json.JSONException
import org.json.JSONObject

/**
 * Backend Shelf → HyperKit whiteboard document.
 * Traditional terminology → Data layer model + file format.
 *
 * A saved board is a small versioned envelope around the drawing engine's own
 * JSON. The engine payload is kept as an opaque string, so:
 *  - the Backend Shelf knows nothing about DrawBox types,
 *  - the engine JSON is stored byte-for-byte (no re-formatting, no data loss),
 *  - future migrations can switch on [formatVersion], [engine] and [engineVersion].
 *
 * File shape (one JSON object):
 * ```
 * {
 *   "hyperkit": { "format": 1, "id": "...", "title": "...", "createdAt": 0, "updatedAt": 0 },
 *   "engine":   { "name": "drawbox", "version": "2.1.0", "payload": "<DrawBox JSON as a string>" }
 * }
 * ```
 */
data class BoardDocument(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val engine: String,
    val engineVersion: String,
    val enginePayload: String,
    val formatVersion: Int = FORMAT_VERSION,
) {
    fun toJson(): String {
        val header = JSONObject()
            .put("format", formatVersion)
            .put("id", id)
            .put("title", title)
            .put("createdAt", createdAt)
            .put("updatedAt", updatedAt)
        val engineObj = JSONObject()
            .put("name", engine)
            .put("version", engineVersion)
            .put("payload", enginePayload)
        return JSONObject()
            .put(KEY_HEADER, header)
            .put(KEY_ENGINE, engineObj)
            .toString(2)
    }

    companion object {
        /** Bump when the envelope itself changes shape. */
        const val FORMAT_VERSION = 1
        const val ENGINE_DRAWBOX = "drawbox"
        private const val KEY_HEADER = "hyperkit"
        private const val KEY_ENGINE = "engine"

        /** True when [text] looks like a HyperKit envelope (as opposed to raw engine JSON). */
        fun isEnvelope(text: String): Boolean = try {
            JSONObject(text).has(KEY_HEADER)
        } catch (_: JSONException) {
            false
        }

        /**
         * Parses an envelope. Returns null when [text] is not a valid envelope.
         * A newer [formatVersion] than this build knows is still returned
         * (best-effort read); callers decide what to do with it.
         */
        fun fromJson(text: String): BoardDocument? = try {
            val root = JSONObject(text)
            val header = root.getJSONObject(KEY_HEADER)
            val engineObj = root.getJSONObject(KEY_ENGINE)
            BoardDocument(
                id = header.getString("id"),
                title = header.optString("title", ""),
                createdAt = header.optLong("createdAt", 0L),
                updatedAt = header.optLong("updatedAt", 0L),
                engine = engineObj.getString("name"),
                engineVersion = engineObj.optString("version", ""),
                enginePayload = engineObj.getString("payload"),
                formatVersion = header.optInt("format", FORMAT_VERSION),
            )
        } catch (_: JSONException) {
            null
        }
    }
}
