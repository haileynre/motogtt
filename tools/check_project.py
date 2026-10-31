#!/usr/bin/env python3
"""Static packaging checks only. Does not substitute for Kotlin compilation or device tests."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"


def main():
    count = 0
    for path in ROOT.rglob("*.xml"):
        ET.parse(path)
        count += 1
    ids = []
    for module in ["phone", "wear"]:
        build = (ROOT / module / "build.gradle.kts").read_text()
        ids.append(re.search(r'applicationId = "([^"]+)"', build).group(1))
        manifest = ET.parse(ROOT / module / "src/main/AndroidManifest.xml").getroot()
        for element in manifest.findall("application/*"):
            if element.tag not in ["activity", "service"]:
                continue
            name = element.get(ANDROID + "name")
            namespace = re.search(r'namespace = "([^"]+)"', build).group(1)
            full = namespace + name if name.startswith(".") else name
            source = ROOT / module / "src/main/java" / (full.replace(".", "/") + ".kt")
            assert source.exists(), f"Missing component: {source}"
        assert (ROOT / module / "src/main/res/drawable/app_icon.xml").exists()
    assert ids[0] == ids[1] == "dev.wristflick.remote", "Data Layer IDs must match"
    service = ET.parse(ROOT / "phone/src/main/res/xml/accessibility_service.xml").getroot()
    assert service.get(ANDROID + "canPerformGestures") == "true"
    assert service.get(ANDROID + "canRetrieveWindowContent") == "true"
    watch = ET.parse(ROOT / "wear/src/main/AndroidManifest.xml").getroot()
    assert any(e.get(ANDROID + "name") == "com.google.android.wearable.standalone" and
               e.get(ANDROID + "value") == "false" for e in watch.findall("application/meta-data"))
    assert (ROOT / ".github/workflows/android.yml").exists()
    print(f"PASS: {count} XML files parse; components, app IDs, icon resources and service wiring checked.")
    print("Kotlin compilation, lint, unit tests, APK signatures and device behavior still require the build environment.")


if __name__ == "__main__":
    main()
