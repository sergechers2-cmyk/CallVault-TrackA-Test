#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "upstream" / "app"

activity_src = ROOT / "overlay/app/src/main/java/com/baba/callvault/trackatest/TrackATestActivity.kt"
activity_dst = APP / "src/main/java/com/baba/callvault/trackatest/TrackATestActivity.kt"
activity_dst.parent.mkdir(parents=True, exist_ok=True)
activity_dst.write_text(activity_src.read_text(encoding="utf-8"), encoding="utf-8")

manifest = APP / "src/main/AndroidManifest.xml"
text = manifest.read_text(encoding="utf-8")
activity_block = '''\n        <!-- Experimental Track A probe UI. Present only in the isolated debug build produced by this patch layer. -->\n        <activity\n            android:name=".trackatest.TrackATestActivity"\n            android:exported="true"\n            android:label="Track A Test"\n            android:theme="@style/Theme.CallVault">\n            <intent-filter>\n                <action android:name="android.intent.action.MAIN" />\n                <category android:name="android.intent.category.LAUNCHER" />\n            </intent-filter>\n        </activity>\n'''
if ".trackatest.TrackATestActivity" not in text:
    pattern = r'(?m)^        <activity\n            android:name="\\.MainActivity"'
    if not re.search(pattern, text):
        raise SystemExit("Manifest anchor not found")
    replacement = activity_block + '\n        <activity\n            android:name=".MainActivity"'
    text = re.sub(pattern, replacement, text, count=1)
manifest.write_text(text, encoding="utf-8")

provider = APP / "src/main/java/com/baba/callvault/server/RecorderBinderProvider.kt"
ptext = provider.read_text(encoding="utf-8")
if "import com.baba.callvault.BuildConfig" not in ptext:
    needle = "import com.baba.callvault.services.recording.handoff.TrackAProbe\n"
    if needle not in ptext:
        raise SystemExit("Provider import anchor not found")
    ptext = ptext.replace(needle, needle + "import com.baba.callvault.BuildConfig\n", 1)
ptext = ptext.replace(
    'const val AUTHORITY = "com.baba.callvault.recorder"',
    'const val AUTHORITY = BuildConfig.RECORDER_AUTHORITY',
    1,
)
provider.write_text(ptext, encoding="utf-8")

gradle = ROOT / "upstream" / "app" / "build.gradle.kts"
gtext = gradle.read_text(encoding="utf-8")
if "RECORDER_AUTHORITY" not in gtext:
    marker = 'val isolateTestApp = providers.gradleProperty("isolateTestApp").isPresent\n'
    if marker not in gtext:
        raise SystemExit("Gradle isolateTestApp anchor not found")
    insertion = marker + 'val recorderAuthority = if (isolateTestApp) "com.baba.callvault.instrtest.recorder" else "com.baba.callvault.recorder"\n'
    gtext = gtext.replace(marker, insertion, 1)
    lines = gtext.splitlines(keepends=True)
    inserted = False
    out = []
    for line in lines:
        out.append(line)
        if 'buildConfigField("String", "CI_BUILD_NUMBER"' in line:
            out.append('        buildConfigField("String", "RECORDER_AUTHORITY", "\\"$recorderAuthority\\"")\n')
            inserted = True
    if not inserted:
        raise SystemExit("Gradle CI_BUILD_NUMBER anchor not found")
    gtext = ''.join(out)
gradle.write_text(gtext, encoding="utf-8")

print("Applied Track A test overlay successfully.")
