#!/usr/bin/env python3
"""Fetch the official wrapper JAR only, verifying Gradle's published SHA-256."""
import hashlib
from pathlib import Path
import urllib.request

URL = "https://raw.githubusercontent.com/gradle/gradle/v8.11.1/gradle/wrapper/gradle-wrapper.jar"
SHA256 = "2db75c40782f5e8ba1fc278a5574bab070adccb2d21ca5a6e5ed840888448046"


def main():
    root = Path(__file__).resolve().parents[1]
    destination = root / "gradle/wrapper/gradle-wrapper.jar"
    if destination.exists() and hashlib.sha256(destination.read_bytes()).hexdigest() == SHA256:
        print("Verified Gradle Wrapper already present.")
        return
    with urllib.request.urlopen(URL, timeout=45) as response:
        data = response.read(200_000)
    if hashlib.sha256(data).hexdigest() != SHA256:
        raise SystemExit("Checksum mismatch. No wrapper JAR was written.")
    destination.parent.mkdir(parents=True, exist_ok=True)
    destination.write_bytes(data)
    (root / "gradlew").chmod(0o755)
    print("Official Gradle Wrapper downloaded and checksum verified.")


if __name__ == "__main__":
    main()
