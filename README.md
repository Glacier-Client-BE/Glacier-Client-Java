# Glacier Client

A Minecraft 1.21.11 client mod for Fabric: HUD modules, cosmetics, zoom, free look, custom menus and bundled
performance mods, tuned for low-end laptops. Client side only.

## Features
- 50 HUD modules (FPS, CPS, coordinates, keystrokes, armor, potions, ping, memory, ...) with a drag-and-drop editor
- Zoom, Free Look, toggle sprint, fullbright, no hurt cam, low fire, no fog, weather and FOV changers
- Cosmetics and a custom start, play, pause and loading screens
- Low-end laptop preset that can be switched off again, plus FPS caps for menus and unfocused windows
- Bundled: Sodium, Lithium, FerriteCore, ImmediatelyFast (see `src/main/resources/assets/glacier/THIRD_PARTY.md`)

## Install
1. Install Fabric Loader 0.19.0 or newer for Minecraft 1.21.11 and Java 21.
2. Put `fabric-api` and `glacier-client-1.0.0.jar` in your `mods` folder.
3. Optional: Mod Menu.

Remove standalone copies of Sodium, Lithium, FerriteCore or ImmediatelyFast, as Glacier ships its own. OptiFabric is not compatible.

## Build
Requires JDK 21 (Loom does not support newer JDKs).
```
export JAVA_HOME="$HOME/jdks/jdk-21.0.11+10"
./gradlew build        # jar in build/libs
./gradlew runClient    # dev client
```

## Keybinds (rebindable in Controls)
| Key | Action |
|-----|--------|
| Right Shift | Quick bar / mod menu |
| C | Zoom |
| Left Alt | Free look |

## Configuration
- `.minecraft/config/glacier/config.json` - module state and settings
- `.minecraft/config/glacier/performance.json` - `unfocusedFpsCap` (default 30, 0 disables)
- `.minecraft/config/glacier/perf-restore.json` - original options saved while the low-end preset is on

Recommended JVM arguments are shown on the Performance page (`-Xms1G -Xmx3G` with tuned G1).

## Troubleshooting
- Game crashes at startup on a wrong Java: use Java 21.
- Settings reset: an unreadable config is copied to `config.json.corrupt` and defaults load.
- Options stuck on low values after a crash: the next launch restores them from `perf-restore.json`; disable the preset on the Performance page.
- Duplicate Sodium/Lithium errors: delete the standalone jars from `mods`.
- Logs: `.minecraft/logs/latest.log`, search for `Glacier`.

## License
All rights reserved, see `LICENSE`.
