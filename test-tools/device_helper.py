"""Small adb helper used for the DDL Saver 1.0.0 device acceptance run.

It only drives the phone through public adb commands: screenshots, UI hierarchy dumps,
taps located by visible text, text input and key events. Nothing here reads app data.

Usage (from the repository root, with adb on PATH or ADB set):
    python test-tools/device_helper.py shot 01-home              # save <out>/screenshots/01-home.jpg
    python test-tools/device_helper.py texts                     # list visible texts with centres
    python test-tools/device_helper.py tap-text 保存事项         # tap the first node whose text matches
    python test-tools/device_helper.py tap-text 设置 --contains --index 1
    python test-tools/device_helper.py wait-text 事项详情 15     # wait up to 15 s
    python test-tools/device_helper.py tap 600 1200 | text QA-A | key BACK | swipe x1 y1 x2 y2 [ms]

Environment: ADB (path to adb), ANDROID_SERIAL (device), EVIDENCE_DIR (output folder).
On Windows with HONOR phones bound to the generic WinUSB driver, ADB_LIBUSB=1 is set
automatically so adb can see the device without a vendor driver.
"""
from __future__ import annotations

import io
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

ADB = os.environ.get("ADB", "adb")
OUT = os.environ.get("EVIDENCE_DIR", ".")
ENV = dict(os.environ, ADB_LIBUSB=os.environ.get("ADB_LIBUSB", "1"))


def adb(*args: str, binary: bool = False, check: bool = True):
    cmd = [ADB] + (["-s", os.environ["ANDROID_SERIAL"]] if os.environ.get("ANDROID_SERIAL") else []) + list(args)
    result = subprocess.run(cmd, capture_output=True, env=ENV)
    if check and result.returncode != 0:
        raise SystemExit(f"adb {' '.join(args)} failed: {result.stderr.decode('utf-8', 'replace').strip()}")
    return result.stdout if binary else result.stdout.decode("utf-8", "replace")


def screenshot(name: str, width: int = 720, quality: int = 82) -> str:
    """Capture the screen and store a resized JPEG so evidence stays small in Git."""
    from PIL import Image  # Pillow is only needed for screenshots

    png = adb("exec-out", "screencap", "-p", binary=True)
    image = Image.open(io.BytesIO(png)).convert("RGB")
    if image.width > width:
        image = image.resize((width, round(image.height * width / image.width)), Image.LANCZOS)
    folder = os.path.join(OUT, "screenshots")
    os.makedirs(folder, exist_ok=True)
    path = os.path.join(folder, f"{name}.jpg")
    image.save(path, "JPEG", quality=quality, optimize=True)
    return path


def nodes() -> list[dict]:
    """Dump the current UI hierarchy; Compose text is exposed through accessibility nodes."""
    for attempt in range(4):
        out = adb("shell", "uiautomator", "dump", "/sdcard/ddl_ui.xml", check=False)
        if "dumped to" in out:
            break
        time.sleep(0.7)
    raw = adb("exec-out", "cat", "/sdcard/ddl_ui.xml", binary=True)
    root = ET.fromstring(raw.decode("utf-8", "replace"))
    found = []
    for node in root.iter("node"):
        bounds = re.findall(r"\d+", node.get("bounds", ""))
        if len(bounds) != 4:
            continue
        x1, y1, x2, y2 = map(int, bounds)
        label = node.get("text") or node.get("content-desc") or ""
        found.append({"text": label, "x": (x1 + x2) // 2, "y": (y1 + y2) // 2,
                      "bounds": (x1, y1, x2, y2), "clickable": node.get("clickable") == "true",
                      "checked": node.get("checked") == "true", "selected": node.get("selected") == "true"})
    return found


def find(text: str, contains: bool = False) -> list[dict]:
    return [n for n in nodes() if n["text"] and ((text in n["text"]) if contains else n["text"] == text)]


def tap_text(text: str, contains: bool = False, index: int = 0) -> dict:
    matches = find(text, contains)
    if len(matches) <= index:
        raise SystemExit(f"text not found: {text!r} (matches={len(matches)})")
    target = matches[index]
    adb("shell", "input", "tap", str(target["x"]), str(target["y"]))
    return target


def wait_text(text: str, timeout: float = 10.0, contains: bool = False) -> bool:
    deadline = time.time() + timeout
    while time.time() < deadline:
        if find(text, contains):
            return True
        time.sleep(0.8)
    return False


def type_text(value: str) -> None:
    """adb input only supports ASCII; spaces must be escaped."""
    if not value.isascii():
        raise SystemExit("adb input text supports ASCII only")
    adb("shell", "input", "text", value.replace(" ", "%s"))


def main(argv: list[str]) -> None:
    if not argv:
        print(__doc__)
        return
    cmd, args = argv[0], argv[1:]
    contains = "--contains" in args
    index = int(args[args.index("--index") + 1]) if "--index" in args else 0
    plain = [a for i, a in enumerate(args) if not a.startswith("--") and (i == 0 or args[i - 1] != "--index")]
    if cmd == "shot":
        print(screenshot(plain[0]))
    elif cmd == "texts":
        for n in nodes():
            if n["text"]:
                flags = "".join(f for f, on in (("C", n["clickable"]), ("✓", n["checked"]), ("S", n["selected"])) if on)
                print(f"{n['x']:5d},{n['y']:5d} {flags:3s} {n['text']}")
    elif cmd == "find":
        for n in find(plain[0], contains):
            print(n)
    elif cmd == "tap-text":
        print(tap_text(plain[0], contains, index))
    elif cmd == "wait-text":
        ok = wait_text(plain[0], float(plain[1]) if len(plain) > 1 else 10.0, contains)
        print("found" if ok else "timeout")
        sys.exit(0 if ok else 1)
    elif cmd == "tap":
        adb("shell", "input", "tap", plain[0], plain[1])
    elif cmd == "swipe":
        adb("shell", "input", "swipe", *plain[:4], plain[4] if len(plain) > 4 else "300")
    elif cmd == "text":
        type_text(plain[0])
    elif cmd == "key":
        adb("shell", "input", "keyevent", f"KEYCODE_{plain[0].upper()}")
    else:
        raise SystemExit(f"unknown command {cmd}")


if __name__ == "__main__":
    main(sys.argv[1:])
