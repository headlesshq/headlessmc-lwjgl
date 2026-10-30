"""Checks that HeadlessMc replaces every LWJGL classes jar of the given Minecraft versions with a patched jar.

Mirrors PatchedLwjglPatcher.patchedJar of HeadlessMc: every jar of an LWJGL artifact, with or without a classifier like
lwjgl-3.4.1-unsafe.jar, is replaced by the patched jar of the same module, except jars of native libraries. A library
that maps to no patched jar but contains classes would stay on the classpath unpatched and load native libraries, or
mix with the patched core, so it fails the check, before any Minecraft version is launched. Jars without classes, like
the placeholder of LWJGL 2's natives artifact lwjgl-platform, are harmless.

Usage: check_lwjgl_coverage.py <patched directory> <Minecraft version>...
"""

import io
import json
import sys
import urllib.request
import zipfile
from pathlib import Path

MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json"


def fetch(url):
    with urllib.request.urlopen(url, timeout=60) as response:
        return json.load(response)


def has_classes(url):
    with urllib.request.urlopen(url, timeout=60) as response:
        with zipfile.ZipFile(io.BytesIO(response.read())) as jar:
            return any(name.endswith(".class") for name in jar.namelist())


def patched_jar(group, artifact, version):
    """The patched jar replacing the classes jar of an LWJGL artifact, or None if HeadlessMc would keep it."""
    if version.startswith("3.") and group == "org.lwjgl" and (artifact == "lwjgl" or artifact.startswith("lwjgl-")):
        return f"lwjgl3/{artifact}.jar"
    if version.startswith("2.") and group == "org.lwjgl.lwjgl" and artifact in ("lwjgl", "lwjgl_util"):
        return f"lwjgl2/{artifact}.jar"
    return None


def classes_jars(library):
    """The coordinates of the classes jars of a library, without the jars of native libraries."""
    parts = library["name"].split(":")
    group, artifact, version = parts[:3]
    classifier = parts[3] if len(parts) > 3 else None
    if classifier is not None and classifier.startswith("natives"):
        return []
    # LWJGL 2 and old LWJGL 3 list native jars as classifiers of an entry, the entry itself may have no classes jar
    if classifier is None and "natives" in library and "artifact" not in library.get("downloads", {}):
        return []
    return [(group, artifact, version, classifier)]


def main():
    patched = Path(sys.argv[1])
    versions = sorted(set(sys.argv[2:]))
    urls = {entry["id"]: entry["url"] for entry in fetch(MANIFEST)["versions"]}
    errors = []
    for mc in versions:
        if mc not in urls:
            errors.append(f"{mc}: not in the version manifest")
            continue

        for library in fetch(urls[mc])["libraries"]:
            if not library["name"].startswith("org.lwjgl"):
                continue

            for group, artifact, version, classifier in classes_jars(library):
                coordinates = ":".join(filter(None, (group, artifact, version, classifier)))
                jar = patched_jar(group, artifact, version)
                if jar is None:
                    url = library.get("downloads", {}).get("artifact", {}).get("url")
                    if url is None or has_classes(url):
                        errors.append(f"{mc}: {coordinates} is not replaced by HeadlessMc, it stays unpatched")
                elif not (patched / jar).is_file():
                    errors.append(f"{mc}: {coordinates} has no patched jar {jar}, add the module to the patched LWJGL")

    for error in errors:
        print(f"::error::{error}")

    print(f"Checked the LWJGL libraries of {len(versions)} Minecraft versions: {len(errors)} errors")
    sys.exit(1 if errors else 0)


if __name__ == "__main__":
    main()
