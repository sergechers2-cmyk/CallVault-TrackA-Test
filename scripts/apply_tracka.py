#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "upstream" / "app"

activity_src = ROOT / "overlay/app/src/main/java/com/baba/callvault/trackatest/TrackATestActivity.kt"
activity_dst = APP / "src/main/java/com/baba/callvault/trackatest/TrackATestActivity.kt"
activity_dst.parent.mkdir(parents=True, exist_ok=True)
activity_dst.write_text(activity_src.read_text(encoding="utf-8"), encoding="utf-8")

# v2.2.0 already provides isolated applicationId/provider-authority handling through
# -PisolateTestApp and ${recorderAuthority}. We deliberately leave those upstream files alone.
manifest = APP / "src/main/AndroidManifest.xml"
text = manifest.read_text(encoding="utf-8")

if ".trackatest.TrackATestActivity" not in text:
    activity_block = '''\n        <!-- Experimental Track A probe UI. Present only in the isolated debug build. -->\n        <activity\n            android:name=".trackatest.TrackATestActivity"\n            android:exported="true"\n            android:label="Track A Test">\n            <intent-filter>\n                <action android:name="android.intent.action.MAIN" />\n                <category android:name="android.intent.category.LAUNCHER" />\n            </intent-filter>\n        </activity>\n'''
    marker = "\n    </application>"
    if marker not in text:
        raise SystemExit("Manifest closing application tag not found")
    text = text.replace(marker, activity_block + marker, 1)
    manifest.write_text(text, encoding="utf-8")

print("Applied Track A test overlay successfully (activity + manifest entry only).")
