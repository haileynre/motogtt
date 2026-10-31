#!/usr/bin/env python3
"""Run after assembling both APKs in an Android SDK environment."""
import os
from pathlib import Path
import re
import subprocess


def main():
    root = Path(__file__).resolve().parents[1]
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not sdk:
        raise SystemExit("Set ANDROID_HOME to the installed Android SDK directory.")
    tools = Path(sdk) / "build-tools/35.0.0"
    suffix = ".bat" if os.name == "nt" else ""
    apksigner = tools / ("apksigner" + suffix)
    aapt = tools / ("aapt.exe" if os.name == "nt" else "aapt")
    fingerprints = []
    for module in ["phone", "wear"]:
        apk = root / f"{module}/build/outputs/apk/debug/{module}-debug.apk"
        signed = subprocess.check_output([str(apksigner), "verify", "--print-certs", str(apk)], text=True)
        match = re.search(r"Signer #1 certificate SHA-256 digest: (\w+)", signed)
        if not match:
            raise SystemExit(f"No signing certificate found for {module}.")
        fingerprints.append(match.group(1))
        manifest = subprocess.check_output([str(aapt), "dump", "badging", str(apk)], text=True)
        if "package: name='dev.wristflick.remote'" not in manifest:
            raise SystemExit(f"Wrong application ID in {module}.")
        print(f"{module}: signed APK with correct application ID")
    if fingerprints[0] != fingerprints[1]:
        raise SystemExit("Phone and watch signing certificates do not match.")
    print("Phone and watch use the same signing certificate.")


if __name__ == "__main__":
    main()
