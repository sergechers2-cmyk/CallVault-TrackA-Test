#!/usr/bin/env python3
from __future__ import annotations
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "upstream" / "app"

activity_src = ROOT / "overlay/app/src/main/java/com/baba/callvault/trackatest/TrackATestActivity.kt"
activity_dst = APP / "src/main/java/com/baba/callvault/trackatest/TrackATestActivity.kt"
activity_dst.parent.mkdir(parents=True, exist_ok=True)
activity_dst.write_text(activity_src.read_text(encoding="utf-8"), encoding="utf-8")

# DIAGNOSTIC-4 intentionally builds the normal CallVault package. It does NOT start a second
# RecorderServer. The diagnostic Activity runs inside the same app process as RecorderConnection,
# so it can probe the already-connected production daemon without touching Wireless debugging.
gradle_file = APP / "build.gradle.kts"
gradle_text = gradle_file.read_text(encoding="utf-8")
gradle_text = gradle_text.replace(
    'applicationIdSuffix = ".trackadiag3"',
    '# DIAGNOSTIC-4 uses the normal applicationId intentionally; no isolated suffix.', 1)
gradle_text = gradle_text.replace(
    'manifestPlaceholders["recorderAuthority"] = "com.baba.callvault.trackadiag3.recorder"',
    'manifestPlaceholders["recorderAuthority"] = "com.baba.callvault.recorder"', 1)
gradle_file.write_text(gradle_text, encoding="utf-8")

provider_file = APP / "src/main/java/com/baba/callvault/server/RecorderBinderProvider.kt"
provider_text = provider_file.read_text(encoding="utf-8")
provider_text = provider_text.replace(
    'const val AUTHORITY = "com.baba.callvault.trackadiag3.recorder"',
    'const val AUTHORITY = "com.baba.callvault.recorder"', 1)
provider_file.write_text(provider_text, encoding="utf-8")

manifest = APP / "src/main/AndroidManifest.xml"
manifest_text = manifest.read_text(encoding="utf-8")
manifest_text = manifest_text.replace(
    'android:label="CallVault Track A DIAGNOSTIC 3"',
    'android:label="@string/app_name"', 1)
manifest_text = manifest_text.replace(
    'android:label="CallVault Track A DIAGNOSTIC 3">',
    'android:label="Track A DIAGNOSTIC 4">',
    1)
manifest.write_text(manifest_text, encoding="utf-8")

print("Applied Track A DIAGNOSTIC-4 main-process overlay.")
