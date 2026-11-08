# Changelog

## 1.0.0

First release, rebuilt from scratch for Minecraft 1.21.11 (Fabric, Java 21, Mojang mappings).

- Module system with a persistent config (`config/glacier/config.json`, atomic writes, corrupt files backed up).
- HUD modules: FPS, CPS, coordinates, keystrokes, armor, potion timers, ping, memory and more, with a HUD editor.
- Zoom (C), Free Look (Left Alt), toggle sprint, fullbright, no hurt cam, low fire, no fog, weather and FOV changers.
- Cosmetics layer and Glacier-styled start, play, pause and progress screens (native Java screens).
- Right Shift opens the quick bar and the mod menu.
- Performance: low-end laptop preset (reversible), unfocused and menu FPS caps, particle limiter, recommended JVM arguments.
- Bundles Sodium, Lithium, FerriteCore and ImmediatelyFast (jar-in-jar, unmodified).
