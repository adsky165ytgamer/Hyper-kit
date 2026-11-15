# Whiteboard UPS: DrawBox

- Upstream: https://github.com/akshay2211/DrawBox  (Apache License 2.0, see LICENSE)
- Version: 2.1.0 (`DrawBox/gradle.properties`)
- Copied here: `DrawBox/` (engine) and `drawbox-ui/` (toolbars), **unmodified**.
- Deliberately not copied: androidApp, shared, desktopApp, iosApp, webApp, docs.
- Local patches: none. If one is ever needed, list it in `PATCHES.md` next to this file.
- Upgrading: replace the two folders with the newer upstream copy, update `DRAWBOX_VERSION`
  in `app/src/main/kotlin/com/hyperkit/integration/DrawBoxLink.kt`, and check `gradle/libs.versions.toml`
  against upstream's catalog.
