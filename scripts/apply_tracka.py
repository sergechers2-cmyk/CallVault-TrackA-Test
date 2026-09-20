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
# -PisolateTestApp and ${recorderAuthority}. We keep the isolated provider authority, but the
# daemon also reads RecorderBinderProvider.AUTHORITY from the APK at runtime. Upstream leaves
# that constant hardcoded to the production authority, so an isolated daemon would deliver
# its Binder to the release installation. For this diagnostic build daemon + isolated app
# must use the same isolated authority.
provider_file = APP / "src/main/java/com/baba/callvault/server/RecorderBinderProvider.kt"
provider_text = provider_file.read_text(encoding="utf-8")
production_authority = 'const val AUTHORITY = "com.baba.callvault.recorder"'
test_authority = 'const val AUTHORITY = "com.baba.callvault.instrtest.recorder"'
if production_authority in provider_text:
    provider_text = provider_text.replace(production_authority, test_authority, 1)
elif test_authority not in provider_text:
    raise SystemExit("RecorderBinderProvider AUTHORITY constant not found")
provider_file.write_text(provider_text, encoding="utf-8")

manifest = APP / "src/main/AndroidManifest.xml"
text = manifest.read_text(encoding="utf-8")

if ".trackatest.TrackATestActivity" not in text:
    activity_block = '''\n        <!-- Experimental Track A probe UI. Present only in the isolated debug build. -->\n        <activity\n            android:name=".trackatest.TrackATestActivity"\n            android:exported="true"\n            android:label="Track A Test">\n            <intent-filter>\n                <action android:name="android.intent.action.MAIN" />\n                <category android:name="android.intent.category.LAUNCHER" />\n            </intent-filter>\n        </activity>\n'''
    marker = "\n    </application>"
    if marker not in text:
        raise SystemExit("Manifest closing application tag not found")
    text = text.replace(marker, activity_block + marker, 1)
    manifest.write_text(text, encoding="utf-8")

print("Applied Track A diagnostic overlay successfully (activity + manifest + isolated daemon authority).")
