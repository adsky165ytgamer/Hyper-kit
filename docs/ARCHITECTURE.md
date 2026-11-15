# HyperKit — Architecture

## 1. What HyperKit is
HyperKit is an Android application ecosystem that grows feature by feature.
Milestone 1 is the application shell plus the first feature: a classroom
whiteboard. It is native Android, Kotlin, Jetpack Compose, with a
**Material 3 Expressive** design language.

## 2. The Three Shelves (two vocabularies, one structure)

| My terminology (HyperKit) | Traditional terminology | Where it lives now |
|---|---|---|
| **Frontend Shelf** | Presentation / UI layer | `app/src/main/kotlin/com/hyperkit/frontend/` |
| **Integration + API Shelf** | Integration / API layer | `app/src/main/kotlin/com/hyperkit/integration/` |
| **Backend Shelf** | Backend / Data layer | `app/src/main/kotlin/com/hyperkit/backend/` |
| **UPS** (Underlying Project Source) | Underlying project source / upstream library | `ups/` |
| **Whiteboard UPS** | DrawBox drawing foundation | `ups/drawbox/` |
| **Whiteboard feature** | Feature module (here: sub-package) | `frontend/whiteboard/`, `backend/whiteboard/` |

The Shelves are **packages inside one Gradle module (`:app`)**, not separate
modules. Nothing was split until a real requirement asks for it.

Dependency direction (each arrow = "may use"):

```
Frontend Shelf  ->  Integration + API Shelf  ->  Backend Shelf
      \                    |
       \-------------------+-->  Whiteboard UPS (DrawBox)
```

- Frontend Shelf never touches storage directly; it calls `BoardService`.
- Backend Shelf knows nothing about DrawBox; it stores the drawing as an opaque JSON string.
- DrawBox types (`DrawBoxController`, `DrawingSerializer`, …) are used only by
  `WhiteboardScreen.kt` and `integration/DrawBoxLink.kt`.

"Integration" and "API" share one Shelf: today it holds the board service and
the file-exchange (Android file pickers / MediaStore). Future external APIs
(plural) join here.

## 3. UPS — Underlying Project Source
A UPS is an existing open-source project HyperKit is built **around**, not
rebuilt. **DrawBox** (https://github.com/akshay2211/DrawBox, Apache-2.0,
v2.1.0) is the Whiteboard UPS.

### Why DrawBox
It already provides the hard parts: infinite canvas, pan/zoom, freehand,
shapes, text and image elements, selection and editing, undo/redo, JSON
serialization, SVG and PNG export, and a controller/state architecture.
HyperKit does not reimplement any of it.

### Where DrawBox is integrated
- `ups/drawbox/DrawBox/` — drawing engine (canvas composable, controller, reducer, serializers, exporters). **Unmodified.**
- `ups/drawbox/drawbox-ui/` — reusable toolbars (`ControlsBar`, `ContextBar`) and icons. **Unmodified.**
- `settings.gradle.kts` includes both as Gradle projects (`:DrawBox`, `:drawbox-ui`); `app/build.gradle.kts` depends on them.
- Not imported: DrawBox's demo apps (`androidApp`, `shared`, `desktopApp`, `iosApp`, `webApp`) and docs.
- The toolbar wiring in `WhiteboardScreen.kt` follows DrawBox's sample `HomeScreen`, trimmed (see FILE_REFERENCE).

## 4. How the app works today
1. `MainActivity` builds `BoardRepository` → `BoardService` and shows `HyperKitApp`.
2. `HyperKitApp` applies `HyperKitTheme` and shows `WhiteboardScreen` (the only destination; no navigation library yet).
3. `WhiteboardScreen` hosts the DrawBox canvas with DrawBox's tool bars. Pinch-zoom, pan, pen, shapes, text, images, selection, undo/redo all come from DrawBox.
4. **Autosave:** ~0.5 s after the drawing stops changing, the board is saved through `BoardService` to `files/boards/<id>.hyperkit.json`. Pan/zoom/selection do not trigger saves. An untouched new board is never written.
5. **Reopen:** the last open board loads at launch. Top-left title → *My boards* (open / delete). Menu → *New board*.
6. **Export/import:** menu → PNG, SVG, JSON export (to Pictures/Documents `HyperKit` folder), JSON import (becomes a new board), insert image.

## 5. Document format and future compatibility
DrawBox's own JSON (`SerializableDrawing`) has only `bgColor` and `elements`
and **no version field**. HyperKit does not change it. Instead the Backend
Shelf wraps it:

```json
{
  "hyperkit": { "format": 1, "id": "…", "title": "…", "createdAt": 0, "updatedAt": 0 },
  "engine":   { "name": "drawbox", "version": "2.1.0", "payload": "<DrawBox JSON as a string>" }
}
```

- `format` versions the envelope; `engine.name` + `engine.version` record which UPS wrote the payload → migrations can be written per engine version.
- The payload is stored as a string, byte-for-byte as DrawBox produced it.
- Import accepts both this envelope and plain DrawBox JSON.
- A board with a newer `format` than the app knows is still read (best effort).
- Export JSON writes plain DrawBox JSON (so DrawBox tools can open it).
- Not decided yet (needs your call later): cloud-sync conflict rules, multi-page boards, renaming boards.

## 6. Extending the whiteboard without modifying the UPS
- **New UI** (a toolbar button, a classroom shape pack): add composables under `frontend/whiteboard/` and call existing `DrawBoxController` functions (`setMode`, `insertImage`, `insertText`, `onIntent(...)`).
- **New persistence** (rename, folders, cloud): extend `BoardRepository` / `BoardDocument` (Backend Shelf) and expose it through `BoardService` (Integration + API Shelf).
- **New DrawBox features**: bump the sources under `ups/drawbox/` by replacing the two folders with a newer upstream copy; update `DRAWBOX_VERSION` in `DrawBoxLink.kt`.
- Only if DrawBox cannot do something, patch it — and record the patch in `ups/drawbox/PATCHES.md` (none today).
