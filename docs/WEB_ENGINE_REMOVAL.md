# Web engine removal: dependency report

Method: Grep over `src/main/java` for `net.glacier.web`, `org.mozilla` and the web screen class names.
`ScreenFactory.NATIVE = true` is a compile-time constant, so every `: new XxxWebScreen(...)` branch in
`ScreenFactory` is dead, but still compiles and keeps the classes reachable.

## Still used by the native path (do not delete as-is)
| Live reference | Needs |
|---|---|
| `NativePlayScreen`, `NativeProgressScreen`, `NativeQuickBar`, `NativeStartScreen` import `web.RenderUtil` (text, images, ellipsize, FA font) | Move `RenderUtil` (it references `Node` and `Css.Gradient`, so extract those few helpers or keep those two types) to `ui/` first |
| `NativeStartScreen` uses `StartApi.DISCORD` | Inline the constant |
| `NativePlayScreen` / `NativeProgressScreen` call `WebPage.drawPanorama(...)` | Move panorama drawing into `Draw` or a native helper |
| `NativePauseScreen` line ~210 creates `new StartScreen()` (web) as the multiplayer return screen. This is a live bug: after disconnecting from a server the web start screen opens instead of `ScreenFactory.start()` | Replace with `ScreenFactory.start()` |
| `MixinScreenSwap` imports `ProgressWebScreen` | Import is unused (only occurrence), drop it |

## Dead once the above are moved (safe follow-up deletion)
- `ui/screen`: `WebScreen`, `WebPage`, `StartScreen`, `PlayWebScreen`, `ProgressWebScreen`, `PauseWebScreen`, `HudQuickScreen`
- `web/`: `AbsoluteLayout, Css, CssApply, CssIndex, CssParser, CssProps, CssSelectorMatch, CssValues, FlexLayout, GridLayout, HtmlParser, InlineLayout, JsBridge, Layout, LayoutCache, LayoutContext, LayoutSizes, Node, ScreenApi, StartApi, WebApi, WebView` (except what `RenderUtil` still needs: `Node`, `Css.Gradient`)
- Resources: `src/main/resources/assets/glacier/web/**` (html, css, js, pages/*.js)
- Dependency: `include implementation("org.mozilla:rhino:1.7.15")` in `build.gradle`. Only `JsBridge`, `ScreenApi`, `StartApi`, `WebApi` import `org.mozilla`. Removing it shrinks the jar.
- `ScreenFactory`: remove the `NATIVE` flag and the web branches.
- Comments mentioning `WebPage`/`WebScreen`/`ScreenApi`/`WebApi` in the Native* screens and `pages/*` are stale after removal.
- `GlacierClient` imported `WebScreen` unused; that import was already removed.
- Accessor mixins' javadoc mentions `ProgressWebScreen` (comment only).

## Suggested order
1. Relocate `RenderUtil`, `DISCORD`, `drawPanorama`; fix the `new StartScreen()` calls.
2. Delete the web screens, `web/` and `assets/glacier/web`, remove Rhino, drop `NATIVE`.
3. Build and smoke-test the start, play, pause, progress, quick bar and menu screens.
