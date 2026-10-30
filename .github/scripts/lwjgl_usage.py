"""Finds the LWJGL methods that applications call but the patched LWJGL only implements by a guess.

The behaviour catalog patches every LWJGL method by the first matching rule. Rules that only express a guess, like
"return an empty buffer", list each method they resolve in the lock files. This scans the bytecode of applications,
e.g. Minecraft and its mod loaders, for calls to LWJGL methods, resolves each call to the declaring class in the
patched jars, like the JVM does, and reports the calls that end up at a guessed method: those need a reviewed
behaviour, or an entry in the allowlist after review.

Usage: lwjgl_usage.py --locks <catalog dir>... --patched <patched jars dir> [--allow <allowlist>] <jar or dir>...
Exits with 1 if a guessed method is called that is not allowlisted.
"""

import argparse
import struct
import sys
import zipfile
from pathlib import Path

LWJGL = ("org/lwjgl/",)
# the rules of Rules.java that only guess, which list every method they resolve in the lock files; other rules list a
# method only if it is a shim of an older LWJGL version
GUESSING_RULES = {"default", "object", "interface-proxy", "nio-buffer-empty", "lwjgl-buffer-empty"}
METHOD_REFS = (10, 11)  # CONSTANT_Methodref, CONSTANT_InterfaceMethodref


def constant_pool(data):
    """Returns the method references and the class name and super class of a class file."""
    count = struct.unpack_from(">H", data, 8)[0]
    entries = [None] * count
    offset = 10
    index = 1
    while index < count:
        tag = data[offset]
        if tag == 1:
            length = struct.unpack_from(">H", data, offset + 1)[0]
            entries[index] = (1, data[offset + 3:offset + 3 + length].decode("utf-8", "replace"))
            offset += 3 + length
        elif tag in (3, 4):
            offset += 5
        elif tag in (5, 6):
            offset += 9
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            entries[index] = (tag, struct.unpack_from(">H", data, offset + 1)[0])
            offset += 3
        elif tag in (9, 10, 11, 12, 17, 18):
            entries[index] = (tag, struct.unpack_from(">HH", data, offset + 1))
            offset += 5
        elif tag == 15:
            offset += 4
        else:
            raise ValueError(f"unknown constant pool tag {tag}")
        index += 1

    def utf8(i):
        return entries[i][1]

    def class_name(i):
        return utf8(entries[i][1])

    refs = set()
    for entry in entries:
        if entry and entry[0] in METHOD_REFS:
            class_index, name_and_type = entry[1]
            name_index, descriptor_index = entries[name_and_type][1]
            refs.add((class_name(class_index), utf8(name_index) + utf8(descriptor_index)))

    access_offset = offset
    this_class = class_name(struct.unpack_from(">H", data, access_offset + 2)[0])
    super_index = struct.unpack_from(">H", data, access_offset + 4)[0]
    super_class = class_name(super_index) if super_index else None
    interface_count = struct.unpack_from(">H", data, access_offset + 6)[0]
    interfaces = [class_name(struct.unpack_from(">H", data, access_offset + 8 + 2 * i)[0]) for i in range(interface_count)]
    return refs, this_class, super_class, interfaces


def class_files(path):
    """Yields (name, bytes) of every class file in a jar, a directory of jars, or a single jar, nested jars included."""
    path = Path(path).expanduser()
    if not path.exists():
        return
    if path.is_dir():
        for jar in sorted(path.rglob("*.jar")):
            yield from class_files(jar)
        return

    try:
        with zipfile.ZipFile(path) as jar:
            yield from zip_classes(jar, str(path))
    except zipfile.BadZipFile:
        return


def zip_classes(jar, origin):
    import io
    for name in jar.namelist():
        if name.endswith(".class") and not name.endswith("module-info.class"):
            yield origin, jar.read(name)
        elif name.endswith(".jar"):
            try:
                with zipfile.ZipFile(io.BytesIO(jar.read(name))) as nested:
                    yield from zip_classes(nested, origin + "!" + name)
            except zipfile.BadZipFile:
                continue


def methods_of(data):
    """Returns the names+descriptors of the methods declared by a class file."""
    _, _, _, _ = constant_pool(data)
    # re-parse to reach the methods table: skip fields first
    count = struct.unpack_from(">H", data, 8)[0]
    offset = 10
    index = 1
    utf8 = {}
    while index < count:
        tag = data[offset]
        if tag == 1:
            length = struct.unpack_from(">H", data, offset + 1)[0]
            utf8[index] = data[offset + 3:offset + 3 + length].decode("utf-8", "replace")
            offset += 3 + length
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):
            offset += 5
        elif tag in (5, 6):
            offset += 9
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            offset += 3
        elif tag == 15:
            offset += 4
        index += 1

    offset += 6
    interface_count = struct.unpack_from(">H", data, offset)[0]
    offset += 2 + 2 * interface_count

    def skip_members(offset):
        members = []
        member_count = struct.unpack_from(">H", data, offset)[0]
        offset += 2
        for _ in range(member_count):
            _, name, descriptor, attribute_count = struct.unpack_from(">HHHH", data, offset)
            members.append(utf8[name] + utf8[descriptor])
            offset += 8
            for _ in range(attribute_count):
                length = struct.unpack_from(">I", data, offset + 2)[0]
                offset += 6 + length
        return members, offset

    _, offset = skip_members(offset)
    methods, _ = skip_members(offset)
    return set(methods)


def load_hierarchy(patched):
    """Maps every class of the patched jars to its super types and declared methods."""
    hierarchy = {}
    for _, data in class_files(patched):
        _, name, super_class, interfaces = constant_pool(data)
        hierarchy[name] = ([c for c in [super_class] + interfaces if c], methods_of(data))
    return hierarchy


def resolve(hierarchy, owner, method):
    """The class declaring the method, searching super classes and interfaces like the JVM."""
    pending = [owner]
    seen = set()
    while pending:
        current = pending.pop(0)
        if current in seen or current not in hierarchy:
            continue
        seen.add(current)
        supers, methods = hierarchy[current]
        if method in methods:
            return current
        pending.extend(supers)
    return None


def load_guesses(lock_directories):
    """Maps owner -> {method: behaviour} of every method a guessing rule resolved, from the lock files."""
    guesses = {}
    for directory in lock_directories:
        for lock in sorted(Path(directory).glob("*.lock")):
            owner = None
            for line in lock.read_text().splitlines():
                if line.startswith("    ") and owner:
                    method, _, behaviour = line.strip().partition(" ")
                    rule = behaviour.split(":", 1)[0].split(" ", 1)[0]
                    if rule in GUESSING_RULES:
                        guesses.setdefault(owner, {})[method] = behaviour
                elif line and not line[0].isspace() and "/" in line.split(" ")[0]:
                    owner = line.split(" ")[0]
    return guesses


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--locks", nargs="+", required=True)
    parser.add_argument("--patched", required=True)
    parser.add_argument("--allow")
    parser.add_argument("inputs", nargs="+")
    args = parser.parse_args()

    hierarchy = load_hierarchy(args.patched)
    guesses = load_guesses(args.locks)
    allowed = set()
    if args.allow:
        for line in Path(args.allow).read_text().splitlines():
            line = line.split("#", 1)[0].strip()
            if line:
                allowed.add(line)

    found = {}
    for path in args.inputs:
        for origin, data in class_files(path):
            try:
                refs, name, _, _ = constant_pool(data)
            except (ValueError, struct.error, IndexError, KeyError, TypeError):
                continue
            if name.startswith(LWJGL):
                continue
            for owner, method in refs:
                if not owner.startswith(LWJGL):
                    continue
                declaring = resolve(hierarchy, owner, method)
                behaviour = guesses.get(declaring, {}).get(method) if declaring else None
                if behaviour is not None:
                    found.setdefault(f"{declaring}.{method}", (behaviour, set()))[1].add(name)

    unreviewed = 0
    for key in sorted(found):
        behaviour, callers = found[key]
        status = "allowed" if key in allowed else "GUESSED"
        if key not in allowed:
            unreviewed += 1
            print(f"::error::{key} {behaviour} is called by {', '.join(sorted(callers)[:3])}"
                  f"{' and more' if len(callers) > 3 else ''}")
        else:
            print(f"{status} {key} {behaviour}")

    print(f"{len(found)} guessed LWJGL methods are called, {unreviewed} are not reviewed")
    sys.exit(1 if unreviewed else 0)


if __name__ == "__main__":
    main()
