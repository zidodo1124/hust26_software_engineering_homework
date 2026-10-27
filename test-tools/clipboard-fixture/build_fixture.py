"""Build an isolated QA APK offline; never install it or alter the product build."""
from datetime import datetime, timezone
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import subprocess
import xml.etree.ElementTree as ET
import zipfile

BASE = Path(__file__).resolve().parent
WORKSPACE = next(path for path in BASE.parents if (path / "settings.gradle.kts").is_file())
EXPECTED_ASSETS = {
    "QA1027-blue-landscape.png", "QA1027-orange-portrait.png",
    "QA1027-exif-rotate90.jpg", "QA1027-broken.png",
}
PACKAGE = "cn.edu.hust.ddlsaver.qa.clipboard"

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--sdk", type=Path, help="Android SDK root; otherwise environment or workspace tools")
parser.add_argument("--jdk", type=Path, help="JDK 17+ root; otherwise environment or workspace tools")
parser.add_argument("--build-tools", default="35.0.0", help="Installed Build Tools version (tested: 35.0.0)")
args = parser.parse_args()

SDK = args.sdk or Path(os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
                       or WORKSPACE / ".local-tools/android-sdk")
local_jdks = sorted((WORKSPACE / ".local-tools").glob("jdk-17*"))
configured_jdk = args.jdk or os.environ.get("JAVA_HOME") or (local_jdks[-1] if local_jdks else None)
if configured_jdk is None:
    raise SystemExit("JDK not found. Set JAVA_HOME or pass --jdk with an existing JDK 17+ directory.")
JDK = Path(configured_jdk).resolve()
SDK = SDK.resolve()
BUILD_TOOLS = SDK / "build-tools" / args.build_tools
ANDROID_JAR = SDK / "platforms/android-35/android.jar"
EXE = ".exe" if os.name == "nt" else ""
SCRIPT = ".bat" if os.name == "nt" else ""
tools = {
    "javac": JDK / "bin" / ("javac" + EXE),
    "keytool": JDK / "bin" / ("keytool" + EXE),
    "aapt2": BUILD_TOOLS / ("aapt2" + EXE),
    "d8": BUILD_TOOLS / ("d8" + SCRIPT),
    "zipalign": BUILD_TOOLS / ("zipalign" + EXE),
    "apksigner": BUILD_TOOLS / ("apksigner" + SCRIPT),
}
for required in [ANDROID_JAR, *tools.values()]:
    if not required.is_file():
        raise SystemExit(f"Required existing build dependency is unavailable: {required}")

# The entire output tree is already ignored by the repository's .local-tools/ rule.
ARTIFACT_ROOT = WORKSPACE / ".local-tools/phone-functional/clipboard-fixture"
BUILD = ARTIFACT_ROOT / "repro-builds" / datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S-%f")
BUILD.mkdir(parents=True)
CLASSES, DEX = BUILD / "classes", BUILD / "dex"
CLASSES.mkdir()
DEX.mkdir()
ENV = os.environ.copy()
ENV["JAVA_HOME"] = str(JDK)
ENV["PATH"] = str(JDK / "bin") + os.pathsep + ENV.get("PATH", "")
command_logs = []

def run(command):
    command = [str(value) for value in command]
    completed = subprocess.run(command, cwd=BASE, env=ENV, capture_output=True, text=True,
                               encoding="utf-8", errors="replace", timeout=90)
    command_logs.append({"tool": Path(command[0]).name, "exitCode": completed.returncode,
                         "stdout": completed.stdout, "stderr": completed.stderr})
    if completed.returncode != 0:
        (BUILD / "build-failure.json").write_text(
            json.dumps(command_logs, ensure_ascii=False, indent=2), encoding="utf-8")
        raise RuntimeError(f"{Path(command[0]).name} failed: {completed.stderr or completed.stdout}")
    return completed.stdout + completed.stderr

version = run([tools["javac"], "-version"])
major = re.search(r"javac\s+(\d+)", version)
if not major or int(major.group(1)) < 17:
    raise SystemExit("This toolchain requires JDK 17 or later. Select it explicitly with --jdk.")

asset_manifest = json.loads((BASE / "assets-manifest.json").read_text(encoding="utf-8"))
asset_entries = {entry["file"]: entry for entry in asset_manifest["files"]}
actual_assets = {path.name for path in (BASE / "assets").iterdir() if path.is_file()}
if set(asset_entries) != EXPECTED_ASSETS or actual_assets != EXPECTED_ASSETS:
    raise RuntimeError("Only the four reviewed synthetic fixture assets may be packaged.")
for name, metadata in asset_entries.items():
    data = (BASE / "assets" / name).read_bytes()
    if len(data) != metadata["byteSize"] or hashlib.sha256(data).hexdigest().upper() != metadata["sha256"]:
        raise RuntimeError(f"Fixture asset verification failed: {name}")

android = "{http://schemas.android.com/apk/res/android}"
manifest = ET.parse(BASE / "AndroidManifest.xml").getroot()
if manifest.attrib.get("package") != PACKAGE or any("permission" in node.tag for node in manifest):
    raise RuntimeError("Unexpected package or permission declaration in QA manifest.")
providers = manifest.findall("application/provider")
if len(providers) != 1 or providers[0].attrib.get(android + "exported") != "false" \
        or providers[0].attrib.get(android + "grantUriPermissions") != "true":
    raise RuntimeError("The QA provider must remain private with normal URI grants enabled.")

run([tools["javac"], "-encoding", "UTF-8", "-source", "8", "-target", "8",
     "-classpath", ANDROID_JAR, "-d", CLASSES, *sorted((BASE / "src").rglob("*.java"))])
run([tools["d8"], "--lib", ANDROID_JAR, "--min-api", "26", "--output", DEX,
     *sorted(CLASSES.rglob("*.class"))])
unsigned = BUILD / "unsigned.apk"
run([tools["aapt2"], "link", "-o", unsigned, "-I", ANDROID_JAR,
     "--manifest", BASE / "AndroidManifest.xml", "-A", BASE / "assets"])
with zipfile.ZipFile(unsigned, "a", zipfile.ZIP_DEFLATED) as archive:
    for dex_file in DEX.glob("*.dex"):
        archive.write(dex_file, dex_file.name)
aligned = BUILD / "aligned.apk"
run([tools["zipalign"], "-f", "4", unsigned, aligned])

# This local disposable debug key is unrelated to production signing credentials.
key = ARTIFACT_ROOT / "qa-debug.keystore"
if not key.exists():
    run([tools["keytool"], "-genkeypair", "-keystore", key, "-storepass", "android",
         "-keypass", "android", "-alias", "qa-fixture", "-keyalg", "RSA", "-keysize", "2048",
         "-validity", "3650", "-dname", "CN=DDL Saver QA Fixture,O=Local test,C=CN"])
signed = BUILD / "clipboard-fixture.apk"
run([tools["apksigner"], "sign", "--ks", key, "--ks-key-alias", "qa-fixture",
     "--ks-pass", "pass:android", "--key-pass", "pass:android", "--out", signed, aligned])
signature = run([tools["apksigner"], "verify", "--verbose", "--print-certs", signed])
permissions = run([tools["aapt2"], "dump", "permissions", signed])
badging = run([tools["aapt2"], "dump", "badging", signed])
binary_manifest = run([tools["aapt2"], "dump", "xmltree", signed, "--file", "AndroidManifest.xml"])
if "uses-permission" in permissions or f"package: name='{PACKAGE}'" not in badging:
    raise RuntimeError("Unexpected package or requested permission in the compiled QA APK.")
if "minSdkVersion:'26'" not in badging or "targetSdkVersion:'35'" not in badging:
    raise RuntimeError("Unexpected SDK declarations in the compiled QA APK.")
if "Verified using v2 scheme (APK Signature Scheme v2): true" not in signature:
    raise RuntimeError("The compiled QA APK must pass v2 signature verification.")
with zipfile.ZipFile(signed) as archive:
    if "classes.dex" not in archive.namelist() or any(
        archive.read("assets/" + name) != (BASE / "assets" / name).read_bytes()
        for name in EXPECTED_ASSETS
    ):
        raise RuntimeError("Compiled APK contents do not match the reviewed fixtures.")
result = {
    "builtUtc": datetime.now(timezone.utc).isoformat(), "package": PACKAGE,
    "apk": str(signed), "byteSize": signed.stat().st_size,
    "sha256": hashlib.sha256(signed.read_bytes()).hexdigest().upper(),
    "minSdk": 26, "targetSdk": 35, "buildTools": args.build_tools,
    "requestedPermissions": [], "installedOnDevice": False, "deviceOperationsPerformed": False,
    "signatureVerification": signature, "packageInspection": badging,
    "permissionInspection": permissions, "binaryManifestInspection": binary_manifest,
    "commands": command_logs,
}
verification_file = BUILD / "build-verification.json"
verification_file.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps({key: result[key] for key in ["package", "apk", "byteSize", "sha256", "minSdk",
                 "targetSdk", "requestedPermissions", "installedOnDevice"]}, indent=2))
print("Verification: " + str(verification_file))
