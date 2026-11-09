"""Starts an installed Fabric profile straight from .minecraft (offline, throwaway game dir) to smoke-test a jar.

Usage: python tools/launch_test.py <profile id, e.g. fabric-loader-0.20.1-26.3> <java.exe> <game dir>
Prints the PID and returns; the game log is <game dir>/logs/latest.log.
"""
import json
import os
import subprocess
import sys

MC = os.path.join(os.environ["APPDATA"], ".minecraft")


def load(vid):
    with open(os.path.join(MC, "versions", vid, vid + ".json"), encoding="utf-8") as f:
        return json.load(f)


def allowed(lib):
    rules = lib.get("rules")
    if not rules:
        return True
    ok = False
    for r in rules:
        os_name = r.get("os", {}).get("name")
        if os_name is None or os_name == "windows":
            ok = r["action"] == "allow"
    return ok


_CACHE = None


def gradle_cache():
    """File name -> path for every jar Gradle has downloaded (the launcher may keep its libraries elsewhere)."""
    global _CACHE
    if _CACHE is None:
        _CACHE = {}
        root = os.path.join(os.path.expanduser("~"), ".gradle", "caches", "modules-2", "files-2.1")
        for d, _, files in os.walk(root):
            for f in files:
                if f.endswith(".jar"):
                    _CACHE.setdefault(f, os.path.join(d, f))
    return _CACHE


def lib_path(lib):
    art = lib.get("downloads", {}).get("artifact")
    if art and art.get("path"):
        return os.path.join(MC, "libraries", art["path"].replace("/", os.sep))
    parts = lib["name"].split(":")
    group, name, ver = parts[0], parts[1], parts[2]
    classifier = "-" + parts[3] if len(parts) > 3 else ""
    return os.path.join(MC, "libraries", *group.split("."), name, ver, f"{name}-{ver}{classifier}.jar")


def main():
    profile, java, game_dir = sys.argv[1:4]
    child = load(profile)
    parent = load(child["inheritsFrom"])
    cp, seen = [], set()
    for lib in child.get("libraries", []) + parent.get("libraries", []):
        if not allowed(lib):
            continue
        key = ":".join(lib["name"].split(":")[:2] + lib["name"].split(":")[3:])
        if key in seen:
            continue
        seen.add(key)
        p = lib_path(lib)
        if not os.path.exists(p):
            p = gradle_cache().get(os.path.basename(p))
            if p is None:
                print("missing library", lib["name"])
                continue
        cp.append(p)
    client = os.path.join(MC, "versions", parent["id"], parent["id"] + ".jar")
    if not os.path.exists(client):
        client = os.path.join(os.path.expanduser("~"), ".gradle", "caches", "fabric-loom", parent["id"], "minecraft-client.jar")
    cp.append(client)
    os.makedirs(game_dir, exist_ok=True)
    index = parent["assetIndex"]["id"]
    assets = os.path.join(MC, "assets")
    if not os.path.exists(os.path.join(assets, "indexes", index + ".json")):
        assets = os.path.join(os.path.expanduser("~"), ".gradle", "caches", "fabric-loom", "assets")
    cmd = [java, "-Xmx2G", "-cp", os.pathsep.join(cp), child["mainClass"],
           "--version", profile, "--gameDir", game_dir, "--assetsDir", assets,
           "--assetIndex", index, "--accessToken", "0", "--username", "GlacierTest",
           "--width", "854", "--height", "480"]
    log = open(os.path.join(game_dir, "stdout.log"), "w")
    p = subprocess.Popen(cmd, cwd=game_dir, stdout=log, stderr=subprocess.STDOUT)
    print("pid", p.pid)


if __name__ == "__main__":
    main()
