#!/usr/bin/env python3
from __future__ import annotations
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "upstream" / "app"

activity_src = ROOT / "overlay/app/src/main/java/com/baba/callvault/trackatest/TrackATestActivity.kt"
activity_dst = APP / "src/main/java/com/baba/callvault/trackatest/TrackATestActivity.kt"
activity_dst.parent.mkdir(parents=True, exist_ok=True)
activity_dst.write_text(activity_src.read_text(encoding="utf-8"), encoding="utf-8")

# DIAGNOSTIC-3 uses a package and provider authority that are different from both production
# CallVault and DIAGNOSTIC-2, so Android installs it as a genuinely separate application.
gradle_file = APP / "build.gradle.kts"
gradle_text = gradle_file.read_text(encoding="utf-8")
if 'applicationIdSuffix = ".instrtest"' in gradle_text:
    gradle_text = gradle_text.replace(
        'applicationIdSuffix = ".instrtest"',
        'applicationIdSuffix = ".trackadiag3"', 1)
elif 'applicationIdSuffix = ".trackadiag3"' not in gradle_text:
    raise SystemExit("Expected isolated applicationIdSuffix not found")
if 'manifestPlaceholders["recorderAuthority"] = "com.baba.callvault.instrtest.recorder"' in gradle_text:
    gradle_text = gradle_text.replace(
        'manifestPlaceholders["recorderAuthority"] = "com.baba.callvault.instrtest.recorder"',
        'manifestPlaceholders["recorderAuthority"] = "com.baba.callvault.trackadiag3.recorder"', 1)
elif 'manifestPlaceholders["recorderAuthority"] = "com.baba.callvault.trackadiag3.recorder"' not in gradle_text:
    raise SystemExit("Expected isolated recorder authority placeholder not found")
gradle_file.write_text(gradle_text, encoding="utf-8")

provider_file = APP / "src/main/java/com/baba/callvault/server/RecorderBinderProvider.kt"
provider_text = provider_file.read_text(encoding="utf-8")
for old in (
    'const val AUTHORITY = "com.baba.callvault.instrtest.recorder"',
    'const val AUTHORITY = "com.baba.callvault.recorder"',
):
    if old in provider_text:
        provider_text = provider_text.replace(
            old,
            'const val AUTHORITY = "com.baba.callvault.trackadiag3.recorder"', 1)
        break
if 'const val AUTHORITY = "com.baba.callvault.trackadiag3.recorder"' not in provider_text:
    raise SystemExit("RecorderBinderProvider AUTHORITY constant not found")
provider_file.write_text(provider_text, encoding="utf-8")

manifest = APP / "src/main/AndroidManifest.xml"
manifest_text = manifest.read_text(encoding="utf-8")
if 'android:label="@string/app_name"' in manifest_text:
    manifest_text = manifest_text.replace(
        'android:label="@string/app_name"',
        'android:label="CallVault Track A DIAGNOSTIC 3"', 1)
elif 'android:label="CallVault Track A DIAGNOSTIC 3"' not in manifest_text:
    raise SystemExit("Application label not found in manifest")

if ".trackatest.TrackATestActivity" not in manifest_text:
    activity_block = '''
        <!-- Experimental Track A probe UI. Present only in the isolated debug build. -->
        <activity
            android:name=".trackatest.TrackATestActivity"
            android:exported="true"
            android:label="CallVault Track A DIAGNOSTIC 3">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
'''
    marker = "\n    </application>"
    if marker not in manifest_text:
        raise SystemExit("Manifest closing application tag not found")
    manifest_text = manifest_text.replace(marker, activity_block + marker, 1)
manifest.write_text(manifest_text, encoding="utf-8")

print("Applied Track A DIAGNOSTIC-3 overlay.")
