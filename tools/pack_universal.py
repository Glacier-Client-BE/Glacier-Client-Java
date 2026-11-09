"""Packs the per-version Glacier jars into one jar that works on every supported Minecraft version.

The output is a thin Fabric mod whose only content is the per-version jars nested under META-INF/jars.
Fabric Loader reads all of them and loads the one whose "minecraft" dependency matches the running game.

Usage: python tools/pack_universal.py <out.jar> <version jar> [<version jar> ...]
"""
import json
import sys
import zipfile


def main():
    out, jars = sys.argv[1], sys.argv[2:]
    if not jars:
        sys.exit(__doc__)
    nested, version, icon = [], None, None
    for path in jars:
        with zipfile.ZipFile(path) as z:
            meta = json.loads(z.read("fabric.mod.json"))
            mc = meta["depends"]["minecraft"].lstrip("~>=^ ")
            version = version or meta["version"].split("+")[0]
            if icon is None and meta.get("icon"):
                icon = (meta["icon"], z.read(meta["icon"]))
        nested.append((f"META-INF/jars/glacier-client-{version}+mc{mc}.jar", path, mc))
    wrapper = {
        "schemaVersion": 1,
        "id": "glacier-universal",
        "version": version,
        "name": "Glacier Client",
        "description": "Glacier Client for Minecraft " + ", ".join(n[2] for n in nested) + " in one jar.",
        "authors": ["Pepe"],
        "license": "All-Rights-Reserved",
        "environment": "client",
        "jars": [{"file": n[0]} for n in nested],
        "depends": {"fabricloader": ">=0.19.0"},
        "custom": {"modmenu": {"badges": ["library"]}},
    }
    if icon:
        wrapper["icon"] = icon[0]
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        z.writestr("fabric.mod.json", json.dumps(wrapper, indent=2))
        if icon:
            z.writestr(icon[0], icon[1])
        for name, path, _ in nested:
            # nested jars are already compressed
            z.write(path, name, compress_type=zipfile.ZIP_STORED)
    print("wrote", out, "with", [n[2] for n in nested])


if __name__ == "__main__":
    main()
