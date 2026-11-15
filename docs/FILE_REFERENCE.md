# HyperKit — File and directory reference

## Directory map

| Path | My terminology | Traditional terminology |
|---|---|---|
| `app/` | HyperKit application | Android app module (`:app`) |
| `app/src/main/kotlin/com/hyperkit/frontend/` | Frontend Shelf | Presentation layer |
| `app/src/main/kotlin/com/hyperkit/frontend/whiteboard/` | Frontend Shelf → Whiteboard feature | Whiteboard screen + components |
| `app/src/main/kotlin/com/hyperkit/integration/` | Integration + API Shelf | Integration layer / adapters |
| `app/src/main/kotlin/com/hyperkit/backend/` | Backend Shelf | Data layer |
| `ups/drawbox/` | Whiteboard UPS | DrawBox library sources (unmodified) |
| `docs/` | — | Documentation |

## Important files

Filename: "MainActivity.kt"
My terminology: HyperKit entry point → wires the three Shelves.
Traditional terminology: Android Activity / composition root.
Responsibility: Creates `BoardRepository` and `BoardService` by hand (no DI framework) and shows `HyperKitApp`.

Filename: "HyperKitApp.kt"
My terminology: Frontend Shelf → application shell.
Traditional terminology: Presentation Layer → root composable / navigation host.
Responsibility: Applies the theme and shows the Whiteboard feature, the only destination so far.

Filename: "HyperKitTheme.kt"
My terminology: Frontend Shelf → design language.
Traditional terminology: Presentation Layer → Compose theme.
Responsibility: Material 3 Expressive theme (expressive motion, dynamic colour on Android 12+).

Filename: "WhiteboardScreen.kt"
My terminology: Frontend Shelf → Whiteboard feature.
Traditional terminology: Presentation Layer → Whiteboard Screen.
Responsibility: Displays the DrawBox canvas and toolbars, connects them to the DrawBox controller, runs autosave/reopen through `BoardService`, handles exports and inline text editing. Toolbar wiring is adapted from DrawBox's sample `HomeScreen`.

Filename: "WhiteboardChrome.kt"
My terminology: Frontend Shelf → Whiteboard feature.
Traditional terminology: Presentation Layer → stateless UI component.
Responsibility: Top bar with board title, zoom controls and menu button.

Filename: "BoardSheets.kt"
My terminology: Frontend Shelf → Whiteboard feature.
Traditional terminology: Presentation Layer → bottom sheets.
Responsibility: Board menu (new, export, import, insert image, grid, clear) and the My boards list.

Filename: "BoardService.kt"
My terminology: Integration + API Shelf → board service.
Traditional terminology: Application service between Presentation and Data layers.
Responsibility: The single door the Frontend Shelf uses to save, open, list, delete and import boards; future cloud/API hooks plug in here.

Filename: "DrawBoxLink.kt"
My terminology: Integration + API Shelf → Whiteboard UPS bridge.
Traditional terminology: Adapter around a third-party library.
Responsibility: Converts the DrawBox controller state to/from DrawBox JSON; records the integrated DrawBox version.

Filename: "FileExchange.kt"
My terminology: Integration + API Shelf → file exchange.
Traditional terminology: Platform I/O adapter (MediaStore + Storage Access Framework).
Responsibility: Saves PNG/SVG/JSON exports, picks JSON and image files. Derived from DrawBox's sample `AndroidImageSaver`.

Filename: "BoardDocument.kt"
My terminology: Backend Shelf → document format.
Traditional terminology: Data layer → model + serialization.
Responsibility: The versioned HyperKit board envelope around the opaque DrawBox JSON.

Filename: "BoardRepository.kt"
My terminology: Backend Shelf → board storage.
Traditional terminology: Data layer → file-based repository.
Responsibility: Atomic read/write/list/delete of board files in `files/boards/` and remembers the last opened board.

Filename: "settings.gradle.kts"
My terminology: Project wiring → where the Whiteboard UPS is plugged in.
Traditional terminology: Gradle settings.
Responsibility: Includes `:app`, `:DrawBox`, `:drawbox-ui` (the latter two mapped to `ups/drawbox/`).

Filename: "app/build.gradle.kts"
My terminology: HyperKit application build.
Traditional terminology: Android application module build script.
Responsibility: Dependencies, SDK/JDK targets, and the task that copies drawbox-ui toolbar icon resources into the app's assets (same workaround DrawBox's sample uses).

Filename: "gradle/libs.versions.toml"
My terminology: Toolchain pin.
Traditional terminology: Gradle version catalog.
Responsibility: DrawBox's catalog, verbatim, plus one added line (`kotlinx-coroutines-android`).
